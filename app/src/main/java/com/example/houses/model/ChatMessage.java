package com.example.houses.model;

import lombok.Data;

@Data
public class ChatMessage {
    private Long id;
    private String sender;
    private String content;
    private String chatLogin;

    private String timestamp; // сервер отдаёт LocalDateTime; мы принимаем как String


}
