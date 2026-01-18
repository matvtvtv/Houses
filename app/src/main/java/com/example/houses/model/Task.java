package com.example.houses.model;

import lombok.Data;

@Data
public class Task {
    private Long id;
    private String chatLogin;
    private String userLogin;
    private String targetLogin; // кому назначена задача
    private String title;
    private String description;
    private int money;
    private boolean completed;
    private boolean repeat;
    private String[] days;
    private String startDate;
    private String createdAt;
}