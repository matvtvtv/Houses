package com.example.houses.adapter;

import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.houses.R;
import com.example.houses.model.TaskInstanceDto;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface OnTaskActionListener {
        void onClaim(TaskInstanceDto task, int position);   // "Взять"
        void onStart(TaskInstanceDto task, int position);   // "Начать"
        void onFinish(TaskInstanceDto task, int position);  // "Завершить" (открыть диалог сохранения)
        void onConfirmByParent(TaskInstanceDto task, int position); // "Подтвердить" (parent)
        void onOpenComments(TaskInstanceDto task, int position);
        void onEdit(TaskInstanceDto task, int position);

    }

    private static final String TAG = "TaskAdapter";

    private final List<TaskInstanceDto> allItems = new ArrayList<>();
    private final List<TaskInstanceDto> visibleItems = new ArrayList<>();

    private java.time.LocalDate selectedDate;
    public String currentUserRole;
    public String currentUserLogin;

    private final OnTaskActionListener listener;

    private final Set<Long> reminderSentIds = new HashSet<>();

    public TaskAdapter(String userRole, String userLogin, OnTaskActionListener listener) {
        this.currentUserRole = userRole;
        this.currentUserLogin = userLogin;
        this.listener = listener;
    }

    // PUBLIC API
    public void setAll(List<TaskInstanceDto> list) {
        allItems.clear();
        if (list != null) allItems.addAll(list);
        applyFilter();
    }

    public void addOrUpdate(TaskInstanceDto task) {
        boolean updated = false;
        if (task == null) return;
        for (int i = 0; i < allItems.size(); i++) {
            TaskInstanceDto t = allItems.get(i);
            if (t.instanceId != null && t.instanceId.equals(task.instanceId)) {
                allItems.set(i, task);
                updated = true;
                break;
            }
        }
        if (!updated) allItems.add(task);
        applyFilter();
    }

    public void setSelectedDate(java.time.LocalDate date) {
        this.selectedDate = date;
        applyFilter();
    }

    public void refresh() {
        applyFilter();
    }

    public List<TaskInstanceDto> getTasksForOneHourReminder() {
        List<TaskInstanceDto> result = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();

        for (TaskInstanceDto t : allItems) {
            try {
                if (t == null) continue;
                if (Boolean.TRUE.equals(t.completed)) continue;
                if (t.startTime == null || t.startTime.isEmpty()) continue;
                if (t.instanceId == null) continue;
                if (reminderSentIds.contains(t.instanceId)) continue;
                if (t.taskDate == null || t.taskDate.isEmpty()) continue;

                java.time.LocalDate taskDate = java.time.LocalDate.parse(t.taskDate);
                if (!taskDate.equals(today)) continue;

                java.time.LocalTime start = java.time.LocalTime.parse(t.startTime);
                long minutes = java.time.Duration.between(now, start).toMinutes();

                if (minutes > 0 && minutes <= 60) {
                    result.add(t);
                }
            } catch (Exception ex) {
                Log.d(TAG, "Reminder parse error: " + ex.getMessage());
            }
        }

        return result;
    }

    public void markReminderSent(Long instanceId) {
        if (instanceId == null) return;
        reminderSentIds.add(instanceId);
    }

    public void clearReminderMarks() {
        reminderSentIds.clear();
    }

    // FILTER
    private void applyFilter() {
        visibleItems.clear();
        for (TaskInstanceDto t : allItems) {
            if (t == null) continue;
            if ("CHILD".equals(currentUserRole)) {
                if (t.targetLogin != null && !t.targetLogin.isEmpty()) {
                    if (!t.targetLogin.equals(currentUserLogin)) continue;
                }
            }
            if (selectedDate != null && t.taskDate != null) {
                try {
                    java.time.LocalDate taskDate = java.time.LocalDate.parse(t.taskDate);
                    if (!selectedDate.equals(taskDate)) continue;
                } catch (Exception e) {
                    continue;
                }
            }
            visibleItems.add(t);
        }
        notifyDataSetChanged();
    }

    // time threshold helper (unchanged)
    public long getMillisUntilNextTimeThreshold() {
        try {
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.Duration best = null;
            for (TaskInstanceDto t : allItems) {
                if (t.startTime != null && !t.startTime.isEmpty()) {
                    try {
                        java.time.LocalTime s = java.time.LocalTime.parse(t.startTime);
                        if (s.isAfter(now)) {
                            java.time.Duration d = java.time.Duration.between(now, s);
                            if (best == null || d.compareTo(best) < 0) best = d;
                        }
                    } catch (Exception ignored) {}
                }
                if (t.endTime != null && !t.endTime.isEmpty()) {
                    try {
                        java.time.LocalTime e = java.time.LocalTime.parse(t.endTime);
                        if (e.isAfter(now)) {
                            java.time.Duration d = java.time.Duration.between(now, e);
                            if (best == null || d.compareTo(best) < 0) best = d;
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (best == null) return -1;
            long millis = best.toMillis();
            return Math.max(0, millis + 1000);
        } catch (Exception e) {
            return -1;
        }
    }

    // ADAPTER
    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TaskInstanceDto t = visibleItems.get(position);
        if (t == null) return;

        // 1. Сброс состояния к дефолту (белый фон, стандартные цвета)
        resetToDefaultStyle(holder);

        // Заполнение данных
        holder.title.setText(t.title != null ? t.title : "");
        holder.desc.setText(t.description != null ? t.description : "");
        holder.money.setText(t.money > 0 ? ("+" + t.money) : "");

        int importance = t.importance; // в TaskInstanceDto есть поле importance
        bindImportance(holder, importance);

        // Управление временем
        if (t.startTime != null && !t.startTime.isEmpty()) {
            holder.tvTaskTime.setVisibility(View.VISIBLE);
            holder.tvTaskTime.setText(t.startTime + (t.endTime != null && !t.endTime.isEmpty() ? " – " + t.endTime : ""));
        } else {
            holder.tvTaskTime.setVisibility(View.GONE);
        }
        // показать Edit для PARENT и ADMIN (только для шаблона/владельца или по условиям)
        if ("PARENT".equals(currentUserRole) || "ADMIN".equals(currentUserRole)) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> listener.onEdit(t, position));
        }


        // 2. Логика статусов и ЦВЕТОВОЙ ПОДСВЕТКИ
        if (Boolean.TRUE.equals(t.confirmedByParent)) {
            // --- ПОДТВЕРЖДЕНО (СЕРЫЙ) ---
            applyStatusTheme(holder, "Статус: Подтверждена", 0xFFF5F5F5, 0xFFBDBDBD, 0xFFE0E0E0, 0xFF9E9E9E);
            makeTaskGray(holder);
        }
        else if (Boolean.TRUE.equals(t.completed)) {
            // --- ЗАВЕРШЕНО (ЗЕЛЕНЫЙ) ---
            applyStatusTheme(holder, "Статус: Проверьте!", 0xFFE8F5E9, 0xFF2E7D32, 0xFF81C784, 0xFFC8E6C9);
            setupParentButtons(holder, t, position);
        }
        else if (t.userLogin == null || t.userLogin.isEmpty()) {
            // --- НЕ ВЗЯТА (СТАНДАРТ/ФИОЛЕТОВЫЙ) ---
            applyStatusTheme(holder, "Статус: Ожидает", 0xFFFFFFFF, 0xFF8E54E9, 0xFFF0F0F0, 0xFFF3E5F5);
            setupChildButtons(holder, t, position, false);
        }
        else if (Boolean.TRUE.equals(t.started)) {
            // --- В ПРОЦЕССЕ (ЖЕЛТЫЙ) ---
            applyStatusTheme(holder, "Статус: В работе — " + t.userLogin, 0xFFFFFDE7, 0xFFF57F17, 0xFFFFF176, 0xFFFFF9C4);
            setupChildButtons(holder, t, position, true);
        }
        else {
            // --- ПРОСТО ВЗЯТА (ГОЛУБОЙ) ---
            applyStatusTheme(holder, "Статус: Взята — " + t.userLogin, 0xFFE3F2FD, 0xFF1976D2, 0xFF64B5F6, 0xFFBBDEFB);
            setupChildButtons(holder, t, position, true);
        }
    }

    /**
     * Универсальный метод для смены "темы" карточки
     */
    private void applyStatusTheme(VH holder, String statusText, int cardBg, int accentColor, int strokeColor, int badgeBg) {
        holder.tvStartedBy.setText(statusText);
        holder.cardView.setCardBackgroundColor(cardBg);
        holder.cardView.setStrokeColor(strokeColor);

        // Меняем цвет текста и фона баджа статуса
        holder.tvStartedBy.setTextColor(accentColor);
        holder.tvStartedBy.setBackgroundTintList(ColorStateList.valueOf(badgeBg));
    }
    private void setupParentButtons(VH holder, TaskInstanceDto t, int position) {
        holder.btnComments.setVisibility(View.VISIBLE);
        holder.btnComments.setOnClickListener(v -> listener.onOpenComments(t, position));

        if (("PARENT".equals(currentUserRole) || "ADMIN".equals(currentUserRole))) {
            if (!Boolean.TRUE.equals(t.confirmedByParent)) {
                holder.btnConfirm.setVisibility(View.VISIBLE);
                holder.btnConfirm.setOnClickListener(v -> listener.onConfirmByParent(t, position));
            }
        }
    }

    private void setupChildButtons(VH holder, TaskInstanceDto t, int position, boolean isTaken) {
        if (!"CHILD".equals(currentUserRole)) {
            holder.btnComments.setVisibility(View.VISIBLE);
            return;
        }

        if (!isTaken) {
            holder.btnTake.setVisibility(View.VISIBLE);
            holder.btnTake.setText("Взять");
            holder.btnTake.setOnClickListener(v -> listener.onClaim(t, position));
            holder.btnComments.setVisibility(View.VISIBLE);
        } else if (currentUserLogin.equals(t.userLogin)) {
            holder.btnStart.setVisibility(View.VISIBLE);
            if (!Boolean.TRUE.equals(t.started)) {
                holder.btnStart.setText("Начать");
                holder.btnStart.setOnClickListener(v -> listener.onStart(t, position));
            } else {
                holder.btnStart.setText("Завершить");
                holder.btnStart.setOnClickListener(v -> listener.onFinish(t, position));
            }
        }
    }
    private void resetToDefaultStyle(VH holder) {
        holder.itemView.setAlpha(1f);
        holder.cardView.setStrokeWidth(3); // чуть толще для видимости границ
        holder.title.setTextColor(holder.defaultTitleColor);
        holder.desc.setTextColor(holder.defaultDescColor);
        holder.money.setTextColor(0xFF2E7D32); // Зеленый для денег всегда

        holder.btnTake.setVisibility(View.GONE);
        holder.btnStart.setVisibility(View.GONE);
        holder.btnConfirm.setVisibility(View.GONE);
        holder.btnComments.setVisibility(View.GONE);
        holder.btnEdit.setVisibility(View.GONE);

    }

    private void makeTaskGray(VH holder) {
        holder.itemView.setAlpha(0.6f);
        holder.title.setTextColor(0xFF9E9E9E);
        holder.desc.setTextColor(0xFFBDBDBD);
        holder.money.setTextColor(0xFF9E9E9E);
        // Скрываем все кнопки кроме результата
        holder.btnTake.setVisibility(View.GONE);
        holder.btnStart.setVisibility(View.GONE);
        holder.btnConfirm.setVisibility(View.GONE);
    }
    private void bindImportance(VH holder, int importance) {
        // importance: 1 — низкая, 2 — средняя, 3 — высокая
        int stripeColor;
        int badgeTextColor = 0xFF000000; // текст на бейдже (по умолчанию чёрный)
        int badgeBgColor;

        switch (importance) {
            case 3:
                stripeColor = 0xFFD32F2F; // красный — высокая
                badgeBgColor = 0xFFFFCDD2;
                break;
            case 2:
                stripeColor = 0xFFFFA000; // оранжевый — средняя
                badgeBgColor = 0xFFFFECB3;
                break;
            case 1:
            default:
                stripeColor = 0xFF2E7D32; // зелёный — низкая
                badgeBgColor = 0xFFC8E6C9;
                break;
        }

        // Устанавливаем цвет полосы и бейджа
        holder.tvPriorityBadge.setBackgroundTintList(ColorStateList.valueOf(badgeBgColor));
        holder.tvPriorityBadge.setTextColor(badgeTextColor);

        // Поставим текст — цифру важности
        holder.tvPriorityBadge.setText(String.valueOf(Math.max(1, Math.min(3, importance))));
        holder.tvPriorityBadge.setVisibility(View.VISIBLE);
    }


    static class VH extends RecyclerView.ViewHolder {
        com.google.android.material.card.MaterialCardView cardView;
        TextView title, desc, money, tvTaskTime, tvStartedBy;
        MaterialButton btnTake, btnStart, btnConfirm, btnComments;

        // сохраняем цвета по-умолчанию чтобы при recycle можно было вернуть
        final int defaultTitleColor;
        final int defaultDescColor;
        final int defaultMoneyColor;
        final int defaultStartedColor;
        TextView tvPriorityBadge;
        MaterialButton btnEdit;



        VH(@NonNull View v) {
            super(v);
            cardView = (com.google.android.material.card.MaterialCardView) v;
            title = v.findViewById(R.id.tvTaskTitle);
            desc = v.findViewById(R.id.tvTaskDesc);
            money = v.findViewById(R.id.tvTaskMoney);
            tvTaskTime = v.findViewById(R.id.tvTaskTime);
            tvStartedBy = v.findViewById(R.id.tvStartedBy);
            tvPriorityBadge = v.findViewById(R.id.tvPriorityBadge);
            btnEdit = v.findViewById(R.id.btnEdit);


            btnTake = v.findViewById(R.id.btnTake);
            btnStart = v.findViewById(R.id.btnStart);
            btnConfirm = v.findViewById(R.id.btnConfirm);
            btnComments = v.findViewById(R.id.btnComments);

            defaultTitleColor = title.getCurrentTextColor();
            defaultDescColor = desc.getCurrentTextColor();
            defaultMoneyColor = money.getCurrentTextColor();
            defaultStartedColor = tvStartedBy.getCurrentTextColor();
        }
    }
}
