package info.ragozin.util.socketstifler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.MBeanServer;
import javax.management.MXBean;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class LatencyProxy {
    private static final Log LOG = LogFactory.getLog(LatencyProxy.class);

    private final int packetSize = 16 << 10;
    private final AtomicLong connectionCount = new AtomicLong();
    private final AtomicLong client2server = new AtomicLong();
    private final AtomicLong server2client = new AtomicLong();
    private final Map<Socket, AtomicInteger> closeCounter = new ConcurrentHashMap<>();
    private volatile long latency = 0;

    public static LatencyProxy start(SocketAddress accept, SocketAddress forward) throws Exception {
        LatencyProxy proxy = new LatencyProxy();
        int inPort = ((InetSocketAddress) accept).getPort();
        int outPort = ((InetSocketAddress) forward).getPort();
        ObjectName name = ObjectName.getInstance("info.ragozin:type=SocketPoxy,src=" + inPort + ",fwd=" + outPort);

        ServerSocket sock = new ServerSocket();
        sock.bind(accept);

        AcceptThread acc = proxy.new AcceptThread(sock, forward);
        acc.start();

        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            platformMBeanServer.unregisterMBean(name);
        } catch (Exception ignored) {
        }

        platformMBeanServer.registerMBean(proxy.getMBean(), name);

        return proxy;
    }

    private ProxyMBean getMBean() {
        return new ProxyMBean() {

            @Override
            public long getClient2Server() {
                return client2server.get();
            }

            @Override
            public long getServer2Client() {
                return server2client.get();
            }

            @Override
            public long getTotalConnectionCount() {
                return connectionCount.get();
            }

            @Override
            public long getLatencyMicros() {
                return TimeUnit.NANOSECONDS.toMicros(latency);
            }

            @Override
            public void setLatencyMicros(long latencyMicrocs) {
                latency = TimeUnit.MICROSECONDS.toNanos(latencyMicrocs);
            }
        };
    }


    @MXBean
    public interface ProxyMBean {

        long getClient2Server();

        long getServer2Client();

        long getTotalConnectionCount();

        long getLatencyMicros();

        void setLatencyMicros(long latencyMicrocs);
    }

    private class AcceptThread extends Thread {

        private final ServerSocket socket;
        private final SocketAddress forward;

        public AcceptThread(ServerSocket socket, SocketAddress forward) {
            this.socket = socket;
            this.forward = forward;
            this.setDaemon(true);
            this.setName("ACCEPT[" + socket.getLocalPort() + "]");
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Socket clientSide = socket.accept();
                    try {
                        Socket serverSide = new Socket();
                        serverSide.connect(forward);

                        closeCounter.put(clientSide, new AtomicInteger(2));
                        closeCounter.put(serverSide, new AtomicInteger(2));

                        ForwardThread a = new ForwardThread(clientSide, serverSide, client2server);
                        ForwardThread b = new ForwardThread(serverSide, clientSide, server2client);
                        a.start();
                        b.start();
                        connectionCount.incrementAndGet();
                    } catch (IOException e) {
                        LOG.error("Accept failed", e);
                    }
                }
            } catch (IOException e) {
                LOG.error("Accept failed", e);
            }
        }
    }

    private class ForwardThread extends Thread {

        private final Socket input;
        private final Socket output;
        private final AtomicLong byteCounter;

        public ForwardThread(Socket input, Socket output, AtomicLong byteCounter) {
            super();
            this.input = input;
            this.output = output;
            this.byteCounter = byteCounter;
            this.setDaemon(true);
            this.setName("FWD[" + input.getRemoteSocketAddress() + "->" + output.getRemoteSocketAddress() + "]");
        }

        @Override
        public void run() {
            try (InputStream inputStream = input.getInputStream();
                 OutputStream outputStream = output.getOutputStream()) {
                byte[] buf = new byte[packetSize];

                int n;
                while ((n = inputStream.read(buf)) > 0) {
                    if (latency > 0) {
                        LockSupport.parkNanos(latency);
                    }
                    byteCounter.addAndGet(n);
                    outputStream.write(buf, 0, n);
                }
            } catch (IOException e) {
                LOG.error("Forwarding exception", e);
            } finally {
                silenceClose(input);
                silenceClose(output);
            }
        }

        private void silenceClose(Socket socket) {
            if (socket == null || socket.isClosed()) {
                return;
            }
            AtomicInteger counter = closeCounter.getOrDefault(socket, new AtomicInteger(1));
            if (counter.decrementAndGet() > 0) {
                return;
            }
            closeCounter.remove(socket);
            try {
                socket.close();
            } catch (IOException e) {
                LOG.warn(String.format("Silence close exception: %s", socket), e);
            }
        }
    }
}
