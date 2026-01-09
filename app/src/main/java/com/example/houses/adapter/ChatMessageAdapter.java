package com.example.houses.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.R;
import com.example.houses.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.VH> {

    private static final int TYPE_MY = 1;
    private static final int TYPE_AN = 2;

    private final List<ChatMessage> items = new ArrayList<>();
    private final String myLogin;

    public ChatMessageAdapter(String myLogin) {
        this.myLogin = myLogin;
    }

    public void addMessage(ChatMessage m) {
        items.add(m);
        notifyItemInserted(items.size() - 1);
    }

    public void setAll(List<ChatMessage> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage m = items.get(position);
        return m.getSender().equals(myLogin) ? TYPE_MY : TYPE_AN;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_MY)
                ? R.layout.item_message_my
                : R.layout.item_message_an;

        View v = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);

        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChatMessage m = items.get(position);
        holder.tv.setText(m.getSender()+": "+m.getContent());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;

        VH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvMessage);
        }
    }
}
