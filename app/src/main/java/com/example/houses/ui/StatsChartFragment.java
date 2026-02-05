package com.example.houses.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.houses.R;
import com.example.houses.model.DailyStats;
import com.example.houses.model.UserStats;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StatsChartFragment extends Fragment {

    private static final String TAG = "StatsChartFragment";
    private static final String BASE_URL_USER = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/stats/";
    private static final String BASE_URL_DAILY = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/stats/daily";

    private BarChart chart;
    private OkHttpClient httpClient;
    private Gson gson;
    private String chatLogin = "";
    private String userLogin = "";
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM", new Locale("ru"));
    private final DateTimeFormatter shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", new Locale("ru"));

    private TextView tvEmptyState;
    private View cardContainer;

    // Поля для отображения статистики за 2 месяца
    private TextView tvCurrentMonthMoney;
    private TextView tvCurrentMonthTasks;
    private TextView tvCurrentMonthLabel;
    private TextView tvLastMonthMoney;
    private TextView tvLastMonthTasks;
    private TextView tvLastMonthLabel;
    private View cardCurrentMonth;
    private View cardLastMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1) Инициализируем view-поля
        chart = view.findViewById(R.id.chartStats);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        cardContainer = view.findViewById(R.id.cardContainer);

        // Инициализация полей для карточек
        tvCurrentMonthMoney = view.findViewById(R.id.tvCurrentMonthMoney);
        tvCurrentMonthTasks = view.findViewById(R.id.tvCurrentMonthTasks);
        tvCurrentMonthLabel = view.findViewById(R.id.tvCurrentMonthLabel);
        tvLastMonthMoney = view.findViewById(R.id.tvLastMonthMoney);
        tvLastMonthTasks = view.findViewById(R.id.tvLastMonthTasks);
        tvLastMonthLabel = view.findViewById(R.id.tvLastMonthLabel);
        cardCurrentMonth = view.findViewById(R.id.cardCurrentMonth);
        cardLastMonth = view.findViewById(R.id.cardLastMonth);

        // 2) Инициализируем httpClient и gson
        httpClient = new OkHttpClient();
        gson = new Gson();

        // 3) Читаем аргументы и prefs
        Bundle args = getArguments();
        if (args != null) {
            userLogin = args.getString("userLogin", "");
            chatLogin = args.getString("chatLogin", "");
        }

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        if (userLogin == null || userLogin.isEmpty()) {
            userLogin = firstNonEmpty(
                    args != null ? args.getString("userLogin", null) : null,
                    prefs.getString("login", null),
                    prefs.getString("user_login", null)
            );
        }
        if (chatLogin == null || chatLogin.isEmpty()) {
            chatLogin = firstNonEmpty(
                    args != null ? args.getString("chatLogin", null) : null,
                    prefs.getString("chatLogin", null),
                    prefs.getString("chat_login", null)
            );
        }

        if (chatLogin == null) chatLogin = "";
        if (userLogin == null) userLogin = "";

        Log.d(TAG, "Chart for user=" + userLogin + ", chat=" + chatLogin);

        // 4) Настраиваем chart до загрузки данных
        setupChart();

        loadMonthlyStats(); // карточки
        loadDailyStats();   // график
    }

    private String firstNonEmpty(String... values) {
        for (String s : values) {
            if (s != null && !s.trim().isEmpty()) return s.trim();
        }
        return "";
    }

    private void setupChart() {
        if (chart == null) return;

        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(false);
        if (chart.getDescription() != null) chart.getDescription().setEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setExtraBottomOffset(8f);
        chart.setExtraTopOffset(16f);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setScaleEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(2, true);
        xAxis.setTextColor(Color.parseColor("#666666"));
        xAxis.setTextSize(11f);
        xAxis.setYOffset(8f);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setAxisLineWidth(1f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F0F0F0"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.parseColor("#666666"));
        leftAxis.setTextSize(11f);
        leftAxis.setXOffset(8f);
        leftAxis.setAxisLineColor(Color.TRANSPARENT);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.animateY(1000, Easing.EaseOutQuart);

        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {}

            @Override
            public void onNothingSelected() {}
        });
    }

    private void loadDailyStats() {
        if (httpClient == null || gson == null || userLogin.isEmpty() || chatLogin.isEmpty()) {
            showEmptyState();
            return;
        }

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(13);

        HttpUrl url = HttpUrl.parse(BASE_URL_DAILY).newBuilder()
                .addQueryParameter("chatLogin", chatLogin)
                .addQueryParameter("userLogin", userLogin)
                .addQueryParameter("from", from.toString())
                .addQueryParameter("to", to.toString())
                .build();

        Request request = new Request.Builder().url(url).get().build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    showError("Ошибка загрузки: " + e.getMessage());
                    showEmptyState();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!isAdded()) return;

                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> showEmptyState());
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";
                Type listType = new TypeToken<ArrayList<DailyStats>>() {}.getType();
                List<DailyStats> statsList;

                try {
                    statsList = gson.fromJson(body, listType);
                } catch (Exception ex) {
                    Log.e(TAG, "Daily parse error", ex);
                    statsList = new ArrayList<>();
                }

                if (statsList == null || statsList.isEmpty()) {
                    requireActivity().runOnUiThread(() -> showEmptyState());
                    return;
                }

                // Сделаем явную final-ссылку, чтобы лямбда могла её захватить
                final List<DailyStats> finalStatsList = statsList;

                requireActivity().runOnUiThread(() -> updateDailyChart(finalStatsList));
            }

        });
    }

    private void updateDailyChart(List<DailyStats> statsList) {
        if (chart == null) return;

        Collections.sort(statsList, (a, b) -> a.getDateAsLocal().compareTo(b.getDateAsLocal()));

        List<BarEntry> entries = new ArrayList<>();
        final String[] dateLabels = new String[statsList.size()];
        final String[] dayLabels = new String[statsList.size()];
        int maxValue = 0;

        for (int i = 0; i < statsList.size(); i++) {
            DailyStats stats = statsList.get(i);
            int count = stats.getCompletedTasksCount();

            entries.add(new BarEntry(i, count));
            dateLabels[i] = stats.getDateAsLocal().format(DateTimeFormatter.ofPattern("dd.MM"));
            dayLabels[i] = stats.getDateAsLocal().format(DateTimeFormatter.ofPattern("EEE", new Locale("ru")));

            if (count > maxValue) maxValue = count;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Выполненные задачи");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.65f);

        chart.setData(barData);

        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dateLabels.length) {
                    return (index % 2 == 0) ? dayLabels[index] : dateLabels[index];
                }
                return "";
            }
        });

        if (maxValue > 0) {
            float average = calculateAverage(entries);

            LimitLine avgLine = new LimitLine(average, "Среднее");
            avgLine.setLineColor(Color.parseColor("#FFB74D"));
            avgLine.setLineWidth(1.5f);
            avgLine.enableDashedLine(10f, 5f, 0f);

            chart.getAxisLeft().removeAllLimitLines();
            chart.getAxisLeft().addLimitLine(avgLine);
            chart.getAxisLeft().setAxisMaximum(maxValue * 1.2f);
        }

        chart.animateY(1200, Easing.EaseOutElastic);
        chart.invalidate();

        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
        chart.setVisibility(View.VISIBLE);
    }

    private void loadMonthlyStats() {
        if (httpClient == null || userLogin.isEmpty() || chatLogin.isEmpty()) {
            showEmptyState();
            return;
        }

        String url = BASE_URL_USER + chatLogin + "/" + userLogin;
        Log.d(TAG, "Loading stats from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    showError("Ошибка загрузки: " + e.getMessage());
                    showEmptyState();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!isAdded()) return;

                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        showError("Ошибка сервера: " + response.code());
                        showEmptyState();
                    });
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";

                try {
                    UserStats stats = gson.fromJson(body, UserStats.class);
                    if (stats != null && stats.getTasksByMonth() != null && !stats.getTasksByMonth().isEmpty()) {
                        requireActivity().runOnUiThread(() -> {
                            updateMonthlyUI(stats);
                            updateChartFromUserStats(stats);
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> showEmptyState());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse error", e);
                    requireActivity().runOnUiThread(() -> showEmptyState());
                }
            }
        });
    }

    // Обновление UI с данными за месяца (карточки)
    private void updateMonthlyUI(UserStats stats) {
        YearMonth current = YearMonth.now();
        YearMonth last = current.minusMonths(1);

        String currentKey = current.toString(); // "2026-02"
        String lastKey = last.toString();       // "2026-01"

        int currentMoney = stats.getMoneyByMonth() != null ?
                stats.getMoneyByMonth().getOrDefault(currentKey, 0) : 0;
        int currentTasks = stats.getTasksByMonth() != null ?
                stats.getTasksByMonth().getOrDefault(currentKey, 0) : 0;

        int lastMoney = stats.getMoneyByMonth() != null ?
                stats.getMoneyByMonth().getOrDefault(lastKey, 0) : 0;
        int lastTasks = stats.getTasksByMonth() != null ?
                stats.getTasksByMonth().getOrDefault(lastKey, 0) : 0;

        if (tvCurrentMonthMoney != null) {
            tvCurrentMonthMoney.setText(currentMoney + " 💰");
            tvCurrentMonthTasks.setText(currentTasks + " ✅");
            tvCurrentMonthLabel.setText(capitalize(current.format(monthFormatter)));
        }

        if (tvLastMonthMoney != null) {
            tvLastMonthMoney.setText(lastMoney + " 💰");
            tvLastMonthTasks.setText(lastTasks + " ✅");
            tvLastMonthLabel.setText(capitalize(last.format(monthFormatter)));
        }

        if (cardCurrentMonth != null) cardCurrentMonth.setVisibility(View.VISIBLE);
        if (cardLastMonth != null) cardLastMonth.setVisibility(View.VISIBLE);
    }

    private void updateChartFromUserStats(UserStats stats) {
        if (chart == null) return;

        YearMonth current = YearMonth.now();
        YearMonth last = current.minusMonths(1);

        String currentKey = current.toString();
        String lastKey = last.toString();

        Map<String, Integer> tasksByMonth = stats.getTasksByMonth();
        if (tasksByMonth == null) {
            showEmptyState();
            return;
        }

        int currentTasks = tasksByMonth.getOrDefault(currentKey, 0);
        int lastTasks = tasksByMonth.getOrDefault(lastKey, 0);

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, lastTasks));
        entries.add(new BarEntry(1, currentTasks));

        final String[] monthLabels = new String[]{
                capitalize(last.format(shortMonthFormatter)),
                capitalize(current.format(shortMonthFormatter))
        };

        int maxValue = Math.max(currentTasks, lastTasks);

        BarDataSet dataSet = new BarDataSet(entries, "Выполненные задачи");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setDrawValues(false);
        dataSet.setBarShadowColor(Color.parseColor("#1A000000"));
        dataSet.setBarBorderWidth(0f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        chart.setData(barData);

        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < monthLabels.length) {
                    return monthLabels[index];
                }
                return "";
            }
        });

        if (maxValue > 0) {
            float average = calculateAverage(entries);
            LimitLine avgLine = new LimitLine(average, "Среднее");
            avgLine.setLineColor(Color.parseColor("#FFB74D"));
            avgLine.setLineWidth(1.5f);
            avgLine.setTextColor(Color.parseColor("#FF9800"));
            avgLine.setTextSize(10f);
            avgLine.enableDashedLine(10f, 5f, 0f);
            avgLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);

            chart.getAxisLeft().removeAllLimitLines();
            chart.getAxisLeft().addLimitLine(avgLine);
            chart.getAxisLeft().setAxisMaximum(maxValue * 1.2f);
        } else {
            chart.getAxisLeft().setAxisMaximum(10f);
        }

        chart.animateY(1200, Easing.EaseOutElastic);
        chart.invalidate();

        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
        if (chart != null) chart.setVisibility(View.VISIBLE);
    }

    private float calculateAverage(List<BarEntry> entries) {
        float sum = 0;
        for (BarEntry entry : entries) sum += entry.getY();
        return entries.isEmpty() ? 0 : sum / entries.size();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void showError(String message) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showEmptyState() {
        if (!isAdded()) return;
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.VISIBLE);
        if (chart != null) chart.setVisibility(View.GONE);
        if (cardCurrentMonth != null) cardCurrentMonth.setVisibility(View.GONE);
        if (cardLastMonth != null) cardLastMonth.setVisibility(View.GONE);
    }
}
