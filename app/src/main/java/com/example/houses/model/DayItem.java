package com.example.houses.model;



import java.time.LocalDate;

public class DayItem {
    public LocalDate date;
    public boolean selected;

    public DayItem(LocalDate date, boolean selected) {
        this.date = date;
        this.selected = selected;
    }
}
