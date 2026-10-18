/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package okhttp3.internal;

import javax.annotation.Nullable;
import javax.net.ssl.SSLSocket;
import okhttp3.Address;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.Exchange;
import okhttp3.internal.connection.RealConnectionPool;

public abstract class Internal {
    public static Internal instance;

    public static void initializeInstanceForTests() {
        new OkHttpClient();
    }

    public abstract void addLenient(Headers.Builder var1, String var2);

    public abstract void addLenient(Headers.Builder var1, String var2, String var3);

    public abstract RealConnectionPool realConnectionPool(ConnectionPool var1);

    public abstract boolean equalsNonHost(Address var1, Address var2);

    public abstract int code(Response.Builder var1);

    public abstract void apply(ConnectionSpec var1, SSLSocket var2, boolean var3);

    public abstract Call newWebSocketCall(OkHttpClient var1, Request var2);

    public abstract void initExchange(Response.Builder var1, Exchange var2);

    @Nullable
    public abstract Exchange exchange(Response var1);
}

