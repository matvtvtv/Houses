package com.example.houses.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.houses.R;
import com.example.houses.model.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NewTaskDialog extends Dialog {

    private TextInputEditText editTitle, editDesc, editMoney;
    private MaterialButton btnCreate, btnCancel, btnPickDate;
    private TextView tvStartDate;
    // Заменяем CheckBox на Chip
    private Chip chipMon, chipTue, chipWed, chipThu, chipFri, chipSat, chipSun;

    private final String chatLogin;
    private final NewTaskListener listener;

    private LocalDate selectedDate = null;
    private static final DateTimeFormatter FORMATTER_RU = DateTimeFormatter.ofPattern("d MMMM yyyy");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    public interface NewTaskListener {
        void onTaskCreated(Task task);
    }

    public NewTaskDialog(@NonNull Context ctx, String chatLogin, NewTaskListener listener) {
        super(ctx);
        this.chatLogin = chatLogin;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_new_task);

        // Инициализация полей ввода
        editTitle = findViewById(R.id.editTaskTitle);
        editDesc = findViewById(R.id.editTaskDesc);
        editMoney = findViewById(R.id.editTaskMoney);

        tvStartDate = findViewById(R.id.tvStartDate);
        btnPickDate = findViewById(R.id.btnPickDate);

        // Инициализация Чипсов (Chips)
        chipMon = findViewById(R.id.chipMon);
        chipTue = findViewById(R.id.chipTue);
        chipWed = findViewById(R.id.chipWed);
        chipThu = findViewById(R.id.chipThu);
        chipFri = findViewById(R.id.chipFri);
        chipSat = findViewById(R.id.chipSat);
        chipSun = findViewById(R.id.chipSun);

        btnCreate = findViewById(R.id.btnCreateTask);
        btnCancel = findViewById(R.id.btnCancelTask);

        // Выбор даты
        btnPickDate.setOnClickListener(v -> {
            LocalDate now = LocalDate.now();
            DatePickerDialog dp = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                        tvStartDate.setText(selectedDate.format(FORMATTER_RU));
                    }, now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth());
            dp.show();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnCreate.setOnClickListener(v -> {
            if (validateAndCreate()) {
                dismiss();
            }
        });
    }

    private boolean validateAndCreate() {
        String title = editTitle.getText().toString().trim();
        String desc = editDesc.getText().toString().trim();
        String moneyStr = editMoney.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(moneyStr)) {
            Toast.makeText(getContext(), "Заполните название и награду", Toast.LENGTH_SHORT).show();
            return false;
        }

        int money;
        try {
            money = Integer.parseInt(moneyStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Награда должна быть числом", Toast.LENGTH_SHORT).show();
            return false;
        }

        Task task = new Task();
        task.setChatLogin(chatLogin);
        task.setTitle(title);
        task.setDescription(desc);
        task.setMoney(money);
        task.setCompleted(false);

        // Сбор выбранных дней через Чипсы
        List<String> daysList = new ArrayList<>();
        if (chipMon.isChecked()) daysList.add("MONDAY");
        if (chipTue.isChecked()) daysList.add("TUESDAY");
        if (chipWed.isChecked()) daysList.add("WEDNESDAY");
        if (chipThu.isChecked()) daysList.add("THURSDAY");
        if (chipFri.isChecked()) daysList.add("FRIDAY");
        if (chipSat.isChecked()) daysList.add("SATURDAY");
        if (chipSun.isChecked()) daysList.add("SUNDAY");

        if (!daysList.isEmpty()) {
            task.setDays(daysList.toArray(new String[0]));
            task.setRepeat(true);
        } else {
            task.setRepeat(false);
            task.setDays(null);
        }

        if (selectedDate != null) {
            task.setStartDate(selectedDate.format(ISO));
        }

        if (listener != null) listener.onTaskCreated(task);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getWindow() != null) {
            // Делаем диалог на всю ширину для красоты Material Design
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            // Убираем стандартный фон диалога, чтобы были видны наши скругления
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}