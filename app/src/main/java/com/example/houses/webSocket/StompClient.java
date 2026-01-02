package com.example.houses.webSocket;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.houses.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class StompClient {

    private static final String TAG = "StompClient";
    private final String serverUrl = "wss://t7lvb7zl-8080.euw.devtunnels.ms/chat";

    private final OkHttpClient client;
    private WebSocket ws;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    private StompListener listener;
    private boolean connected = false;
    private boolean connecting = false;

    public StompClient() {
        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(15, TimeUnit.SECONDS)
                .build();
    }

    public interface StompListener {
        void onConnected();
        void onMessage(ChatMessage message);
        void onError(String reason);
    }

    public void setListener(StompListener l) {
        this.listener = l;
    }

    public void connect() {
        if (connected || connecting) return;
        connecting = true;

        Request req = new Request.Builder().url(serverUrl).build();
        ws = client.newWebSocket(req, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d(TAG, "WS open");
                connecting = false;

                String connect =
                        "CONNECT\n" +
                                "accept-version:1.2\n" +
                                "host:t7lvb7zl-8080.euw.devtunnels.ms\n" + // ✅ ДВОЕТОЧИЕ
                                "heart-beat:10000,10000\n\n" +
                                "\u0000";
                webSocket.send(connect);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleFrame(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e(TAG, "WS fail", t);
                postError(t.getMessage());
                connected = false;
                connecting = false;
                reconnect();
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WS closing: " + reason);
                connected = false;
                ws.close(1000, null);
                reconnect();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WS closed: " + reason);
                connected = false;
                reconnect();
            }
        });
    }

    private void reconnect() {
        mainHandler.postDelayed(() -> {
            Log.d(TAG, "Reconnecting...");
            connect();
        }, 3000);
    }

    private void handleFrame(String frame) {
        String[] parts = frame.split("\u0000");
        for (String p : parts) {
            if (p.trim().isEmpty()) continue;
            String[] lines = p.split("\n", 2);
            String command = lines[0].trim();
            String headersAndBody = lines.length > 1 ? lines[1] : "";

            Log.d(TAG, "Frame command: " + command);

            switch (command) {
                case "CONNECTED":
                    connected = true;
                    postConnected();
                    subscribe("/topic/messages");
                    break;

                case "MESSAGE":
                    int idx = headersAndBody.indexOf("\n\n");
                    String body = headersAndBody;
                    if (idx != -1) body = headersAndBody.substring(idx + 2);
                    body = body.trim();
                    try {
                        ChatMessage cm = gson.fromJson(body, ChatMessage.class);
                        postMessage(cm);
                    } catch (JsonSyntaxException ex) {
                        Log.e(TAG, "JSON parse error: " + ex.getMessage());
                    }
                    break;

                case "ERROR":
                    postError("STOMP ERROR: " + headersAndBody);
                    break;
            }
        }
    }

    private void postConnected() {
        mainHandler.post(() -> {
            if (listener != null) listener.onConnected();
        });
    }

    private void postMessage(ChatMessage m) {
        mainHandler.post(() -> {
            if (listener != null) listener.onMessage(m);
        });
    }

    private void postError(String reason) {
        mainHandler.post(() -> {
            if (listener != null) listener.onError(reason);
        });
    }

    public void subscribe(String destination) {
        if (!connected) return;
        String id = "sub-" + UUID.randomUUID();
        String frame = "SUBSCRIBE\nid:" + id + "\ndestination:" + destination + "\n\n\u0000";
        ws.send(frame);
    }

    public void send(String destination, Object payload) {
        if (!connected) {
            Log.w(TAG, "send() called before STOMP CONNECTED");
            return;
        }
        String json = gson.toJson(payload);
        String frame = "SEND\ndestination:" + destination + "\ncontent-type:application/json\n\n" + json + "\u0000";
        ws.send(frame);
    }

    public void disconnect() {
        if (ws != null) {
            ws.send("DISCONNECT\n\n\u0000");
            ws.close(1000, "bye");
            connected = false;
        }
    }
    public static class MessageDTO {
        public String sender;
        public String content;

        public MessageDTO(String sender, String content) {
            this.sender = sender;
            this.content = content;
        }
    }
}
