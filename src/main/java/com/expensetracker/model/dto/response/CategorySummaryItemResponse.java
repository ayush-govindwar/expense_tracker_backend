package com.expensetracker.model.dto.response;

import java.math.BigDecimal;

public class CategorySummaryItemResponse {

    private String categoryName;
    private String icon;
    private BigDecimal amount;
    private double percentage;

    public CategorySummaryItemResponse(String categoryName, String icon, BigDecimal amount, double percentage) {
        this.categoryName = categoryName;
        this.icon = icon;
        this.amount = amount;
        this.percentage = percentage;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getIcon() {
        return icon;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public double getPercentage() {
        return percentage;
    }
}
