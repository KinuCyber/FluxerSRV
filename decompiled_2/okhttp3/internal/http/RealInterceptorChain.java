/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package okhttp3.internal.http;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import okhttp3.internal.connection.Exchange;
import okhttp3.internal.connection.Transmitter;

public final class RealInterceptorChain
implements Interceptor.Chain {
    private final List<Interceptor> interceptors;
    private final Transmitter transmitter;
    @Nullable
    private final Exchange exchange;
    private final int index;
    private final Request request;
    private final Call call;
    private final int connectTimeout;
    private final int readTimeout;
    private final int writeTimeout;
    private int calls;

    public RealInterceptorChain(List<Interceptor> interceptors, Transmitter transmitter, @Nullable Exchange exchange, int index, Request request, Call call, int connectTimeout, int readTimeout, int writeTimeout) {
        this.interceptors = interceptors;
        this.transmitter = transmitter;
        this.exchange = exchange;
        this.index = index;
        this.request = request;
        this.call = call;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
    }

    @Override
    @Nullable
    public Connection connection() {
        return this.exchange != null ? this.exchange.connection() : null;
    }

    @Override
    public int connectTimeoutMillis() {
        return this.connectTimeout;
    }

    @Override
    public Interceptor.Chain withConnectTimeout(int timeout, TimeUnit unit) {
        int millis = Util.checkDuration("timeout", timeout, unit);
        return new RealInterceptorChain(this.interceptors, this.transmitter, this.exchange, this.index, this.request, this.call, millis, this.readTimeout, this.writeTimeout);
    }

    @Override
    public int readTimeoutMillis() {
        return this.readTimeout;
    }

    @Override
    public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) {
        int millis = Util.checkDuration("timeout", timeout, unit);
        return new RealInterceptorChain(this.interceptors, this.transmitter, this.exchange, this.index, this.request, this.call, this.connectTimeout, millis, this.writeTimeout);
    }

    @Override
    public int writeTimeoutMillis() {
        return this.writeTimeout;
    }

    @Override
    public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) {
        int millis = Util.checkDuration("timeout", timeout, unit);
        return new RealInterceptorChain(this.interceptors, this.transmitter, this.exchange, this.index, this.request, this.call, this.connectTimeout, this.readTimeout, millis);
    }

    public Transmitter transmitter() {
        return this.transmitter;
    }

    public Exchange exchange() {
        if (this.exchange == null) {
            throw new IllegalStateException();
        }
        return this.exchange;
    }

    @Override
    public Call call() {
        return this.call;
    }

    @Override
    public Request request() {
        return this.request;
    }

    @Override
    public Response proceed(Request request) throws IOException {
        return this.proceed(request, this.transmitter, this.exchange);
    }

    public Response proceed(Request request, Transmitter transmitter, @Nullable Exchange exchange) throws IOException {
        if (this.index >= this.interceptors.size()) {
            throw new AssertionError();
        }
        ++this.calls;
        if (this.exchange != null && !this.exchange.connection().supportsUrl(request.url())) {
            throw new IllegalStateException("network interceptor " + this.interceptors.get(this.index - 1) + " must retain the same host and port");
        }
        if (this.exchange != null && this.calls > 1) {
            throw new IllegalStateException("network interceptor " + this.interceptors.get(this.index - 1) + " must call proceed() exactly once");
        }
        RealInterceptorChain next = new RealInterceptorChain(this.interceptors, transmitter, exchange, this.index + 1, request, this.call, this.connectTimeout, this.readTimeout, this.writeTimeout);
        Interceptor interceptor = this.interceptors.get(this.index);
        Response response = interceptor.intercept(next);
        if (exchange != null && this.index + 1 < this.interceptors.size() && next.calls != 1) {
            throw new IllegalStateException("network interceptor " + interceptor + " must call proceed() exactly once");
        }
        if (response == null) {
            throw new NullPointerException("interceptor " + interceptor + " returned null");
        }
        if (response.body() == null) {
            throw new IllegalStateException("interceptor " + interceptor + " returned a response with no body");
        }
        return response;
    }
}

