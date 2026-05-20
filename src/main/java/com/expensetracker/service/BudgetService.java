package com.expensetracker.service;

import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.dto.request.BudgetRequest;
import com.expensetracker.model.dto.response.BudgetAlertResponse;
import com.expensetracker.model.dto.response.BudgetResponse;
import com.expensetracker.model.entity.Budget;
import com.expensetracker.model.entity.Category;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;

    public BudgetService(
            BudgetRepository budgetRepository,
            ExpenseRepository expenseRepository,
            CategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<BudgetResponse> list(Long userId, int month, int year) {
        return budgetRepository.findByUserIdAndMonthAndYear(userId, month, year).stream()
                .map(b -> toResponse(b, userId))
                .toList();
    }

    public List<BudgetAlertResponse> alerts(Long userId, int month, int year) {
        List<BudgetAlertResponse> result = new ArrayList<>();
        for (Budget budget : budgetRepository.findByUserIdAndMonthAndYear(userId, month, year)) {
            BigDecimal spent = spentForBudget(userId, budget);
            double pct = percentage(spent, budget.getAmount());
            String level = alertLevel(pct);
            if (pct >= 60.0) {
                result.add(toAlert(budget, spent, pct, level));
            }
        }
        return result;
    }

    @Transactional
    public BudgetResponse create(UserPrincipal principal, BudgetRequest request) {
        int month = resolveMonth(request);
        int year = resolveYear(request);

        if (request.getCategoryId() == null) {
            if (budgetRepository.findByUserIdAndCategoryIsNullAndMonthAndYear(principal.getId(), month, year).isPresent()) {
                throw new IllegalArgumentException("Overall budget already exists for this month");
            }
        } else {
            resolveCategory(principal.getId(), request.getCategoryId());
            if (budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                    principal.getId(), request.getCategoryId(), month, year).isPresent()) {
                throw new IllegalArgumentException("Budget already exists for this category and month");
            }
        }

        Budget budget = new Budget();
        budget.setUser(principal.getUser());
        budget.setAmount(request.getAmount());
        budget.setMonth(month);
        budget.setYear(year);
        if (request.getCategoryId() != null) {
            budget.setCategory(resolveCategory(principal.getId(), request.getCategoryId()));
        }

        return toResponse(budgetRepository.save(budget), principal.getId());
    }

    @Transactional
    public BudgetResponse update(UserPrincipal principal, Long id, BudgetRequest request) {
        Budget budget = budgetRepository.findByIdAndUserId(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budget.setAmount(request.getAmount());
        if (request.getMonth() != null) {
            budget.setMonth(request.getMonth());
        }
        if (request.getYear() != null) {
            budget.setYear(request.getYear());
        }
        return toResponse(budgetRepository.save(budget), principal.getId());
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        budgetRepository.delete(budget);
    }

    public void checkAlerts(Long userId, LocalDate expenseDate) {
        //later
    }

    private BudgetResponse toResponse(Budget budget, Long userId) {
        BigDecimal spent = spentForBudget(userId, budget);
        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setAmount(budget.getAmount());
        response.setMonth(budget.getMonth());
        response.setYear(budget.getYear());
        response.setSpentAmount(spent);
        response.setPercentageUsed(percentage(spent, budget.getAmount()));

        if (budget.getCategory() == null) {
            response.setOverall(true);
            response.setCategoryName("Overall");
        } else {
            response.setOverall(false);
            response.setCategoryId(budget.getCategory().getId());
            response.setCategoryName(budget.getCategory().getName());
        }
        return response;
    }

    private BudgetAlertResponse toAlert(Budget budget, BigDecimal spent, double pct, String level) {
        String name = budget.getCategory() == null ? "Overall" : budget.getCategory().getName();
        return new BudgetAlertResponse(name, budget.getAmount(), spent, pct, level);
    }

    private BigDecimal spentForBudget(Long userId, Budget budget) {
        LocalDate start = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        if (budget.getCategory() == null) {
            return expenseRepository.sumByUserAndMonth(userId, budget.getMonth(), budget.getYear());
        }
        return expenseRepository.sumAmountByUserIdAndCategoryIdAndExpenseDateBetween(
                userId, budget.getCategory().getId(), start, end);
    }

    private double percentage(BigDecimal spent, BigDecimal budgetAmount) {
        if (budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return spent.multiply(BigDecimal.valueOf(100))
                .divide(budgetAmount, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    static String alertLevel(double percentageUsed) {
        if (percentageUsed > 100) {
            return "EXCEEDED";
        }
        if (percentageUsed > 80) {
            return "HIGH";
        }
        if (percentageUsed >= 60) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Category resolveCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (category.isDefault()) {
            return category;
        }
        if (category.getUser() != null && category.getUser().getId().equals(userId)) {
            return category;
        }
        throw new ResourceNotFoundException("Category not found");
    }

    private int resolveMonth(BudgetRequest request) {
        return request.getMonth() != null ? request.getMonth() : LocalDate.now().getMonthValue();
    }

    private int resolveYear(BudgetRequest request) {
        return request.getYear() != null ? request.getYear() : LocalDate.now().getYear();
    }
}
