package com.example.houses.ui;

import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import com.example.houses.R;
import com.example.houses.model.ChatData;
import com.example.houses.model.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewTaskDialog extends Dialog {

    private TextInputEditText editTitle, editDesc, editMoney;
    private MaterialButton btnCreate, btnCancel, btnPickDate;
    private TextView tvStartDate;
    private Chip chipMon, chipTue, chipWed, chipThu, chipFri, chipSat, chipSun;

    // Новые поля для выбора пользователя
    private AutoCompleteTextView spinnerTargetUser;
    private Map<String, String> userLoginMap = new HashMap<>();
    private List<ChatData> chatUsers = new ArrayList<>();
    private String selectedTargetLogin = null;

    private final String chatLogin;
    private final NewTaskListener listener;

    private LocalDate selectedDate = null;
    private static final DateTimeFormatter FORMATTER_RU = DateTimeFormatter.ofPattern("d MMMM yyyy");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
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

    // Метод для установки списка пользователей (вызвать перед show())
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

        // Инициализация полей ввода
        editTitle = findViewById(R.id.editTaskTitle);
        editDesc = findViewById(R.id.editTaskDesc);
        editMoney = findViewById(R.id.editTaskMoney);
        scrollView = findViewById(R.id.scrollView);

        tvStartDate = findViewById(R.id.tvStartDate);
        btnPickDate = findViewById(R.id.btnPickDate);

        // Инициализация Чипсов
        chipMon = findViewById(R.id.chipMon);
        chipTue = findViewById(R.id.chipTue);
        chipWed = findViewById(R.id.chipWed);
        chipThu = findViewById(R.id.chipThu);
        chipFri = findViewById(R.id.chipFri);
        chipSat = findViewById(R.id.chipSat);
        chipSun = findViewById(R.id.chipSun);

        btnCreate = findViewById(R.id.btnCreateTask);
        btnCancel = findViewById(R.id.btnCancelTask);

        // Инициализация спиннера пользователей
        spinnerTargetUser = findViewById(R.id.spinnerTargetUser);
        setupUserSpinner();

        // Обработчик выбора пользователя
        spinnerTargetUser.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDisplay = (String) parent.getItemAtPosition(position);
            selectedTargetLogin = userLoginMap.get(selectedDisplay);
        });

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

        tvToggleExtra = findViewById(R.id.tvToggleExtra);
        cardExtra = findViewById(R.id.cardExtra);

        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);

        tvToggleExtra.setOnClickListener(v -> {
            if (cardExtra.getVisibility() == View.GONE) {
                cardExtra.setVisibility(View.VISIBLE);
                tvToggleExtra.setText("Дополнительные параметры ▴");
            } else {
                cardExtra.setVisibility(View.GONE);
                tvToggleExtra.setText("Дополнительные параметры ▾");
            }
        });

        btnStartTime.setOnClickListener(v -> {
            TimePickerDialog tp = new TimePickerDialog(getContext(),
                    (view, hour, minute) -> {
                        startTime = String.format("%02d:%02d", hour, minute);
                        btnStartTime.setText("Начало: " + startTime);
                    }, 8, 0, true);
            tp.show();
        });

        btnEndTime.setOnClickListener(v -> {
            TimePickerDialog tp = new TimePickerDialog(getContext(),
                    (view, hour, minute) -> {
                        endTime = String.format("%02d:%02d", hour, minute);
                        btnEndTime.setText("Конец: " + endTime);
                    }, 22, 0, true);
            tp.show();
        });
        scrollView.setOnTouchListener((v, event) -> {
            hideKeyboard(); // скрываем клавиатуру при любом касании/движении
            return false; // возвращаем false, чтобы скролл всё равно работал
        });

    }

    private void setupUserSpinner() {
        if (spinnerTargetUser == null) return;

        List<String> displayNames = new ArrayList<>();
        userLoginMap.clear();

        // Опция "Не назначено" (доступно всем)
        displayNames.add("Любой (не назначено)");
        userLoginMap.put("Любой (не назначено)", null);

        for (ChatData user : chatUsers) {
            // Фильтруем: показываем только CHILD
            if ("CHILD".equals(user.getUserRole())) {
                String display = user.getUserLogin() + " 👶";
                displayNames.add(display);
                userLoginMap.put(display, user.getUserLogin());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                displayNames
        );
        spinnerTargetUser.setAdapter(adapter);

        // Опционально: если нет детей, можно скрыть спиннер или показать сообщение
        if (displayNames.size() == 1) { // Только "Любой", детей нет
            // spinnerTargetUser.setEnabled(false); // Заблокировать выбор
            // или просто оставить только опцию "Любой"
        }
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

        // Время доступности
        task.setStartTime(startTime);
        task.setEndTime(endTime);

        // Назначенный пользователь (target)
        task.setTargetLogin(selectedTargetLogin);

        // Период дня
        RadioGroup rgPartDay = findViewById(R.id.rgPartDay);
        int pdId = rgPartDay.getCheckedRadioButtonId();
        if (pdId == R.id.rbMorning) task.setPartDay("MORNING");
        else if (pdId == R.id.rbDay) task.setPartDay("DAY");
        else if (pdId == R.id.rbEvening) task.setPartDay("EVENING");

        // Важность
        RadioGroup rgImportance = findViewById(R.id.rgImportance);
        int impId = rgImportance.getCheckedRadioButtonId();
        if (impId == R.id.rbImp1) task.setImportance(1);
        else if (impId == R.id.rbImp2) task.setImportance(2);
        else if (impId == R.id.rbImp3) task.setImportance(3);
        else task.setImportance(1);

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
    private void hideKeyboard() {
        if (getWindow() == null) return;

        InputMethodManager imm =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm == null) return;

        View view = getWindow().getDecorView();
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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