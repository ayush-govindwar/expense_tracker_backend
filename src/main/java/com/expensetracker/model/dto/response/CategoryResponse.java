package com.expensetracker.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CategoryResponse {

    private Long id;
    private String name;
    private String icon;
    private boolean isDefault;

    public CategoryResponse(Long id, String name, String icon, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.isDefault = isDefault;
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

    @JsonProperty("isDefault")
    public boolean isDefault() {
        return isDefault;
    }
}
