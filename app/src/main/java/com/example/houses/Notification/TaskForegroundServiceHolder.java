package com.example.houses.Notification;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.houses.model.Task;
import com.example.houses.model.TaskInstanceDto;

import java.util.ArrayList;
import java.util.List;

public class TaskForegroundServiceHolder {
    private static final String TAG = "TaskFGServiceHolder";

    private static TaskForegroundService serviceInstance;
    public static TaskForegroundService hService; // Добавлено для совместимости с фрагментом
    private static final List<TaskInstanceDto> pending = new ArrayList<>();

    public static synchronized void setService(TaskForegroundService service) {
        Log.d(TAG, "setService -> service set");
        serviceInstance = service;
        hService = service; // Обновляем публичное поле
        if (serviceInstance != null && !pending.isEmpty()) {
            for (TaskInstanceDto t : pending) {
                serviceInstance.enqueueTask(t);
            }
            pending.clear();
        }
    }

    public static synchronized void clearService() {
        Log.d(TAG, "clearService -> service cleared");
        serviceInstance = null;
        hService = null; // Очищаем публичное поле
    }

    public static synchronized void enqueue(TaskInstanceDto instance, Context context) {
        if (instance == null) return;
        if (serviceInstance != null) {
            Log.d(TAG, "enqueue -> delivered directly to service");
            serviceInstance.enqueueTask(instance);
        } else {
            Log.d(TAG, "enqueue -> service not ready, buffering and starting service");
            pending.add(instance);
            Intent intent = new Intent(context.getApplicationContext(), TaskForegroundService.class);
            try {
                context.getApplicationContext().startForegroundService(intent);
            } catch (Exception ex) {
                try {
                    context.getApplicationContext().startService(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start service", e);
                }
            }
        }
    }
}