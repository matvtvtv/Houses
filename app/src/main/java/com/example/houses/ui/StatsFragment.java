package com.example.houses.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.R;
import com.example.houses.adapter.StatsAdapter;
import com.example.houses.model.UserStats;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StatsFragment extends Fragment {

    private static final String BASE_URL = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/stats/";
    private RecyclerView recyclerStats;
    private StatsAdapter adapter;
    private OkHttpClient httpClient;
    private Gson gson;
    private String chatLogin;
    private String userLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        chatLogin = prefs.getString("chatLogin", "");
        userLogin = prefs.getString("login", "");

        if (chatLogin.isEmpty() || userLogin.isEmpty()) {
            Toast.makeText(requireContext(), "Ошибка: данные пользователя не найдены", Toast.LENGTH_SHORT).show();
            return;
        }

        httpClient = new OkHttpClient();
        gson = new Gson();

        recyclerStats = view.findViewById(R.id.recyclerStats);
        recyclerStats.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new StatsAdapter(userLogin);
        recyclerStats.setAdapter(adapter);

        loadLeaderboard();
    }

    private void loadLeaderboard() {
        String url = BASE_URL + chatLogin + "/leaderboard?sortBy=money";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Ошибка соединения", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String body = response.body().string();
                Type listType = new TypeToken<ArrayList<UserStats>>(){}.getType();
                List<UserStats> statsList = gson.fromJson(body, listType);

                requireActivity().runOnUiThread(() -> adapter.setData(statsList));
            }
        });
    }
}