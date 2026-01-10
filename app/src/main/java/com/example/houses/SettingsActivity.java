package com.example.houses;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.houses.DB.DatabaseHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 101;

    private CircleImageView imgAvatar;
    private Button button2;
    private Button btnChoose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        imgAvatar = findViewById(R.id.imgAvatar);
        button2 = findViewById(R.id.button2);
        btnChoose = findViewById(R.id.btnChooseAvatar);

        loadAvatar();

        btnChoose.setOnClickListener(v -> openGallery());
        button2.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));

                // Квадрат и ресайз
                int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
                Bitmap resizedBitmap = cropAndResizeBitmap(bitmap, size, size);

                // Сохраняем в БД
                saveAvatarToDatabase(resizedBitmap, size);

                // Отображаем
                imgAvatar.setImageBitmap(resizedBitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap cropAndResizeBitmap(Bitmap originalBitmap, int width, int height) {
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        int cropX = (originalWidth - width) / 2;
        int cropY = (originalHeight - height) / 2;

        Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, cropX, cropY, width, height);
        return Bitmap.createScaledBitmap(croppedBitmap, width, height, true);
    }

    private void saveAvatarToDatabase(Bitmap bitmap, int size_img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            if (size_img > 512) {
                int compression = Math.max(10, (int) (1000 / (float) size_img) * 5);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, compression, baos);
                } else {
                    bitmap.compress(Bitmap.CompressFormat.WEBP, compression, baos);
                }
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            }

            byte[] imageBytes = baos.toByteArray();

            // Сохраняем в БД через DatabaseHelper
            ContentValues values = new ContentValues();
            values.put("avatar", imageBytes);

            String login = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("login", "account");
            DatabaseHelper.getInstance(this).updateUserAvatar(login, values);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAvatar() {
        String login = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("login", "account");
        byte[] avatarBytes = DatabaseHelper.getInstance(this).getUserAvatar(login);

        if (avatarBytes != null && avatarBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length);
            imgAvatar.setImageBitmap(bitmap);
        }
    }
}
