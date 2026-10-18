/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.websocket.ws.client;

import com.fluxer.srv.lib.websocket.ws.client.Huffman;

class FixedDistanceHuffman
extends Huffman {
    private static final FixedDistanceHuffman INSTANCE = new FixedDistanceHuffman();

    private FixedDistanceHuffman() {
        super(FixedDistanceHuffman.buildCodeLensFromSym());
    }

    private static int[] buildCodeLensFromSym() {
        int[] codeLengths = new int[32];
        for (int symbol = 0; symbol < 32; ++symbol) {
            codeLengths[symbol] = 5;
        }
        return codeLengths;
    }

    public static FixedDistanceHuffman getInstance() {
        return INSTANCE;
    }
}

