package com.example.houses.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.houses.R;
import com.example.houses.model.DailyStats;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StatsChartFragment extends Fragment {

    private static final String BASE_URL = "https://t7lvb7zl-8080.euw.devtunnels.ms/api/stats/daily";
    private BarChart chart;
    private OkHttpClient httpClient;
    private Gson gson;
    private String chatLogin;
    private String userLogin;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats_chart, container, false);
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

        chart = view.findViewById(R.id.chartStats);
        setupChart();
        loadStats();
    }

    private void setupChart() {
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.getDescription().setEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(14);

        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);
    }

    private void loadStats() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(13);

        String url = BASE_URL + "?chatLogin=" + chatLogin + "&userLogin=" + userLogin +
                "&from=" + from + "&to=" + to;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Ошибка загрузки статистики", Toast.LENGTH_SHORT).show();
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
                Type listType = new TypeToken<ArrayList<DailyStats>>(){}.getType();
                List<DailyStats> statsList = gson.fromJson(body, listType);

                requireActivity().runOnUiThread(() -> updateChart(statsList));
            }
        });
    }

    private void updateChart(List<DailyStats> statsList) {
        if (statsList == null || statsList.isEmpty()) return;

        List<BarEntry> entries = new ArrayList<>();
        final String[] dateLabels = new String[statsList.size()];

        for (int i = 0; i < statsList.size(); i++) {
            DailyStats stats = statsList.get(i);
            entries.add(new BarEntry(i, stats.getCompletedTasksCount()));
            dateLabels[i] = stats.getDateAsLocal().format(dateFormatter);

        }

        BarDataSet dataSet = new BarDataSet(entries, "Выполненные задачи");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        chart.setData(barData);

        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return (index >= 0 && index < dateLabels.length) ? dateLabels[index] : "";
            }
        });

        chart.invalidate();
    }

}