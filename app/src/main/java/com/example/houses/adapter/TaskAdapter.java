package com.example.houses.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.R;
import com.example.houses.model.TaskInstanceDto;
import com.example.houses.utils.ImageUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface OnTaskActionListener {
        void onClaim(TaskInstanceDto task, int position);
        void onComplete(TaskInstanceDto task, int position);
        void onOpenComments(TaskInstanceDto task, int position);
    }

    private static final String TAG = "TaskAdapter";

    private final List<TaskInstanceDto> allItems = new ArrayList<>();
    private final List<TaskInstanceDto> visibleItems = new ArrayList<>();

    private LocalDate selectedDate;
    public String currentUserRole;
    public String currentUserLogin;

    private final OnTaskActionListener listener;

    public TaskAdapter(String userRole, String userLogin, OnTaskActionListener listener) {
        this.currentUserRole = userRole;
        this.currentUserLogin = userLogin;
        this.listener = listener;
    }

    // ================== PUBLIC API ==================

    public void setAll(List<TaskInstanceDto> list) {
        allItems.clear();
        if (list != null) allItems.addAll(list);
        applyFilter();
    }

    public void addOrUpdate(TaskInstanceDto task) {
        boolean updated = false;

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

    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
        applyFilter();
    }

    // ================== FILTERING ==================

    private void applyFilter() {
        visibleItems.clear();

        for (TaskInstanceDto t : allItems) {
            Log.d(TAG, "Task data: startTime=" + t.startTime +
                    ", endTime=" + t.endTime +
                    ", partDay=" + t.partDay +
                    ", targetLogin=" + t.targetLogin);

            // 1. ФИЛЬТР ПО targetLogin (строгое правило)
            if (t.targetLogin != null && !t.targetLogin.isEmpty()) {
                if (!t.targetLogin.equals(currentUserLogin)) {
                    continue; // задача НЕ для этого пользователя
                }
            }


            // 2. Дата
            if (selectedDate != null && t.taskDate != null) {
                try {
                    LocalDate taskDate = LocalDate.parse(t.taskDate);
                    if (!selectedDate.equals(taskDate)) continue;
                } catch (Exception e) {
                    continue;
                }
            }

            // 3. Время
            if (!isTimeInRange(t.startTime, t.endTime)) continue;

            // 4. Период дня
            if (!matchesPartDay(t.partDay)) continue;

            visibleItems.add(t);
        }

        notifyDataSetChanged();
    }

    private boolean isTimeInRange(String start, String end) {
        try {
            LocalTime now = LocalTime.now();

            // Если есть startTime и сейчас раньше — не показываем
            if (start != null) {
                LocalTime s = LocalTime.parse(start);
                if (now.isBefore(s)) {
                    Log.d(TAG, "Too early, hiding task");
                    return false;
                }
            }

            // Если есть endTime и сейчас позже — не показываем
            if (end != null) {
                LocalTime e = LocalTime.parse(end);
                if (now.isAfter(e)) {
                    Log.d(TAG, "Too late, hiding task");
                    return false;
                }
            }

            return true; // В диапазоне или нет ограничений

        } catch (Exception e) {
            Log.e(TAG, "Time parse error: " + e.getMessage());
            return true; // При ошибке парсинга показываем на всякий случай
        }
    }

    public void refresh() {
        applyFilter();
    }

    private boolean matchesPartDay(String partDay) {
        if (partDay == null) return true;

        int hour = LocalTime.now().getHour();

        switch (partDay) {
            case "MORNING": return hour >= 6 && hour < 12;
            case "DAY":     return hour >= 12 && hour < 18;
            case "EVENING": return hour >= 18 && hour < 23;
            default: return true;
        }
    }

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
            // добавим небольшой буфер 1 сек
            return Math.max(0, millis + 1000);
        } catch (Exception e) {
            return -1;
        }
    }

    // ================== ADAPTER ==================

    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TaskInstanceDto t = visibleItems.get(position);

        // ===== Цвет по важности =====
        int importance = t.importance > 0 ? t.importance : 1;
        switch (importance) {
            case 3:
                holder.itemView.setBackgroundResource(R.drawable.bg_task_important);
                break;
            case 2:
                holder.itemView.setBackgroundResource(R.drawable.bg_task_medium);
                break;
            default:
                holder.itemView.setBackgroundResource(R.drawable.bg_task_normal);
        }

        // ===== Текст =====
        holder.title.setText(t.title != null ? t.title : "");
        holder.desc.setText(t.description != null ? t.description : "");
        holder.desc.setVisibility(
                t.description != null && !t.description.isEmpty() ? View.VISIBLE : View.GONE
        );

        holder.money.setText(String.valueOf(t.money));

        // ===== Комментарий =====
        if (t.comment != null && !t.comment.isEmpty()) {
            holder.tvComment.setText(t.comment);
            holder.tvComment.setVisibility(View.VISIBLE);
        } else {
            holder.tvComment.setVisibility(View.GONE);
        }

        // ===== Время / Часть дня =====
        StringBuilder timeBuilder = new StringBuilder();
        if (t.startTime != null && !t.startTime.isEmpty()) {
            timeBuilder.append(t.startTime);
        }
        if (t.endTime != null && !t.endTime.isEmpty()) {
            if (timeBuilder.length() > 0) {
                timeBuilder.append(" - ");
            } else {
                timeBuilder.append("До ");
            }
            timeBuilder.append(t.endTime);
        }

        // Если времени нет, но есть часть дня
        if (timeBuilder.length() == 0 && t.partDay != null && !t.partDay.isEmpty()) {
            switch (t.partDay) {
                case "MORNING": timeBuilder.append("☀️ Утро (6:00-12:00)"); break;
                case "DAY":     timeBuilder.append("🌤 День (12:00-18:00)"); break;
                case "EVENING": timeBuilder.append("🌙 Вечер (18:00-23:00)"); break;
                default:        timeBuilder.append(t.partDay);
            }
        }

        if (timeBuilder.length() > 0) {
            holder.tvTaskTime.setText(timeBuilder.toString());
            holder.tvTaskTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvTaskTime.setVisibility(View.GONE);
        }

        // ===== Кто выполняет =====
        if (t.userLogin != null && !t.userLogin.isEmpty()) {
            holder.tvStartedBy.setText("Выполняет: " + t.userLogin);
            holder.tvStartedBy.setVisibility(View.VISIBLE);
        } else {
            holder.tvStartedBy.setVisibility(View.GONE);
        }


        // ===== Дни повтора =====
        if (t.repeat && t.repeatDays != null && !t.repeatDays.isEmpty()) {
            holder.repeatDays.setText(String.join(", ", t.repeatDays));
            holder.repeatDays.setVisibility(View.VISIBLE);
        } else {
            holder.repeatDays.setVisibility(View.GONE);
        }

        // ===== Фото =====
        holder.photoContainer.removeAllViews();
        if (t.photoBase64 != null && !t.photoBase64.isEmpty()) {
            holder.photoContainer.setVisibility(View.VISIBLE);
            for (String base64 : t.photoBase64) {
                ImageView iv = new ImageView(holder.itemView.getContext());
                int size = (int) (80 * holder.itemView.getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(size, size);
                p.setMargins(0, 0, 16, 0);
                iv.setLayoutParams(p);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackgroundResource(R.drawable.bg_photo_rounding);
                iv.setImageBitmap(ImageUtils.base64ToBitmap(base64));
                holder.photoContainer.addView(iv);
            }
        } else {
            holder.photoContainer.setVisibility(View.GONE);
        }

        // ===== Статус =====
        holder.cbCompleted.setChecked(t.completed);
        holder.itemView.setAlpha(t.completed ? 0.6f : 1f);

        // ===== Кнопки =====
        holder.btnRespond.setVisibility(View.GONE);

        if (!t.completed) {
            if ("PARENT".equals(currentUserRole) || "ADMIN".equals(currentUserRole)) {
                holder.btnRespond.setText("Подтвердить");
                holder.btnRespond.setVisibility(View.VISIBLE);
                holder.btnRespond.setOnClickListener(v ->
                        listener.onComplete(t, position));
            } else {
                // Для обычных пользователей - проверяем, назначена ли задача им
                boolean isMyTask = (t.targetLogin == null || t.targetLogin.isEmpty())
                        || t.targetLogin.equals(currentUserLogin);

                if (isMyTask) {
                    if (t.userLogin == null || t.userLogin.isEmpty()) {
                        holder.btnRespond.setText("Взять");
                        holder.btnRespond.setVisibility(View.VISIBLE);
                        holder.btnRespond.setOnClickListener(v ->
                                listener.onClaim(t, position));
                    } else if (currentUserLogin.equals(t.userLogin)) {
                        holder.btnRespond.setText("В процессе");
                        holder.btnRespond.setVisibility(View.VISIBLE);
                        holder.btnRespond.setEnabled(false);
                    }
                }
            }
        }

        holder.btnComments.setOnClickListener(v ->
                listener.onOpenComments(t, holder.getBindingAdapterPosition()));
    }

    // ================== VIEW HOLDER ==================

    static class VH extends RecyclerView.ViewHolder {
        TextView title, desc, money, repeatDays, tvComment, tvStartedBy, tvTaskTime;
        CheckBox cbCompleted;
        Button btnRespond, btnComments;
        LinearLayout photoContainer;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tvTaskTitle);
            desc = v.findViewById(R.id.tvTaskDesc);
            money = v.findViewById(R.id.tvTaskMoney);
            repeatDays = v.findViewById(R.id.tvTaskRepeatDays);
            cbCompleted = v.findViewById(R.id.cbTaskCompleted);
            btnRespond = v.findViewById(R.id.btnRespond);
            btnComments = v.findViewById(R.id.btnComments);
            tvComment = v.findViewById(R.id.tvComment);
            tvStartedBy = v.findViewById(R.id.tvStartedBy);
            photoContainer = v.findViewById(R.id.photoContainer);
            tvTaskTime = v.findViewById(R.id.tvTaskTime);
        }
    }
}