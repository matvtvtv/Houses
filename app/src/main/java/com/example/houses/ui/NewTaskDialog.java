package com.example.houses.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.houses.R;
import com.example.houses.model.Task;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NewTaskDialog extends Dialog {

    private EditText editTitle, editDesc, editMoney;
    private Button btnCreate, btnCancel, btnPickDate, btnClearDate;
    private TextView tvStartDate;
    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;

    private final String chatLogin;
    private final NewTaskListener listener;

    private LocalDate selectedDate = null;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    public interface NewTaskListener {
        void onTaskCreated(Task task);
    }

    public NewTaskDialog(Context ctx, String chatLogin, NewTaskListener listener) {
        super(ctx);
        this.chatLogin = chatLogin;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_new_task);

        editTitle = findViewById(R.id.editTaskTitle);
        editDesc = findViewById(R.id.editTaskDesc);
        editMoney = findViewById(R.id.editTaskMoney);

        tvStartDate = findViewById(R.id.tvStartDate);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnClearDate = findViewById(R.id.btnClearDate);

        cbMon = findViewById(R.id.cbMon);
        cbTue = findViewById(R.id.cbTue);
        cbWed = findViewById(R.id.cbWed);
        cbThu = findViewById(R.id.cbThu);
        cbFri = findViewById(R.id.cbFri);
        cbSat = findViewById(R.id.cbSat);
        cbSun = findViewById(R.id.cbSun);

        btnCreate = findViewById(R.id.btnCreateTask);
        btnCancel = findViewById(R.id.btnCancelTask);

        btnPickDate.setOnClickListener(v -> {
            LocalDate now = LocalDate.now();
            DatePickerDialog dp = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                        tvStartDate.setText("Дата начала: " + selectedDate.format(ISO));
                    }, now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth());
            dp.show();
        });

        btnClearDate.setOnClickListener(v -> {
            selectedDate = null;
            tvStartDate.setText("Дата начала: не задана");
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnCreate.setOnClickListener(v -> {
            String title = editTitle.getText().toString().trim();
            String desc = editDesc.getText().toString().trim();
            String moneyStr = editMoney.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(moneyStr)) {
                Toast.makeText(getContext(), "Заполните название и награду", Toast.LENGTH_SHORT).show();
                return;
            }

            int money;
            try {
                money = Integer.parseInt(moneyStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Награда должна быть числом", Toast.LENGTH_SHORT).show();
                return;
            }

            Task task = new Task();
            task.setChatLogin(chatLogin);
            task.setTitle(title);
            task.setDescription(desc);
            task.setMoney(money);
            task.setCompleted(false);

            // days
            List<String> daysList = new ArrayList<>();
            if (cbMon.isChecked()) daysList.add("MONDAY");
            if (cbTue.isChecked()) daysList.add("TUESDAY");
            if (cbWed.isChecked()) daysList.add("WEDNESDAY");
            if (cbThu.isChecked()) daysList.add("THURSDAY");
            if (cbFri.isChecked()) daysList.add("FRIDAY");
            if (cbSat.isChecked()) daysList.add("SATURDAY");
            if (cbSun.isChecked()) daysList.add("SUNDAY");

            if (!daysList.isEmpty()) {
                task.setDays(daysList.toArray(new String[0]));
                task.setRepeat(true);
            }

            // start date
            if (selectedDate != null) {
                task.setStartDate(selectedDate.format(ISO));
            }

            if (listener != null) listener.onTaskCreated(task);
            dismiss();
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (getWindow() != null) {
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

}
