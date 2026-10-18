/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson;

import com.fluxer.srv.lib.gson.JsonParseException;

public final class JsonIOException
extends JsonParseException {
    private static final long serialVersionUID = 1L;

    public JsonIOException(String msg) {
        super(msg);
    }

    public JsonIOException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public JsonIOException(Throwable cause) {
        super(cause);
    }
}

