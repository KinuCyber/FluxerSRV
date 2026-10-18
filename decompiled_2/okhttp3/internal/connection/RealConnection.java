/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package okhttp3.internal.connection;

import java.io.IOException;
import java.lang.ref.Reference;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownServiceException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import okhttp3.Address;
import okhttp3.Call;
import okhttp3.CertificatePinner;
import okhttp3.Connection;
import okhttp3.ConnectionSpec;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.internal.Internal;
import okhttp3.internal.Util;
import okhttp3.internal.Version;
import okhttp3.internal.connection.ConnectionSpecSelector;
import okhttp3.internal.connection.Exchange;
import okhttp3.internal.connection.RealConnectionPool;
import okhttp3.internal.connection.RouteException;
import okhttp3.internal.connection.Transmitter;
import okhttp3.internal.http.ExchangeCodec;
import okhttp3.internal.http1.Http1ExchangeCodec;
import okhttp3.internal.http2.ConnectionShutdownException;
import okhttp3.internal.http2.ErrorCode;
import okhttp3.internal.http2.Http2Connection;
import okhttp3.internal.http2.Http2ExchangeCodec;
import okhttp3.internal.http2.Http2Stream;
import okhttp3.internal.http2.StreamResetException;
import okhttp3.internal.platform.Platform;
import okhttp3.internal.tls.OkHostnameVerifier;
import okhttp3.internal.ws.RealWebSocket;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public final class RealConnection
extends Http2Connection.Listener
implements Connection {
    private static final String NPE_THROW_WITH_NULL = "throw with null exception";
    private static final int MAX_TUNNEL_ATTEMPTS = 21;
    public final RealConnectionPool connectionPool;
    private final Route route;
    private Socket rawSocket;
    private Socket socket;
    private Handshake handshake;
    private Protocol protocol;
    private Http2Connection http2Connection;
    private BufferedSource source;
    private BufferedSink sink;
    boolean noNewExchanges;
    int routeFailureCount;
    int successCount;
    private int refusedStreamCount;
    private int allocationLimit = 1;
    final List<Reference<Transmitter>> transmitters = new ArrayList<Reference<Transmitter>>();
    long idleAtNanos = Long.MAX_VALUE;

    public RealConnection(RealConnectionPool connectionPool, Route route) {
        this.connectionPool = connectionPool;
        this.route = route;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void noNewExchanges() {
        assert (!Thread.holdsLock(this.connectionPool));
        RealConnectionPool realConnectionPool = this.connectionPool;
        synchronized (realConnectionPool) {
            this.noNewExchanges = true;
        }
    }

    static RealConnection testConnection(RealConnectionPool connectionPool, Route route, Socket socket, long idleAtNanos) {
        RealConnection result = new RealConnection(connectionPool, route);
        result.socket = socket;
        result.idleAtNanos = idleAtNanos;
        return result;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void connect(int connectTimeout, int readTimeout, int writeTimeout, int pingIntervalMillis, boolean connectionRetryEnabled, Call call, EventListener eventListener) {
        if (this.protocol != null) {
            throw new IllegalStateException("already connected");
        }
        RouteException routeException = null;
        List<ConnectionSpec> connectionSpecs = this.route.address().connectionSpecs();
        ConnectionSpecSelector connectionSpecSelector = new ConnectionSpecSelector(connectionSpecs);
        if (this.route.address().sslSocketFactory() == null) {
            if (!connectionSpecs.contains(ConnectionSpec.CLEARTEXT)) {
                throw new RouteException(new UnknownServiceException("CLEARTEXT communication not enabled for client"));
            }
            String host = this.route.address().url().host();
            if (!Platform.get().isCleartextTrafficPermitted(host)) {
                throw new RouteException(new UnknownServiceException("CLEARTEXT communication to " + host + " not permitted by network security policy"));
            }
        } else if (this.route.address().protocols().contains((Object)Protocol.H2_PRIOR_KNOWLEDGE)) {
            throw new RouteException(new UnknownServiceException("H2_PRIOR_KNOWLEDGE cannot be used with HTTPS"));
        }
        while (true) {
            try {
                if (this.route.requiresTunnel()) {
                    this.connectTunnel(connectTimeout, readTimeout, writeTimeout, call, eventListener);
                    if (this.rawSocket == null) {
                        break;
                    }
                } else {
                    this.connectSocket(connectTimeout, readTimeout, call, eventListener);
                }
                this.establishProtocol(connectionSpecSelector, pingIntervalMillis, call, eventListener);
                eventListener.connectEnd(call, this.route.socketAddress(), this.route.proxy(), this.protocol);
            }
            catch (IOException e) {
                Util.closeQuietly(this.socket);
                Util.closeQuietly(this.rawSocket);
                this.socket = null;
                this.rawSocket = null;
                this.source = null;
                this.sink = null;
                this.handshake = null;
                this.protocol = null;
                this.http2Connection = null;
                eventListener.connectFailed(call, this.route.socketAddress(), this.route.proxy(), null, e);
                if (routeException == null) {
                    routeException = new RouteException(e);
                    continue;
                }
                routeException.addConnectException(e);
                if (connectionRetryEnabled && connectionSpecSelector.connectionFailed(e)) continue;
                throw routeException;
            }
            break;
        }
        if (this.route.requiresTunnel() && this.rawSocket == null) {
            ProtocolException exception = new ProtocolException("Too many tunnel connections attempted: 21");
            throw new RouteException(exception);
        }
        if (this.http2Connection != null) {
            RealConnectionPool realConnectionPool = this.connectionPool;
            synchronized (realConnectionPool) {
                this.allocationLimit = this.http2Connection.maxConcurrentStreams();
            }
        }
    }

    private void connectTunnel(int connectTimeout, int readTimeout, int writeTimeout, Call call, EventListener eventListener) throws IOException {
        Request tunnelRequest = this.createTunnelRequest();
        HttpUrl url = tunnelRequest.url();
        for (int i = 0; i < 21; ++i) {
            this.connectSocket(connectTimeout, readTimeout, call, eventListener);
            tunnelRequest = this.createTunnel(readTimeout, writeTimeout, tunnelRequest, url);
            if (tunnelRequest == null) break;
            Util.closeQuietly(this.rawSocket);
            this.rawSocket = null;
            this.sink = null;
            this.source = null;
            eventListener.connectEnd(call, this.route.socketAddress(), this.route.proxy(), null);
        }
    }

    private void connectSocket(int connectTimeout, int readTimeout, Call call, EventListener eventListener) throws IOException {
        block4: {
            Proxy proxy = this.route.proxy();
            Address address = this.route.address();
            this.rawSocket = proxy.type() == Proxy.Type.DIRECT || proxy.type() == Proxy.Type.HTTP ? address.socketFactory().createSocket() : new Socket(proxy);
            eventListener.connectStart(call, this.route.socketAddress(), proxy);
            this.rawSocket.setSoTimeout(readTimeout);
            try {
                Platform.get().connectSocket(this.rawSocket, this.route.socketAddress(), connectTimeout);
            }
            catch (ConnectException e) {
                ConnectException ce = new ConnectException("Failed to connect to " + this.route.socketAddress());
                ce.initCause(e);
                throw ce;
            }
            try {
                this.source = Okio.buffer(Okio.source(this.rawSocket));
                this.sink = Okio.buffer(Okio.sink(this.rawSocket));
            }
            catch (NullPointerException npe) {
                if (!NPE_THROW_WITH_NULL.equals(npe.getMessage())) break block4;
                throw new IOException(npe);
            }
        }
    }

    private void establishProtocol(ConnectionSpecSelector connectionSpecSelector, int pingIntervalMillis, Call call, EventListener eventListener) throws IOException {
        if (this.route.address().sslSocketFactory() == null) {
            if (this.route.address().protocols().contains((Object)Protocol.H2_PRIOR_KNOWLEDGE)) {
                this.socket = this.rawSocket;
                this.protocol = Protocol.H2_PRIOR_KNOWLEDGE;
                this.startHttp2(pingIntervalMillis);
                return;
            }
            this.socket = this.rawSocket;
            this.protocol = Protocol.HTTP_1_1;
            return;
        }
        eventListener.secureConnectStart(call);
        this.connectTls(connectionSpecSelector);
        eventListener.secureConnectEnd(call, this.handshake);
        if (this.protocol == Protocol.HTTP_2) {
            this.startHttp2(pingIntervalMillis);
        }
    }

    private void startHttp2(int pingIntervalMillis) throws IOException {
        this.socket.setSoTimeout(0);
        this.http2Connection = new Http2Connection.Builder(true).socket(this.socket, this.route.address().url().host(), this.source, this.sink).listener(this).pingIntervalMillis(pingIntervalMillis).build();
        this.http2Connection.start();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private void connectTls(ConnectionSpecSelector connectionSpecSelector) throws IOException {
        Address address = this.route.address();
        SSLSocketFactory sslSocketFactory = address.sslSocketFactory();
        boolean success = false;
        SSLSocket sslSocket = null;
        try {
            sslSocket = (SSLSocket)sslSocketFactory.createSocket(this.rawSocket, address.url().host(), address.url().port(), true);
            ConnectionSpec connectionSpec = connectionSpecSelector.configureSecureSocket(sslSocket);
            if (connectionSpec.supportsTlsExtensions()) {
                Platform.get().configureTlsExtensions(sslSocket, address.url().host(), address.protocols());
            }
            sslSocket.startHandshake();
            SSLSession sslSocketSession = sslSocket.getSession();
            Handshake unverifiedHandshake = Handshake.get(sslSocketSession);
            if (!address.hostnameVerifier().verify(address.url().host(), sslSocketSession)) {
                List<Certificate> peerCertificates = unverifiedHandshake.peerCertificates();
                if (peerCertificates.isEmpty()) throw new SSLPeerUnverifiedException("Hostname " + address.url().host() + " not verified (no certificates)");
                X509Certificate cert = (X509Certificate)peerCertificates.get(0);
                throw new SSLPeerUnverifiedException("Hostname " + address.url().host() + " not verified:\n    certificate: " + CertificatePinner.pin(cert) + "\n    DN: " + cert.getSubjectDN().getName() + "\n    subjectAltNames: " + OkHostnameVerifier.allSubjectAltNames(cert));
            }
            address.certificatePinner().check(address.url().host(), unverifiedHandshake.peerCertificates());
            String maybeProtocol = connectionSpec.supportsTlsExtensions() ? Platform.get().getSelectedProtocol(sslSocket) : null;
            this.socket = sslSocket;
            this.source = Okio.buffer(Okio.source(this.socket));
            this.sink = Okio.buffer(Okio.sink(this.socket));
            this.handshake = unverifiedHandshake;
            this.protocol = maybeProtocol != null ? Protocol.get(maybeProtocol) : Protocol.HTTP_1_1;
            success = true;
            if (sslSocket != null) {
                Platform.get().afterHandshake(sslSocket);
            }
            if (success) return;
        }
        catch (AssertionError e) {
            try {
                if (!Util.isAndroidGetsocknameError(e)) throw e;
                throw new IOException((Throwable)((Object)e));
            }
            catch (Throwable throwable) {
                if (sslSocket != null) {
                    Platform.get().afterHandshake(sslSocket);
                }
                if (success) throw throwable;
                Util.closeQuietly(sslSocket);
                throw throwable;
            }
        }
        Util.closeQuietly(sslSocket);
        return;
    }

    private Request createTunnel(int readTimeout, int writeTimeout, Request tunnelRequest, HttpUrl url) throws IOException {
        Response response;
        String requestLine = "CONNECT " + Util.hostHeader(url, true) + " HTTP/1.1";
        block4: while (true) {
            Http1ExchangeCodec tunnelCodec = new Http1ExchangeCodec(null, null, this.source, this.sink);
            this.source.timeout().timeout(readTimeout, TimeUnit.MILLISECONDS);
            this.sink.timeout().timeout(writeTimeout, TimeUnit.MILLISECONDS);
            tunnelCodec.writeRequest(tunnelRequest.headers(), requestLine);
            tunnelCodec.finishRequest();
            response = tunnelCodec.readResponseHeaders(false).request(tunnelRequest).build();
            tunnelCodec.skipConnectBody(response);
            switch (response.code()) {
                case 200: {
                    if (!this.source.getBuffer().exhausted() || !this.sink.buffer().exhausted()) {
                        throw new IOException("TLS tunnel buffered too many bytes!");
                    }
                    return null;
                }
                case 407: {
                    tunnelRequest = this.route.address().proxyAuthenticator().authenticate(this.route, response);
                    if (tunnelRequest != null) continue block4;
                    throw new IOException("Failed to authenticate with proxy");
                    if (!"close".equalsIgnoreCase(response.header("Connection"))) continue block4;
                    return tunnelRequest;
                }
            }
            break;
        }
        throw new IOException("Unexpected response code for CONNECT: " + response.code());
    }

    private Request createTunnelRequest() throws IOException {
        Request proxyConnectRequest = new Request.Builder().url(this.route.address().url()).method("CONNECT", null).header("Host", Util.hostHeader(this.route.address().url(), true)).header("Proxy-Connection", "Keep-Alive").header("User-Agent", Version.userAgent()).build();
        Response fakeAuthChallengeResponse = new Response.Builder().request(proxyConnectRequest).protocol(Protocol.HTTP_1_1).code(407).message("Preemptive Authenticate").body(Util.EMPTY_RESPONSE).sentRequestAtMillis(-1L).receivedResponseAtMillis(-1L).header("Proxy-Authenticate", "OkHttp-Preemptive").build();
        Request authenticatedRequest = this.route.address().proxyAuthenticator().authenticate(this.route, fakeAuthChallengeResponse);
        return authenticatedRequest != null ? authenticatedRequest : proxyConnectRequest;
    }

    boolean isEligible(Address address, @Nullable List<Route> routes) {
        if (this.transmitters.size() >= this.allocationLimit || this.noNewExchanges) {
            return false;
        }
        if (!Internal.instance.equalsNonHost(this.route.address(), address)) {
            return false;
        }
        if (address.url().host().equals(this.route().address().url().host())) {
            return true;
        }
        if (this.http2Connection == null) {
            return false;
        }
        if (routes == null || !this.routeMatchesAny(routes)) {
            return false;
        }
        if (address.hostnameVerifier() != OkHostnameVerifier.INSTANCE) {
            return false;
        }
        if (!this.supportsUrl(address.url())) {
            return false;
        }
        try {
            address.certificatePinner().check(address.url().host(), this.handshake().peerCertificates());
        }
        catch (SSLPeerUnverifiedException e) {
            return false;
        }
        return true;
    }

    private boolean routeMatchesAny(List<Route> candidates) {
        int size = candidates.size();
        for (int i = 0; i < size; ++i) {
            Route candidate = candidates.get(i);
            if (candidate.proxy().type() != Proxy.Type.DIRECT || this.route.proxy().type() != Proxy.Type.DIRECT || !this.route.socketAddress().equals(candidate.socketAddress())) continue;
            return true;
        }
        return false;
    }

    public boolean supportsUrl(HttpUrl url) {
        if (url.port() != this.route.address().url().port()) {
            return false;
        }
        if (!url.host().equals(this.route.address().url().host())) {
            return this.handshake != null && OkHostnameVerifier.INSTANCE.verify(url.host(), (X509Certificate)this.handshake.peerCertificates().get(0));
        }
        return true;
    }

    ExchangeCodec newCodec(OkHttpClient client, Interceptor.Chain chain) throws SocketException {
        if (this.http2Connection != null) {
            return new Http2ExchangeCodec(client, this, chain, this.http2Connection);
        }
        this.socket.setSoTimeout(chain.readTimeoutMillis());
        this.source.timeout().timeout(chain.readTimeoutMillis(), TimeUnit.MILLISECONDS);
        this.sink.timeout().timeout(chain.writeTimeoutMillis(), TimeUnit.MILLISECONDS);
        return new Http1ExchangeCodec(client, this, this.source, this.sink);
    }

    RealWebSocket.Streams newWebSocketStreams(final Exchange exchange) throws SocketException {
        this.socket.setSoTimeout(0);
        this.noNewExchanges();
        return new RealWebSocket.Streams(true, this.source, this.sink){

            @Override
            public void close() throws IOException {
                exchange.bodyComplete(-1L, true, true, null);
            }
        };
    }

    @Override
    public Route route() {
        return this.route;
    }

    public void cancel() {
        Util.closeQuietly(this.rawSocket);
    }

    @Override
    public Socket socket() {
        return this.socket;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public boolean isHealthy(boolean doExtensiveChecks) {
        if (this.socket.isClosed()) return false;
        if (this.socket.isInputShutdown()) return false;
        if (this.socket.isOutputShutdown()) {
            return false;
        }
        if (this.http2Connection != null) {
            return this.http2Connection.isHealthy(System.nanoTime());
        }
        if (!doExtensiveChecks) return true;
        try {
            int readTimeout222 = this.socket.getSoTimeout();
            try {
                this.socket.setSoTimeout(1);
                if (this.source.exhausted()) {
                    boolean bl = false;
                    return bl;
                }
                boolean bl = true;
                return bl;
            }
            finally {
                this.socket.setSoTimeout(readTimeout222);
            }
        }
        catch (SocketTimeoutException readTimeout222) {
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    @Override
    public void onStream(Http2Stream stream) throws IOException {
        stream.close(ErrorCode.REFUSED_STREAM, null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void onSettings(Http2Connection connection) {
        RealConnectionPool realConnectionPool = this.connectionPool;
        synchronized (realConnectionPool) {
            this.allocationLimit = connection.maxConcurrentStreams();
        }
    }

    @Override
    public Handshake handshake() {
        return this.handshake;
    }

    public boolean isMultiplexed() {
        return this.http2Connection != null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void trackFailure(@Nullable IOException e) {
        assert (!Thread.holdsLock(this.connectionPool));
        RealConnectionPool realConnectionPool = this.connectionPool;
        synchronized (realConnectionPool) {
            if (e instanceof StreamResetException) {
                ErrorCode errorCode = ((StreamResetException)e).errorCode;
                if (errorCode == ErrorCode.REFUSED_STREAM) {
                    ++this.refusedStreamCount;
                    if (this.refusedStreamCount > 1) {
                        this.noNewExchanges = true;
                        ++this.routeFailureCount;
                    }
                } else if (errorCode != ErrorCode.CANCEL) {
                    this.noNewExchanges = true;
                    ++this.routeFailureCount;
                }
            } else if (!this.isMultiplexed() || e instanceof ConnectionShutdownException) {
                this.noNewExchanges = true;
                if (this.successCount == 0) {
                    if (e != null) {
                        this.connectionPool.connectFailed(this.route, e);
                    }
                    ++this.routeFailureCount;
                }
            }
        }
    }

    @Override
    public Protocol protocol() {
        return this.protocol;
    }

    public String toString() {
        return "Connection{" + this.route.address().url().host() + ":" + this.route.address().url().port() + ", proxy=" + this.route.proxy() + " hostAddress=" + this.route.socketAddress() + " cipherSuite=" + (this.handshake != null ? this.handshake.cipherSuite() : "none") + " protocol=" + (Object)((Object)this.protocol) + '}';
    }
}

