package com.example.houses.model;

import lombok.Data;

@Data
public class LoginRequest {
    public String login;
    public String password;

    public LoginRequest(String login, String password) {
        this.login = login;
        this.password = password;

    }

}
