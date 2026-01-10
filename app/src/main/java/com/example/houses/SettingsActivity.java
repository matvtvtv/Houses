package com.example.houses;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;

import com.example.houses.DB.DatabaseHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 101;
    private static final int AVATAR_SIZE = 256;

    private CircleImageView imgAvatar;
    private Button btnChoose, button2, button3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        imgAvatar = findViewById(R.id.imgAvatar);
        btnChoose = findViewById(R.id.btnChooseAvatar);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);

        loadAvatar();

        btnChoose.setOnClickListener(v -> openGallery());

        button2.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        button3.setOnClickListener(v ->
                startActivity(new Intent(this, RegistrationActivity.class)));
    }

    // -------------------- GALLERY --------------------

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

            try {
                Bitmap decoded = decodeBitmapFromUri(uri, 512);
                Bitmap avatar = cropSquare(decoded, AVATAR_SIZE);

                imgAvatar.setImageBitmap(avatar);
                saveAvatarToDatabase(avatar);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // -------------------- BITMAP UTILS --------------------

    private Bitmap decodeBitmapFromUri(Uri uri, int reqSize) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        InputStream is = getContentResolver().openInputStream(uri);
        BitmapFactory.decodeStream(is, null, options);
        is.close();

        options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize);
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        is = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
        is.close();

        return bitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }

    private Bitmap cropSquare(Bitmap bitmap, int size) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int min = Math.min(w, h);

        Bitmap cropped = Bitmap.createBitmap(
                bitmap,
                (w - min) / 2,
                (h - min) / 2,
                min,
                min
        );

        if (cropped != bitmap) bitmap.recycle();

        return Bitmap.createScaledBitmap(cropped, size, size, true);
    }

    private byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, baos);
        } else {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        }

        return baos.toByteArray();
    }

    // -------------------- DATABASE --------------------

    private void saveAvatarToDatabase(Bitmap bitmap) {
        byte[] avatarBytes = bitmapToBytes(bitmap);

        ContentValues values = new ContentValues();
        values.put("avatar", avatarBytes);

        String login = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getString("login", "account");

        DatabaseHelper.getInstance(this).updateUserAvatar(login, values);
    }

    private void loadAvatar() {
        String login = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getString("login", "account");

        byte[] avatarBytes = DatabaseHelper.getInstance(this).getUserAvatar(login);

        if (avatarBytes != null && avatarBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length);
            imgAvatar.setImageBitmap(bitmap);
        }
    }
}
