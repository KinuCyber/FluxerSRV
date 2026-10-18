/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson;

import com.fluxer.srv.lib.gson.JsonElement;
import com.fluxer.srv.lib.gson.JsonSerializationContext;
import java.lang.reflect.Type;

public interface JsonSerializer<T> {
    public JsonElement serialize(T var1, Type var2, JsonSerializationContext var3);
}

