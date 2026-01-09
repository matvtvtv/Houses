package com.example.houses.model;

import lombok.Data;

@Data
public class ChatModel {
    private long id;
    private String chatLogin;
    private String chatName;
    private String userRole;
    private int money;


}
