/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson.internal.sql;

import com.fluxer.srv.lib.gson.Gson;
import com.fluxer.srv.lib.gson.JsonSyntaxException;
import com.fluxer.srv.lib.gson.TypeAdapter;
import com.fluxer.srv.lib.gson.TypeAdapterFactory;
import com.fluxer.srv.lib.gson.reflect.TypeToken;
import com.fluxer.srv.lib.gson.stream.JsonReader;
import com.fluxer.srv.lib.gson.stream.JsonToken;
import com.fluxer.srv.lib.gson.stream.JsonWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

final class SqlDateTypeAdapter
extends TypeAdapter<Date> {
    static final TypeAdapterFactory FACTORY = new TypeAdapterFactory(){

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            return typeToken.getRawType() == Date.class ? new SqlDateTypeAdapter() : null;
        }
    };
    private final DateFormat format = new SimpleDateFormat("MMM d, yyyy");

    private SqlDateTypeAdapter() {
    }

    @Override
    public synchronized Date read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        try {
            long utilDate = this.format.parse(in.nextString()).getTime();
            return new Date(utilDate);
        }
        catch (ParseException e) {
            throw new JsonSyntaxException(e);
        }
    }

    @Override
    public synchronized void write(JsonWriter out, Date value) throws IOException {
        out.value(value == null ? null : this.format.format(value));
    }
}

