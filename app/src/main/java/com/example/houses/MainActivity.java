package com.example.houses;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.adapter.ChatAdapter;
import com.example.houses.model.ChatMessage;
import com.example.houses.webSocket.StompClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;



public class MainActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private static final String SERVER_HTTP_HISTORY = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/chat/history";

    private ChatAdapter adapter;
    private StompClient stompClient;
    private OkHttpClient httpClient;
    private Gson gson = new Gson();

    private EditText editMessage;
    private Button btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("login", "TEL2");
        editor.apply();

        RecyclerView rv = findViewById(R.id.recyclerMessages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        String login = preferences.getString("login", "account");
        adapter = new ChatAdapter(login);
        rv.setAdapter(adapter);

        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        btnSend.setEnabled(false); // запрещаем до подключения STOMP

        httpClient = new OkHttpClient();




        loadHistory();

        stompClient = new StompClient();
        stompClient.setListener(new StompClient.StompListener() {
            @Override
            public void onConnected() {
                Log.d("MainActivity", "STOMP connected");
                runOnUiThread(() -> btnSend.setEnabled(true));
            }

            @Override
            public void onMessage(ChatMessage message) {
                runOnUiThread(() -> {
                    adapter.addMessage(message);
                    rv.smoothScrollToPosition(adapter.getItemCount() - 1);
                });
            }

            @Override
            public void onError(String reason) {
                Log.e("MainActivity", "STOMP error: " + reason);
            }
        });

        stompClient.connect();

        btnSend.setOnClickListener((View v) -> {
            String text = editMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            StompClient.MessageDTO payload = new StompClient.MessageDTO(login, text);
            stompClient.send("/app/send", payload);
            editMessage.setText("");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stompClient != null) stompClient.disconnect();
    }

    private void loadHistory() {
        Request req = new Request.Builder().url(SERVER_HTTP_HISTORY).build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MainActivity", "history fail", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("MainActivity", "history not successful: " + response.code());
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
                    Log.e("MainActivity", "JSON parse error", ex);
                    return;
                }
                if (list == null) return;

                runOnUiThread(() -> adapter.setAll(list));
            }
        });
    }
}
