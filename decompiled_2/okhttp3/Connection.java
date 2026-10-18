/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package okhttp3;

import java.net.Socket;
import javax.annotation.Nullable;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Route;

public interface Connection {
    public Route route();

    public Socket socket();

    @Nullable
    public Handshake handshake();

    public Protocol protocol();
}

