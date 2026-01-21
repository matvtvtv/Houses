package com.example.houses;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.houses.Notification.NotificationForegroundService;
import com.example.houses.Notification.NotificationWorker;
import com.example.houses.adapter.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;
    private static final String CHANNEL_ID = "daily_notification_channel";

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        requestNotificationPermission();

        SharedPreferences preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        boolean isFirstRun = preferences.getBoolean("isFirstRun", true);
        String login = preferences.getString("login", "1");
        if (isFirstRun) {
            startActivity(new Intent(this, RegistrationActivity.class));
            editor.putBoolean("isFirstRun", false);
            editor.apply();
        } else if (login.equals("1")) {
            startActivity(new Intent(this, RegistrationActivity.class));
        }

        Intent serviceIntent = new Intent(this, NotificationForegroundService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }



        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNavigation);

        viewPager.setAdapter(new ViewPagerAdapter(this));
        viewPager.setOffscreenPageLimit(5);
        viewPager.setUserInputEnabled(false);

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_tasks) {
                viewPager.setCurrentItem(0, false);
                return true;
            }
            if (item.getItemId() == R.id.nav_chat) {
                viewPager.setCurrentItem(1, false);
                return true;
            }
            if (item.getItemId() == R.id.nav_stats) {
                viewPager.setCurrentItem(2, false);
                return true;
            }
            if (item.getItemId() == R.id.nav_stats_chat) {
                viewPager.setCurrentItem(3, false);
                return true;
            }
            if (item.getItemId() == R.id.nav_settings) {
                viewPager.setCurrentItem(4, false);
                return true;
            }
            return false;
        });

        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        hideKeyboard();
                        if (position == 0) bottomNav.setSelectedItemId(R.id.nav_tasks);
                        if (position == 1) bottomNav.setSelectedItemId(R.id.nav_chat);
                        if (position == 2) bottomNav.setSelectedItemId(R.id.nav_stats);
                        if (position == 3) bottomNav.setSelectedItemId(R.id.nav_stats_chat);
                        if (position == 4) bottomNav.setSelectedItemId(R.id.nav_settings);
                    }
                }
        );

        // Запланировать ежедневное уведомление
        scheduleDailyNotification();
    }

    public ViewPager2 getViewPager() {
        return viewPager;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View current = getCurrentFocus();

        if (imm == null) return;

        if (current != null) {
            imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
            current.clearFocus();
        } else {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Ежедневные уведомления",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Ежедневные напоминания для задач.");
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null);
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void scheduleDailyNotification() {
        WorkManager workManager = WorkManager.getInstance(this);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12); // 14:00
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

        WorkRequest dailyNotificationWorkRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        workManager.enqueue(dailyNotificationWorkRequest);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            } else {
                Log.d("PermissionCheck", "Уведомления уже разрешены");
            }
        } else {
            Log.d("PermissionCheck", "Запрос разрешения не требуется (Android < 13)");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PermissionCheck", "Разрешение на уведомления получено");
            } else {
                Log.d("PermissionCheck", "Разрешение на уведомления ОТКАЗАНО");
            }
        }
    }
}
