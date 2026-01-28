package com.example.houses;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.houses.model.LoginRequest;
import com.example.houses.model.RegisterRequest;
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
    private Button btnRegister,btnEnterance;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private static final String BASE_URL ="https://t7lvb7zl-8080.euw.devtunnels.ms/api/user/login";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnEnterance = findViewById(R.id.btnEnterance);



        btnEnterance.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(
                    LoginActivity.this,
                    RegistrationActivity.class
            ));
            finish();

        });
    }


    public void login(){
        String login = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        LoginRequest dto = new LoginRequest(login,password);

        String json = gson.toJson(dto);

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this,
                                "Server error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
                Log.v("LoginActivity", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {

                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("login", login)
                            .apply();

                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this,
                                "Entrance successful " ,
                                Toast.LENGTH_SHORT).show();


                        startActivity(new Intent(
                                LoginActivity.this,
                                MainActivity.class
                        ));
                        finish();
                    });

                } else {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this,
                                    "Invalid login or password",
                                    Toast.LENGTH_SHORT).show());
                }
            }
        });



    }




}