package info.ragozin.util.socketstifler.latency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

class LatencyProxy {
    private static final Log LOG = LogFactory.getLog(LatencyProxy.class);

    private final ReentrantLock socketCloseLock = new ReentrantLock();
    private final ProxyStats proxyStats = new ProxyStats();
    private final ProxyProperties proxyProperties = new ProxyProperties();

    public void startAccepting(ServerSocket sock, SocketAddress forward) {
        AcceptThread acc = new AcceptThread(sock, forward);
        acc.start();
    }

    public ProxyMBean getMBean() {
        return new ProxyMBeanImpl(proxyProperties, proxyStats);
    }

    private class AcceptThread extends Thread {
        private final ServerSocket socket;
        private final SocketAddress forward;

        public AcceptThread(
                ServerSocket socket,
                SocketAddress forward
        ) {
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
                    LOG.info("Accepted connection: " + clientSide);
                    try {
                        Socket serverSide = new Socket();
                        serverSide.connect(forward);
                        LOG.info("Create forwarding connection: " + serverSide);
                        ForwardThread a = new ForwardThread(clientSide, serverSide, true);
                        ForwardThread b = new ForwardThread(serverSide, clientSide, false);
                        a.start();
                        b.start();
                        proxyStats.addConnection();
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
        private static final int PACKET_SIZE = 8 << 10;

        private final Socket input;
        private final Socket output;
        private final boolean client2server;

        public ForwardThread(Socket input, Socket output, boolean client2server) {
            super();
            this.input = input;
            this.output = output;
            this.client2server = client2server;
            this.setDaemon(true);
            this.setName("FWD[" + input.getRemoteSocketAddress() + "->" + output.getRemoteSocketAddress() + "]");
        }

        @Override
        public void run() {
            try (InputStream inputStream = input.getInputStream();
                 OutputStream outputStream = output.getOutputStream()) {
                byte[] buffer = new byte[PACKET_SIZE];

                int n;
                while ((n = inputStream.read(buffer)) >= 0) {
                    long latency = proxyProperties.getLatency();
                    if (latency > 0) {
                        LockSupport.parkNanos(latency);
                    }
                    if (n == 0) {
                        continue;
                    }
                    proxyStats.addBytes(client2server, n);
                    outputStream.write(buffer, 0, n);
                    outputStream.flush();
                }
                LOG.info(String.format("Read %s successfully finished", input));
            } catch (IOException e) {
                LOG.error("Forwarding exception", e);
            } finally {
                silenceInputClose(input);
                silenceOutputClose(output);
            }
        }

        private void silenceInputClose(Socket socket) {
            socketCloseLock.lock();
            try {
                if (socket == null) {
                    return;
                }
                try {
                    LOG.info(String.format("Socket (%s) close input", socket));
                    socket.shutdownInput();
                } catch (IOException e) {
                    LOG.warn(String.format("Socket (%s) shutdown input failed", socket), e);
                }
                if (!socket.isOutputShutdown()) {
                    return;
                }
            } finally {
                socketCloseLock.unlock();
            }
            silenceClose(socket);
        }

        private void silenceOutputClose(Socket socket) {
            socketCloseLock.lock();
            try {
                if (socket == null || socket.isClosed()) {
                    LOG.warn(String.format("Socket (%s) is null or closed", socket));
                    return;
                }
                try {
                    LOG.info(String.format("Socket (%s) close output", socket));
                    socket.shutdownOutput();
                } catch (IOException e) {
                    LOG.warn(String.format("Socket (%s) shutdown output failed", socket), e);
                }
                if (!socket.isInputShutdown()) {
                    return;
                }
            } finally {
                socketCloseLock.unlock();
            }
            silenceClose(socket);
        }

        private void silenceClose(Socket socket) {
            LOG.info(String.format("Socket (%s) close", socket));
            try {
                socket.close();
            } catch (IOException e) {
                LOG.warn(String.format("Silence close exception: %s", socket), e);
            }
        }
    }
}
