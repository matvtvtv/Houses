package com.example.houses;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.adapter.ChatAdapter;
import com.example.houses.model.ChatModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;       // <- правильный

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    RecyclerView rvChats;
    FloatingActionButton fabCreate;
    Button button;
    ChatAdapter adapter;
    List<ChatModel> chats = new ArrayList<>();

    String login;
    String role;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        boolean isFirstRun = preferences.getBoolean("isFirstRun", true);
        if (isFirstRun) { startActivity(new Intent(this, RegistrationActivity.class));
            editor.putBoolean("isFirstRun", false);
            editor.apply();
        }
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        login = prefs.getString("login", "");
        role = prefs.getString("role", "");


        rvChats = findViewById(R.id.rvChats);
        fabCreate = findViewById(R.id.fabCreateChat);
        button = findViewById(R.id.button);

        rvChats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(chats, chat -> openChat(chat));
        rvChats.setAdapter(adapter);



        fabCreate.setOnClickListener(v -> {
            if(role=="PARENT"){
            startActivity(new Intent(this, CreateChatActivity.class));
            finish();}
            else {
                startActivity(new Intent(this, JoinChatActivity.class));
                finish();
            }
        });
        button.setOnClickListener(v -> {

                startActivity(new Intent(this, RegistrationActivity.class));
                finish();

        });
        loadChats();
        editor.apply();
    }
    private void loadChats() {
        String url = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/chats_data/get_chats/" + login;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<ChatModel>>(){}.getType();
                    List<ChatModel> result =
                            gson.fromJson(response.body().string(), type);

                    runOnUiThread(() -> {
                        chats.clear();
                        chats.addAll(result);
                        adapter.notifyDataSetChanged();
                    });
                }
            }
        });
    }

    private void openChat(ChatModel chat) {
        Intent i = new Intent(this, TaskActivity.class);
        preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("chatLogin", chat.getChatLogin());
        editor.apply();

        startActivity(i);
        finish();
    }
}