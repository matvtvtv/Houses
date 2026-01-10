package com.example.houses;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.DB.DatabaseHelper;
import com.example.houses.adapter.ChatMessageAdapter;
import com.example.houses.model.ChatMessage;
import com.example.houses.webSocket.StompClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;



public class ChatMassageActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private static final String SERVER_HTTP_HISTORY = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/chat/history/";

    private ChatMessageAdapter adapter;
    private StompClient stompClient;
    private OkHttpClient httpClient;
    private Gson gson = new Gson();

    private EditText editMessage;
    private Button btnSend;
    private Button button_task, buttonReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_message);


        preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);


        RecyclerView rv = findViewById(R.id.recyclerMessages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        String login = preferences.getString("login", "account");
        String chatLogin = preferences.getString("chatLogin", "1");
        adapter = new ChatMessageAdapter(login);
        rv.setAdapter(adapter);

        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        button_task = findViewById(R.id.button_task);



        btnSend.setEnabled(false); // запрещаем до подключения STOMP
        button_task.setOnClickListener(v -> {
            Intent intent = new Intent(this, TaskActivity.class);
            startActivity(intent);
        });


        httpClient= new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        loadHistory();

        stompClient = new StompClient(this);

        stompClient.setListener(new StompClient.StompListener() {
            @Override
            public void onConnected() {
                Log.d("ChatMassageActivity", "STOMP connected");
                stompClient.subscribeToChat(chatLogin);

                runOnUiThread(() -> btnSend.setEnabled(true));
            }


            @Override
            public void onChatMessage(ChatMessage message) {
                runOnUiThread(() -> {
                    adapter.addMessage(message);
                    rv.smoothScrollToPosition(adapter.getItemCount() - 1);
                });
            }

            @Override
            public void onTask(com.example.houses.model.Task task) {

            }

            @Override
            public void onError(String reason) {
                Log.e("ChatMassageActivity", "STOMP error: " + reason);
            }
        });

        stompClient.connect();

        btnSend.setOnClickListener(v -> {
            String text = editMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            byte[] avatarBytes = DatabaseHelper.getInstance(this).getUserAvatar(login);

            String avatarBase64 = null;
            if (avatarBytes != null) {
                avatarBase64 = android.util.Base64.encodeToString(avatarBytes, android.util.Base64.DEFAULT);
            }
            Log.e("ChatMassageActivity", "S: " + avatarBase64);

            StompClient.MessageDTO payload = new StompClient.MessageDTO(login, text, avatarBase64);
            stompClient.send("/app/chat/" + preferences.getString("chatLogin","") + "/send", payload);
            editMessage.setText("");
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stompClient != null) stompClient.disconnect();
    }

    private void loadHistory() {
        Request req = new Request.Builder().url(SERVER_HTTP_HISTORY+preferences.getString("chatLogin","3")).build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("ChatMassageActivity", "history fail", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("ChatMassageActivity", "history not successful: " + response.code());
                    return;
                }
                ResponseBody bodyObj = response.body();
                if (bodyObj == null) return;

                String body = bodyObj.string();
                Type listType = new TypeToken<List<ChatMessage>>() {}.getType();
                final List<ChatMessage> list;

                try {
                    list = gson.fromJson(body, listType);
                } catch (Exception ex) {
                    Log.e("ChatMassageActivity", "JSON parse error", ex);
                    return;
                }
                if (list == null) return;
                Log.e("ChatMassageActivity", list.get(1).getImage());
                runOnUiThread(() -> adapter.setAll(list));
            }
        });
    }

}
