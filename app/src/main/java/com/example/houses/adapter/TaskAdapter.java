package com.example.houses.adapter;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.R;
import com.example.houses.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    private final List<Task> items = new ArrayList<>();

    public TaskAdapter() {}

    public void setAll(List<Task> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void addTask(Task t) {
        items.add(t);
        notifyItemInserted(items.size() - 1);
    }

    public void updateTask(Task t) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() != null && items.get(i).getId().equals(t.getId())) {
                items.set(i, t);
                notifyItemChanged(i);
                return;
            }
        }
        // если новой — добавить
        addTask(t);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Task t = items.get(position);
        holder.title.setText(t.getTitle());
        holder.desc.setText(t.getDescription());
        holder.money.setText(String.valueOf(t.getMoney()));
        holder.cbCompleted.setChecked(t.isCompleted());
        // можно добавить callback'и по нажатию на checkbox
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title;
        TextView desc;
        TextView money;
        CheckBox cbCompleted;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTaskTitle);
            desc = itemView.findViewById(R.id.tvTaskDesc);
            cbCompleted = itemView.findViewById(R.id.cbTaskCompleted);
            money=itemView.findViewById(R.id.tvTaskMoney);
        }
    }
}
