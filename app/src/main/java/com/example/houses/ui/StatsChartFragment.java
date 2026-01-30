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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StatsChartFragment extends Fragment {

    private static final String TAG = "StatsChartFragment";
    private static final String BASE_URL = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/stats/daily";

    private BarChart chart;
    private OkHttpClient httpClient;
    private Gson gson;
    private String chatLogin;
    private String userLogin;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM");
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE", new Locale("ru"));
    private TextView tvEmptyState;
    private View cardContainer;

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
            userLogin = prefs.getString("login", "");
        }
        if (chatLogin == null || chatLogin.isEmpty()) {
            chatLogin = prefs.getString("chatLogin", "");
        }

        if (chatLogin == null) chatLogin = "";
        if (userLogin == null) userLogin = "";

        // 4) Настраиваем chart до загрузки данных
        setupChart();

        // 5) Загружаем статистику
        loadStats();
    }

    private void setupChart() {
        if (chart == null) return;

        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(false);
        chart.getDescription().setEnabled(false);
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
        xAxis.setLabelCount(7, true);
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
            public void onValueSelected(Entry e, Highlight h) { /* haptic, тултипы и т.д. */ }

            @Override
            public void onNothingSelected() {}
        });
    }

    private void loadStats() {
        if (httpClient == null || gson == null) {
            Log.e(TAG, "httpClient/gson not initialized");
            return;
        }

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(13);

        String fromStr = from.toString();
        String toStr = to.toString();

        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addQueryParameter("chatLogin", chatLogin)
                .addQueryParameter("userLogin", userLogin)
                .addQueryParameter("from", fromStr)
                .addQueryParameter("to", toStr)
                .build();

        Log.d(TAG, "Request URL: " + url.toString());

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        showError("Ошибка загрузки статистики: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!isAdded()) return;

                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Response code: " + response.code() + ", body: " + body);

                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() ->
                            showError("Ошибка сервера: " + response.code()));
                    return;
                }

                Type listType = new TypeToken<ArrayList<DailyStats>>() {}.getType();
                final List<DailyStats> statsList;
                try {
                    statsList = gson.fromJson(body, listType);
                } catch (Exception ex) {
                    Log.e(TAG, "Can't parse stats JSON", ex);
                    // вместо method reference используем лямбду
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> showEmptyState());
                    return;
                }

                if (statsList == null || statsList.isEmpty()) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> showEmptyState());
                    return;
                }

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> updateChart(statsList));
            }
        });
    }

    private void updateChart(List<DailyStats> statsList) {
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
            dateLabels[i] = stats.getDateAsLocal().format(dateFormatter);
            dayLabels[i] = stats.getDateAsLocal().format(dayFormatter);
            if (count > maxValue) maxValue = count;
        }

        int startColor = Color.parseColor("#4CAF50");
        BarDataSet dataSet = new BarDataSet(entries, "Выполненные задачи");
        dataSet.setColor(startColor);
        dataSet.setDrawValues(false);
        dataSet.setBarShadowColor(Color.parseColor("#1A000000"));
        dataSet.setBarBorderWidth(0f);

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
            avgLine.setTextColor(Color.parseColor("#FF9800"));
            avgLine.setTextSize(10f);
            avgLine.enableDashedLine(10f, 5f, 0f);
            avgLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);

            chart.getAxisLeft().removeAllLimitLines();
            chart.getAxisLeft().addLimitLine(avgLine);
            chart.getAxisLeft().setAxisMaximum(maxValue * 1.2f);
        }

        chart.animateY(1200, Easing.EaseOutElastic);
        chart.invalidate();

        // скрываем пустое состояние
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
        if (chart != null) chart.setVisibility(View.VISIBLE);
    }

    private float calculateAverage(List<BarEntry> entries) {
        float sum = 0;
        for (BarEntry entry : entries) sum += entry.getY();
        return entries.isEmpty() ? 0 : sum / entries.size();
    }



    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void showEmptyState() {
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.VISIBLE);
        if (chart != null) chart.setVisibility(View.GONE);
        Toast.makeText(requireContext(), "Нет данных за выбранный период", Toast.LENGTH_SHORT).show();
    }

}
