/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson.internal.bind;

import com.fluxer.srv.lib.gson.Gson;
import com.fluxer.srv.lib.gson.ToNumberPolicy;
import com.fluxer.srv.lib.gson.ToNumberStrategy;
import com.fluxer.srv.lib.gson.TypeAdapter;
import com.fluxer.srv.lib.gson.TypeAdapterFactory;
import com.fluxer.srv.lib.gson.internal.LinkedTreeMap;
import com.fluxer.srv.lib.gson.reflect.TypeToken;
import com.fluxer.srv.lib.gson.stream.JsonReader;
import com.fluxer.srv.lib.gson.stream.JsonToken;
import com.fluxer.srv.lib.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;

public final class ObjectTypeAdapter
extends TypeAdapter<Object> {
    private static final TypeAdapterFactory DOUBLE_FACTORY = ObjectTypeAdapter.newFactory(ToNumberPolicy.DOUBLE);
    private final Gson gson;
    private final ToNumberStrategy toNumberStrategy;

    private ObjectTypeAdapter(Gson gson, ToNumberStrategy toNumberStrategy) {
        this.gson = gson;
        this.toNumberStrategy = toNumberStrategy;
    }

    private static TypeAdapterFactory newFactory(final ToNumberStrategy toNumberStrategy) {
        return new TypeAdapterFactory(){

            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                if (type.getRawType() == Object.class) {
                    return new ObjectTypeAdapter(gson, toNumberStrategy);
                }
                return null;
            }
        };
    }

    public static TypeAdapterFactory getFactory(ToNumberStrategy toNumberStrategy) {
        if (toNumberStrategy == ToNumberPolicy.DOUBLE) {
            return DOUBLE_FACTORY;
        }
        return ObjectTypeAdapter.newFactory(toNumberStrategy);
    }

    @Override
    public Object read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        switch (token) {
            case BEGIN_ARRAY: {
                ArrayList<Object> list = new ArrayList<Object>();
                in.beginArray();
                while (in.hasNext()) {
                    list.add(this.read(in));
                }
                in.endArray();
                return list;
            }
            case BEGIN_OBJECT: {
                LinkedTreeMap<String, Object> map = new LinkedTreeMap<String, Object>();
                in.beginObject();
                while (in.hasNext()) {
                    map.put(in.nextName(), this.read(in));
                }
                in.endObject();
                return map;
            }
            case STRING: {
                return in.nextString();
            }
            case NUMBER: {
                return this.toNumberStrategy.readNumber(in);
            }
            case BOOLEAN: {
                return in.nextBoolean();
            }
            case NULL: {
                in.nextNull();
                return null;
            }
        }
        throw new IllegalStateException();
    }

    @Override
    public void write(JsonWriter out, Object value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        TypeAdapter<?> typeAdapter = this.gson.getAdapter(value.getClass());
        if (typeAdapter instanceof ObjectTypeAdapter) {
            out.beginObject();
            out.endObject();
            return;
        }
        typeAdapter.write(out, value);
    }
}

