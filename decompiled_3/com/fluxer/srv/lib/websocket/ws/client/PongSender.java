/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.websocket.ws.client;

import com.fluxer.srv.lib.websocket.ws.client.PayloadGenerator;
import com.fluxer.srv.lib.websocket.ws.client.PeriodicalFrameSender;
import com.fluxer.srv.lib.websocket.ws.client.WebSocket;
import com.fluxer.srv.lib.websocket.ws.client.WebSocketFrame;

class PongSender
extends PeriodicalFrameSender {
    private static final String TIMER_NAME = "PongSender";

    public PongSender(WebSocket webSocket, PayloadGenerator generator) {
        super(webSocket, TIMER_NAME, generator);
    }

    protected WebSocketFrame createFrame(byte[] payload) {
        return WebSocketFrame.createPongFrame(payload);
    }
}

