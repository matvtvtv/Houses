package com.example.houses.model;

import android.util.Log;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import lombok.Data;

@Data
public class DailyStats {
    private String date;
    private int completedTasksCount;

    public LocalDate getDateAsLocal() {
        if (date == null) return LocalDate.now(); // fallback
        try {
            return LocalDate.parse(date); // ISO yyyy-MM-dd
        } catch (DateTimeParseException ex) {
            // если сервер отдал, например, "2025-01-30T00:00:00", попробуем подрезать
            try {
                String trimmed = date.split("T")[0];
                return LocalDate.parse(trimmed);
            } catch (Exception ex2) {
                // на крайний случай — лог и возвращаем сегодня
                Log.e("DailyStats", "Can't parse date: " + date, ex2);
                return LocalDate.now();
            }
        }
    }



}