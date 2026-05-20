package com.expensetracker.model.dto.response;

public class ExpenseCategoryResponse {

    private Long id;
    private String name;
    private String icon;

    public ExpenseCategoryResponse(Long id, String name, String icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }
}
