/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson;

import com.fluxer.srv.lib.gson.JsonElement;
import com.fluxer.srv.lib.gson.JsonIOException;
import com.fluxer.srv.lib.gson.JsonParseException;
import com.fluxer.srv.lib.gson.JsonSyntaxException;
import com.fluxer.srv.lib.gson.internal.Streams;
import com.fluxer.srv.lib.gson.stream.JsonReader;
import com.fluxer.srv.lib.gson.stream.JsonToken;
import com.fluxer.srv.lib.gson.stream.MalformedJsonException;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class JsonStreamParser
implements Iterator<JsonElement> {
    private final JsonReader parser;
    private final Object lock;

    public JsonStreamParser(String json) {
        this(new StringReader(json));
    }

    public JsonStreamParser(Reader reader) {
        this.parser = new JsonReader(reader);
        this.parser.setLenient(true);
        this.lock = new Object();
    }

    @Override
    public JsonElement next() throws JsonParseException {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        try {
            return Streams.parse(this.parser);
        }
        catch (StackOverflowError e) {
            throw new JsonParseException("Failed parsing JSON source to Json", e);
        }
        catch (OutOfMemoryError e) {
            throw new JsonParseException("Failed parsing JSON source to Json", e);
        }
        catch (JsonParseException e) {
            throw e.getCause() instanceof EOFException ? new NoSuchElementException() : e;
        }
    }

    @Override
    public boolean hasNext() {
        Object object = this.lock;
        synchronized (object) {
            try {
                return this.parser.peek() != JsonToken.END_DOCUMENT;
            }
            catch (MalformedJsonException e) {
                throw new JsonSyntaxException(e);
            }
            catch (IOException e) {
                throw new JsonIOException(e);
            }
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

