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
    private boolean editable = true;

    public interface CommentPhotoListener {
        void onSave(String comment, List<String> photos);
    }

    public CommentPhotoDialog(
            @NonNull Context context,
            TaskInstanceDto task,
            CommentPhotoListener listener,
            ActivityResultLauncher<String> launcher,
            boolean editable
    ) {
        super(context);
        this.task = task;
        this.listener = listener;
        this.pickPhotoLauncher = launcher;
        this.editable = editable;
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

        if (!editable) {
            // read-only mode for parent: disable editing, hide add button, change save -> close
            editComment.setEnabled(false);
            btnAddPhoto.setVisibility(View.GONE);
            btnSave.setText("Закрыть");
            btnSave.setOnClickListener(v -> dismiss());
            btnCancel.setVisibility(View.GONE);
            return;
        }

        btnAddPhoto.setOnClickListener(v -> {
            if (pickPhotoLauncher != null) pickPhotoLauncher.launch("image/*");
        });

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
            // Получаем ширину экрана
            int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;

            int targetWidth = (int) (screenWidth * 0.4); // 30% от экрана
            int targetHeight = (int) (screenHeight * 0.4); // 30% от экрана

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(targetWidth, targetHeight);
            params.setMargins(8, 8, 8, 8);
            imageView.setLayoutParams(params);



            imageView.setImageBitmap(
                    ImageUtils.base64ToBitmap(base64)
            );

            if (editable) {
                // клик для удаления фото в режиме редактирования
                imageView.setOnClickListener(v -> {
                    photoBase64List.remove(base64);
                    displayImages();
                });
            }

            photoContainer.addView(imageView);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (getWindow() != null) {
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}
