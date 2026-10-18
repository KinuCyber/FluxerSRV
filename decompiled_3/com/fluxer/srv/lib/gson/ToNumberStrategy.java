/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson;

import com.fluxer.srv.lib.gson.stream.JsonReader;
import java.io.IOException;

public interface ToNumberStrategy {
    public Number readNumber(JsonReader var1) throws IOException;
}

