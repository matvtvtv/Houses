package com.example.houses.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.Notification.TaskForegroundService;
import com.example.houses.Notification.TaskForegroundServiceHolder;
import com.example.houses.R;
import com.example.houses.adapter.DateAdapter;
import com.example.houses.adapter.TaskAdapter;
import com.example.houses.model.ChatData;
import com.example.houses.model.Task;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
import android.widget.Toast;

public class TaskFragment extends Fragment {

    private static final String TAG = "TaskFragment";
    private static final String SERVER_HTTP_TASKS = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/tasks/";

    private SharedPreferences preferences;
    private TaskAdapter adapter;
    private StompClient stompClient;
    private LinearLayout coinContainer;
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
    private View rootView;

    private final List<TaskInstanceDto> allTasks = new ArrayList<>();

    private ImageView btnCreate;
    private ActivityResultLauncher<String> pickPhotoLauncher;
    private CommentPhotoDialog currentDialog;
    private View rootLayout;
    private TextView textView;
    private TextView textCoins;

    private Handler refreshHandler;
    private Runnable scheduledRefreshRunnable;
    private static final long REFRESH_INTERVAL_MS = 60_000L;
    private boolean isCreateDialogOpen = false;
    public String chatLogin,userLogin,userRole;


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
        chatLogin = preferences.getString("chatLogin", "1");
        userLogin = preferences.getString("login", "1");
        userRole = preferences.getString("role", "CHILD");

        httpClient = new OkHttpClient();

        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        rootLayout = view.findViewById(R.id.rootLayout);
        textView = view.findViewById(R.id.textView);
        coinContainer = view.findViewById(R.id.coinContainer);
        textView.setText("логин группы: " + chatLogin);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        btnCreate = view.findViewById(R.id.btnCreateTask);
        textCoins = view.findViewById(R.id.textCoins);

        if ("CHILD".equals(userRole)) {
            coinContainer.setVisibility(View.VISIBLE);
            textCoins.setVisibility(View.VISIBLE);
            loadUserMoney(userLogin, chatLogin);
        } else {
            btnCreate.setVisibility(View.VISIBLE);
            textCoins.setVisibility(View.GONE);
            coinContainer.setVisibility(View.GONE);
        }

        // при создании адаптера — реализация listener'а:
        adapter = new TaskAdapter(userRole, userLogin, new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onClaim(TaskInstanceDto instance, int position) {
                // Взять: устанавливаем userLogin и отправляем на сервер (оптимистично)
                instance.userLogin = userLogin;
                instance.started = false;
                requireActivity().runOnUiThread(() -> adapter.refresh());
                updateTaskInstance(instance); // PUT с userLogin и started
            }

            @Override
            public void onStart(TaskInstanceDto instance, int position) {
                // Начать: ставим started=true и отправляем
                instance.started = true;
                requireActivity().runOnUiThread(() -> adapter.refresh());
                updateTaskInstance(instance); // PUT с started=true
            }

            @Override
            public void onFinish(TaskInstanceDto instance, int position) {
                CommentPhotoDialog dialog = new CommentPhotoDialog(requireContext(),
                        instance,
                        (comment, photos) -> {
                            // ваш существующий код сохранения
                            instance.comment = comment;
                            instance.photoBase64 = photos;
                            instance.completed = true;
                            updateTaskInstance(instance);
                            requireActivity().runOnUiThread(() -> adapter.refresh());
                        },
                        pickPhotoLauncher,
                        true // editable
                );
                currentDialog = dialog; // <-- важно!
                dialog.setOnDismissListener(d -> currentDialog = null); // очищаем ссылку при закрытии
                dialog.show();
            }


            @Override
            public void onOpenComments(TaskInstanceDto instance, int position) {
                // Просмотр — если родитель, readonly, если исполнитель — editable only if it's his task and not yet completed
                boolean editable = "CHILD".equals(userRole) && userLogin.equals(instance.userLogin) && !instance.completed;
                CommentPhotoDialog dialog = new CommentPhotoDialog(requireContext(),
                        instance,
                        (comment, photos) -> {
                            // ваш существующий код сохранения
                            instance.comment = comment;
                            instance.photoBase64 = photos;
                            instance.completed = true;
                            updateTaskInstance(instance);
                            requireActivity().runOnUiThread(() -> adapter.refresh());
                        },
                        pickPhotoLauncher,
                        editable
                );

                currentDialog = dialog; // <-- важно!
                dialog.setOnDismissListener(d -> currentDialog = null); // очищаем ссылку при закрытии
                dialog.show();
            }
            @Override
            public void onConfirmByParent(TaskInstanceDto instance, int position) {
                // можно показать диалог подтверждения у родителя, затем:
                confirmTaskOnServer(instance.instanceId, instance, position);
            }
            @Override
            public void onEdit(TaskInstanceDto instance, int position) {
                Task prefill = new Task();
                prefill.setTitle(instance.title);
                prefill.setDescription(instance.description);
                prefill.setMoney(instance.money);
                prefill.setStartTime(instance.startTime);
                prefill.setEndTime(instance.endTime);
                prefill.setPartDay(instance.partDay);
                prefill.setImportance(instance.importance);
                prefill.setTargetLogin(instance.targetLogin);

                if (instance.repeatDays != null) {
                    prefill.setDays(instance.repeatDays.toArray(new String[0]));
                    prefill.setRepeat(instance.repeat);
                }
                if (instance.templateId != null) {
                    prefill.setId(instance.templateId); // Важно: сохраняем ID шаблона
                }

                NewTaskDialog editDialog = new NewTaskDialog(requireContext(), chatLogin, prefill, task -> {
                    // Формируем payload для обновления
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("title", task.getTitle());
                    payload.put("description", task.getDescription());
                    payload.put("money", task.getMoney());
                    payload.put("repeat", task.isRepeat());
                    payload.put("repeatDays", task.getDays() != null ? Arrays.asList(task.getDays()) : null);
                    payload.put("startDate", task.getStartDate());
                    payload.put("targetLogin", task.getTargetLogin());
                    payload.put("importance", task.getImportance());
                    payload.put("partDay", task.getPartDay());
                    payload.put("startTime", task.getStartTime());
                    payload.put("endTime", task.getEndTime());

                    // Отправляем через HTTP PUT
                    if (task.getId() != null) {
                        updateTemplateByHttp(payload, task.getId());
                    }
                });

                editDialog.setOnDismissListener(d -> {
                    isCreateDialogOpen = false;
                    btnCreate.setEnabled(true);
                });

                editDialog.show();
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
            if (isCreateDialogOpen) return;

            isCreateDialogOpen = true;
            btnCreate.setEnabled(false);

            loadChatUsersAndShowDialog(chatLogin, userRole);
        });



        if (stompClient == null) {
            stompClient = new StompClient(requireContext());
        }

        stompClient.setListener(new StompClient.StompListener() {
            @Override
            public void onConnected() {
                if (!isAdded()) return;
                if (stompClient != null && chatLogin != null && !chatLogin.isEmpty()) {
                    stompClient.subscribeToTasks(chatLogin);
                }
            }

            @Override
            public void onTaskInstance(TaskInstanceDto instanceDto) {
                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    if (adapter != null) {
                        adapter.addOrUpdate(instanceDto);
                        scheduleSmartRefresh();
                    }

                    if ("CHILD".equals(userRole)) {
                        loadUserMoney(userLogin, chatLogin);
                    }

                    boolean isMyTask =
                            userLogin != null && userLogin.equals(instanceDto.userLogin);

                    if (!isMyTask) {
                        if (TaskForegroundServiceHolder.hService == null) {
                            Intent serviceIntent =
                                    new Intent(requireContext(), TaskForegroundService.class);
                            requireContext().startForegroundService(serviceIntent);
                        }
                        TaskForegroundServiceHolder.enqueue(instanceDto, requireContext());
                    }
                });
            }

            @Override
            public void onExchangeUpdate(com.example.houses.model.ExchangeOffer offer) {
                // noop
            }

            @Override
            public void onChatMessage(com.example.houses.model.ChatMessage m) {}

            @Override
            public void onError(String reason) {
                Log.e(TAG, reason);
            }
        });

        stompClient.connect();

        // загрузим задачи в диапазоне (после инициализации адаптера)
        loadTasksRange(chatLogin, LocalDate.now(), LocalDate.now().plusDays(13));

        refreshHandler = new Handler(Looper.getMainLooper());
        scheduleSmartRefresh();
    }
    private void updateTemplateByHttp(Map<String, Object> payload, Long templateId) {
        // ВАЖНО: убран пробел перед templateId
        String url = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/tasks/template/" + templateId;
        String json = gson.toJson(payload);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request req = new Request.Builder().url(url).put(body).build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to update template", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Update template response: " + response.code());

                requireActivity().runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "Задача обновлена", Toast.LENGTH_SHORT).show();
                        // Перезагружаем список задач
                        loadTasksRange(chatLogin, selectedDate, selectedDate.plusDays(13));
                    } else {
                        Toast.makeText(requireContext(), "Ошибка: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
                response.close();
            }
        });
    }


private void loadChatUsersAndShowDialog(String chatLogin, String currentUserRole) {
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

    private void loadUserMoney(String userLogin, String chatLogin) {
        String url = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/chats_data/get_chats_users/" + chatLogin;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to load user money", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;

                String body = response.body().string();

                Type listType = new TypeToken<List<ChatData>>(){}.getType();
                List<ChatData> users = gson.fromJson(body, listType);

                if (users != null) {
                    for (ChatData user : users) {
                        if (user.getUserLogin().equals(userLogin)) {
                            if (!isAdded()) return;

                            requireActivity().runOnUiThread(() -> {
                                if (textCoins != null) {
                                    textCoins.setText(String.valueOf(user.getMoney()));
                                }
                            });
                            break;
                        }
                    }
                }
            }
        });
    }
    private void confirmTaskOnServer(Long instanceId, TaskInstanceDto instance, int position) {
        if (instanceId == null) {
            Log.e(TAG, "Cannot confirm task: instanceId is null");
            return;
        }

        // правильный эндпоинт в контроллере: @PatchMapping("/{id}/confirm")
        String url = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/tasks/" + instanceId + "/confirm";
        Log.d(TAG, "Confirm URL: " + url);

        // OkHttp требует body для PATCH — отправляем пустой JSON
        RequestBody body = RequestBody.create("{}", MediaType.parse("application/json"));
        Request req = new Request.Builder()
                .url(url)
                .patch(body)
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Confirm request failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                int code = response.code();
                Log.d(TAG, "Confirm response code: " + code);
                String respBody = response.body() != null ? response.body().string() : null;
                Log.d(TAG, "Confirm response body: " + respBody);

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Confirm error: " + code + ", body: " + respBody);
                    response.close();
                    return;
                }


                instance.confirmedByParent = true;
                requireActivity().runOnUiThread(() -> adapter.addOrUpdate(instance));

                response.close();
            }
        });
    }




    private void showNewTaskDialog(String chatLogin, List<ChatData> users, String currentUserRole) {
        if (!isAdded()) return;

        NewTaskDialog dialog = new NewTaskDialog(requireContext(), chatLogin, task -> {
            if (stompClient != null) {
                stompClient.sendTask(chatLogin, task);
            }
        });

        if (users != null && !users.isEmpty()
                && ("PARENT".equals(currentUserRole) || "ADMIN".equals(currentUserRole))) {

            dialog.setChatUsers(users);
        }

        dialog.setOnDismissListener(d -> {
            isCreateDialogOpen = false;   // 🔓 разблокируем
            btnCreate.setEnabled(true);
        });

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
        if (instance == null || instance.instanceId == null) {
            Log.e(TAG, "updateTaskInstance: instance or instanceId is null");
            return;
        }

        // ВАЖНО: правильный PUT-эндпоинт на бекенде:
        // @PutMapping("/instance/{instanceId}")
        String url = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/tasks/instance/" + instance.instanceId;
        Log.d(TAG, "Update URL: " + url);

        Map<String, Object> payload = new HashMap<>();
        payload.put("comment", instance.comment != null ? instance.comment : "");
        payload.put("photoBase64", instance.photoBase64 != null ? instance.photoBase64 : new ArrayList<>());
        payload.put("userLogin", instance.userLogin != null ? instance.userLogin : "");

        // Отправляем явные boolean (false если null), чтобы Jackson не пытался писать null в примитив
        payload.put("completed", instance.completed != null && instance.completed);
        payload.put("started", instance.started != null && instance.started);
        payload.put("confirmedByParent", instance.confirmedByParent != null && instance.confirmedByParent);

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
                Log.d(TAG, "Update response code: " + response.code());
                String respBody = response.body() != null ? response.body().string() : null;
                Log.d(TAG, "Update response body: " + respBody);
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
                // 1) отправляем напоминания за час до старта
                List<TaskInstanceDto> reminders = adapter.getTasksForOneHourReminder();
                for (TaskInstanceDto t : reminders) {
                    sendReminder(t);
                    adapter.markReminderSent(t.instanceId);
                }

                // 2) обновляем видимые данные
                adapter.refresh();
            }
            // рекурсивно планируем дальше
            scheduleSmartRefresh();
        };

        refreshHandler.postDelayed(scheduledRefreshRunnable, delay);
        Log.d(TAG, "Scheduled smart refresh in ms: " + delay);
    }

    private void sendReminder(TaskInstanceDto task) {
        if (task == null) return;

        Context ctx = requireContext();

        // стартуем foreground service, если ещё не запущен
        if (TaskForegroundServiceHolder.hService == null) {
            Intent serviceIntent = new Intent(ctx, TaskForegroundService.class);
            ctx.startForegroundService(serviceIntent);
        }

        // используем существующий хелпер для создания/показa уведомления
        TaskForegroundServiceHolder.enqueue(task, ctx);
    }



    private void initDaysList() {
        List<com.example.houses.model.DayItem> days = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 14; i++) {
            days.add(new com.example.houses.model.DayItem(today.plusDays(i), i == 0));
        }

        dateAdapter = new DateAdapter(days, item -> {
            selectedDate = item.date;
            adapter.setSelectedDate(selectedDate);
        });

        recyclerDays.setAdapter(dateAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (refreshHandler != null && scheduledRefreshRunnable != null) {
            refreshHandler.removeCallbacks(scheduledRefreshRunnable);
            scheduledRefreshRunnable = null;
        }

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
