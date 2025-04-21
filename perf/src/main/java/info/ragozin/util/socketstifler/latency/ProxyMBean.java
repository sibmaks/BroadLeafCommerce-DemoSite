package info.ragozin.util.socketstifler.latency;

import javax.management.MXBean;

@MXBean
public interface ProxyMBean {

    long getClient2Server();

    long getServer2Client();

    long getTotalConnectionCount();

    long getLatencyMicros();

    void setLatencyMicros(long latencyMicrocs);
}