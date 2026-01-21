package com.example.houses.Notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.houses.R;
import com.example.houses.model.TaskInstanceDto;

import java.util.ArrayList;
import java.util.List;

public class TaskForegroundService extends Service {

    private static final String CHANNEL_ID = "task_channel";
    private static final int NOTIF_ID = 101;
    private static final String TAG = "TaskFGService";

    private final List<TaskInstanceDto> tasksQueue = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        TaskForegroundServiceHolder.setService(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TaskForegroundServiceHolder.clearService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Task Service")
                .setContentText("Service running...")
                .setSmallIcon(R.drawable.bg_day_item)
                .build();

        startForeground(NOTIF_ID, notification);

        // Можно обработать очередь
        for (TaskInstanceDto t : tasksQueue) {
            showNotification(t);
        }
        tasksQueue.clear();

        return START_STICKY;
    }

    public void enqueueTask(TaskInstanceDto taskInstance) {
        if (taskInstance == null) return;
        tasksQueue.add(taskInstance);
        showNotification(taskInstance);
    }

    private void showNotification(TaskInstanceDto taskInstance) {
        if (taskInstance == null) return;

        String title = taskInstance.title != null ? taskInstance.title : "Новая задача";
        String content = taskInstance.description != null ? taskInstance.description : "";

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.bg_day_item)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Если instanceId == null, используем временный fallback
        int notifId = taskInstance.getInstanceId() != null
                ? taskInstance.getInstanceId().hashCode()
                : (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

        if (manager != null) manager.notify(notifId, notification);
    }


    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Tasks Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
