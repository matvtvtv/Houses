package com.example.houses.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.ChatMassageActivity;
import com.example.houses.MainActivity;
import com.example.houses.R;
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

public class TaskFragment extends Fragment {

    private SharedPreferences preferences;
    private static final String SERVER_HTTP_TASKS = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/tasks/";

    private TaskAdapter adapter;
    private StompClient stompClient;
    private OkHttpClient httpClient;
    private Gson gson = new Gson();

    private EditText editTitle, editDesc, editMoney;
    private Button btnCreate;
    private TextView text;
    private Button button_menu, button_chat;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_task, container, false);

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideKeyboard(view);
        preferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String chatLogin = preferences.getString("chatLogin", "1");

        rootView = view.findViewById(R.id.rootLayout);
        if (rootView != null) {
            rootView.setFocusableInTouchMode(true);
            rootView.requestFocus();
        }
        RecyclerView rv = view.findViewById(R.id.recyclerTasks);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter();
        rv.setAdapter(adapter);


        editTitle = view.findViewById(R.id.editTaskTitle);
        editDesc = view.findViewById(R.id.editTaskDesc);
        editMoney = view.findViewById(R.id.editTaskMoney);
        btnCreate = view.findViewById(R.id.btnCreateTask);

        text = view.findViewById(R.id.textView);
        text.setText(chatLogin);

        // Установка обработчиков
        if (button_menu != null) {
            button_menu.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                startActivity(intent);
            });
        }

        if (button_chat != null) {
            button_chat.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ChatMassageActivity.class);
                startActivity(intent);
            });
        }

        // Главный обработчик касаний для скрытия клавиатуры
        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View focusedView = requireActivity().getCurrentFocus();
                if (focusedView instanceof EditText) {
                    Rect outRect = new Rect();
                    focusedView.getGlobalVisibleRect(outRect);

                    // Если касание было вне EditText
                    if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                        focusedView.clearFocus();
                        hideKeyboard(focusedView);
                        return true; // Поглощаем событие
                    }
                }
            }
            return false; // Продолжаем распространение события
        });


        httpClient = new OkHttpClient();
        loadTasks(chatLogin);

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    View v = requireActivity().getCurrentFocus();
                    if (v != null) {
                        v.clearFocus();
                        hideKeyboard(v);
                    }
                }
            }
        });

        stompClient = new StompClient(requireContext());
        stompClient.setListener(new StompClient.StompListener() {
            @Override
            public void onConnected() {
                Log.d("TaskFragment", "WS connected");
                stompClient.subscribeToTasks(chatLogin);
            }

            @Override
            public void onChatMessage(com.example.houses.model.ChatMessage message) {}

            @Override
            public void onTask(Task task) {
                requireActivity().runOnUiThread(() -> adapter.updateTask(task));
            }

            @Override
            public void onError(String reason) {
                Log.e("TaskFragment", "WS error: " + reason);
            }
        });

        stompClient.connect();

        btnCreate.setOnClickListener(v -> {
            String t = editTitle.getText().toString().trim();
            String d = editDesc.getText().toString().trim();
            String money = editMoney.getText().toString().trim();

            if (t.isEmpty() || money.isEmpty()) {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int m;
            try {
                m = Integer.parseInt(money);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Money must be a number", Toast.LENGTH_SHORT).show();
                return;
            }

            Task newTask = new Task();
            newTask.setChatLogin(chatLogin);
            newTask.setTitle(t);
            newTask.setDescription(d);
            newTask.setMoney(m);
            newTask.setCompleted(false);

            stompClient.sendTask(chatLogin, newTask);

            editTitle.setText("");
            editDesc.setText("");
            editMoney.setText("");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
        adapter = null;
        rootView = null;
    }

    private void loadTasks(String chatId) {
        Request req = new Request.Builder().url(SERVER_HTTP_TASKS + chatId).build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("TaskFragment", "tasks history fail", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("TaskFragment", "tasks history not successful: " + response.code());
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
                    Log.e("TaskFragment", "JSON parse error", ex);
                    return;
                }

                if (list == null) return;

                requireActivity().runOnUiThread(() -> adapter.setAll(list));
            }
        });
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}