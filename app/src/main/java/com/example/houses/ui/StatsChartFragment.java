package com.example.houses.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.github.mikephil.charting.renderer.Renderer;
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

        Bundle args = getArguments();
        if (args != null && args.containsKey("userLogin")) {
            userLogin = args.getString("userLogin");
        } else {
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            userLogin = prefs.getString("login", "");
        }

        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        cardContainer = view.findViewById(R.id.cardContainer);


        httpClient = new OkHttpClient();
        gson = new Gson();

        chart = view.findViewById(R.id.chartStats);
        setupChart();
        loadStats();
    }

    private void setupChart() {
        // Отключаем фон сетки и стандартные элементы
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(false); // Значения будут показываться при нажатии
        chart.getDescription().setEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setExtraBottomOffset(8f);
        chart.setExtraTopOffset(16f);

        // Включаем двойной тап для сброса зума
        chart.setDoubleTapToZoomEnabled(true);
        chart.setScaleEnabled(false); // Отключаем масштабирование жестами для фиксированного вида

        // Настройка оси X
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7, true); // Показываем 7 меток для читаемости
        xAxis.setTextColor(Color.parseColor("#666666"));
        xAxis.setTextSize(11f);
        xAxis.setYOffset(8f);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setAxisLineWidth(1f);

        // Настройка оси Y (левая)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F0F0F0"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.parseColor("#666666"));
        leftAxis.setTextSize(11f);
        leftAxis.setXOffset(8f);
        leftAxis.setAxisLineColor(Color.TRANSPARENT);

        // Убираем правую ось
        chart.getAxisRight().setEnabled(false);

        // Настройка легенды
        chart.getLegend().setEnabled(false); // Убираем легенду для минималистичного вида

        // Настройка анимации
        chart.animateY(1000, Easing.EaseOutQuart);

        // Слушатель нажатий для показа значений
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // Можно добавить haptic feedback
            }

            @Override
            public void onNothingSelected() {
            }
        });
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
                if (!isAdded()) return; // фрагмент уже отсоединился
                requireActivity().runOnUiThread(() -> {
                    showError("Ошибка загрузки статистики");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!isAdded()) return; // фрагмент уже отсоединился

                if (!response.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        showError("Ошибка сервера: " + response.code());
                    });
                    return;
                }

                String body = response.body().string();
                Type listType = new TypeToken<ArrayList<DailyStats>>(){}.getType();
                List<DailyStats> statsList = gson.fromJson(body, listType);

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    if (statsList == null || statsList.isEmpty()) {
                        showEmptyState();
                    } else {
                        updateChart(statsList);
                    }
                });
            }

        });
    }

    private void updateChart(List<DailyStats> statsList) {
        // Сортируем по дате если нужно
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

        // Создаем градиентный цвет
        int startColor = Color.parseColor("#4CAF50"); // Зеленый
        int endColor = Color.parseColor("#81C784");   // Светло-зеленый

        BarDataSet dataSet = new BarDataSet(entries, "Выполненные задачи");
        dataSet.setColor(startColor);

        // Настройка закругленных углов (только верхние)
        dataSet.setBarBorderWidth(1f);
        dataSet.setBarBorderColor(Color.parseColor("#388E3C")); // Темно-зеленая граница

        // Отключаем стандартные значения на столбцах
        dataSet.setDrawValues(false);

        // Добавляем тень/свечение через эффекты (если поддерживается)
        dataSet.setBarShadowColor(Color.parseColor("#1A000000"));

        // Настройка ширины столбцов
        dataSet.setBarBorderWidth(0f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.65f); // Ширина столбца относительно доступного пространства

        chart.setData(barData);

        // Форматтер для оси X с днями недели и датами
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dateLabels.length) {
                    // Чередуем: день недели/дата через одну метку для экономии места
                    if (index % 2 == 0) {
                        return dayLabels[index];
                    } else {
                        return dateLabels[index];
                    }
                }
                return "";
            }
        });

        // Добавляем среднюю линию если есть данные
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

            // Устанавливаем максимум оси Y с запасом
            chart.getAxisLeft().setAxisMaximum(maxValue * 1.2f);
        }

        // Анимация появления
        chart.animateY(1200, Easing.EaseOutElastic);
        chart.invalidate();
    }

    private float calculateAverage(List<BarEntry> entries) {
        float sum = 0;
        for (BarEntry entry : entries) {
            sum += entry.getY();
        }
        return sum / entries.size();
    }

    private void showEmptyState() {
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(View.VISIBLE);
            chart.setVisibility(View.GONE);
        }
        Toast.makeText(requireContext(), "Нет данных за выбранный период", Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}