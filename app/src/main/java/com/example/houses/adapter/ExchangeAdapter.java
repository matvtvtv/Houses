package com.example.houses.adapter;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.R;
import com.example.houses.model.ExchangeOffer;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExchangeAdapter extends RecyclerView.Adapter<ExchangeAdapter.VH> {

    private final List<ExchangeOffer> data = new ArrayList<>();

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_exchange, p, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        ExchangeOffer o = data.get(i);

        h.title.setText(o.getTitle() != null ? o.getTitle() : "Без названия");

        // ИСПРАВЛЕНО: проверка на null для month
        Month month = o.getMonth();
        if (month != null) {
            h.month.setText(month.getDisplayName(TextStyle.FULL_STANDALONE,
                    new Locale("ru", "RU")));
        } else {
            h.month.setText("Месяц не указан");
        }

        h.desc.setText(o.getDescription() != null ? o.getDescription() : "");

        if (o.isActive()) {
            h.itemView.setAlpha(1.0f);
        } else {
            h.itemView.setAlpha(0.5f);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void addOrUpdate(ExchangeOffer offer) {
        if (offer == null || offer.getId() == null) return;

        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId().equals(offer.getId())) {
                data.set(i, offer);
                notifyItemChanged(i);
                return;
            }
        }
        data.add(0, offer);
        notifyItemInserted(0);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, month, desc;

        VH(View v) {
            super(v);
            title = v.findViewById(R.id.tvTitle);
            month = v.findViewById(R.id.tvMonth);
            desc = v.findViewById(R.id.tvDescription);
        }
    }
}