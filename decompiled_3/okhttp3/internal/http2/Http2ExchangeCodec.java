/*
 * Decompiled with CFR 0.152.
 */
package okhttp3.internal.http2;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Internal;
import okhttp3.internal.Util;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.http.ExchangeCodec;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.http.RequestLine;
import okhttp3.internal.http.StatusLine;
import okhttp3.internal.http2.ErrorCode;
import okhttp3.internal.http2.Header;
import okhttp3.internal.http2.Http2Connection;
import okhttp3.internal.http2.Http2Stream;
import okio.Sink;
import okio.Source;

public final class Http2ExchangeCodec
implements ExchangeCodec {
    private static final String CONNECTION = "connection";
    private static final String HOST = "host";
    private static final String KEEP_ALIVE = "keep-alive";
    private static final String PROXY_CONNECTION = "proxy-connection";
    private static final String TRANSFER_ENCODING = "transfer-encoding";
    private static final String TE = "te";
    private static final String ENCODING = "encoding";
    private static final String UPGRADE = "upgrade";
    private static final List<String> HTTP_2_SKIPPED_REQUEST_HEADERS = Util.immutableList("connection", "host", "keep-alive", "proxy-connection", "te", "transfer-encoding", "encoding", "upgrade", ":method", ":path", ":scheme", ":authority");
    private static final List<String> HTTP_2_SKIPPED_RESPONSE_HEADERS = Util.immutableList("connection", "host", "keep-alive", "proxy-connection", "te", "transfer-encoding", "encoding", "upgrade");
    private final Interceptor.Chain chain;
    private final RealConnection realConnection;
    private final Http2Connection connection;
    private volatile Http2Stream stream;
    private final Protocol protocol;
    private volatile boolean canceled;

    public Http2ExchangeCodec(OkHttpClient client, RealConnection realConnection, Interceptor.Chain chain, Http2Connection connection) {
        this.realConnection = realConnection;
        this.chain = chain;
        this.connection = connection;
        this.protocol = client.protocols().contains((Object)Protocol.H2_PRIOR_KNOWLEDGE) ? Protocol.H2_PRIOR_KNOWLEDGE : Protocol.HTTP_2;
    }

    @Override
    public RealConnection connection() {
        return this.realConnection;
    }

    @Override
    public Sink createRequestBody(Request request, long contentLength) {
        return this.stream.getSink();
    }

    @Override
    public void writeRequestHeaders(Request request) throws IOException {
        if (this.stream != null) {
            return;
        }
        boolean hasRequestBody = request.body() != null;
        List<Header> requestHeaders = Http2ExchangeCodec.http2HeadersList(request);
        this.stream = this.connection.newStream(requestHeaders, hasRequestBody);
        if (this.canceled) {
            this.stream.closeLater(ErrorCode.CANCEL);
            throw new IOException("Canceled");
        }
        this.stream.readTimeout().timeout(this.chain.readTimeoutMillis(), TimeUnit.MILLISECONDS);
        this.stream.writeTimeout().timeout(this.chain.writeTimeoutMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void flushRequest() throws IOException {
        this.connection.flush();
    }

    @Override
    public void finishRequest() throws IOException {
        this.stream.getSink().close();
    }

    @Override
    public Response.Builder readResponseHeaders(boolean expectContinue) throws IOException {
        Headers headers = this.stream.takeHeaders();
        Response.Builder responseBuilder = Http2ExchangeCodec.readHttp2HeadersList(headers, this.protocol);
        if (expectContinue && Internal.instance.code(responseBuilder) == 100) {
            return null;
        }
        return responseBuilder;
    }

    public static List<Header> http2HeadersList(Request request) {
        Headers headers = request.headers();
        ArrayList<Header> result = new ArrayList<Header>(headers.size() + 4);
        result.add(new Header(Header.TARGET_METHOD, request.method()));
        result.add(new Header(Header.TARGET_PATH, RequestLine.requestPath(request.url())));
        String host = request.header("Host");
        if (host != null) {
            result.add(new Header(Header.TARGET_AUTHORITY, host));
        }
        result.add(new Header(Header.TARGET_SCHEME, request.url().scheme()));
        int size = headers.size();
        for (int i = 0; i < size; ++i) {
            String name = headers.name(i).toLowerCase(Locale.US);
            if (HTTP_2_SKIPPED_REQUEST_HEADERS.contains(name) && (!name.equals(TE) || !headers.value(i).equals("trailers"))) continue;
            result.add(new Header(name, headers.value(i)));
        }
        return result;
    }

    public static Response.Builder readHttp2HeadersList(Headers headerBlock, Protocol protocol) throws IOException {
        StatusLine statusLine = null;
        Headers.Builder headersBuilder = new Headers.Builder();
        int size = headerBlock.size();
        for (int i = 0; i < size; ++i) {
            String name = headerBlock.name(i);
            String value = headerBlock.value(i);
            if (name.equals(":status")) {
                statusLine = StatusLine.parse("HTTP/1.1 " + value);
                continue;
            }
            if (HTTP_2_SKIPPED_RESPONSE_HEADERS.contains(name)) continue;
            Internal.instance.addLenient(headersBuilder, name, value);
        }
        if (statusLine == null) {
            throw new ProtocolException("Expected ':status' header not present");
        }
        return new Response.Builder().protocol(protocol).code(statusLine.code).message(statusLine.message).headers(headersBuilder.build());
    }

    @Override
    public long reportedContentLength(Response response) {
        return HttpHeaders.contentLength(response);
    }

    @Override
    public Source openResponseBodySource(Response response) {
        return this.stream.getSource();
    }

    @Override
    public Headers trailers() throws IOException {
        return this.stream.trailers();
    }

    @Override
    public void cancel() {
        this.canceled = true;
        if (this.stream != null) {
            this.stream.closeLater(ErrorCode.CANCEL);
        }
    }
}

