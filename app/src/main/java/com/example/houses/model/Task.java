package com.example.houses.model;

import lombok.Data;

@Data
public class Task {
    private Long id;
    private String chatLogin;
    private String userLogin;
    private String targetLogin;
    private String title;
    private String description;
    private int money;
    private boolean completed;
    public boolean started;
    public boolean confirmedByParent;
    private boolean repeat;
    private String[] days;
    private String startTime;
    private String endTime;
    private String partDay;
    private int importance;

    private String startDate;
    private String createdAt;
}