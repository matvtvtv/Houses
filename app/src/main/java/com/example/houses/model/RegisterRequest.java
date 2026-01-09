package com.example.houses.model;



public class RegisterRequest {
    public String login;
    public String name;
    public String password;
    public String role;

    public RegisterRequest(String login, String name, String password, String role) {
        this.login = login;
        this.name = name;
        this.password = password;
        this.role = role;
    }
}

