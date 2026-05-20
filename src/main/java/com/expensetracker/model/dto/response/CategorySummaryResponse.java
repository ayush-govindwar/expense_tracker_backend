package com.expensetracker.model.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CategorySummaryResponse {

    private int month;
    private int year;
    private BigDecimal totalSpend = BigDecimal.ZERO;
    private List<CategorySummaryItemResponse> categories = new ArrayList<>();

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

    public BigDecimal getTotalSpend() {
        return totalSpend;
    }

    public void setTotalSpend(BigDecimal totalSpend) {
        this.totalSpend = totalSpend;
    }

    public List<CategorySummaryItemResponse> getCategories() {
        return categories;
    }

    public void setCategories(List<CategorySummaryItemResponse> categories) {
        this.categories = categories;
    }
}
