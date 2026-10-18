/*
 * Decompiled with CFR 0.152.
 */
package okhttp3.internal.http;

import java.io.IOException;
import java.net.ProtocolException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import okhttp3.internal.connection.Exchange;
import okhttp3.internal.http.HttpMethod;
import okhttp3.internal.http.RealInterceptorChain;
import okio.BufferedSink;
import okio.Okio;

public final class CallServerInterceptor
implements Interceptor {
    private final boolean forWebSocket;

    public CallServerInterceptor(boolean forWebSocket) {
        this.forWebSocket = forWebSocket;
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Response response;
        int code;
        RealInterceptorChain realChain = (RealInterceptorChain)chain;
        Exchange exchange = realChain.exchange();
        Request request = realChain.request();
        long sentRequestMillis = System.currentTimeMillis();
        exchange.writeRequestHeaders(request);
        boolean responseHeadersStarted = false;
        Response.Builder responseBuilder = null;
        if (HttpMethod.permitsRequestBody(request.method()) && request.body() != null) {
            if ("100-continue".equalsIgnoreCase(request.header("Expect"))) {
                exchange.flushRequest();
                responseHeadersStarted = true;
                exchange.responseHeadersStart();
                responseBuilder = exchange.readResponseHeaders(true);
            }
            if (responseBuilder == null) {
                BufferedSink bufferedRequestBody;
                if (request.body().isDuplex()) {
                    exchange.flushRequest();
                    bufferedRequestBody = Okio.buffer(exchange.createRequestBody(request, true));
                    request.body().writeTo(bufferedRequestBody);
                } else {
                    bufferedRequestBody = Okio.buffer(exchange.createRequestBody(request, false));
                    request.body().writeTo(bufferedRequestBody);
                    bufferedRequestBody.close();
                }
            } else {
                exchange.noRequestBody();
                if (!exchange.connection().isMultiplexed()) {
                    exchange.noNewExchangesOnConnection();
                }
            }
        } else {
            exchange.noRequestBody();
        }
        if (request.body() == null || !request.body().isDuplex()) {
            exchange.finishRequest();
        }
        if (!responseHeadersStarted) {
            exchange.responseHeadersStart();
        }
        if (responseBuilder == null) {
            responseBuilder = exchange.readResponseHeaders(false);
        }
        if ((code = (response = responseBuilder.request(request).handshake(exchange.connection().handshake()).sentRequestAtMillis(sentRequestMillis).receivedResponseAtMillis(System.currentTimeMillis()).build()).code()) == 100) {
            response = exchange.readResponseHeaders(false).request(request).handshake(exchange.connection().handshake()).sentRequestAtMillis(sentRequestMillis).receivedResponseAtMillis(System.currentTimeMillis()).build();
            code = response.code();
        }
        exchange.responseHeadersEnd(response);
        response = this.forWebSocket && code == 101 ? response.newBuilder().body(Util.EMPTY_RESPONSE).build() : response.newBuilder().body(exchange.openResponseBody(response)).build();
        if ("close".equalsIgnoreCase(response.request().header("Connection")) || "close".equalsIgnoreCase(response.header("Connection"))) {
            exchange.noNewExchangesOnConnection();
        }
        if ((code == 204 || code == 205) && response.body().contentLength() > 0L) {
            throw new ProtocolException("HTTP " + code + " had non-zero Content-Length: " + response.body().contentLength());
        }
        return response;
    }
}

