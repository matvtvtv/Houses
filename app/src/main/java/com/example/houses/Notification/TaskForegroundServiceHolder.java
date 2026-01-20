package com.example.houses.Notification;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.houses.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskForegroundServiceHolder {
    private static final String TAG = "TaskFGServiceHolder";

    private static TaskForegroundService serviceInstance;
    // буфер задач, если сервис ещё не инициализирован
    private static final List<Task> pending = new ArrayList<>();

    public static synchronized void setService(TaskForegroundService service) {
        Log.d(TAG, "setService -> service set");
        serviceInstance = service;
        // пересылаем накопленные задачи
        if (serviceInstance != null && !pending.isEmpty()) {
            for (Task t : pending) {
                serviceInstance.enqueueTask(t);
            }
            pending.clear();
        }
    }

    public static synchronized void clearService() {
        Log.d(TAG, "clearService -> service cleared");
        serviceInstance = null;
    }

    public static synchronized void enqueue(Task task, Context context) {
        if (task == null) return;
        if (serviceInstance != null) {
            Log.d(TAG, "enqueue -> delivered directly to service");
            serviceInstance.enqueueTask(task);
        } else {
            Log.d(TAG, "enqueue -> service not ready, buffering and starting service");
            // буферизуем задачу
            pending.add(task);
            // стартуем сервис (если ещё не стартован) — startForegroundService безопасен на Android >= O
            Intent intent = new Intent(context.getApplicationContext(), TaskForegroundService.class);
            try {
                context.getApplicationContext().startForegroundService(intent);
            } catch (Exception ex) {
                // На некоторых устройствах нужен просто startService (fallback)
                try {
                    context.getApplicationContext().startService(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start service", e);
                }
            }
        }
    }
}
