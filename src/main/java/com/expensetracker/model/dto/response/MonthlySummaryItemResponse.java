package com.expensetracker.model.dto.response;

import java.math.BigDecimal;

public class MonthlySummaryItemResponse {

    private int month;
    private int year;
    private BigDecimal totalAmount;

    public MonthlySummaryItemResponse(int month, int year, BigDecimal totalAmount) {
        this.month = month;
        this.year = year;
        this.totalAmount = totalAmount;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
