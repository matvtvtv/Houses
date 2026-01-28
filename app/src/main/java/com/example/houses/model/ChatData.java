package com.example.houses.model;

import lombok.Data;

@Data
public class ChatData {
    private Long id;
    private String chatLogin;
    private String userLogin;
    private String userRole;
    private String userName; // если есть на сервере
    private int money;
}