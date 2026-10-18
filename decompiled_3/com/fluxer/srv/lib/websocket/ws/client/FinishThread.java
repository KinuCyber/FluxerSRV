/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.websocket.ws.client;

import com.fluxer.srv.lib.websocket.ws.client.ThreadType;
import com.fluxer.srv.lib.websocket.ws.client.WebSocket;
import com.fluxer.srv.lib.websocket.ws.client.WebSocketThread;

class FinishThread
extends WebSocketThread {
    public FinishThread(WebSocket ws) {
        super("FinishThread", ws, ThreadType.FINISH_THREAD);
    }

    public void runMain() {
        this.mWebSocket.finish();
    }
}

