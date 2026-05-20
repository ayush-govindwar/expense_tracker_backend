package com.expensetracker.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyTrendItemResponse {

    private LocalDate date;
    private BigDecimal amount;

    public DailyTrendItemResponse(LocalDate date, BigDecimal amount) {
        this.date = date;
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
