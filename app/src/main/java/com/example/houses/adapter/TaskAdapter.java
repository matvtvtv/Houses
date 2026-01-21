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
        void onRespond(TaskInstanceDto task, int position);
        void onOpenComments(TaskInstanceDto task, int position);
    }


    private final List<TaskInstanceDto> allItems = new ArrayList<>();

    private final List<TaskInstanceDto> visibleItems = new ArrayList<>();

    private LocalDate selectedDate;
    private final OnTaskActionListener listener;

    public TaskAdapter(OnTaskActionListener listener) {
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

        // 3. Комментарий (Проверьте ID tvComment в XML!)
        if (t.comment != null && !t.comment.isEmpty()) {
            holder.tvComment.setText(t.comment);
            holder.tvComment.setVisibility(View.VISIBLE);
        } else {
            holder.tvComment.setVisibility(View.GONE);
        }

        // 4. Дни повтора
        if (t.repeat && t.repeatDays != null && !t.repeatDays.isEmpty()) {
            holder.repeatDays.setText(String.join(", ", t.repeatDays).toUpperCase());
            holder.repeatDays.setVisibility(View.VISIBLE);
        } else {
            holder.repeatDays.setVisibility(View.GONE);
        }

        // 5. Обработка ФОТО (Исправленная логика)
        View parentScroll = (View) holder.photoContainer.getParent(); // Это наш HorizontalScrollView
        holder.photoContainer.removeAllViews(); // Обязательно очищаем старые фото

        if (t.photoBase64 != null && !t.photoBase64.isEmpty()) {
            // Если фото есть — показываем и список, и скролл
            holder.photoContainer.setVisibility(View.VISIBLE);
            if (parentScroll != null) parentScroll.setVisibility(View.VISIBLE);

            for (String base64 : t.photoBase64) {
                ImageView imageView = new ImageView(holder.itemView.getContext());

                // Размер фото (например, 80dp)
                int size = (int) (80 * holder.itemView.getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(0, 0, 16, 0);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                // Скругление (требует наличия bg_photo_rounding в drawable)
                imageView.setClipToOutline(true);
                imageView.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
                imageView.setBackgroundResource(R.drawable.bg_photo_rounding);

                // Установка изображения
                imageView.setImageBitmap(ImageUtils.base64ToBitmap(base64));
                holder.photoContainer.addView(imageView);
            }
        } else {
            // Если фото нет — полностью скрываем блок
            holder.photoContainer.setVisibility(View.GONE);
            if (parentScroll != null) parentScroll.setVisibility(View.GONE);
        }

        // 6. Статус и Кнопки
        holder.cbCompleted.setChecked(t.completed);
        holder.itemView.setAlpha(t.completed ? 0.6f : 1.0f);

        holder.btnRespond.setOnClickListener(v -> {
            if (listener != null) listener.onRespond(t, holder.getBindingAdapterPosition());
        });

        holder.btnComments.setOnClickListener(v -> {
            if (listener != null) listener.onOpenComments(t, holder.getBindingAdapterPosition());
        });
    }



    static class VH extends RecyclerView.ViewHolder {
        TextView title, desc, money, repeatDays, tvComment;
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
            photoContainer = itemView.findViewById(R.id.photoContainer);
        }
    }

}
