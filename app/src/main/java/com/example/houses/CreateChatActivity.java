package com.example.houses;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
    private Button btnCreate,btnJoinChat;

    private static final String URL = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/chats_data/register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_chat);

        editChatLogin = findViewById(R.id.editChatLogin);
        editChatName = findViewById(R.id.editChatName);
        btnCreate = findViewById(R.id.btnCreateChat);
        btnJoinChat = findViewById(R.id.btnJoinChat);


        btnCreate.setOnClickListener(v -> createChat());
        btnCreate.setOnClickListener(v -> {
            startActivity(new Intent(CreateChatActivity.this, JoinChatActivity.class));

        });
    }

    private void createChat() {
        String chatLogin = editChatLogin.getText().toString().trim();
        String chatName = editChatName.getText().toString().trim();

        if (chatLogin.isEmpty() || chatName.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String userLogin = prefs.getString("login", "");

        ChatRegisterRequest requestObj = new ChatRegisterRequest(
                chatLogin,
                chatName,
                userLogin,
                "PARENT"
        );

        Gson gson = new Gson();
        String json = gson.toJson(requestObj);

        RequestBody body = RequestBody.create(
                json, MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(CreateChatActivity.this,
                                "Server error", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(CreateChatActivity.this,
                                "Chat created", Toast.LENGTH_SHORT).show();
                        SharedPreferences prefs =
                                getSharedPreferences("AppPrefs", MODE_PRIVATE);

                        prefs.edit()
                                .putString("chatLogin", chatLogin)
                                .apply();

                        startActivity(new Intent(CreateChatActivity.this, MainActivity.class));

                        finish();
                    } else {
                        Toast.makeText(CreateChatActivity.this,
                                "Chat already exists", Toast.LENGTH_SHORT).show();
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
