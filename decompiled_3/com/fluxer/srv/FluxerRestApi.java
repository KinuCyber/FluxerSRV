/*
 * Decompiled with CFR 0.152.
 */
package com.fluxer.srv;

import com.fluxer.srv.lib.gson.Gson;
import com.fluxer.srv.lib.gson.JsonObject;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FluxerRestApi {
    private static final String BASE_URL = "https://api.fluxer.app/v1";
    private static final Gson GSON = new Gson();
    private final String token;
    private final OkHttpClient httpClient;

    public FluxerRestApi(String token, OkHttpClient httpClient) {
        this.token = token;
        this.httpClient = httpClient;
    }

    public void sendMessage(String channelId, String content, boolean announcement) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("content", content);
        if (announcement) {
            body.addProperty("announcement", true);
        }
        this.post("/channels/" + channelId + "/messages", body);
    }

    public void sendMessage(String channelId, String content) throws IOException {
        this.sendMessage(channelId, content, false);
    }

    private void post(String route, JsonObject body) throws IOException {
        this.post(route, body, true);
    }

    private void post(String route, JsonObject body, boolean authorize) throws IOException {
        Request.Builder builder = new Request.Builder().url(BASE_URL + route).post(RequestBody.create(MediaType.parse("application/json"), body.toString()));
        if (authorize) {
            builder.header("Authorization", "Bot " + this.token);
        }
        try (Response response = this.httpClient.newCall(builder.build()).execute();){
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "null";
                throw new IOException("Unexpected code " + response + " | Body: " + errorBody);
            }
        }
    }
}

