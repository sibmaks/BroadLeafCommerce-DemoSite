package info.ragozin.util.socketstifler.latency;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author sibmaks
 */
public class ProxyStats {
    private final AtomicLong connectionCount = new AtomicLong();
    private final AtomicLong client2server = new AtomicLong();
    private final AtomicLong server2client = new AtomicLong();

    public long getClient2Server() {
        return client2server.get();
    }

    public long getServer2Client() {
        return server2client.get();
    }

    public long getConnectionCount() {
        return connectionCount.get();
    }

    public void addConnection() {
        connectionCount.incrementAndGet();
    }

    public void addBytes(boolean isClient2server, int n) {
        if (isClient2server) {
            client2server.addAndGet(n);
        } else {
            server2client.addAndGet(n);
        }
    }
}
