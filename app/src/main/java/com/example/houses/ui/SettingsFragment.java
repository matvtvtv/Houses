package com.example.houses.ui;


import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import de.hdodenhof.circleimageview.CircleImageView;
import com.example.houses.DB.DatabaseHelper;
import com.example.houses.R;
import com.example.houses.RegistrationActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;





public class SettingsFragment extends Fragment {

    private static final int AVATAR_SIZE = 256;

    private CircleImageView imgAvatar;
    private Button btnChoose, button2, button3;

    // Современный API для выбора изображений
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleImageUri(uri);
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgAvatar = view.findViewById(R.id.imgAvatar);
        btnChoose = view.findViewById(R.id.btnChooseAvatar);

        button3 = view.findViewById(R.id.button3);

        loadAvatar();

        btnChoose.setOnClickListener(v -> openGallery());


        button3.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RegistrationActivity.class)));
    }

    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }

    private void handleImageUri(Uri uri) {
        try {
            Bitmap decoded = decodeBitmapFromUri(uri, 512);
            Bitmap avatar = cropSquare(decoded, AVATAR_SIZE);

            imgAvatar.setImageBitmap(avatar);
            saveAvatarToDatabase(avatar);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap decodeBitmapFromUri(Uri uri, int reqSize) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        InputStream is = requireContext().getContentResolver().openInputStream(uri);
        BitmapFactory.decodeStream(is, null, options);
        is.close();

        options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize);
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        is = requireContext().getContentResolver().openInputStream(uri);
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

    private void saveAvatarToDatabase(Bitmap bitmap) {
        byte[] avatarBytes = bitmapToBytes(bitmap);

        ContentValues values = new ContentValues();
        values.put("avatar", avatarBytes);

        String login = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                .getString("login", "account");

        DatabaseHelper.getInstance(requireContext()).updateUserAvatar(login, values);
    }

    private void loadAvatar() {
        String login = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                .getString("login", "account");

        byte[] avatarBytes = DatabaseHelper.getInstance(requireContext()).getUserAvatar(login);

        if (avatarBytes != null && avatarBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length);
            imgAvatar.setImageBitmap(bitmap);
        }
    }
}