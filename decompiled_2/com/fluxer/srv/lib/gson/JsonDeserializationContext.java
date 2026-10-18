/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson;

import com.fluxer.srv.lib.gson.JsonElement;
import com.fluxer.srv.lib.gson.JsonParseException;
import java.lang.reflect.Type;

public interface JsonDeserializationContext {
    public <T> T deserialize(JsonElement var1, Type var2) throws JsonParseException;
}

