package com.example.houses.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.houses.R;
import com.example.houses.model.ExchangeOffer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewExchangeDialog extends Dialog {

    public interface Listener {
        void onCreated(ExchangeOffer offer);
    }

    private final String chatLogin;
    private final Listener listener;

    private EditText etTitle, etDescription;
    private AutoCompleteTextView spinnerMonth;
    private Button btnCreate, btnCancel;

    private final OkHttpClient http = new OkHttpClient();

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, typeOfT, context) ->
                    Instant.parse(json.getAsString()))
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.toString()))
            // ИСПРАВЛЕНО: поддержка и числа и строки для Month
            .registerTypeAdapter(Month.class, (JsonSerializer<Month>) (src, type, context) ->
                    new JsonPrimitive(src.getValue())) // Отправляем как число
            .registerTypeAdapter(Month.class, (JsonDeserializer<Month>) (json, type, context) -> {
                if (json.isJsonPrimitive()) {
                    JsonPrimitive primitive = json.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        // Приходит число (1-12)
                        int monthValue = primitive.getAsInt();
                        return Month.of(monthValue);
                    } else if (primitive.isString()) {
                        // Приходит строка ("JANUARY")
                        String str = primitive.getAsString();
                        try {
                            // Пробуем как число
                            int val = Integer.parseInt(str);
                            return Month.of(val);
                        } catch (NumberFormatException e) {
                            // Пробуем как имя enum
                            return Month.valueOf(str);
                        }
                    }
                }
                return null;
            })
            .create();

    public NewExchangeDialog(
            @NonNull Context context,
            String chatLogin,
            Listener listener
    ) {
        super(context);
        this.chatLogin = chatLogin;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_new_exchange);

        etTitle = findViewById(R.id.editExchangeTitle);
        spinnerMonth = findViewById(R.id.spinnerMonth);
        etDescription = findViewById(R.id.editExchangeDesc);

        btnCreate = findViewById(R.id.btnCreateExchange);
        btnCancel = findViewById(R.id.btnCancelExchange);

        setupMonthSpinner();

        btnCreate.setOnClickListener(v -> createExchange());
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void setupMonthSpinner() {
        String[] months = Arrays.stream(Month.values())
                .map(m -> m.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE,
                        new Locale("ru", "RU")))
                .toArray(String[]::new);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                R.layout.list_item_month,
                months
        );

        spinnerMonth.setAdapter(adapter);
        Month currentMonth = LocalDate.now().getMonth();
        spinnerMonth.setText(months[currentMonth.getValue() - 1], false);
    }

    private Month getSelectedMonth() {
        String selected = spinnerMonth.getText().toString();
        Month[] months = Month.values();
        String[] monthNames = Arrays.stream(months)
                .map(m -> m.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE,
                        new Locale("ru", "RU")))
                .toArray(String[]::new);

        for (int i = 0; i < monthNames.length; i++) {
            if (monthNames[i].equals(selected)) {
                return months[i];
            }
        }
        return LocalDate.now().getMonth();
    }

    private void createExchange() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        Month month = getSelectedMonth();

        if (title.isEmpty()) {
            toast("Заполните название");
            return;
        }

        String ownerLogin = getContext()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("login", "");

        ExchangeOffer offer = new ExchangeOffer();
        offer.setChatLogin(chatLogin);
        offer.setTitle(title);
        offer.setDescription(desc);
        offer.setMonth(month);
        offer.setOwnerLogin(ownerLogin);
        offer.setActive(true);

        RequestBody body = RequestBody.create(
                gson.toJson(offer),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://t7lvb7zl-8080.euw.devtunnels.ms/api/exchange/create")
                .post(body)
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUi(() -> toast("Ошибка сети"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {

                if (!response.isSuccessful()) {
                    runOnUi(() -> toast("Ошибка сервера: " + response.code()));
                    return;
                }

                String responseBody = response.body().string();
                ExchangeOffer created = gson.fromJson(responseBody, ExchangeOffer.class);

                runOnUi(() -> {
                    if (listener != null) listener.onCreated(created);
                    dismiss();
                });
            }
        });
    }

    private void runOnUi(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getWindow() != null) {
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}