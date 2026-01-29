package com.example.houses.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.R;
import com.example.houses.model.UserStats;

import java.util.ArrayList;
import java.util.List;

public class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.StatsViewHolder> {

    private final String currentUserLogin;
    private final OnUserClickListener listener;
    private List<UserStats> statsList = new ArrayList<>();

    public StatsAdapter(String currentUserLogin, OnUserClickListener listener) {
        this.currentUserLogin = currentUserLogin;
        this.listener = listener;
    }

    public void setData(List<UserStats> newData) {
        this.statsList = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stats, parent, false);
        return new StatsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatsViewHolder holder, int position) {
        UserStats stats = statsList.get(position);

        holder.tvLogin.setText(stats.getUserLogin());
        holder.tvMoney.setText(stats.getMoney() + " монет");
        holder.tvTasks.setText(stats.getTotalCompletedTasks() + " задач");

        if (stats.getUserLogin().equals(currentUserLogin)) {
            holder.itemView.setBackgroundResource(R.drawable.bg_current_user);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(stats.getUserLogin());
            }
        });
    }

    @Override
    public int getItemCount() {
        return statsList.size();
    }

    static class StatsViewHolder extends RecyclerView.ViewHolder {
        TextView tvLogin, tvMoney, tvTasks;

        StatsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogin = itemView.findViewById(R.id.tvUserLogin);
            tvMoney = itemView.findViewById(R.id.tvMoney);
            tvTasks = itemView.findViewById(R.id.tvCompletedTasks);
        }
    }
    public interface OnUserClickListener {
        void onUserClick(String userLogin);
    }

}