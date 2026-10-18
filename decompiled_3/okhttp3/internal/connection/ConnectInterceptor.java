/*
 * Decompiled with CFR 0.152.
 */
package okhttp3.internal.connection;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.Exchange;
import okhttp3.internal.connection.Transmitter;
import okhttp3.internal.http.RealInterceptorChain;

public final class ConnectInterceptor
implements Interceptor {
    public final OkHttpClient client;

    public ConnectInterceptor(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        RealInterceptorChain realChain = (RealInterceptorChain)chain;
        Request request = realChain.request();
        Transmitter transmitter = realChain.transmitter();
        boolean doExtensiveHealthChecks = !request.method().equals("GET");
        Exchange exchange = transmitter.newExchange(chain, doExtensiveHealthChecks);
        return realChain.proceed(request, transmitter, exchange);
    }
}

