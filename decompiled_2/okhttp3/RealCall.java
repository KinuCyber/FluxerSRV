/*
 * Decompiled with CFR 0.152.
 */
package okhttp3;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.NamedRunnable;
import okhttp3.internal.Util;
import okhttp3.internal.cache.CacheInterceptor;
import okhttp3.internal.connection.ConnectInterceptor;
import okhttp3.internal.connection.Transmitter;
import okhttp3.internal.http.BridgeInterceptor;
import okhttp3.internal.http.CallServerInterceptor;
import okhttp3.internal.http.RealInterceptorChain;
import okhttp3.internal.http.RetryAndFollowUpInterceptor;
import okhttp3.internal.platform.Platform;
import okio.Timeout;

final class RealCall
implements Call {
    final OkHttpClient client;
    private Transmitter transmitter;
    final Request originalRequest;
    final boolean forWebSocket;
    private boolean executed;

    private RealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
        this.client = client;
        this.originalRequest = originalRequest;
        this.forWebSocket = forWebSocket;
    }

    static RealCall newRealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
        RealCall call = new RealCall(client, originalRequest, forWebSocket);
        call.transmitter = new Transmitter(client, call);
        return call;
    }

    @Override
    public Request request() {
        return this.originalRequest;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Response execute() throws IOException {
        Object object = this;
        synchronized (object) {
            if (this.executed) {
                throw new IllegalStateException("Already Executed");
            }
            this.executed = true;
        }
        this.transmitter.timeoutEnter();
        this.transmitter.callStart();
        try {
            this.client.dispatcher().executed(this);
            object = this.getResponseWithInterceptorChain();
            return object;
        }
        finally {
            this.client.dispatcher().finished(this);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void enqueue(Callback responseCallback) {
        RealCall realCall = this;
        synchronized (realCall) {
            if (this.executed) {
                throw new IllegalStateException("Already Executed");
            }
            this.executed = true;
        }
        this.transmitter.callStart();
        this.client.dispatcher().enqueue(new AsyncCall(responseCallback));
    }

    @Override
    public void cancel() {
        this.transmitter.cancel();
    }

    @Override
    public Timeout timeout() {
        return this.transmitter.timeout();
    }

    @Override
    public synchronized boolean isExecuted() {
        return this.executed;
    }

    @Override
    public boolean isCanceled() {
        return this.transmitter.isCanceled();
    }

    @Override
    public RealCall clone() {
        return RealCall.newRealCall(this.client, this.originalRequest, this.forWebSocket);
    }

    String toLoggableString() {
        return (this.isCanceled() ? "canceled " : "") + (this.forWebSocket ? "web socket" : "call") + " to " + this.redactedUrl();
    }

    String redactedUrl() {
        return this.originalRequest.url().redact();
    }

    Response getResponseWithInterceptorChain() throws IOException {
        ArrayList<Interceptor> interceptors = new ArrayList<Interceptor>();
        interceptors.addAll(this.client.interceptors());
        interceptors.add(new RetryAndFollowUpInterceptor(this.client));
        interceptors.add(new BridgeInterceptor(this.client.cookieJar()));
        interceptors.add(new CacheInterceptor(this.client.internalCache()));
        interceptors.add(new ConnectInterceptor(this.client));
        if (!this.forWebSocket) {
            interceptors.addAll(this.client.networkInterceptors());
        }
        interceptors.add(new CallServerInterceptor(this.forWebSocket));
        RealInterceptorChain chain = new RealInterceptorChain(interceptors, this.transmitter, null, 0, this.originalRequest, this, this.client.connectTimeoutMillis(), this.client.readTimeoutMillis(), this.client.writeTimeoutMillis());
        boolean calledNoMoreExchanges = false;
        try {
            Response response = chain.proceed(this.originalRequest);
            if (this.transmitter.isCanceled()) {
                Util.closeQuietly(response);
                throw new IOException("Canceled");
            }
            Response response2 = response;
            return response2;
        }
        catch (IOException e) {
            calledNoMoreExchanges = true;
            throw this.transmitter.noMoreExchanges(e);
        }
        finally {
            if (!calledNoMoreExchanges) {
                this.transmitter.noMoreExchanges(null);
            }
        }
    }

    final class AsyncCall
    extends NamedRunnable {
        private final Callback responseCallback;
        private volatile AtomicInteger callsPerHost;

        AsyncCall(Callback responseCallback) {
            super("OkHttp %s", RealCall.this.redactedUrl());
            this.callsPerHost = new AtomicInteger(0);
            this.responseCallback = responseCallback;
        }

        AtomicInteger callsPerHost() {
            return this.callsPerHost;
        }

        void reuseCallsPerHostFrom(AsyncCall other) {
            this.callsPerHost = other.callsPerHost;
        }

        String host() {
            return RealCall.this.originalRequest.url().host();
        }

        Request request() {
            return RealCall.this.originalRequest;
        }

        RealCall get() {
            return RealCall.this;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        void executeOn(ExecutorService executorService) {
            assert (!Thread.holdsLock(RealCall.this.client.dispatcher()));
            boolean success = false;
            try {
                executorService.execute(this);
                success = true;
            }
            catch (RejectedExecutionException e) {
                InterruptedIOException ioException = new InterruptedIOException("executor rejected");
                ioException.initCause(e);
                RealCall.this.transmitter.noMoreExchanges(ioException);
                this.responseCallback.onFailure(RealCall.this, ioException);
            }
            finally {
                if (!success) {
                    RealCall.this.client.dispatcher().finished(this);
                }
            }
        }

        @Override
        protected void execute() {
            boolean signalledCallback = false;
            RealCall.this.transmitter.timeoutEnter();
            try {
                Response response = RealCall.this.getResponseWithInterceptorChain();
                signalledCallback = true;
                this.responseCallback.onResponse(RealCall.this, response);
            }
            catch (IOException e) {
                if (signalledCallback) {
                    Platform.get().log(4, "Callback failure for " + RealCall.this.toLoggableString(), e);
                } else {
                    this.responseCallback.onFailure(RealCall.this, e);
                }
            }
            catch (Throwable t) {
                RealCall.this.cancel();
                if (!signalledCallback) {
                    IOException canceledException = new IOException("canceled due to " + t);
                    canceledException.addSuppressed(t);
                    this.responseCallback.onFailure(RealCall.this, canceledException);
                }
                throw t;
            }
            finally {
                RealCall.this.client.dispatcher().finished(this);
            }
        }
    }
}

