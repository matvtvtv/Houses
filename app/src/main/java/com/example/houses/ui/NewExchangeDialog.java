package com.example.houses.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
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

    private EditText etTitle, etCost, etDescription;
    private Button btnCreate, btnCancel;

    private final OkHttpClient http = new OkHttpClient();

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, typeOfT, context) ->
                    Instant.parse(json.getAsString()))
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.toString()))
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
        etCost = findViewById(R.id.editExchangeCost);
        etDescription = findViewById(R.id.editExchangeDesc);

        btnCreate = findViewById(R.id.btnCreateExchange);
        btnCancel = findViewById(R.id.btnCancelExchange);

        btnCreate.setOnClickListener(v -> createExchange());
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void createExchange() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String costStr = etCost.getText().toString().trim();

        if (title.isEmpty() || costStr.isEmpty()) {
            toast("Заполните название и стоимость");
            return;
        }

        int cost;
        try {
            cost = Integer.parseInt(costStr);
        } catch (NumberFormatException e) {
            toast("Стоимость должна быть числом");
            return;
        }

        String ownerLogin = getContext()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getString("login", "");

        ExchangeOffer offer = new ExchangeOffer();
        offer.setChatLogin(chatLogin);
        offer.setTitle(title);
        offer.setDescription(desc);
        offer.setCost(cost);
        offer.setOwnerLogin(ownerLogin);
        offer.setActive(true);

        RequestBody body = RequestBody.create(
                gson.toJson(offer),
                MediaType.parse("application/json")
        );

        // ИСПРАВЛЕНО: убраны пробелы в URL
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