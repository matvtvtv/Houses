package com.example.houses.ui;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.houses.R;
import com.example.houses.model.TaskInstanceDto;
import com.example.houses.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class CommentPhotoDialog extends Dialog {

    private EditText editComment;
    private Button btnAddPhoto, btnSave, btnCancel;
    private LinearLayout photoContainer;
    private final TaskInstanceDto task;
    private final CommentPhotoListener listener;

    private List<String> photoBase64List = new ArrayList<>();
    private ActivityResultLauncher<String> pickPhotoLauncher;


    public interface CommentPhotoListener {
        void onSave(String comment, List<String> photos);
    }



        public CommentPhotoDialog(
                @NonNull Context context,
                TaskInstanceDto task,
                CommentPhotoListener listener,
                ActivityResultLauncher<String> launcher
        ) {
            super(context);
            this.task = task;
            this.listener = listener;
            this.pickPhotoLauncher = launcher;
        }


        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_comment_photo);

        editComment = findViewById(R.id.editComment);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        photoContainer = findViewById(R.id.photoContainer);

        if (task.comment != null) {
            editComment.setText(task.comment);
        }

        if (task.photoBase64 != null) {
            photoBase64List.addAll(task.photoBase64);
            displayImages();
        }


            btnAddPhoto.setOnClickListener(v -> pickPhotoLauncher.launch("image/*"));

        btnSave.setOnClickListener(v -> {
            String comment = editComment.getText().toString().trim();
            if (TextUtils.isEmpty(comment) && photoBase64List.isEmpty()) {
                Toast.makeText(getContext(), "Добавьте комментарий или фото", Toast.LENGTH_SHORT).show();
                return;
            }

            listener.onSave(comment, photoBase64List);
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

        public void handleImageSelected(Uri uri) {
            if (uri == null) return;

            try {
                String base64 = ImageUtils.uriToBase64(getContext(), uri);
                if (base64 != null) {
                    photoBase64List.add(base64);
                    displayImages();
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
            }
        }

        private void displayImages() {
        photoContainer.removeAllViews();

        for (String base64 : photoBase64List) {
            ImageView imageView = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(300, 300);
            params.setMargins(8, 8, 8, 8);
            imageView.setLayoutParams(params);
            imageView.setImageBitmap(
                    ImageUtils.base64ToBitmap(base64)
            );


            // Клик для удаления фото
            imageView.setOnClickListener(v -> {
                photoBase64List.remove(base64);
                displayImages();
            });

            photoContainer.addView(imageView);
        }
    }
}