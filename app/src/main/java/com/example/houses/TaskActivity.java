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

import com.example.houses.adapter.TaskAdapter;
import com.example.houses.model.Task;
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

public class TaskActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private static final String SERVER_HTTP_TASKS = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/tasks/";

    private TaskAdapter adapter;
    private StompClient stompClient;
    private OkHttpClient httpClient;
    private Gson gson = new Gson();

    private EditText editTitle, editDesc;
    private Button btnCreate;
    private Button button_rec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task); // создадим пример разметки ниже

        preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String chatId = preferences.getString("chatId", "1");

        RecyclerView rv = findViewById(R.id.recyclerTasks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter();
        rv.setAdapter(adapter);

        editTitle = findViewById(R.id.editTaskTitle);
        editDesc = findViewById(R.id.editTaskDesc);
        btnCreate = findViewById(R.id.btnCreateTask);
        button_rec = findViewById(R.id.button_rec);

        button_rec.setOnClickListener(v -> {
            Intent intent = new Intent(this, TaskActivity.class);
            startActivity(intent);
        });


        httpClient = new OkHttpClient();

        loadTasks(chatId);

        stompClient = new StompClient(this);
        stompClient.setListener(new StompClient.StompListener() {
            @Override
            public void onConnected() {
                Log.d("TasksActivity", "WS connected");
                stompClient.subscribeToTasks(chatId);
            }

            @Override
            public void onChatMessage(com.example.houses.model.ChatMessage message) {
                // не используем на экране задач
            }

            @Override
            public void onTask(Task task) {
                runOnUiThread(() -> {
                    adapter.updateTask(task);
                });
            }

            @Override
            public void onError(String reason) {
                Log.e("TasksActivity", "WS error: " + reason);
            }
        });

        stompClient.connect();

        btnCreate.setOnClickListener((View v) -> {
            String t = editTitle.getText().toString().trim();
            String d = editDesc.getText().toString().trim();
            if (t.isEmpty()) return;

            Task newTask = new Task();
            newTask.setChatId(chatId);
            newTask.setTitle(t);
            newTask.setDescription(d);
            newTask.setCompleted(false);

            // отправляем через STOMP -> backend @MessageMapping("/tasks/{chatId}/create")
            stompClient.sendTask(chatId, newTask);

            editTitle.setText("");
            editDesc.setText("");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stompClient != null) stompClient.disconnect();
    }

    private void loadTasks(String chatId) {
        Request req = new Request.Builder().url(SERVER_HTTP_TASKS + chatId).build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("TasksActivity", "tasks history fail", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("TasksActivity", "tasks history not successful: " + response.code());
                    return;
                }
                ResponseBody bodyObj = response.body();
                if (bodyObj == null) return;

                String body = bodyObj.string();
                Type listType = new TypeToken<List<Task>>() {}.getType();
                final List<Task> list;
                try {
                    list = gson.fromJson(body, listType);
                } catch (Exception ex) {
                    Log.e("TasksActivity", "JSON parse error", ex);
                    return;
                }
                if (list == null) return;

                runOnUiThread(() -> adapter.setAll(list));
            }
        });
    }
}
