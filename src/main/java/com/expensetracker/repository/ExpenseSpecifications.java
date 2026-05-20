package com.expensetracker.repository;

import com.expensetracker.model.entity.Expense;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    public static Specification<Expense> forUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Expense> categoryId(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Expense> expenseDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from);
    }

    public static Specification<Expense> expenseDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to);
    }

    public static Specification<Expense> minAmount(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    public static Specification<Expense> maxAmount(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), max);
    }

    public static Specification<Expense> commentsContain(String q) {
        String pattern = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("comments")), pattern);
    }

    public static Specification<Expense> categoryName(String name) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("category").get("name")), name.toLowerCase());
    }

    public static Specification<Expense> paymentMethod(String method) {
        return (root, query, cb) ->
                cb.equal(cb.upper(root.get("paymentMethod")), method.toUpperCase());
    }
}
