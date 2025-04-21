package info.ragozin.util.socketstifler.latency;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;

public class LatencyProxyStarter {

    public static void start(SocketAddress accept, SocketAddress forward) throws Exception {
        LatencyProxy proxy = new LatencyProxy();
        int inPort = ((InetSocketAddress) accept).getPort();
        int outPort = ((InetSocketAddress) forward).getPort();
        ObjectName name = ObjectName.getInstance("info.ragozin:type=SocketPoxy,src=" + inPort + ",fwd=" + outPort);

        ServerSocket sock = new ServerSocket();
        sock.bind(accept);

        proxy.startAccepting(sock, forward);

        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            platformMBeanServer.unregisterMBean(name);
        } catch (Exception ignored) {
        }

        platformMBeanServer.registerMBean(proxy.getMBean(), name);
    }
}
