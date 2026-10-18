/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv;

import com.fluxer.srv.FluxerListener;
import com.fluxer.srv.FluxerRestApi;
import com.fluxer.srv.lib.gson.Gson;
import com.fluxer.srv.lib.gson.JsonObject;
import com.fluxer.srv.lib.websocket.ws.client.WebSocket;
import com.fluxer.srv.lib.websocket.ws.client.WebSocketAdapter;
import com.fluxer.srv.lib.websocket.ws.client.WebSocketException;
import com.fluxer.srv.lib.websocket.ws.client.WebSocketFactory;
import com.fluxer.srv.lib.websocket.ws.client.WebSocketFrame;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;

public class FluxerClient {
    private static final String GATEWAY_URL = "wss://gateway.fluxer.app/?v=1&encoding=json";
    private static final Gson GSON = new Gson();
    private final String token;
    private WebSocket ws;
    private final FluxerRestApi restApi;
    private int sequence = 0;
    private String sessionId;
    private Timer heartbeatTimer;
    private FluxerListener listener;
    private final Logger logger;

    public FluxerClient(String token, Logger logger, OkHttpClient httpClient) {
        this.token = token;
        this.logger = logger;
        this.restApi = new FluxerRestApi(token, httpClient);
    }

    public void setListener(FluxerListener listener) {
        this.listener = listener;
    }

    public void connect() throws Exception {
        this.ws = new WebSocketFactory().setConnectionTimeout(5000).createSocket(GATEWAY_URL).addListener(new WebSocketAdapter(){

            @Override
            public void onTextMessage(WebSocket websocket, String text) throws Exception {
                FluxerClient.this.handleMessage(text);
            }

            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                FluxerClient.this.logger.warning("[Fluxer] Disconnected from gateway. Attempting to reconnect...");
                FluxerClient.this.stopHeartbeat();
            }

            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                FluxerClient.this.logger.severe("[Fluxer] Gateway error: " + cause.getMessage());
            }
        }).connect();
    }

    private void handleMessage(String text) {
        JsonObject packet = GSON.fromJson(text, JsonObject.class);
        if (!packet.has("op")) {
            return;
        }
        int op = packet.get("op").getAsInt();
        if (packet.has("s") && !packet.get("s").isJsonNull()) {
            this.sequence = packet.get("s").getAsInt();
        }
        switch (op) {
            case 10: {
                int heartbeatInterval = packet.getAsJsonObject("d").get("heartbeat_interval").getAsInt();
                this.startHeartbeat(heartbeatInterval);
                this.identify();
                break;
            }
            case 0: {
                String type = packet.get("t").getAsString();
                this.handleDispatch(type, packet.getAsJsonObject("d"));
                break;
            }
        }
    }

    private void identify() {
        JsonObject payload = new JsonObject();
        payload.addProperty("op", 2);
        JsonObject d = new JsonObject();
        d.addProperty("token", this.token.replace("Bot ", ""));
        JsonObject properties = new JsonObject();
        properties.addProperty("os", "linux");
        properties.addProperty("browser", "fluxer-srv");
        properties.addProperty("device", "fluxer-srv");
        d.add("properties", properties);
        payload.add("d", d);
        this.ws.sendText(payload.toString());
    }

    private void startHeartbeat(int interval) {
        this.stopHeartbeat();
        this.heartbeatTimer = new Timer(true);
        this.heartbeatTimer.scheduleAtFixedRate(new TimerTask(){

            @Override
            public void run() {
                JsonObject heartbeat = new JsonObject();
                heartbeat.addProperty("op", 1);
                heartbeat.add("d", FluxerClient.this.sequence == 0 ? null : GSON.toJsonTree(FluxerClient.this.sequence));
                FluxerClient.this.ws.sendText(heartbeat.toString());
            }
        }, interval, (long)interval);
    }

    private void stopHeartbeat() {
        if (this.heartbeatTimer != null) {
            this.heartbeatTimer.cancel();
            this.heartbeatTimer = null;
        }
    }

    private void handleDispatch(String type, JsonObject data) {
        if (type.equals("READY")) {
            this.sessionId = data.get("session_id").getAsString();
            this.logger.info("[Fluxer] Logged in as " + data.getAsJsonObject("user").get("username").getAsString());
        } else if (type.equals("MESSAGE_CREATE") && this.listener != null) {
            this.listener.onMessageCreate(data);
        }
    }

    public FluxerRestApi getRestApi() {
        return this.restApi;
    }

    public void shutdown() {
        this.stopHeartbeat();
        if (this.ws != null) {
            this.ws.disconnect();
        }
    }
}

