package info.ragozin.util.socketstifler.latency;

import java.util.concurrent.TimeUnit;

/**
 * @author sibmaks
 */
public class ProxyMBeanImpl implements ProxyMBean {
    private final ProxyProperties proxyProperties;
    private final ProxyStats proxyStats;

    public ProxyMBeanImpl(ProxyProperties proxyProperties, ProxyStats proxyStats) {
        this.proxyProperties = proxyProperties;
        this.proxyStats = proxyStats;
    }

    @Override
    public long getClient2Server() {
        return proxyStats.getClient2Server();
    }

    @Override
    public long getServer2Client() {
        return proxyStats.getServer2Client();
    }

    @Override
    public long getTotalConnectionCount() {
        return proxyStats.getConnectionCount();
    }

    @Override
    public long getLatencyMicros() {
        return TimeUnit.NANOSECONDS.toMicros(proxyProperties.getLatency());
    }

    @Override
    public void setLatencyMicros(long latencyMicrocs) {
        proxyProperties.setLatency(TimeUnit.MICROSECONDS.toNanos(latencyMicrocs));
    }
}
