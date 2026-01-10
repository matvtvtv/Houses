package com.example.houses;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

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

public class RegistrationActivity extends AppCompatActivity {

    EditText etLogin, etName, etPassword;
    private Button btnRegister;
    private RadioGroup rgRole;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private static final String BASE_URL ="https://t7lvb7zl-8080.euw.devtunnels.ms/api/user/register";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        etLogin = findViewById(R.id.etLogin);
        etName = findViewById(R.id.etName);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        rgRole = findViewById(R.id.rgRole);


        btnRegister.setOnClickListener(v -> register());

    }

    private void register() {
        String login = etLogin.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();



        if (login.isEmpty() || name.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = rgRole.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Choose role", Toast.LENGTH_SHORT).show();
            return;
        }

        String role;
        if (selectedId == R.id.rbParent) {
            role = "PARENT";
        } else {
            role = "CHILD";
        }

        RegisterRequest dto = new RegisterRequest(login, name, password, role);

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
                        Toast.makeText(RegistrationActivity.this,
                                "Server error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
                Log.v("RedistationActivity", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {

                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("login", login)
                            .putString("role", role)
                            .apply();

                    runOnUiThread(() -> {
                        Toast.makeText(RegistrationActivity.this,
                                "Registration successful " ,
                                Toast.LENGTH_SHORT).show();
                        Toast.makeText(RegistrationActivity.this,
                                role,
                                Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(
                                RegistrationActivity.this,
                                MainActivity.class
                        ));
                        finish();
                    });

                } else {
                    runOnUiThread(() ->
                            Toast.makeText(RegistrationActivity.this,
                                    "User already exists",
                                    Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

}
