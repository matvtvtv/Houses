package com.example.houses.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.R;
import com.example.houses.model.DayItem;

import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DayVH> {

    public interface OnDateClickListener {
        void onDateSelected(DayItem item);
    }

    private final List<DayItem> days;
    private final OnDateClickListener listener;

    public DateAdapter(List<DayItem> days, OnDateClickListener listener) {
        this.days = days;
        this.listener = listener;
        Log.d("DateAdapter", "Adapter created with " + days.size() + " items");
    }

    @NonNull
    @Override
    public DayVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day, parent, false);
        Log.d("DateAdapter", "onCreateViewHolder");
        return new DayVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DayVH h, int pos) {
        DayItem item = days.get(pos);
        Log.d("DateAdapter", "onBindViewHolder position: " + pos + ", date: " + item.date);

        h.tvDate.setText(String.valueOf(item.date.getDayOfMonth()));

        String dayName = item.date.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, new Locale("ru"));
        h.tvDay.setText(dayName);

        String monthName = item.date.getMonth()
                .getDisplayName(TextStyle.SHORT, new Locale("ru"));
        h.tvMonth.setText(monthName);

        h.itemView.setSelected(item.selected);

        h.itemView.setOnClickListener(v -> {
            for (DayItem d : days) d.selected = false;
            item.selected = true;
            notifyDataSetChanged();
            listener.onDateSelected(item);
        });
    }


    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayVH extends RecyclerView.ViewHolder {
        TextView tvDay, tvDate, tvMonth;

        DayVH(@NonNull View v) {
            super(v);
            tvDay = v.findViewById(R.id.tvDay);
            tvDate = v.findViewById(R.id.tvDate);
            tvMonth = v.findViewById(R.id.tvMonth);
            Log.d("DateAdapter.DayVH", "ViewHolder created");
        }
    }
}