/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package okhttp3.internal.http;

import java.io.IOException;
import javax.annotation.Nullable;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.RealConnection;
import okio.Sink;
import okio.Source;

public interface ExchangeCodec {
    public static final int DISCARD_STREAM_TIMEOUT_MILLIS = 100;

    public RealConnection connection();

    public Sink createRequestBody(Request var1, long var2) throws IOException;

    public void writeRequestHeaders(Request var1) throws IOException;

    public void flushRequest() throws IOException;

    public void finishRequest() throws IOException;

    @Nullable
    public Response.Builder readResponseHeaders(boolean var1) throws IOException;

    public long reportedContentLength(Response var1) throws IOException;

    public Source openResponseBodySource(Response var1) throws IOException;

    public Headers trailers() throws IOException;

    public void cancel();
}

