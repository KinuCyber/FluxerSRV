/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson.internal;

public final class $Gson$Preconditions {
    private $Gson$Preconditions() {
        throw new UnsupportedOperationException();
    }

    public static <T> T checkNotNull(T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return obj;
    }

    public static void checkArgument(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }
}

