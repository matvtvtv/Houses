package com.example.houses.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.houses.Notification.TaskForegroundService;
import com.example.houses.Notification.TaskForegroundServiceHolder;
import com.example.houses.R;
import com.example.houses.adapter.DateAdapter;
import com.example.houses.adapter.TaskAdapter;
import com.example.houses.model.ChatData;
import com.example.houses.model.DayItem;
import com.example.houses.model.TaskInstanceDto;
import com.example.houses.webSocket.StompClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import android.os.Handler;
import android.os.Looper;

public class TaskFragment extends Fragment {

    private static final String TAG = "TaskFragment";
    // ИСПРАВЛЕНО: Убран пробел в конце URL
    private static final String SERVER_HTTP_TASKS = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/tasks/";

    private SharedPreferences preferences;
    private TaskAdapter adapter;
    private StompClient stompClient;
    private OkHttpClient httpClient;

    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, context) ->
                    LocalDate.parse(json.getAsString()))
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, type, context) ->
                    new JsonPrimitive(src.toString()))
            .create();

    private RecyclerView recyclerDays, recyclerTasks;
    private DateAdapter dateAdapter;
    private LocalDate selectedDate;
    private ViewPager2 viewPager;
    private View rootView;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private final List<TaskInstanceDto> allTasks = new ArrayList<>();

    private EditText editTitle, editDesc, editMoney;
    private ImageView btnCreate;
    private ActivityResultLauncher<String> pickPhotoLauncher;
    private CommentPhotoDialog currentDialog;
    private View rootLayout;
    private TextView textView;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private Runnable scheduledRefreshRunnable;
    private static final long REFRESH_INTERVAL_MS = 60_000L;

    public TaskFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferences = requireActivity()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String chatLogin = preferences.getString("chatLogin", "1");
        String userLogin = preferences.getString("login", "1");
        String userRole = preferences.getString("role", "CHILD");

        httpClient = new OkHttpClient();
        loadTasksRange(chatLogin, LocalDate.now(), LocalDate.now().plusDays(13));

        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        rootLayout = view.findViewById(R.id.rootLayout);
        textView = view.findViewById(R.id.textView);
        textView.setText("логин группы: " + chatLogin);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        btnCreate = view.findViewById(R.id.btnCreateTask);
        if(userRole.equals("CHILD")) {
            btnCreate.setVisibility(View.GONE);
        } else {
            btnCreate.setVisibility(View.VISIBLE);
        }
        adapter = new TaskAdapter(userRole, userLogin, new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onClaim(TaskInstanceDto instance, int position) {
                instance.userLogin = userLogin;
                updateTaskInstance(instance);
            }

            @Override
            public void onComplete(TaskInstanceDto instance, int position) {
                patchTaskInstanceStatus(instance.instanceId, true);
            }

            @Override
            public void onOpenComments(TaskInstanceDto instance, int position) {
                currentDialog = new CommentPhotoDialog(
                        requireContext(),
                        instance,
                        (comment, photos) -> {
                            instance.comment = comment;
                            instance.photoBase64 = photos;
                            updateTaskInstance(instance);
                        },
                        pickPhotoLauncher
                );
                currentDialog.show();
            }
        });

        recyclerTasks.setAdapter(adapter);

        selectedDate = LocalDate.now();
        adapter.setSelectedDate(selectedDate);

        recyclerDays = view.findViewById(R.id.recyclerDays);
        recyclerDays.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        initDaysList();

        btnCreate.setOnClickListener(v -> {
            loadChatUsersAndShowDialog(chatLogin, userRole);
        });

        stompClient = new StompClient(requireContext());
        stompClient.setListener(new StompClient.StompListener() {
            @Override
            public void onConnected() {
                stompClient.subscribeToTasks(chatLogin);
            }

            @Override
            public void onTaskInstance(TaskInstanceDto instanceDto) {
                requireActivity().runOnUiThread(() -> {
                    adapter.addOrUpdate(instanceDto);
                    scheduleSmartRefresh();

                    boolean isMyTask = userLogin.equals(instanceDto.userLogin);
                    if (!isMyTask) {
                        if (TaskForegroundServiceHolder.hService == null) {
                            Intent serviceIntent = new Intent(requireContext(), TaskForegroundService.class);
                            requireContext().startForegroundService(serviceIntent);
                        }
                        TaskForegroundServiceHolder.enqueue(instanceDto, requireContext());
                    }
                });
            }

            @Override
            public void onChatMessage(com.example.houses.model.ChatMessage m) {}

            @Override
            public void onError(String reason) {
                Log.e(TAG, reason);
            }
        });

        stompClient.connect();
        refreshHandler = new Handler(Looper.getMainLooper());
        scheduleSmartRefresh();
    }

    private void loadChatUsersAndShowDialog(String chatLogin, String currentUserRole) {
        // ИСПРАВЛЕНО: Убран пробел в конце URL
        String url = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/chats_data/get_chats_users/" + chatLogin;
        Request req = new Request.Builder().url(url).build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to load users", e);
                requireActivity().runOnUiThread(() ->
                        showNewTaskDialog(chatLogin, null, currentUserRole)
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() ->
                            showNewTaskDialog(chatLogin, null, currentUserRole)
                    );
                    return;
                }

                String body = response.body().string();
                Type listType = new TypeToken<List<ChatData>>(){}.getType();
                List<ChatData> users = gson.fromJson(body, listType);

                requireActivity().runOnUiThread(() ->
                        showNewTaskDialog(chatLogin, users, currentUserRole)
                );
            }
        });
    }

    private void showNewTaskDialog(String chatLogin, List<ChatData> users, String currentUserRole) {
        NewTaskDialog dialog = new NewTaskDialog(requireContext(), chatLogin, task -> {
            if (stompClient != null) {
                stompClient.sendTask(chatLogin, task);
            }
        });

        if (users != null && !users.isEmpty() &&
                ("PARENT".equals(currentUserRole) || "ADMIN".equals(currentUserRole))) {

            String currentUserLogin = preferences.getString("login", "");
            List<ChatData> filteredUsers = new ArrayList<>();
            for (ChatData user : users) {
                filteredUsers.add(user);
            }

            dialog.setChatUsers(filteredUsers);
        }

        dialog.show();
    }

    private void loadTasksRange(String chatLogin, LocalDate from, LocalDate to) {
        String url = SERVER_HTTP_TASKS + chatLogin + "?from=" + from + "&to=" + to;
        Request req = new Request.Builder().url(url).build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "tasks history fail", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                ResponseBody bodyObj = response.body();
                if (bodyObj == null) return;
                String body = bodyObj.string();
                Type listType = new TypeToken<List<TaskInstanceDto>>(){}.getType();
                final List<TaskInstanceDto> list = gson.fromJson(body, listType);
                if (list == null) return;
                requireActivity().runOnUiThread(() -> {
                    adapter.setAll(list);
                    scheduleSmartRefresh();
                });
            }
        });
    }

    private void updateTaskInstance(TaskInstanceDto instance) {
        // ИСПРАВЛЕНО: Убран пробел в конце URL
        String url = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/tasks/instance/" + instance.instanceId;

        Map<String, Object> payload = new HashMap<>();
        payload.put("comment", instance.comment);
        payload.put("photoBase64", instance.photoBase64);
        payload.put("completed", instance.completed);
        payload.put("userLogin", instance.userLogin);

        String json = gson.toJson(payload);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request req = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to update task", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Update error: " + response.code());
                }
                response.close();
            }
        });
    }

    private void scheduleSmartRefresh() {
        if (refreshHandler == null) refreshHandler = new Handler(Looper.getMainLooper());
        if (scheduledRefreshRunnable != null) {
            refreshHandler.removeCallbacks(scheduledRefreshRunnable);
            scheduledRefreshRunnable = null;
        }

        if (adapter == null) return;

        long delay = adapter.getMillisUntilNextTimeThreshold();
        if (delay <= 0) {
            delay = REFRESH_INTERVAL_MS;
        }

        scheduledRefreshRunnable = () -> {
            if (adapter != null) {
                adapter.refresh();
            }
            scheduleSmartRefresh();
        };

        refreshHandler.postDelayed(scheduledRefreshRunnable, delay);
        Log.d(TAG, "Scheduled smart refresh in ms: " + delay);
    }

    private void cancelSmartRefresh() {
        if (refreshHandler != null && scheduledRefreshRunnable != null) {
            refreshHandler.removeCallbacks(scheduledRefreshRunnable);
            scheduledRefreshRunnable = null;
        }
    }

    private void initDaysList() {
        List<DayItem> days = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 14; i++) {
            days.add(new DayItem(today.plusDays(i), i == 0));
        }

        dateAdapter = new DateAdapter(days, item -> {
            selectedDate = item.date;
            adapter.setSelectedDate(selectedDate);
        });

        recyclerDays.setAdapter(dateAdapter);
    }

    private void patchTaskInstanceStatus(Long instanceId, boolean completed) {
        // ИСПРАВЛЕНО: Убран пробел в конце URL
        String url = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/tasks/instance/" + instanceId + "/status";

        Map<String, Boolean> payload = new HashMap<>();
        payload.put("completed", completed);

        String json = gson.toJson(payload);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request req = new Request.Builder()
                .url(url)
                .patch(body)
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to update task status", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Status update error: " + response.code());
                }
                response.close();
            }
        });
    }

    @Nullable
    private ViewPager2 findParentViewPager(View view) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent instanceof ViewPager2) return (ViewPager2) parent;
            parent = parent.getParent();
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelSmartRefresh();

        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
        adapter = null;
        rootView = null;
        allTasks.clear();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (currentDialog != null) {
                        currentDialog.handleImageSelected(uri);
                    }
                }
        );
    }
}