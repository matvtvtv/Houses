package com.example.houses.Notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.houses.R;
import com.example.houses.model.Task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TaskForegroundService extends Service {

    private static final String TAG = "TaskForegroundService";
    private static final String CHANNEL_ID = "houses_task_channel";
    private static final int FOREGROUND_ID = 1001;

    // Очередь новых задач
    private final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        createNotificationChannel();

        // старт foreground notification (информативный persistent)
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Houses")
                .setContentText("Ожидание новых задач...")
                .setSmallIcon(R.drawable.bg_day_item)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(FOREGROUND_ID, notification);
        }

        // зарегистрировать себя в Holder, чтобы другие компоненты могли доставлять задачи
        TaskForegroundServiceHolder.setService(this);

        // thread для обработки очереди (показывает отдельные уведомления)
        Thread worker = new Thread(this::processTasks, "TaskFGService-Worker");
        worker.start();
    }

    private void processTasks() {
        try {
            while (running) {
                Task task = taskQueue.take(); // блокирует, пока не появится задача
                showTaskNotification(task);
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "Worker interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    /** Добавляем задачу в очередь для уведомления */
    public void enqueueTask(Task task) {
        if (task == null) return;
        Log.d(TAG, "enqueueTask: " + task.getTitle());
        taskQueue.offer(task);
    }

    private void showTaskNotification(Task task) {
        if (task == null) return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.bg_day_item)
                .setContentTitle(task.getTitle() != null ? ("Новая задача: " + task.getTitle()) : "Новая задача")
                .setContentText(task.getDescription() != null ? task.getDescription() : "")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            int id = task.getId() != null ? task.getId().hashCode() : (int) System.currentTimeMillis();
            manager.notify(id, builder.build());
            Log.d(TAG, "notify id=" + id);
        } else {
            Log.w(TAG, "NotificationManager == null");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Задачи Houses",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Уведомления о новых задачах");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        // Если нужно, можно прочитать intent extras (например, при желании сразу передавать задачу через Intent)
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        running = false;
        TaskForegroundServiceHolder.clearService();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // не биндимся
    }
}
