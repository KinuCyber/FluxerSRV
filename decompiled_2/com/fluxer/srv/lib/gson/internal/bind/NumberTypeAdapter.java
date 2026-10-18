/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson.internal.bind;

import com.fluxer.srv.lib.gson.Gson;
import com.fluxer.srv.lib.gson.JsonSyntaxException;
import com.fluxer.srv.lib.gson.ToNumberPolicy;
import com.fluxer.srv.lib.gson.ToNumberStrategy;
import com.fluxer.srv.lib.gson.TypeAdapter;
import com.fluxer.srv.lib.gson.TypeAdapterFactory;
import com.fluxer.srv.lib.gson.reflect.TypeToken;
import com.fluxer.srv.lib.gson.stream.JsonReader;
import com.fluxer.srv.lib.gson.stream.JsonToken;
import com.fluxer.srv.lib.gson.stream.JsonWriter;
import java.io.IOException;

public final class NumberTypeAdapter
extends TypeAdapter<Number> {
    private static final TypeAdapterFactory LAZILY_PARSED_NUMBER_FACTORY = NumberTypeAdapter.newFactory(ToNumberPolicy.LAZILY_PARSED_NUMBER);
    private final ToNumberStrategy toNumberStrategy;

    private NumberTypeAdapter(ToNumberStrategy toNumberStrategy) {
        this.toNumberStrategy = toNumberStrategy;
    }

    private static TypeAdapterFactory newFactory(ToNumberStrategy toNumberStrategy) {
        NumberTypeAdapter adapter = new NumberTypeAdapter(toNumberStrategy);
        return new TypeAdapterFactory(){

            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                return type.getRawType() == Number.class ? NumberTypeAdapter.this : null;
            }
        };
    }

    public static TypeAdapterFactory getFactory(ToNumberStrategy toNumberStrategy) {
        if (toNumberStrategy == ToNumberPolicy.LAZILY_PARSED_NUMBER) {
            return LAZILY_PARSED_NUMBER_FACTORY;
        }
        return NumberTypeAdapter.newFactory(toNumberStrategy);
    }

    @Override
    public Number read(JsonReader in) throws IOException {
        JsonToken jsonToken = in.peek();
        switch (jsonToken) {
            case NULL: {
                in.nextNull();
                return null;
            }
            case NUMBER: 
            case STRING: {
                return this.toNumberStrategy.readNumber(in);
            }
        }
        throw new JsonSyntaxException("Expecting number, got: " + (Object)((Object)jsonToken));
    }

    @Override
    public void write(JsonWriter out, Number value) throws IOException {
        out.value(value);
    }
}

