/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package okhttp3.internal.connection;

import java.io.IOException;
import java.lang.ref.Reference;
import java.net.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okhttp3.Address;
import okhttp3.Route;
import okhttp3.internal.Util;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.connection.RouteDatabase;
import okhttp3.internal.connection.Transmitter;
import okhttp3.internal.platform.Platform;

public final class RealConnectionPool {
    private static final Executor executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp ConnectionPool", true));
    private final int maxIdleConnections;
    private final long keepAliveDurationNs;
    private final Runnable cleanupRunnable = () -> {
        long waitNanos;
        while ((waitNanos = this.cleanup(System.nanoTime())) != -1L) {
            if (waitNanos <= 0L) continue;
            long waitMillis = waitNanos / 1000000L;
            waitNanos -= waitMillis * 1000000L;
            RealConnectionPool realConnectionPool = this;
            synchronized (realConnectionPool) {
                try {
                    this.wait(waitMillis, (int)waitNanos);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
            }
        }
        return;
    };
    private final Deque<RealConnection> connections = new ArrayDeque<RealConnection>();
    final RouteDatabase routeDatabase = new RouteDatabase();
    boolean cleanupRunning;

    public RealConnectionPool(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
        this.maxIdleConnections = maxIdleConnections;
        this.keepAliveDurationNs = timeUnit.toNanos(keepAliveDuration);
        if (keepAliveDuration <= 0L) {
            throw new IllegalArgumentException("keepAliveDuration <= 0: " + keepAliveDuration);
        }
    }

    public synchronized int idleConnectionCount() {
        int total = 0;
        for (RealConnection connection : this.connections) {
            if (!connection.transmitters.isEmpty()) continue;
            ++total;
        }
        return total;
    }

    public synchronized int connectionCount() {
        return this.connections.size();
    }

    boolean transmitterAcquirePooledConnection(Address address, Transmitter transmitter, @Nullable List<Route> routes, boolean requireMultiplexed) {
        assert (Thread.holdsLock(this));
        for (RealConnection connection : this.connections) {
            if (requireMultiplexed && !connection.isMultiplexed() || !connection.isEligible(address, routes)) continue;
            transmitter.acquireConnectionNoEvents(connection);
            return true;
        }
        return false;
    }

    void put(RealConnection connection) {
        assert (Thread.holdsLock(this));
        if (!this.cleanupRunning) {
            this.cleanupRunning = true;
            executor.execute(this.cleanupRunnable);
        }
        this.connections.add(connection);
    }

    boolean connectionBecameIdle(RealConnection connection) {
        assert (Thread.holdsLock(this));
        if (connection.noNewExchanges || this.maxIdleConnections == 0) {
            this.connections.remove(connection);
            return true;
        }
        this.notifyAll();
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void evictAll() {
        ArrayList<RealConnection> evictedConnections = new ArrayList<RealConnection>();
        RealConnectionPool realConnectionPool = this;
        synchronized (realConnectionPool) {
            Iterator<RealConnection> i = this.connections.iterator();
            while (i.hasNext()) {
                RealConnection connection = i.next();
                if (!connection.transmitters.isEmpty()) continue;
                connection.noNewExchanges = true;
                evictedConnections.add(connection);
                i.remove();
            }
        }
        for (RealConnection connection : evictedConnections) {
            Util.closeQuietly(connection.socket());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    long cleanup(long now) {
        int inUseConnectionCount = 0;
        int idleConnectionCount = 0;
        RealConnection longestIdleConnection = null;
        long longestIdleDurationNs = Long.MIN_VALUE;
        RealConnectionPool realConnectionPool = this;
        synchronized (realConnectionPool) {
            for (RealConnection connection : this.connections) {
                if (this.pruneAndGetAllocationCount(connection, now) > 0) {
                    ++inUseConnectionCount;
                    continue;
                }
                ++idleConnectionCount;
                long idleDurationNs = now - connection.idleAtNanos;
                if (idleDurationNs <= longestIdleDurationNs) continue;
                longestIdleDurationNs = idleDurationNs;
                longestIdleConnection = connection;
            }
            if (longestIdleDurationNs < this.keepAliveDurationNs && idleConnectionCount <= this.maxIdleConnections) {
                if (idleConnectionCount > 0) {
                    return this.keepAliveDurationNs - longestIdleDurationNs;
                }
                if (inUseConnectionCount > 0) {
                    return this.keepAliveDurationNs;
                }
                this.cleanupRunning = false;
                return -1L;
            }
            this.connections.remove(longestIdleConnection);
        }
        Util.closeQuietly(longestIdleConnection.socket());
        return 0L;
    }

    private int pruneAndGetAllocationCount(RealConnection connection, long now) {
        List<Reference<Transmitter>> references = connection.transmitters;
        int i = 0;
        while (i < references.size()) {
            Reference<Transmitter> reference = references.get(i);
            if (reference.get() != null) {
                ++i;
                continue;
            }
            Transmitter.TransmitterReference transmitterRef = (Transmitter.TransmitterReference)reference;
            String message = "A connection to " + connection.route().address().url() + " was leaked. Did you forget to close a response body?";
            Platform.get().logCloseableLeak(message, transmitterRef.callStackTrace);
            references.remove(i);
            connection.noNewExchanges = true;
            if (!references.isEmpty()) continue;
            connection.idleAtNanos = now - this.keepAliveDurationNs;
            return 0;
        }
        return references.size();
    }

    public void connectFailed(Route failedRoute, IOException failure) {
        if (failedRoute.proxy().type() != Proxy.Type.DIRECT) {
            Address address = failedRoute.address();
            address.proxySelector().connectFailed(address.url().uri(), failedRoute.proxy().address(), failure);
        }
        this.routeDatabase.failed(failedRoute);
    }
}

