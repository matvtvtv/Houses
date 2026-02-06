package com.example.houses.ui;


import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.example.houses.DB.DatabaseHelper;
import com.example.houses.R;
import com.example.houses.RegistrationActivity;
import com.example.houses.model.ChatData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;


public class SettingsFragment extends Fragment {

    private static final int AVATAR_SIZE = 256;
    private View cardInternet, cardReport;
    private Button btnDeleteAccount;
    private SwitchCompat switchWifiOnly;

    private CircleImageView imgAvatar;
    private Button btnChoose, button2, button3,btnDelDan;
    String chatLogin;

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

        button3 = view.findViewById(R.id.btnLogout);
        btnDelDan = view.findViewById(R.id.btnDelDan);

        loadAvatar();

        btnChoose.setOnClickListener(v -> openGallery());
        switchWifiOnly = view.findViewById(R.id.switchInternet);
        loadWifiSetting();

        SharedPreferences preferences = requireActivity()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        chatLogin = preferences.getString("chatLogin", "1");
        String role = preferences.getString("role", "");

        button3.setOnClickListener(v -> startActivity(new Intent(requireContext(), RegistrationActivity.class)));
        cardInternet = view.findViewById(R.id.cardInternet);
        cardReport = view.findViewById(R.id.cardReport);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        cardInternet.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
            startActivity(intent);
        });
        cardReport.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("samirmeh001@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Отчёт об ошибке");
            startActivity(emailIntent);
        });
        if(role.equals("ADMIN")){
            btnDelDan.setVisibility(View.VISIBLE);
        }
        else btnDelDan.setVisibility(View.GONE);

        btnDeleteAccount.setOnClickListener(v -> showDeleteDialog());
        btnDelDan.setOnClickListener(v -> loadUsersInChat());



    }
    private void showDeleteUserDialog(String loginToDelete) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить пользователя")
                .setMessage("Удалить " + loginToDelete + " из чата?")
                .setPositiveButton("Удалить", (d, w) -> deleteUserFromChat(loginToDelete))
                .setNegativeButton("Отмена", null)
                .show();
    }
    private void deleteUserFromChat(String loginToDelete) {

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url("https://t7lvb7zl-8080.euw.devtunnels.ms/api/chats_data/delete?login="
                                + loginToDelete + "&chatLogin=" + chatLogin)
                        .delete()
                        .build();

                client.newCall(request).execute().close();

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Пользователь удалён", Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showUsersDialog(List<ChatData> users) {

        String[] logins = new String[users.size()];

        for (int i = 0; i < users.size(); i++) {
            logins[i] = users.get(i).getUserLogin();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Пользователи чата")
                .setItems(logins, (dialog, which) -> {

                    String selectedLogin = users.get(which).getUserLogin();

                    showDeleteUserDialog(selectedLogin);

                })
                .setNegativeButton("Закрыть", null)
                .show();
    }

    private void loadUsersInChat() {

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url("https://t7lvb7zl-8080.euw.devtunnels.ms/api/chats_data/get_chats_users/" + chatLogin)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                String json = response.body().string();

                Gson gson = new Gson();
                Type type = new TypeToken<List<ChatData>>(){}.getType();
                List<ChatData> users = gson.fromJson(json, type);

                requireActivity().runOnUiThread(() -> showUsersDialog(users));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static boolean isWifiOnly(Context context) {
        return context
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getBoolean("wifi_only", false);
    }
    public static boolean isWifiConnected(Context context) {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        android.net.NetworkCapabilities caps =
                cm.getNetworkCapabilities(cm.getActiveNetwork());

        return caps != null &&
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI);
    }

    private void showDeleteDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Удаление аккаунта")
                .setMessage("Вы уверены, что хотите удалить аккаунт? Это действие нельзя отменить.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (dialog, which) -> deleteAccount())
                .show();
    }
    private void loadWifiSetting() {
        boolean wifiOnly = requireContext()
                .getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("wifi_only", false);

        switchWifiOnly.setChecked(wifiOnly);

        switchWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            requireContext()
                    .getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("wifi_only", isChecked)
                    .apply();
        });
    }

    private void deleteAccount() {
        String login = requireContext()
                .getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                .getString("login", "account");

        // 1. Удаляем на сервере
        deleteAccountFromServer(login);

        // 2. Удаляем локально
        DatabaseHelper.getInstance(requireContext()).deleteUser(login);

        // 3. Очищаем SharedPreferences
        requireContext()
                .getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // 4. Переход на регистрацию
        startActivity(new Intent(requireContext(), RegistrationActivity.class));
        requireActivity().finish();
    }
    private void deleteAccountFromServer(String login) {

        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url("https://t7lvb7zl-8080.euw.devtunnels.ms/api/users/delete?login=" + login)
                        .delete()
                        .build();

                client.newCall(request).execute().close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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