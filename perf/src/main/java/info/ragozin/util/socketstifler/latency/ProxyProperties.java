package info.ragozin.util.socketstifler.latency;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author sibmaks
 */
public class ProxyProperties {
    private final AtomicLong latency = new AtomicLong();

    public long getLatency() {
        return latency.get();
    }

    public void setLatency(long latency) {
        this.latency.set(latency);
    }
}
