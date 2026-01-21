package com.example.houses.adapter;

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

import com.bumptech.glide.Glide;
import com.example.houses.R;
import com.example.houses.model.TaskInstanceDto;
import com.example.houses.utils.ImageUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public interface OnTaskActionListener {

            void onClaim(TaskInstanceDto task, int position);     // ребёнок берёт задачу
            void onComplete(TaskInstanceDto task, int position);  // родитель подтверждает
            void onOpenComments(TaskInstanceDto task, int position);

    }


    private final List<TaskInstanceDto> allItems = new ArrayList<>();

    private final List<TaskInstanceDto> visibleItems = new ArrayList<>();
    public String currentUserRole;
    public String currentUserLogin;


    private LocalDate selectedDate;
    private final OnTaskActionListener listener;

    public TaskAdapter(String userRole, String userLogin, OnTaskActionListener listener) {
        this.currentUserRole = userRole;
        this.currentUserLogin = userLogin;
        this.listener = listener;
    }




    /** Установить ВСЕ задачи */
    public void setAll(List<TaskInstanceDto> list) {
        allItems.clear();
        if (list != null) {
            allItems.addAll(list);
        }
        applyFilter();
    }

    /** Добавить или обновить задачу (из WS или UI) */
    public void addOrUpdate(TaskInstanceDto task) {
        boolean updated = false;

        for (int i = 0; i < allItems.size(); i++) {
            TaskInstanceDto current = allItems.get(i);
            if (current.getInstanceId() != null &&
                    current.getInstanceId().equals(task.getInstanceId())) {
                allItems.set(i, task);
                updated = true;
                break;
            }
        }

        if (!updated) {
            allItems.add(task);
        }

        applyFilter();
    }

    /** Установить выбранную дату */
    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
        applyFilter();
    }

    private static final String TAG = "TaskAdapter";

    private void applyFilter() {
        visibleItems.clear();
        Log.d(TAG, "Filtering " + allItems.size() + " tasks for date: " + selectedDate);

        for (TaskInstanceDto t : allItems) {
            if (t.getTaskDate() == null) {
                Log.w(TAG, "Task has null date: " + t.title);
                continue;
            }

            try {
                LocalDate taskDate = LocalDate.parse(t.getTaskDate());
                if (selectedDate.equals(taskDate)) {
                    visibleItems.add(t);
                    Log.d(TAG, "Added task: " + t.title + " for " + taskDate);
                }
            } catch (Exception e) {
                Log.e(TAG, "Invalid date format: " + t.getTaskDate(), e);
            }
        }

        Log.d(TAG, "Visible items: " + visibleItems.size());
        notifyDataSetChanged();
    }


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

        // 1. Заголовок и Описание
        holder.title.setText(t.title != null ? t.title : "");
        holder.desc.setText(t.description != null ? t.description : "");
        holder.desc.setVisibility(t.description != null && !t.description.isEmpty() ? View.VISIBLE : View.GONE);

        // 2. Награда
        holder.money.setText(String.valueOf(t.money));

        // 3. Комментарий
        if (t.comment != null && !t.comment.isEmpty()) {
            holder.tvComment.setText(t.comment);
            holder.tvComment.setVisibility(View.VISIBLE);
        } else {
            holder.tvComment.setVisibility(View.GONE);
        }

        // 4. Кто начал выполнение
        if (t.userLogin != null && !t.userLogin.isEmpty()) {
            holder.tvStartedBy.setText("Выполняет: " + t.userLogin);
            holder.tvStartedBy.setVisibility(View.VISIBLE);
        } else {
            holder.tvStartedBy.setVisibility(View.GONE);
        }

        // 5. Дни повтора
        if (t.repeat && t.repeatDays != null && !t.repeatDays.isEmpty()) {
            holder.repeatDays.setText(String.join(", ", t.repeatDays).toUpperCase());
            holder.repeatDays.setVisibility(View.VISIBLE);
        } else {
            holder.repeatDays.setVisibility(View.GONE);
        }

        // 6. Обработка ФОТО
        View parentScroll = (View) holder.photoContainer.getParent();
        holder.photoContainer.removeAllViews();

        if (t.photoBase64 != null && !t.photoBase64.isEmpty()) {
            holder.photoContainer.setVisibility(View.VISIBLE);
            if (parentScroll != null) parentScroll.setVisibility(View.VISIBLE);

            for (String base64 : t.photoBase64) {
                ImageView imageView = new ImageView(holder.itemView.getContext());
                int size = (int) (80 * holder.itemView.getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(0, 0, 16, 0);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setClipToOutline(true);
                imageView.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
                imageView.setBackgroundResource(R.drawable.bg_photo_rounding);
                imageView.setImageBitmap(ImageUtils.base64ToBitmap(base64));
                holder.photoContainer.addView(imageView);
            }
        } else {
            holder.photoContainer.setVisibility(View.GONE);
            if (parentScroll != null) parentScroll.setVisibility(View.GONE);
        }

        // 7. Статус и Кнопки
        holder.cbCompleted.setChecked(t.completed);
        holder.itemView.setAlpha(t.completed ? 0.6f : 1.0f);

        holder.btnRespond.setVisibility(View.GONE); // Скрываем по умолчанию

        if (!t.completed) {
            if ("PARENT".equals(currentUserRole)) {
                // Родитель видит кнопку "Подтвердить"
                holder.btnRespond.setText("Подтвердить");
                holder.btnRespond.setVisibility(View.VISIBLE);
                holder.btnRespond.setEnabled(true);
                holder.btnRespond.setOnClickListener(v -> {
                    if (listener != null) listener.onComplete(t, position);
                });
            } else {
                // Ребенок: логика взятия задачи
                if (t.userLogin == null || t.userLogin.isEmpty()) {
                    // Задача свободна
                    holder.btnRespond.setText("Взять");
                    holder.btnRespond.setVisibility(View.VISIBLE);
                    holder.btnRespond.setEnabled(true);
                    holder.btnRespond.setOnClickListener(v -> {
                        if (listener != null) listener.onClaim(t, position);
                    });
                } else if (currentUserLogin.equals(t.userLogin)) {
                    // Задача взята мной
                    holder.btnRespond.setText("В процессе");
                    holder.btnRespond.setVisibility(View.VISIBLE);
                    holder.btnRespond.setEnabled(false);
                } else {
                    // Задача взята другим
                    holder.btnRespond.setVisibility(View.GONE);
                }
            }
        }

        holder.btnComments.setOnClickListener(v -> {
            if (listener != null) listener.onOpenComments(t, holder.getBindingAdapterPosition());
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, desc, money, repeatDays, tvComment, tvStartedBy;
        CheckBox cbCompleted;
        Button btnRespond, btnComments;
        LinearLayout photoContainer;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTaskTitle);
            desc = itemView.findViewById(R.id.tvTaskDesc);
            money = itemView.findViewById(R.id.tvTaskMoney);
            repeatDays = itemView.findViewById(R.id.tvTaskRepeatDays);
            cbCompleted = itemView.findViewById(R.id.cbTaskCompleted);
            btnRespond = itemView.findViewById(R.id.btnRespond);
            btnComments = itemView.findViewById(R.id.btnComments);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvStartedBy = itemView.findViewById(R.id.tvStartedBy);
            photoContainer = itemView.findViewById(R.id.photoContainer);
        }
    }
}
