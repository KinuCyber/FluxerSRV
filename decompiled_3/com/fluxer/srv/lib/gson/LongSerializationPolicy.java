/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson;

import com.fluxer.srv.lib.gson.JsonElement;
import com.fluxer.srv.lib.gson.JsonNull;
import com.fluxer.srv.lib.gson.JsonPrimitive;

public enum LongSerializationPolicy {
    DEFAULT{

        @Override
        public JsonElement serialize(Long value) {
            if (value == null) {
                return JsonNull.INSTANCE;
            }
            return new JsonPrimitive(value);
        }
    }
    ,
    STRING{

        @Override
        public JsonElement serialize(Long value) {
            if (value == null) {
                return JsonNull.INSTANCE;
            }
            return new JsonPrimitive(value.toString());
        }
    };


    public abstract JsonElement serialize(Long var1);
}

