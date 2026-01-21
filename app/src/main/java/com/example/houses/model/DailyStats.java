package com.example.houses.model;

import java.time.LocalDate;

import lombok.Data;

@Data
public class DailyStats {
    private String date;
    private int completedTasksCount;

    public LocalDate getDateAsLocal() {
        return LocalDate.parse(date); // или с DateTimeFormatter, если формат другой
    }


}