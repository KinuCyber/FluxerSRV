/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.websocket.ws.client;

import com.fluxer.srv.lib.websocket.ws.client.WebSocketError;
import com.fluxer.srv.lib.websocket.ws.client.WebSocketException;

class NoMoreFrameException
extends WebSocketException {
    private static final long serialVersionUID = 1L;

    public NoMoreFrameException() {
        super(WebSocketError.NO_MORE_FRAME, "No more WebSocket frame from the server.");
    }
}

