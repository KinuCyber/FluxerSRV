/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv.lib.websocket.ws.client;

import com.fluxer.srv.lib.websocket.ws.client.ListenerManager;
import com.fluxer.srv.lib.websocket.ws.client.ThreadType;
import com.fluxer.srv.lib.websocket.ws.client.WebSocket;
import com.fluxer.srv.lib.websocket.ws.client.WebSocketException;
import com.fluxer.srv.lib.websocket.ws.client.WebSocketThread;

class ConnectThread
extends WebSocketThread {
    public ConnectThread(WebSocket ws) {
        super("ConnectThread", ws, ThreadType.CONNECT_THREAD);
    }

    public void runMain() {
        try {
            this.mWebSocket.connect();
        }
        catch (WebSocketException e) {
            this.handleError(e);
        }
    }

    private void handleError(WebSocketException cause) {
        ListenerManager manager = this.mWebSocket.getListenerManager();
        manager.callOnError(cause);
        manager.callOnConnectError(cause);
    }
}

