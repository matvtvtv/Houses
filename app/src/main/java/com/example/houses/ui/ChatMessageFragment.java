package com.example.houses.ui;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.DB.DatabaseHelper;
import com.example.houses.R;
import com.example.houses.adapter.ChatMessageAdapter;
import com.example.houses.model.ChatMessage;
import com.example.houses.webSocket.StompClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ChatMessageFragment extends Fragment {
    private SharedPreferences preferences;
    private static final String SERVER_HTTP_HISTORY = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/chat/history/";

    private ChatMessageAdapter adapter;
    private StompClient stompClient;
    private OkHttpClient httpClient;
    private Gson gson = new Gson();

    private EditText editMessage;
    private ImageView btnSend;
    private View rootView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_chat_message, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferences = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);

        RecyclerView rv = view.findViewById(R.id.recyclerMessages);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        String login = preferences.getString("login", "account");
        String chatLogin = preferences.getString("chatLogin", "1");
        adapter = new ChatMessageAdapter(login);
        rv.setAdapter(adapter);
        rootView = view.findViewById(R.id.main);
        editMessage = view.findViewById(R.id.editMessage);
        btnSend = view.findViewById(R.id.btnSend);

        btnSend.setEnabled(false);

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        loadHistory();

        stompClient = new StompClient(requireContext());

        stompClient.setListener(new StompClient.StompListener() {
            @Override
            public void onConnected() {
                Log.d("ChatMessageFragment", "STOMP connected");
                stompClient.subscribeToChat(chatLogin);

                requireActivity().runOnUiThread(() -> btnSend.setEnabled(true));
            }

            @Override
            public void onChatMessage(ChatMessage message) {
                requireActivity().runOnUiThread(() -> {
                    adapter.addMessage(message);
                    rv.smoothScrollToPosition(adapter.getItemCount() - 1);
                });
            }

            @Override
            public void onTask(com.example.houses.model.Task task) {
                // Обработка задачи
            }

            @Override
            public void onError(String reason) {
                Log.e("ChatMessageFragment", "STOMP error: " + reason);
            }
        });

        stompClient.connect();

        btnSend.setOnClickListener(v -> {
            String text = editMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            byte[] avatarBytes = DatabaseHelper.getInstance(requireContext()).getUserAvatar(login);

            String avatarBase64 = null;
            if (avatarBytes != null) {
                avatarBase64 = android.util.Base64.encodeToString(avatarBytes, android.util.Base64.DEFAULT);
            }
            Log.e("ChatMessageFragment", "S: " + avatarBase64);

            StompClient.MessageDTO payload = new StompClient.MessageDTO(login, text, avatarBase64);
            stompClient.send("/app/chat/" + preferences.getString("chatLogin", "") + "/send", payload);
            editMessage.setText("");
        });

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


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
        adapter = null;
        editMessage = null;
        btnSend = null;
    }

    private void loadHistory() {
        Request req = new Request.Builder()
                .url(SERVER_HTTP_HISTORY + preferences.getString("chatLogin", "3"))
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("ChatMessageFragment", "history fail", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("ChatMessageFragment", "history not successful: " + response.code());
                    return;
                }

                ResponseBody bodyObj = response.body();
                if (bodyObj == null) return;

                String body = bodyObj.string();
                Type listType = new TypeToken<List<ChatMessage>>() {}.getType();
                final List<ChatMessage> list;

                try {
                    list = gson.fromJson(body, listType);
                } catch (Exception ex) {
                    Log.e("ChatMessageFragment", "JSON parse error", ex);
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