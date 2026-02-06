package com.example.houses;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateChatActivity extends AppCompatActivity {

    private EditText editChatLogin, editChatName;
    private Button btnCreate, btnJoinChat;
    private ProgressBar progressBar;

    private static final String URL =
            "https://t7lvb7zl-8080.euw.devtunnels.ms/api/chats_data/register";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_chat);

        editChatLogin = findViewById(R.id.editChatLogin);
        editChatName = findViewById(R.id.editChatName);
        btnCreate = findViewById(R.id.btnCreateChat);
        btnJoinChat = findViewById(R.id.btnJoinChat);
        progressBar = findViewById(R.id.progressBar);

        btnCreate.setOnClickListener(v -> createChat());

        btnJoinChat.setOnClickListener(v -> {
            startActivity(new Intent(CreateChatActivity.this, JoinChatActivity.class));
            finish();
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnCreate.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        btnCreate.setEnabled(true);
    }

    private void createChat() {
        String chatLogin = editChatLogin.getText().toString().trim();
        String chatName = editChatName.getText().toString().trim();

        if (chatLogin.isEmpty() || chatName.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String userLogin = prefs.getString("login", "");

        ChatRegisterRequest requestObj = new ChatRegisterRequest(
                chatLogin,
                chatName,
                userLogin,
                "PARENT"
        );

        String json = gson.toJson(requestObj);

        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(CreateChatActivity.this,
                            "Ошибка сервера",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    hideLoading();

                    if (response.isSuccessful()) {
                        Toast.makeText(CreateChatActivity.this,
                                "Чат успешно создан",
                                Toast.LENGTH_SHORT).show();

                        SharedPreferences prefs =
                                getSharedPreferences("AppPrefs", MODE_PRIVATE);

                        prefs.edit()
                                .putString("chatLogin", chatLogin)
                                .apply();

                        startActivity(new Intent(CreateChatActivity.this, MainActivity.class));
                        finish();

                    } else {
                        Toast.makeText(CreateChatActivity.this,
                                "Чат уже существует",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // DTO
    static class ChatRegisterRequest {
        String chatLogin;
        String chatName;
        String userLogin;
        String userRole;

        public ChatRegisterRequest(String chatLogin, String chatName,
                                   String userLogin, String userRole) {
            this.chatLogin = chatLogin;
            this.chatName = chatName;
            this.userLogin = userLogin;
            this.userRole = userRole;
        }
    }
}
