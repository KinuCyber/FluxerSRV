/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.gson;

import com.fluxer.srv.lib.gson.FieldAttributes;

public interface ExclusionStrategy {
    public boolean shouldSkipField(FieldAttributes var1);

    public boolean shouldSkipClass(Class<?> var1);
}

