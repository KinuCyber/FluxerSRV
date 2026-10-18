/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson;

import com.fluxer.srv.lib.gson.Gson;
import com.fluxer.srv.lib.gson.TypeAdapter;
import com.fluxer.srv.lib.gson.reflect.TypeToken;

public interface TypeAdapterFactory {
    public <T> TypeAdapter<T> create(Gson var1, TypeToken<T> var2);
}

