package com.example.houses.adapter;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.houses.DB.DatabaseHelper;
import com.example.houses.R;
import com.example.houses.model.ChatMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.VH> {

    private static final int TYPE_MY = 1;
    private static final int TYPE_AN = 2;
    private SharedPreferences prefs;

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
        holder.tv.setText(m.getSender() + ": " + m.getContent());

        if (m.getImage() != null && !m.getImage().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(m.getImage(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.imgAvatar.setImageBitmap(bitmap);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        byte[] avatarBytes = DatabaseHelper
                .getInstance(holder.itemView.getContext())
                .getUserAvatar(m.getSender());

        if (avatarBytes != null && avatarBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length);
            holder.imgAvatar.setImageBitmap(bitmap);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }
    }



    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        CircleImageView imgAvatar;

        VH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvMessage);
            imgAvatar=itemView.findViewById(R.id.imgAvatar);
        }
    }
}
