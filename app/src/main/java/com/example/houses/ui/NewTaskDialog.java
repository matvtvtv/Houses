package com.example.houses.ui;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import com.example.houses.R;
import com.example.houses.model.ChatData;
import com.example.houses.model.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewTaskDialog extends Dialog {

    private SharedPreferences preferences;

    private TextInputEditText editTitle, editDesc, editMoney;
    private MaterialButton btnCreate, btnCancel, btnPickDate;
    private TextView tvStartDate;

    private Chip chipMon, chipTue, chipWed, chipThu, chipFri, chipSat, chipSun;

    private AutoCompleteTextView spinnerTargetUser;
    private Map<String, String> userLoginMap = new HashMap<>();
    private List<ChatData> chatUsers = new ArrayList<>();
    private String selectedTargetLogin = null;

    private final String chatLogin;
    private final NewTaskListener listener;

    private String userRole = "CHILD";
    private String userLogin;

    private LocalDate selectedDate = null;
    private static final DateTimeFormatter FORMATTER_RU =
            DateTimeFormatter.ofPattern("d MMMM yyyy");
    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ISO_LOCAL_DATE;

    private MaterialButton btnStartTime, btnEndTime;
    private TextView tvToggleExtra;
    private View cardExtra;
    private NestedScrollView scrollView;

    private String startTime = null;
    private String endTime = null;

    public interface NewTaskListener {
        void onTaskCreated(Task task);
    }

    public NewTaskDialog(@NonNull Context ctx, String chatLogin, NewTaskListener listener) {
        super(ctx);
        this.chatLogin = chatLogin;
        this.listener = listener;
    }

    public void setChatUsers(List<ChatData> users) {
        this.chatUsers = users != null ? users : new ArrayList<>();
        if (spinnerTargetUser != null) {
            setupUserSpinner();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_new_task);

        editTitle = findViewById(R.id.editTaskTitle);
        editDesc = findViewById(R.id.editTaskDesc);
        editMoney = findViewById(R.id.editTaskMoney);
        scrollView = findViewById(R.id.scrollView);

        tvStartDate = findViewById(R.id.tvStartDate);
        btnPickDate = findViewById(R.id.btnPickDate);

        chipMon = findViewById(R.id.chipMon);
        chipTue = findViewById(R.id.chipTue);
        chipWed = findViewById(R.id.chipWed);
        chipThu = findViewById(R.id.chipThu);
        chipFri = findViewById(R.id.chipFri);
        chipSat = findViewById(R.id.chipSat);
        chipSun = findViewById(R.id.chipSun);

        btnCreate = findViewById(R.id.btnCreateTask);
        btnCancel = findViewById(R.id.btnCancelTask);

        spinnerTargetUser = findViewById(R.id.spinnerTargetUser);


        preferences = getContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        userLogin = preferences.getString("login", "1");
        userRole = preferences.getString("role", "CHILD");

        if ("CHILD".equals(userRole)) {
            editMoney.setVisibility(View.GONE);
            spinnerTargetUser.setVisibility(View.GONE);
        }

        spinnerTargetUser.setOnItemClickListener((parent, view, position, id) -> {
            String display = (String) parent.getItemAtPosition(position);
            selectedTargetLogin = userLoginMap.get(display);
        });

        btnPickDate.setOnClickListener(v -> {
            LocalDate now = LocalDate.now();
            DatePickerDialog dp = new DatePickerDialog(
                    getContext(),
                    (view, y, m, d) -> {
                        selectedDate = LocalDate.of(y, m + 1, d);
                        tvStartDate.setText(selectedDate.format(FORMATTER_RU));
                    },
                    now.getYear(),
                    now.getMonthValue() - 1,
                    now.getDayOfMonth()
            );
            dp.show();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnCreate.setOnClickListener(v -> {
            if (validateAndCreate()) dismiss();
        });

        tvToggleExtra = findViewById(R.id.tvToggleExtra);
        cardExtra = findViewById(R.id.cardExtra);

        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);

        tvToggleExtra.setOnClickListener(v -> {
            setupUserSpinner();
            boolean show = cardExtra.getVisibility() == View.GONE;
            cardExtra.setVisibility(show ? View.VISIBLE : View.GONE);
            tvToggleExtra.setText(
                    show ? "Дополнительные параметры ▴" : "Дополнительные параметры ▾"
            );
        });

        btnStartTime.setOnClickListener(v -> {
            new TimePickerDialog(getContext(),
                    (view, h, m) -> {
                        startTime = String.format("%02d:%02d", h, m);
                        btnStartTime.setText("Начало: " + startTime);
                    }, 8, 0, true).show();
        });

        btnEndTime.setOnClickListener(v -> {
            new TimePickerDialog(getContext(),
                    (view, h, m) -> {
                        endTime = String.format("%02d:%02d", h, m);
                        btnEndTime.setText("Конец: " + endTime);
                    }, 22, 0, true).show();
        });

        scrollView.setOnTouchListener((v, e) -> {
            hideKeyboard();
            return false;
        });
    }

    private void setupUserSpinner() {
        if (spinnerTargetUser == null) return;

        List<String> items = new ArrayList<>();
        userLoginMap.clear();

        items.add("Любой (не назначено)");
        userLoginMap.put("Любой (не назначено)", null);

        for (ChatData user : chatUsers) {
            if ("CHILD".equals(user.getUserRole())) {
                items.add(user.getUserLogin());
                userLoginMap.put(user.getUserLogin(), user.getUserLogin());
            }
        }

        spinnerTargetUser.setAdapter(new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                items
        ));
    }

    private boolean validateAndCreate() {
        String title = editTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(getContext(), "Введите название задачи", Toast.LENGTH_SHORT).show();
            return false;
        }

        int money = 0;
        if (!"CHILD".equals(userRole)) {
            String m = editMoney.getText().toString().trim();
            if (TextUtils.isEmpty(m)) {
                Toast.makeText(getContext(), "Введите награду", Toast.LENGTH_SHORT).show();
                return false;
            }
            try {
                money = Integer.parseInt(m);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Награда должна быть числом", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        Task task = new Task();
        task.setChatLogin(chatLogin);
        task.setTitle(title);
        task.setDescription(editDesc.getText().toString().trim());
        task.setMoney(money);
        task.setCompleted(false);
        task.setStartTime(startTime);
        task.setEndTime(endTime);

        task.setTargetLogin(
                "CHILD".equals(userRole) ? userLogin : selectedTargetLogin
        );

        RadioGroup rgDay = findViewById(R.id.rgPartDay);
        int pd = rgDay.getCheckedRadioButtonId();
        if (pd == R.id.rbMorning) task.setPartDay("MORNING");
        else if (pd == R.id.rbDay) task.setPartDay("DAY");
        else if (pd == R.id.rbEvening) task.setPartDay("EVENING");

        List<String> days = new ArrayList<>();
        if (chipMon.isChecked()) days.add("MONDAY");
        if (chipTue.isChecked()) days.add("TUESDAY");
        if (chipWed.isChecked()) days.add("WEDNESDAY");
        if (chipThu.isChecked()) days.add("THURSDAY");
        if (chipFri.isChecked()) days.add("FRIDAY");
        if (chipSat.isChecked()) days.add("SATURDAY");
        if (chipSun.isChecked()) days.add("SUNDAY");

        if (!days.isEmpty()) {
            task.setRepeat(true);
            task.setDays(days.toArray(new String[0]));
        }

        if (selectedDate != null) {
            task.setStartDate(selectedDate.format(ISO));
        }

        listener.onTaskCreated(task);
        return true;
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getWindow() != null) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
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
