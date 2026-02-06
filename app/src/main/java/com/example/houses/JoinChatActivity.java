package com.example.houses;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
    private Button btnJoin;

    private String userLogin;
    private ProgressBar progressJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_chat);

        etChatLogin = findViewById(R.id.etChatLogin);
        btnJoin = findViewById(R.id.btnJoin);
        progressJoin = findViewById(R.id.progressJoin);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        userLogin = prefs.getString("login", "");

        btnJoin.setOnClickListener(v -> joinChat());
    }

    private void showLoading() {
        progressJoin.setVisibility(View.VISIBLE);
        btnJoin.setEnabled(false);
    }

    private void hideLoading() {
        progressJoin.setVisibility(View.GONE);
        btnJoin.setEnabled(true);
    }

    private void joinChat() {

        String chatLogin = etChatLogin.getText().toString().trim();

        if (chatLogin.isEmpty()) {
            Toast.makeText(this, "Введите логин чата", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(); // ← запускаем статус загрузки

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
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(
                                JoinChatActivity.this,
                                "Ошибка подключения к серверу",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) {
                    runOnUiThread(() -> {
                        hideLoading();

                        if (response.isSuccessful()) {

                            SharedPreferences prefs =
                                    getSharedPreferences("AppPrefs", MODE_PRIVATE);

                            prefs.edit()
                                    .putString("chatLogin", chatLogin)
                                    .apply();

                            Toast.makeText(
                                    JoinChatActivity.this,
                                    "Вы успешно вошли в чат",
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
                                    "Чат не найден или вы уже подключены",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            hideLoading();
            Toast.makeText(this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
