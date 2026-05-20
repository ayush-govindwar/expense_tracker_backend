package com.expensetracker.model.dto.response;

import java.util.ArrayList;
import java.util.List;

public class DailyTrendResponse {

    private int month;
    private int year;
    private List<DailyTrendItemResponse> days = new ArrayList<>();

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public List<DailyTrendItemResponse> getDays() {
        return days;
    }

    public void setDays(List<DailyTrendItemResponse> days) {
        this.days = days;
    }
}
