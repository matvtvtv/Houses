package com.example.houses.webSocket;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.houses.model.ChatMessage;
import com.example.houses.model.Task;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class StompClient {

    private final Context context;

    private static final String TAG = "StompClient";
    private final String serverUrl = "wss://t7lvb7zl-8080.euw.devtunnels.ms/chat";

    private SharedPreferences preferences;
    private final OkHttpClient client;
    private WebSocket ws;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    private StompListener listener;
    private boolean connected = false;
    private boolean connecting = false;

    public StompClient(Context context) {
        this.context = context.getApplicationContext();
        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(15, TimeUnit.SECONDS)
                .build();
    }

    public interface StompListener {
        void onConnected();
        void onChatMessage(ChatMessage message);
        void onTask(Task task);
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
                connecting = false;
                Log.d(TAG, "WS open");

                String connect =
                        "CONNECT\n" +
                                "accept-version:1.2\n" +
                                "host:t7lvb7zl-8080.euw.devtunnels.ms\n" +
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
                connected = false;
                connecting = false;
                postError(t.getMessage());
                reconnect();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                connected = false;
                reconnect();
            }
        });
    }

    private void reconnect() {
        mainHandler.postDelayed(this::connect, 3000);
    }

    // ---- core frame handling: parse headers and route message by destination ----
    private void handleFrame(String frame) {
        for (String p : frame.split("\u0000")) {
            if (p.trim().isEmpty()) continue;

            String[] lines = p.split("\n", 2);
            String command = lines[0].trim();
            String headersAndBody = lines.length > 1 ? lines[1] : "";

            Log.d(TAG, "Frame command: " + command);

            switch (command) {
                case "CONNECTED":
                    preferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                    connected = true;
                    postConnected();
                    break;


                case "MESSAGE":
                    // headers и body разделены пустой строкой "\n\n"
                    int idx = headersAndBody.indexOf("\n\n");
                    String headersPart = idx != -1 ? headersAndBody.substring(0, idx) : "";
                    String body = idx != -1 ? headersAndBody.substring(idx + 2) : headersAndBody;
                    body = body.trim();

                    Map<String, String> headers = parseHeaders(headersPart);

                    String destination = headers.get("destination");
                    if (destination == null) {
                        Log.w(TAG, "MESSAGE without destination header");
                        break;
                    }

                    try {
                        if (destination.startsWith("/topic/chat/")) {
                            ChatMessage cm = gson.fromJson(body, ChatMessage.class);
                            postChatMessage(cm);
                        } else if (destination.startsWith("/topic/tasks/")) {
                            Task task = gson.fromJson(body, Task.class);
                            postTask(task);
                        } else {
                            Log.d(TAG, "Unhandled destination: " + destination);
                        }
                    } catch (JsonSyntaxException ex) {
                        Log.e(TAG, "JSON parse error: " + ex.getMessage());
                    }
                    break;

                case "ERROR":
                    postError(headersAndBody);
                    break;
            }
        }
    }

    private Map<String, String> parseHeaders(String headersPart) {
        Map<String, String> map = new HashMap<>();
        if (headersPart == null || headersPart.isEmpty()) return map;

        String[] lines = headersPart.split("\n");
        for (String line : lines) {
            int i = line.indexOf(':');
            if (i == -1) continue;
            String key = line.substring(0, i).trim();
            String value = line.substring(i + 1).trim();
            map.put(key, value);
        }
        return map;
    }

    // ---- post helpers ----

    private void postConnected() {
        mainHandler.post(() -> {
            if (listener != null) listener.onConnected();
        });
    }

    private void postChatMessage(ChatMessage m) {
        mainHandler.post(() -> {
            if (listener != null) listener.onChatMessage(m);
        });
    }

    private void postTask(Task t) {
        mainHandler.post(() -> {
            if (listener != null) listener.onTask(t);
        });
    }

    private void postError(String reason) {
        mainHandler.post(() -> {
            if (listener != null) listener.onError(reason);
        });
    }

    // ---- generic subscribe/send ----

    public void subscribe(String destination) {
        if (!connected) return;
        String id = "sub-" + UUID.randomUUID();
        String frame = "SUBSCRIBE\nid:" + id + "\ndestination:" + destination + "\n\n\u0000";
        ws.send(frame);
    }

    public void subscribeToChat(String chatLogin) {
        subscribe("/topic/chat/" + chatLogin);
    }

    public void subscribeToTasks(String chatLogin) {
        subscribe("/topic/tasks/" + chatLogin);
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

    // convenience for tasks
    public void sendTask( String chatLogin, Object taskPayload) {
        send("/app/tasks/" + chatLogin + "/create", taskPayload);
    }

    public void disconnect() {
        if (ws != null) {
            ws.send("DISCONNECT\n\n\u0000");
            ws.close(1000, "bye");
            connected = false;
        }
    }

    // DTO container (оставляем)
    public static class MessageDTO {
        public String sender;
        public String content;
        public String image;

        public MessageDTO(String sender, String content, String image) {
            this.sender = sender;
            this.content = content;
            this.image = image;

        }
    }
}
