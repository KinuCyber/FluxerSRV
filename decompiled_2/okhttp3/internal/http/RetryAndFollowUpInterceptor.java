/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package okhttp3.internal.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import javax.annotation.Nullable;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.internal.Internal;
import okhttp3.internal.Util;
import okhttp3.internal.connection.Exchange;
import okhttp3.internal.connection.RouteException;
import okhttp3.internal.connection.Transmitter;
import okhttp3.internal.http.HttpMethod;
import okhttp3.internal.http.RealInterceptorChain;
import okhttp3.internal.http2.ConnectionShutdownException;

public final class RetryAndFollowUpInterceptor
implements Interceptor {
    private static final int MAX_FOLLOW_UPS = 20;
    private final OkHttpClient client;

    public RetryAndFollowUpInterceptor(OkHttpClient client) {
        this.client = client;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        RealInterceptorChain realChain = (RealInterceptorChain)chain;
        Transmitter transmitter = realChain.transmitter();
        int followUpCount = 0;
        Response priorResponse = null;
        while (true) {
            Exchange exchange;
            Route route;
            Request followUp;
            Response response;
            transmitter.prepareToConnect(request);
            if (transmitter.isCanceled()) {
                throw new IOException("Canceled");
            }
            boolean success = false;
            try {
                response = realChain.proceed(request, transmitter, null);
                success = true;
            }
            catch (RouteException e) {
                if (this.recover(e.getLastConnectException(), transmitter, false, request)) continue;
                throw e.getFirstConnectException();
            }
            catch (IOException e) {
                boolean requestSendStarted = !(e instanceof ConnectionShutdownException);
                if (this.recover(e, transmitter, requestSendStarted, request)) continue;
                throw e;
            }
            finally {
                if (success) continue;
                transmitter.exchangeDoneDueToException();
                continue;
            }
            if (priorResponse != null) {
                response = response.newBuilder().priorResponse(priorResponse.newBuilder().body(null).build()).build();
            }
            if ((followUp = this.followUpRequest(response, route = (exchange = Internal.instance.exchange(response)) != null ? exchange.connection().route() : null)) == null) {
                if (exchange != null && exchange.isDuplex()) {
                    transmitter.timeoutEarlyExit();
                }
                return response;
            }
            RequestBody followUpBody = followUp.body();
            if (followUpBody != null && followUpBody.isOneShot()) {
                return response;
            }
            Util.closeQuietly(response.body());
            if (transmitter.hasExchange()) {
                exchange.detachWithViolence();
            }
            if (++followUpCount > 20) {
                throw new ProtocolException("Too many follow-up requests: " + followUpCount);
            }
            request = followUp;
            priorResponse = response;
        }
    }

    private boolean recover(IOException e, Transmitter transmitter, boolean requestSendStarted, Request userRequest) {
        if (!this.client.retryOnConnectionFailure()) {
            return false;
        }
        if (requestSendStarted && this.requestIsOneShot(e, userRequest)) {
            return false;
        }
        if (!this.isRecoverable(e, requestSendStarted)) {
            return false;
        }
        return transmitter.canRetry();
    }

    private boolean requestIsOneShot(IOException e, Request userRequest) {
        RequestBody requestBody = userRequest.body();
        return requestBody != null && requestBody.isOneShot() || e instanceof FileNotFoundException;
    }

    private boolean isRecoverable(IOException e, boolean requestSendStarted) {
        if (e instanceof ProtocolException) {
            return false;
        }
        if (e instanceof InterruptedIOException) {
            return e instanceof SocketTimeoutException && !requestSendStarted;
        }
        if (e instanceof SSLHandshakeException && e.getCause() instanceof CertificateException) {
            return false;
        }
        return !(e instanceof SSLPeerUnverifiedException);
    }

    private Request followUpRequest(Response userResponse, @Nullable Route route) throws IOException {
        if (userResponse == null) {
            throw new IllegalStateException();
        }
        int responseCode = userResponse.code();
        String method = userResponse.request().method();
        switch (responseCode) {
            case 407: {
                Proxy selectedProxy;
                Proxy proxy = selectedProxy = route != null ? route.proxy() : this.client.proxy();
                if (selectedProxy.type() != Proxy.Type.HTTP) {
                    throw new ProtocolException("Received HTTP_PROXY_AUTH (407) code while not using proxy");
                }
                return this.client.proxyAuthenticator().authenticate(route, userResponse);
            }
            case 401: {
                return this.client.authenticator().authenticate(route, userResponse);
            }
            case 307: 
            case 308: {
                if (!method.equals("GET") && !method.equals("HEAD")) {
                    return null;
                }
            }
            case 300: 
            case 301: 
            case 302: 
            case 303: {
                if (!this.client.followRedirects()) {
                    return null;
                }
                String location = userResponse.header("Location");
                if (location == null) {
                    return null;
                }
                HttpUrl url = userResponse.request().url().resolve(location);
                if (url == null) {
                    return null;
                }
                boolean sameScheme = url.scheme().equals(userResponse.request().url().scheme());
                if (!sameScheme && !this.client.followSslRedirects()) {
                    return null;
                }
                Request.Builder requestBuilder = userResponse.request().newBuilder();
                if (HttpMethod.permitsRequestBody(method)) {
                    boolean maintainBody = HttpMethod.redirectsWithBody(method);
                    if (HttpMethod.redirectsToGet(method)) {
                        requestBuilder.method("GET", null);
                    } else {
                        RequestBody requestBody = maintainBody ? userResponse.request().body() : null;
                        requestBuilder.method(method, requestBody);
                    }
                    if (!maintainBody) {
                        requestBuilder.removeHeader("Transfer-Encoding");
                        requestBuilder.removeHeader("Content-Length");
                        requestBuilder.removeHeader("Content-Type");
                    }
                }
                if (!Util.sameConnection(userResponse.request().url(), url)) {
                    requestBuilder.removeHeader("Authorization");
                }
                return requestBuilder.url(url).build();
            }
            case 408: {
                if (!this.client.retryOnConnectionFailure()) {
                    return null;
                }
                RequestBody requestBody = userResponse.request().body();
                if (requestBody != null && requestBody.isOneShot()) {
                    return null;
                }
                if (userResponse.priorResponse() != null && userResponse.priorResponse().code() == 408) {
                    return null;
                }
                if (this.retryAfter(userResponse, 0) > 0) {
                    return null;
                }
                return userResponse.request();
            }
            case 503: {
                if (userResponse.priorResponse() != null && userResponse.priorResponse().code() == 503) {
                    return null;
                }
                if (this.retryAfter(userResponse, Integer.MAX_VALUE) == 0) {
                    return userResponse.request();
                }
                return null;
            }
        }
        return null;
    }

    private int retryAfter(Response userResponse, int defaultDelay) {
        String header = userResponse.header("Retry-After");
        if (header == null) {
            return defaultDelay;
        }
        if (header.matches("\\d+")) {
            return Integer.valueOf(header);
        }
        return Integer.MAX_VALUE;
    }
}

