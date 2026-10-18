/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson;

import com.fluxer.srv.lib.gson.JsonParseException;

public final class JsonSyntaxException
extends JsonParseException {
    private static final long serialVersionUID = 1L;

    public JsonSyntaxException(String msg) {
        super(msg);
    }

    public JsonSyntaxException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public JsonSyntaxException(Throwable cause) {
        super(cause);
    }
}

