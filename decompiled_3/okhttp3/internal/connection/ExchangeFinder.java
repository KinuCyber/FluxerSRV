/*
 * Decompiled with CFR 0.152.
 */
package okhttp3.internal.connection;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import okhttp3.Address;
import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Route;
import okhttp3.internal.Util;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.connection.RealConnectionPool;
import okhttp3.internal.connection.RouteException;
import okhttp3.internal.connection.RouteSelector;
import okhttp3.internal.connection.Transmitter;
import okhttp3.internal.http.ExchangeCodec;

final class ExchangeFinder {
    private final Transmitter transmitter;
    private final Address address;
    private final RealConnectionPool connectionPool;
    private final Call call;
    private final EventListener eventListener;
    private RouteSelector.Selection routeSelection;
    private final RouteSelector routeSelector;
    private RealConnection connectingConnection;
    private boolean hasStreamFailure;
    private Route nextRouteToTry;

    ExchangeFinder(Transmitter transmitter, RealConnectionPool connectionPool, Address address, Call call, EventListener eventListener) {
        this.transmitter = transmitter;
        this.connectionPool = connectionPool;
        this.address = address;
        this.call = call;
        this.eventListener = eventListener;
        this.routeSelector = new RouteSelector(address, connectionPool.routeDatabase, call, eventListener);
    }

    public ExchangeCodec find(OkHttpClient client, Interceptor.Chain chain, boolean doExtensiveHealthChecks) {
        int connectTimeout = chain.connectTimeoutMillis();
        int readTimeout = chain.readTimeoutMillis();
        int writeTimeout = chain.writeTimeoutMillis();
        int pingIntervalMillis = client.pingIntervalMillis();
        boolean connectionRetryEnabled = client.retryOnConnectionFailure();
        try {
            RealConnection resultConnection = this.findHealthyConnection(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis, connectionRetryEnabled, doExtensiveHealthChecks);
            return resultConnection.newCodec(client, chain);
        }
        catch (RouteException e) {
            this.trackFailure();
            throw e;
        }
        catch (IOException e) {
            this.trackFailure();
            throw new RouteException(e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RealConnection findHealthyConnection(int connectTimeout, int readTimeout, int writeTimeout, int pingIntervalMillis, boolean connectionRetryEnabled, boolean doExtensiveHealthChecks) throws IOException {
        RealConnection candidate;
        while (true) {
            candidate = this.findConnection(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis, connectionRetryEnabled);
            RealConnectionPool realConnectionPool = this.connectionPool;
            synchronized (realConnectionPool) {
                if (candidate.successCount == 0 && !candidate.isMultiplexed()) {
                    return candidate;
                }
            }
            if (candidate.isHealthy(doExtensiveHealthChecks)) break;
            candidate.noNewExchanges();
        }
        return candidate;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private RealConnection findConnection(int connectTimeout, int readTimeout, int writeTimeout, int pingIntervalMillis, boolean connectionRetryEnabled) throws IOException {
        Socket toClose;
        RealConnection releasedConnection;
        boolean foundPooledConnection = false;
        RealConnection result = null;
        Route selectedRoute = null;
        RealConnectionPool realConnectionPool = this.connectionPool;
        synchronized (realConnectionPool) {
            if (this.transmitter.isCanceled()) {
                throw new IOException("Canceled");
            }
            this.hasStreamFailure = false;
            releasedConnection = this.transmitter.connection;
            Socket socket = toClose = this.transmitter.connection != null && this.transmitter.connection.noNewExchanges ? this.transmitter.releaseConnectionNoEvents() : null;
            if (this.transmitter.connection != null) {
                result = this.transmitter.connection;
                releasedConnection = null;
            }
            if (result == null) {
                if (this.connectionPool.transmitterAcquirePooledConnection(this.address, this.transmitter, null, false)) {
                    foundPooledConnection = true;
                    result = this.transmitter.connection;
                } else if (this.nextRouteToTry != null) {
                    selectedRoute = this.nextRouteToTry;
                    this.nextRouteToTry = null;
                } else if (this.retryCurrentRoute()) {
                    selectedRoute = this.transmitter.connection.route();
                }
            }
        }
        Util.closeQuietly(toClose);
        if (releasedConnection != null) {
            this.eventListener.connectionReleased(this.call, releasedConnection);
        }
        if (foundPooledConnection) {
            this.eventListener.connectionAcquired(this.call, result);
        }
        if (result != null) {
            return result;
        }
        boolean newRouteSelection = false;
        if (!(selectedRoute != null || this.routeSelection != null && this.routeSelection.hasNext())) {
            newRouteSelection = true;
            this.routeSelection = this.routeSelector.next();
        }
        List<Route> routes = null;
        RealConnectionPool realConnectionPool2 = this.connectionPool;
        synchronized (realConnectionPool2) {
            if (this.transmitter.isCanceled()) {
                throw new IOException("Canceled");
            }
            if (newRouteSelection && this.connectionPool.transmitterAcquirePooledConnection(this.address, this.transmitter, routes = this.routeSelection.getAll(), false)) {
                foundPooledConnection = true;
                result = this.transmitter.connection;
            }
            if (!foundPooledConnection) {
                if (selectedRoute == null) {
                    selectedRoute = this.routeSelection.next();
                }
                this.connectingConnection = result = new RealConnection(this.connectionPool, selectedRoute);
            }
        }
        if (foundPooledConnection) {
            this.eventListener.connectionAcquired(this.call, result);
            return result;
        }
        result.connect(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis, connectionRetryEnabled, this.call, this.eventListener);
        this.connectionPool.routeDatabase.connected(result.route());
        Socket socket = null;
        RealConnectionPool realConnectionPool3 = this.connectionPool;
        synchronized (realConnectionPool3) {
            this.connectingConnection = null;
            if (this.connectionPool.transmitterAcquirePooledConnection(this.address, this.transmitter, routes, true)) {
                result.noNewExchanges = true;
                socket = result.socket();
                result = this.transmitter.connection;
                this.nextRouteToTry = selectedRoute;
            } else {
                this.connectionPool.put(result);
                this.transmitter.acquireConnectionNoEvents(result);
            }
        }
        Util.closeQuietly(socket);
        this.eventListener.connectionAcquired(this.call, result);
        return result;
    }

    RealConnection connectingConnection() {
        assert (Thread.holdsLock(this.connectionPool));
        return this.connectingConnection;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void trackFailure() {
        assert (!Thread.holdsLock(this.connectionPool));
        RealConnectionPool realConnectionPool = this.connectionPool;
        synchronized (realConnectionPool) {
            this.hasStreamFailure = true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    boolean hasStreamFailure() {
        RealConnectionPool realConnectionPool = this.connectionPool;
        synchronized (realConnectionPool) {
            return this.hasStreamFailure;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    boolean hasRouteToTry() {
        RealConnectionPool realConnectionPool = this.connectionPool;
        synchronized (realConnectionPool) {
            if (this.nextRouteToTry != null) {
                return true;
            }
            if (this.retryCurrentRoute()) {
                this.nextRouteToTry = this.transmitter.connection.route();
                return true;
            }
            return this.routeSelection != null && this.routeSelection.hasNext() || this.routeSelector.hasNext();
        }
    }

    private boolean retryCurrentRoute() {
        return this.transmitter.connection != null && this.transmitter.connection.routeFailureCount == 0 && Util.sameConnection(this.transmitter.connection.route().address().url(), this.address.url());
    }
}

