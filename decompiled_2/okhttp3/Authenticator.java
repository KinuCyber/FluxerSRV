/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package okhttp3;

import java.io.IOException;
import javax.annotation.Nullable;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public interface Authenticator {
    public static final Authenticator NONE = (route, response) -> null;

    @Nullable
    public Request authenticate(@Nullable Route var1, Response var2) throws IOException;
}

