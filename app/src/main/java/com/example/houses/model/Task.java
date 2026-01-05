package com.example.houses.model;

import lombok.Data;

@Data
public class Task {
    private Long id;
    private String chatId;
    private String title;
    private String description;
    private boolean completed;
    private int money;

    private String createdAt;
}