package com.example.houses;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JoinChatActivity extends AppCompatActivity {

    private EditText etChatLogin;
    private Button btnJoin , button;

    private String userLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_chat);

        etChatLogin = findViewById(R.id.etChatLogin);
        btnJoin = findViewById(R.id.btnJoin);
        button = findViewById(R.id.button);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        userLogin = prefs.getString("login", "");

        btnJoin.setOnClickListener(v -> joinChat());
        button.setOnClickListener(v -> {startActivity(new Intent(JoinChatActivity.this, MainActivity.class));
                    finish();
        }
        );
    }

    private void joinChat() {
        String chatLogin = etChatLogin.getText().toString().trim();

        if (chatLogin.isEmpty()) {
            Toast.makeText(this, "Enter chat login", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("chatLogin", chatLogin);
            json.put("userLogin", userLogin);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://t7lvb7zl-8080.euw.devtunnels.ms/api/chats_data/join")
                    .post(body)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(JoinChatActivity.this,
                                    "Connection error",
                                    Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(
                                    JoinChatActivity.this,
                                    "Joined successfully",
                                    Toast.LENGTH_SHORT
                            ).show();

                            startActivity(new Intent(
                                    JoinChatActivity.this,
                                    MainActivity.class
                            ));
                            finish();
                        } else {
                            Toast.makeText(
                                    JoinChatActivity.this,
                                    "Chat not found or already joined",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
