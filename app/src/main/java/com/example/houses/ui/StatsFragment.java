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
import com.example.houses.model.ChatData;
import com.example.houses.model.UserStats;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StatsFragment extends Fragment {

    // Корень сервера (оттуда формируем два эндпоинта)
    private static final String BASE_ROOT = "https://t7lvb7zl-8080.euw.devtunnels.ms/";
    private static final String LEADERBOARD_PATH = "api/stats/"; // + {chatLogin} + /leaderboard?sortBy=money
    private static final String CHAT_USERS_PATH = "api/chats_data/get_chats_users/"; // + {chatLogin}

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

        if (chatLogin == null) chatLogin = "";
        if (userLogin == null) userLogin = "";

        if (chatLogin.isEmpty() || userLogin.isEmpty()) {
            Toast.makeText(requireContext(), "Ошибка: данные пользователя не найдены", Toast.LENGTH_SHORT).show();
            return;
        }

        httpClient = new OkHttpClient();
        gson = new Gson();

        recyclerStats = view.findViewById(R.id.recyclerStats);
        recyclerStats.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new StatsAdapter(userLogin, clickedUserLogin -> {
            openStatsChart(clickedUserLogin);
        });
        recyclerStats.setAdapter(adapter);


        // Сначала загружаем пользователей чата и фильтруем по роли CHILD, затем лидерборд
        loadChatUsersAndThenLeaderboard();
    }

    /**
     * Запрос списка пользователей чата (ChatData), выбираем логины с ролью CHILD,
     * затем загружаем лидерборд и фильтруем по этим логинам.
     */
    private void loadChatUsersAndThenLeaderboard() {
        String url = BASE_ROOT + CHAT_USERS_PATH + chatLogin;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Ошибка соединения (пользователи чата)", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Ошибка сервера (пользователи чата): " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String body = response.body() != null ? response.body().string() : "[]";
                Type chatDataListType = new TypeToken<ArrayList<ChatData>>(){}.getType();
                List<ChatData> chatUsers;
                try {
                    chatUsers = gson.fromJson(body, chatDataListType);
                } catch (Exception ex) {
                    chatUsers = new ArrayList<>();
                }

                // Собираем логины пользователей с ролью CHILD
                Set<String> childLogins = new HashSet<>();
                for (ChatData cd : chatUsers) {
                    if (cd != null && cd.getUserRole() != null && cd.getUserRole().equalsIgnoreCase("CHILD")) {
                        if (cd.getUserLogin() != null) {
                            childLogins.add(cd.getUserLogin());
                        }
                    }
                }

                if (childLogins.isEmpty()) {
                    // Нет детей — очищаем адаптер и уведомляем пользователя
                    requireActivity().runOnUiThread(() -> {
                        adapter.setData(new ArrayList<>());
                        Toast.makeText(requireContext(), "В этом чате нет пользователей с ролью CHILD", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Есть дети — загружаем лидерборд и отфильтруем по childLogins
                loadLeaderboardFilteredBy(childLogins);
            }
        });
    }


    private void loadLeaderboardFilteredBy(Set<String> childLogins) {
        String url = BASE_ROOT + LEADERBOARD_PATH + chatLogin + "/leaderboard?sortBy=money";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Ошибка соединения (лидерборд)", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Ошибка сервера (лидерборд): " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String body = response.body() != null ? response.body().string() : "[]";
                Type listType = new TypeToken<ArrayList<UserStats>>(){}.getType();
                List<UserStats> statsList;
                try {
                    statsList = gson.fromJson(body, listType);
                } catch (Exception ex) {
                    statsList = new ArrayList<>();
                }

                // Фильтруем — оставляем только те userLogin, которые есть в childLogins
                List<UserStats> filtered = new ArrayList<>();
                for (UserStats s : statsList) {
                    if (s != null && s.getUserLogin() != null && childLogins.contains(s.getUserLogin())) {
                        filtered.add(s);
                    }
                }

                requireActivity().runOnUiThread(() -> adapter.setData(filtered));
            }
        });
    }
    private void openStatsChart(String selectedUserLogin) {
        StatsChartFragment fragment = new StatsChartFragment();

        Bundle args = new Bundle();
        args.putString("userLogin", selectedUserLogin);
        fragment.setArguments(args);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.bottomNavigation, fragment) // ← твой контейнер
                .addToBackStack(null)
                .commit();
    }

}
