package com.expensetracker.model.dto.response;

import java.math.BigDecimal;

public class BudgetAlertResponse {

    private String categoryName;
    private BigDecimal budgetAmount;
    private BigDecimal spentAmount;
    private double percentageUsed;
    private String alertLevel;

    public BudgetAlertResponse(
            String categoryName,
            BigDecimal budgetAmount,
            BigDecimal spentAmount,
            double percentageUsed,
            String alertLevel) {
        this.categoryName = categoryName;
        this.budgetAmount = budgetAmount;
        this.spentAmount = spentAmount;
        this.percentageUsed = percentageUsed;
        this.alertLevel = alertLevel;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public double getPercentageUsed() {
        return percentageUsed;
    }

    public String getAlertLevel() {
        return alertLevel;
    }
}
