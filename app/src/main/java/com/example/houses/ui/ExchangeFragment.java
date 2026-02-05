package com.example.houses.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.R;
import com.example.houses.adapter.ExchangeAdapter;
import com.example.houses.model.ChatMessage;
import com.example.houses.model.ExchangeOffer;
import com.example.houses.model.TaskInstanceDto;
import com.example.houses.webSocket.StompClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.Month;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ExchangeFragment extends Fragment {

    private static final String TAG = "ExchangeFragment";
    private static final String BASE_URL = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/exchange/";

    private ExchangeAdapter adapter;
    private StompClient stomp;
    private String chatLogin,userRole;
    private OkHttpClient httpClient;

    // ИСПРАВЛЕНО: добавлен адаптер для Month
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, type, context) ->
                    Instant.parse(json.getAsString()))
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, type, context) ->
                    new JsonPrimitive(src.toString()))
            // Добавляем поддержку Month как числа и строки
            .registerTypeAdapter(Month.class, (JsonDeserializer<Month>) (json, type, context) -> {
                if (json.isJsonPrimitive()) {
                    com.google.gson.JsonPrimitive primitive = json.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        return Month.of(primitive.getAsInt());
                    } else if (primitive.isString()) {
                        String str = primitive.getAsString();
                        try {
                            return Month.of(Integer.parseInt(str));
                        } catch (NumberFormatException e) {
                            return Month.valueOf(str);
                        }
                    }
                }
                return null;
            })
            .create();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exchange, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        chatLogin = prefs.getString("chatLogin", "");
        userRole = prefs.getString("role", "");

        if (chatLogin == null || chatLogin.isEmpty()) {
            Toast.makeText(requireContext(), "Ошибка: не найден chatLogin", Toast.LENGTH_SHORT).show();
            return;
        }

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        RecyclerView recyclerExchange = view.findViewById(R.id.recyclerExchange);
        recyclerExchange.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ExchangeAdapter();
        recyclerExchange.setAdapter(adapter);

        loadExchanges();

        ImageView btnCreateExchanges = view.findViewById(R.id.btnCreateExchanges);

        if (userRole.equals("CHILD")){
            btnCreateExchanges.setVisibility(View.GONE);
        }

        btnCreateExchanges.setOnClickListener(v -> showNewExchangeDialog());

        stomp = new StompClient(requireContext());
        stomp.setListener(new StompClient.StompListener() {
            @Override
            public void onConnected() {
                if (stomp == null || !isAdded()) return;
                stomp.subscribeToExchange(chatLogin);
            }

            @Override
            public void onExchangeUpdate(ExchangeOffer offer) {
                if (adapter != null && isAdded()) {
                    adapter.addOrUpdate(offer);
                }
                loadExchanges();
            }

            @Override
            public void onChatMessage(ChatMessage message) {}

            @Override
            public void onTaskInstance(TaskInstanceDto instance) {}

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "WebSocket ошибка: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        stomp.connect();
    }

    private void loadExchanges() {
        String url = BASE_URL + chatLogin;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to load exchanges", e);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Ошибка загрузки обменов", Toast.LENGTH_SHORT).show()
                    );
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Server error: " + response.code());
                    return;
                }

                String body = response.body().string();
                Type listType = new com.google.gson.reflect.TypeToken<List<ExchangeOffer>>(){}.getType();
                List<ExchangeOffer> offers = gson.fromJson(body, listType);

                if (offers != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        for (ExchangeOffer offer : offers) {
                            adapter.addOrUpdate(offer);
                        }
                        Log.d(TAG, "Loaded " + offers.size() + " exchanges");
                    });
                }
            }
        });
    }

    private void showNewExchangeDialog() {
        NewExchangeDialog dialog = new NewExchangeDialog(
                requireContext(),
                chatLogin,
                offer -> {
                    if (adapter != null) {
                        adapter.addOrUpdate(offer);
                    }
                    loadExchanges();
                    Toast.makeText(requireContext(), "Обмен создан", Toast.LENGTH_SHORT).show();
                }
        );
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (stomp != null) {
            stomp.disconnect();
            stomp = null;
        }
        adapter = null;
    }
}