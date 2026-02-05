package com.example.houses.model;

import java.util.Map;

import lombok.Data;

@Data
public class UserStats {
    private String userLogin;
    private String chatLogin;
    private int money;
    private int totalCompletedTasks;
    private String fromDate;
    private String toDate;

    // Для разбивки по месяцам (ключ: "2024-01", значение: сумма)
    private Map<String, Integer> moneyByMonth;
    private Map<String, Integer> tasksByMonth;
}