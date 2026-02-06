package com.example.houses;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.houses.model.LoginRequest;
import com.example.houses.model.UserModel;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    EditText etLogin, etPassword;
    Button btnRegister, btnEnterance;
    ProgressBar progressBar;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private static final String LOGIN_URL =
            "https://t7lvb7zl-8080.euw.devtunnels.ms/api/user/login";

    private static final String USER_INFO_URL =
            "https://t7lvb7zl-8080.euw.devtunnels.ms/api/user/enter/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnEnterance = findViewById(R.id.btnEnterance);
        progressBar = findViewById(R.id.progressBar);

        btnEnterance.setOnClickListener(v -> login());

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegistrationActivity.class));
            finish();
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnEnterance.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        btnEnterance.setEnabled(true);
    }

    private void login() {
        String login = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();

        LoginRequest dto = new LoginRequest(login, password);
        String json = gson.toJson(dto);

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(LoginActivity.this,
                            "Ошибка сервера: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(LoginActivity.this,
                                "Неверный логин или пароль",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Логин прошёл — теперь получаем роль
                loadUserRole(login);
            }
        });
    }

    private void loadUserRole(String login) {
        Request request = new Request.Builder()
                .url(USER_INFO_URL + login)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(LoginActivity.this,
                            "Не удалось загрузить данные пользователя",
                            Toast.LENGTH_SHORT).show();
                });
                Log.e("LoginActivity", "Role load failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(LoginActivity.this,
                                "Ошибка получения роли пользователя",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String body = response.body().string();
                UserModel user = gson.fromJson(body, UserModel.class);

                String role = user.getRole();

                SharedPreferences prefs =
                        getSharedPreferences("AppPrefs", MODE_PRIVATE);

                prefs.edit()
                        .putString("login", login)
                        .putString("role", role)
                        .apply();

                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(LoginActivity.this,
                            "Вход выполнен успешно (" + role + ")",
                            Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(
                            LoginActivity.this,
                            MainActivity.class
                    ));
                    finish();
                });
            }
        });
    }
}
