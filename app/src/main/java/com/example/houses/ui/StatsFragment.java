package com.example.houses.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.R;
import com.example.houses.adapter.StatsAdapter;
import com.example.houses.model.ChatData;
import com.example.houses.model.ChatMessage;
import com.example.houses.model.ExchangeOffer;
import com.example.houses.model.TaskInstanceDto;
import com.example.houses.model.UserStats;
import com.example.houses.webSocket.StompClient;
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

    private static final String TAG = "StatsFragment";
    private static final String BASE_ROOT = "https://t7lvb7zl-8080.euw.devtunnels.ms/";
    private static final String LEADERBOARD_PATH = "api/stats/";
    private static final String CHAT_USERS_PATH = "api/chats_data/get_chats_users/";

    private RecyclerView recyclerStats;
    private StatsAdapter adapter;
    private OkHttpClient httpClient;
    private StompClient stompClient;
    private FrameLayout fragmentStatContainer;

    private Gson gson;
    private String chatLogin = "";
    private String userLogin = "";
    private String userRole = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Инициализация views
        recyclerStats = view.findViewById(R.id.recyclerStats);
        fragmentStatContainer = view.findViewById(R.id.fragmentStatContainer);

        // Инициализируем http/gson для потенциальных вызовов
        httpClient = new OkHttpClient();
        gson = new Gson();

        // Загружаем данные пользователя — пробуем несколько ключей на случай различий
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userRole = firstNonEmpty(
                prefs.getString("role", null),
                prefs.getString("userRole", null),
                prefs.getString("ROLE", null)
        );
        userLogin = firstNonEmpty(
                prefs.getString("login", null),
                prefs.getString("user_login", null)
        );
        chatLogin = firstNonEmpty(
                prefs.getString("chatLogin", null),
                prefs.getString("chat_login", null)
        );

        if (userRole == null) userRole = "";
        if (userLogin == null) userLogin = "";
        if (chatLogin == null) chatLogin = "";

        Log.d(TAG, "Role: [" + userRole + "], User: [" + userLogin + "], Chat: [" + chatLogin + "]");

        // Если CHILD — показываем график и используем login из prefs (надёжно)
        if (userRole.toUpperCase().contains("CHILD")) {
            showStatsChart(userLogin);
            return;
        }

        // Для PARENT/ADMIN — показываем список
        setupLeaderboard();
    }

    private String firstNonEmpty(String... values) {
        for (String s : values) {
            if (s != null && !s.trim().isEmpty()) return s.trim();
        }
        return "";
    }

    private void setupLeaderboard() {
        // Показываем список, скрываем контейнер с графиком
        if (fragmentStatContainer != null) fragmentStatContainer.setVisibility(View.GONE);
        if (recyclerStats != null) recyclerStats.setVisibility(View.VISIBLE);

        recyclerStats.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new StatsAdapter(userLogin, this::openStatsChart);
        recyclerStats.setAdapter(adapter);

        loadData();

        // WebSocket для обновлений (только для списка)
        setupWebSocket();
    }

    private void setupWebSocket() {
        if (stompClient == null) {
            stompClient = new StompClient(requireContext());
        }

        stompClient.setListener(new StompClient.StompListener() {
            @Override
            public void onConnected() {
                Log.d(TAG, "WebSocket connected");
            }

            @Override
            public void onTaskInstance(TaskInstanceDto instanceDto) {
                Log.d(TAG, "Task update, reloading...");
                reloadData();
            }

            @Override
            public void onExchangeUpdate(ExchangeOffer offer) {
                Log.d(TAG, "Exchange update, reloading...");
                reloadData();
            }

            @Override
            public void onChatMessage(ChatMessage m) {}

            @Override
            public void onError(String reason) {
                Log.e(TAG, "WebSocket error: " + reason);
            }
        });

        stompClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем при возврате на фрагмент (если это не CHILD)
        if (!userRole.toUpperCase().contains("CHILD") && httpClient != null) {
            loadData();
        }
    }

    private void reloadData() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(this::loadData);
    }

    private void loadData() {
        if (chatLogin == null || chatLogin.isEmpty()) {
            Log.e(TAG, "chatLogin is empty");
            return;
        }
        loadChatUsersAndThenLeaderboard();
    }

    private void showStatsChart(String login) {
        // Включаем контейнер и скрываем список
        if (fragmentStatContainer != null) fragmentStatContainer.setVisibility(View.VISIBLE);
        if (recyclerStats != null) recyclerStats.setVisibility(View.GONE);

        // Создаём фрагмент графика и передаём аргументы
        StatsChartFragment chartFragment = new StatsChartFragment();
        Bundle args = new Bundle();
        args.putString("userLogin", login);
        args.putString("chatLogin", chatLogin);
        chartFragment.setArguments(args);

        // Используем childFragmentManager — график вложен в этот фрагмент
        if (isAdded()) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentStatContainer, chartFragment)
                    .commit(); // commit() вместо commitNow()
        }
    }

    private void loadChatUsersAndThenLeaderboard() {
        String url = BASE_ROOT + CHAT_USERS_PATH + chatLogin;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Ошибка соединения (пользователи чата)", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!isAdded()) return;

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
                    Log.e(TAG, "Parse chat users error", ex);
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
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (adapter != null) adapter.setData(new ArrayList<>());
                        Toast.makeText(requireContext(), "В этом чате нет пользователей с ролью CHILD", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Есть дети — загружаем лидерборд и отфильтруем по childLogins
                loadLeaderboardFilteredBy(childLogins);
            }
        });
    }

    private void openStatsChart(String selectedUserLogin) {
        // Открываем график пользователя в активити (замена фрагмента)
        StatsChartFragment fragment = new StatsChartFragment();
        Bundle args = new Bundle();
        args.putString("userLogin", selectedUserLogin);
        args.putString("chatLogin", chatLogin);
        fragment.setArguments(args);

        // Заменяем главный контейнер активити (убедись, что id exists в activity layout)
        // Если у тебя другой id контейнера — поменяй R.id.bottomNavigation на correct id
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.bottomNavigation, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadLeaderboardFilteredBy(Set<String> childLogins) {
        String url = BASE_ROOT + LEADERBOARD_PATH + chatLogin + "/leaderboard?sortBy=money";
        Log.d(TAG, "Loading leaderboard: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Leaderboard load failed", e);
                // Не показываем Toast при фоновом обновлении
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!isAdded()) return;

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Leaderboard error: " + response.code());
                    return;
                }

                String body = response.body() != null ? response.body().string() : "[]";
                Type listType = new TypeToken<ArrayList<UserStats>>(){}.getType();
                List<UserStats> statsList;
                try {
                    statsList = gson.fromJson(body, listType);
                } catch (Exception ex) {
                    Log.e(TAG, "Parse error", ex);
                    statsList = new ArrayList<>();
                }

                // Фильтруем
                List<UserStats> filtered = new ArrayList<>();
                for (UserStats s : statsList) {
                    if (s != null && s.getUserLogin() != null && childLogins.contains(s.getUserLogin())) {
                        filtered.add(s);
                    }
                }

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    if (adapter != null) {
                        adapter.setData(filtered);
                        Log.d(TAG, "Updated adapter with " + filtered.size() + " items");
                    }
                });
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
    }
}
