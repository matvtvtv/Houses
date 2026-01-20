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
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.example.houses.model.DayItem;
import com.example.houses.model.Task;
import com.example.houses.webSocket.StompClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class TaskFragment extends Fragment {

    private static final String TAG = "TaskFragment";
    private SharedPreferences preferences;
    private static final String SERVER_HTTP_TASKS = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/tasks/";

    private TaskAdapter adapter;
    private StompClient stompClient;
    private OkHttpClient httpClient;
    private Gson gson = new Gson();

    private EditText editTitle, editDesc, editMoney;
    private ImageView btnCreate;
    private TextView text;

    private View rootView;

    private RecyclerView recyclerDays, recyclerTasks;
    private DateAdapter dateAdapter;
    private LocalDate selectedDate;
    private ViewPager2 viewPager;
    private boolean isVisibleToUser = false;


    public TaskFragment() {
    }

    // Храним полный набор задач (нефильтрованный)
    private final List<Task> allTasks = new ArrayList<>();

    // Для парсинга ISO дат (yyyy-MM-dd)
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

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


        Intent serviceIntent = new Intent(requireContext(), TaskForegroundService.class);
        requireActivity().startForegroundService(serviceIntent);




        rootView = view.findViewById(R.id.rootLayout);
        if (rootView != null) {
            rootView.setFocusableInTouchMode(true);
            rootView.requestFocus();
        }

        try {
            viewPager = requireActivity().findViewById(R.id.viewPager);
        } catch (Exception e) {
            viewPager = null;
        }
        if (viewPager == null) {
            viewPager = findParentViewPager(view);
        }

        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter((task, pos) -> {
            String myLogin = preferences.getString("chatLogin", "1");
            task.setUserLogin(myLogin);
            if (stompClient != null) {
                stompClient.sendUpdate(chatLogin, task);
            }
        });
        recyclerTasks.setAdapter(adapter);

        recyclerDays = view.findViewById(R.id.recyclerDays);
        recyclerDays.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));


        recyclerDays.setOnTouchListener((v, event) -> {
            ViewPager2 viewPager = findParentViewPager(v);
            if (viewPager != null) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    viewPager.requestDisallowInterceptTouchEvent(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    viewPager.requestDisallowInterceptTouchEvent(false);
                }
            }
            return false; // Важно: позволяем RecyclerView обработать событие
        });

        recyclerDays.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                ViewPager2 viewPager = findParentViewPager(recyclerView);
                if (viewPager != null) {
                    viewPager.requestDisallowInterceptTouchEvent(
                            newState == RecyclerView.SCROLL_STATE_DRAGGING
                    );
                }
            }
        });

        editTitle = view.findViewById(R.id.editTaskTitle);
        editDesc = view.findViewById(R.id.editTaskDesc);
        editMoney = view.findViewById(R.id.editTaskMoney);
        btnCreate = view.findViewById(R.id.btnCreateTask);

        text = view.findViewById(R.id.textView);
        text.setText("логин группы: "+chatLogin);

        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View focusedView = requireActivity().getCurrentFocus();
                if (focusedView instanceof EditText) {
                    Rect outRect = new Rect();
                    focusedView.getGlobalVisibleRect(outRect);
                    if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                        focusedView.clearFocus();
                        hideKeyboard(focusedView);
                        return true;
                    }
                }
            }
            return false;
        });

        httpClient = new OkHttpClient();
        loadTasks(chatLogin);

        recyclerTasks.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    View v2 = requireActivity().getCurrentFocus();
                    if (v2 != null) {
                        v2.clearFocus();
                        hideKeyboard(v2);
                    }
                }
            }
        });

        stompClient = new StompClient(requireContext());
        stompClient.setListener(new StompClient.StompListener() {
            @Override
            public void onConnected() {
                Log.d(TAG, "WS connected");
                stompClient.subscribeToTasks(chatLogin);
            }

            @Override
            public void onChatMessage(com.example.houses.model.ChatMessage message) { }

            @Override
            public void onTask(Task task) {
                requireActivity().runOnUiThread(() -> {
                    upsertAllTasks(task);
                    applyFilter();

                    String myLogin = preferences.getString("chatLogin", "1");

                    boolean isMyTask =
                            task.getUserLogin() != null &&
                                    task.getUserLogin().equals(myLogin);


                    if (!isMyTask) {
                        // Отправляем задачу в сервис
                        TaskForegroundServiceHolder.enqueue(task, requireContext());
                    }


                });
            }




            @Override
            public void onError(String reason) {
                Log.e(TAG, "WS error: " + reason);
            }
        });
        stompClient.connect();

        // --- days UI (init + listener that filters) ---
        initDaysList();

        // --- create button opens dialog ---
        btnCreate.setOnClickListener(v -> {
            NewTaskDialog dialog = new NewTaskDialog(requireContext(), chatLogin, task -> {
                if (stompClient != null) {
                    stompClient.sendTask(chatLogin, task);
                }
            });
            dialog.show();
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
        allTasks.clear();
    }

    // ---------- HTTP loader ----------
    private void loadTasks(String chatId) {
        Request req = new Request.Builder().url(SERVER_HTTP_TASKS + chatId).build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "tasks history fail", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "tasks history not successful: " + response.code());
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
                    Log.e(TAG, "JSON parse error", ex);
                    return;
                }

                if (list == null) return;

                requireActivity().runOnUiThread(() -> {
                    // replace allTasks and apply filter
                    allTasks.clear();
                    allTasks.addAll(list);
                    applyFilter();
                });
            }
        });
    }

    // ---------- filtering logic ----------
    private void applyFilter() {
        if (selectedDate == null) {
            // if no date selected, show all
            adapter.setAll(new ArrayList<>(allTasks));
            return;
        }
        List<Task> filtered = new ArrayList<>();
        for (Task t : allTasks) {
            if (shouldShowOnDate(t, selectedDate)) filtered.add(t);
        }
        adapter.setAll(filtered);
    }


    private boolean shouldShowOnDate(Task t, LocalDate selected) {
        // parse startDate if present
        LocalDate start = null;
        try {
            if (t.getStartDate() != null && !t.getStartDate().isEmpty()) {
                start = LocalDate.parse(t.getStartDate(), ISO);
            }
        } catch (Exception ex) {
            // некорректный формат даты — считаем как отсутствующий startDate
            Log.w(TAG, "Failed to parse startDate for task id=" + t.getId() + ": " + ex.getMessage());
            start = null;
        }

        String[] days = t.getDays(); // может быть null

        // case: both absent => show always
        if (start == null && (days == null || days.length == 0)) {
            return true;
        }

        // if days defined -> check weekday
        boolean weekdayMatches = false;
        if (days != null && days.length > 0) {
            DayOfWeek dow = selected.getDayOfWeek();
            String name = dow.toString().toUpperCase(Locale.ROOT); // e.g. "MONDAY"
            for (String s : days) {
                if (s == null) continue;
                if (s.trim().equalsIgnoreCase(name)) {
                    weekdayMatches = true;
                    break;
                }
            }
        }

        if (start != null) {
            // has startDate
            if (days != null && days.length > 0) {
                // repeating with start
                return !selected.isBefore(start) && weekdayMatches;
            } else {
                // single occurrence at startDate
                return selected.equals(start);
            }
        } else {
            // no startDate but days defined
            return weekdayMatches;
        }
    }

    // ---------- helpers ----------
    private void upsertAllTasks(Task t) {
        if (t == null) return;
        // try to find by id
        if (t.getId() != null) {
            for (int i = 0; i < allTasks.size(); i++) {
                Task cur = allTasks.get(i);
                if (cur.getId() != null && cur.getId().equals(t.getId())) {
                    allTasks.set(i, t);
                    return;
                }
            }
        }
        // otherwise add
        allTasks.add(t);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // ---------- days list ----------
    private void initDaysList() {
        List<DayItem> days = new ArrayList<>();
        LocalDate today = LocalDate.now();
        selectedDate = today; // default selection = today

        for (int i = 0; i < 14; i++) {
            DayItem dayItem = new DayItem(today.plusDays(i), i == 0);
            days.add(dayItem);
        }

        dateAdapter = new DateAdapter(days, item -> {
            selectedDate = item.date;
            Log.d(TAG, "Date selected: " + selectedDate);
            applyFilter();
        });
        recyclerDays.setAdapter(dateAdapter);

        // apply initial filter for today
        applyFilter();
    }

    @Nullable
    private ViewPager2 findParentViewPager(View view) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent instanceof ViewPager2) {
                return (ViewPager2) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }
    @Override
    public void onResume() {
        super.onResume();
        isVisibleToUser = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isVisibleToUser = false;
    }


}
