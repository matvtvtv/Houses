package com.example.houses.model;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class TaskInstanceDto {
    // instance fields
    public Long instanceId;
    public String taskDate; // yyyy-MM-dd
    public boolean completed;
    public String comment;
    public List<String> photoBase64;

    public String userLogin;

    // template fields
    public Long templateId;
    public String title;
    public String description;
    public int money;
    public String chatLogin;
    public String targetLogin;
    public boolean repeat;
    public List<String> repeatDays;
    public String templateUserLogin;

    // пустой конструктор нужен для Gson
    public TaskInstanceDto() {}
}
