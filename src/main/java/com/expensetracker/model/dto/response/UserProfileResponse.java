package com.expensetracker.model.dto.response;

public class UserProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String currency;

    public UserProfileResponse(Long id, String name, String email, String currency) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getCurrency() {
        return currency;
    }
}
