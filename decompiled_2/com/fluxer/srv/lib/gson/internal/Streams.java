/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson.internal;

import com.fluxer.srv.lib.gson.JsonElement;
import com.fluxer.srv.lib.gson.JsonIOException;
import com.fluxer.srv.lib.gson.JsonNull;
import com.fluxer.srv.lib.gson.JsonParseException;
import com.fluxer.srv.lib.gson.JsonSyntaxException;
import com.fluxer.srv.lib.gson.internal.bind.TypeAdapters;
import com.fluxer.srv.lib.gson.stream.JsonReader;
import com.fluxer.srv.lib.gson.stream.JsonWriter;
import com.fluxer.srv.lib.gson.stream.MalformedJsonException;
import java.io.EOFException;
import java.io.IOException;
import java.io.Writer;

public final class Streams {
    private Streams() {
        throw new UnsupportedOperationException();
    }

    public static JsonElement parse(JsonReader reader) throws JsonParseException {
        boolean isEmpty = true;
        try {
            reader.peek();
            isEmpty = false;
            return TypeAdapters.JSON_ELEMENT.read(reader);
        }
        catch (EOFException e) {
            if (isEmpty) {
                return JsonNull.INSTANCE;
            }
            throw new JsonSyntaxException(e);
        }
        catch (MalformedJsonException e) {
            throw new JsonSyntaxException(e);
        }
        catch (IOException e) {
            throw new JsonIOException(e);
        }
        catch (NumberFormatException e) {
            throw new JsonSyntaxException(e);
        }
    }

    public static void write(JsonElement element, JsonWriter writer) throws IOException {
        TypeAdapters.JSON_ELEMENT.write(writer, element);
    }

    public static Writer writerForAppendable(Appendable appendable) {
        return appendable instanceof Writer ? (Writer)appendable : new AppendableWriter(appendable);
    }

    private static final class AppendableWriter
    extends Writer {
        private final Appendable appendable;
        private final CurrentWrite currentWrite = new CurrentWrite();

        AppendableWriter(Appendable appendable) {
            this.appendable = appendable;
        }

        @Override
        public void write(char[] chars, int offset, int length) throws IOException {
            this.currentWrite.chars = chars;
            this.appendable.append(this.currentWrite, offset, offset + length);
        }

        @Override
        public void write(int i) throws IOException {
            this.appendable.append((char)i);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        static class CurrentWrite
        implements CharSequence {
            char[] chars;

            CurrentWrite() {
            }

            @Override
            public int length() {
                return this.chars.length;
            }

            @Override
            public char charAt(int i) {
                return this.chars[i];
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return new String(this.chars, start, end - start);
            }
        }
    }
}

