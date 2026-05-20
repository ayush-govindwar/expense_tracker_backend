package com.expensetracker.service;

import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.dto.request.ExpenseRequest;
import com.expensetracker.model.dto.response.ExpenseCategoryResponse;
import com.expensetracker.model.dto.response.ExpenseResponse;
import com.expensetracker.model.dto.response.PagedResponse;
import com.expensetracker.model.entity.Category;
import com.expensetracker.model.entity.Expense;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.ExpenseSpecifications;
import com.expensetracker.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;

    public ExpenseService(ExpenseRepository expenseRepository, CategoryRepository categoryRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
    }

    public PagedResponse<ExpenseResponse> list(
            Long userId,
            Long categoryId,
            LocalDate from,
            LocalDate to,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Pageable pageable) {

        Specification<Expense> spec = buildSpec(userId, categoryId, from, to, minAmount, maxAmount);
        Page<Expense> page = expenseRepository.findAll(spec, pageable);
        List<ExpenseResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.from(page, content);
    }

    public ExpenseResponse getById(Long userId, Long id) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse create(UserPrincipal principal, ExpenseRequest request) {
        Category category = resolveCategory(principal.getId(), request.getCategoryId());
        Expense expense = mapRequestToEntity(new Expense(), request, category, principal);
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(UserPrincipal principal, Long id, ExpenseRequest request) {
        Expense expense = expenseRepository.findByIdAndUserId(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        Category category = resolveCategory(principal.getId(), request.getCategoryId());
        mapRequestToEntity(expense, request, category, principal);
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        expenseRepository.delete(expense);
    }

    private Specification<Expense> buildSpec(
            Long userId,
            Long categoryId,
            LocalDate from,
            LocalDate to,
            BigDecimal minAmount,
            BigDecimal maxAmount) {

        List<Specification<Expense>> specs = new ArrayList<>();
        specs.add(ExpenseSpecifications.forUser(userId));

        if (categoryId != null) {
            specs.add(ExpenseSpecifications.categoryId(categoryId));
        }
        if (from != null) {
            specs.add(ExpenseSpecifications.expenseDateFrom(from));
        }
        if (to != null) {
            specs.add(ExpenseSpecifications.expenseDateTo(to));
        }
        if (minAmount != null) {
            specs.add(ExpenseSpecifications.minAmount(minAmount));
        }
        if (maxAmount != null) {
            specs.add(ExpenseSpecifications.maxAmount(maxAmount));
        }

        return Specification.allOf(specs);
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

    private Expense mapRequestToEntity(
            Expense expense,
            ExpenseRequest request,
            Category category,
            UserPrincipal principal) {
        expense.setUser(principal.getUser());
        expense.setCategory(category);
        expense.setAmount(request.getAmount());
        expense.setComments(request.getComments());
        expense.setExpenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setUpiRefId(request.getUpiRefId());
        expense.setRecurring(request.isRecurring());
        expense.setRecurrenceType(request.getRecurrenceType());
        return expense;
    }

    private ExpenseResponse toResponse(Expense expense) {
        Category cat = expense.getCategory();
        ExpenseResponse response = new ExpenseResponse();
        response.setId(expense.getId());
        response.setCategory(new ExpenseCategoryResponse(cat.getId(), cat.getName(), cat.getIcon()));
        response.setAmount(expense.getAmount());
        response.setComments(expense.getComments());
        response.setExpenseDate(expense.getExpenseDate());
        response.setPaymentMethod(expense.getPaymentMethod());
        response.setUpiRefId(expense.getUpiRefId());
        response.setRecurring(expense.isRecurring());
        response.setRecurrenceType(expense.getRecurrenceType());
        response.setCreatedAt(expense.getCreatedAt());
        response.setUpdatedAt(expense.getUpdatedAt());
        return response;
    }
}
