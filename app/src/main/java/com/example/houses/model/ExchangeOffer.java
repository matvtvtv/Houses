package com.example.houses.model;

import java.time.Instant;
import java.time.Month; // Добавить импорт
import lombok.Data;

@Data
public class ExchangeOffer {
    private Long id;
    private String chatLogin;
    private String ownerLogin;
    private Month month; // ИЗМЕНЕНО: вместо Integer cost
    private String title;
    private String description;
    private boolean active = true;
    private Instant createdAt;
}