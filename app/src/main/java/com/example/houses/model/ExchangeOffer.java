package com.example.houses.model;

import java.time.Instant;
import lombok.Data;

@Data
public class ExchangeOffer {
    private Long id;
    private String chatLogin;
    private String ownerLogin;
    private Integer cost;
    private String title;
    private String description;
    private boolean active = true;
    private Instant createdAt;
}