package com.example.houses.model;

import lombok.Data;

@Data
public class Task {
    private Long id;

    private String chatLogin;
    private String userLogin;
    private String title;
    private String description;
    private int money;

    private boolean execution;
    private boolean completed;

    private String createdAt;
}