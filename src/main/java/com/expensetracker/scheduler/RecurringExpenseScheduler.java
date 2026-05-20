package com.expensetracker.scheduler;

import com.expensetracker.model.entity.Expense;
import com.expensetracker.repository.ExpenseRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Copies recurring template expenses onto today's date when the recurrence rule matches.
 * Templates stay recurring; generated rows are one-off (non-recurring).
 */
@Component
public class RecurringExpenseScheduler {

    private final ExpenseRepository expenseRepository;

    public RecurringExpenseScheduler(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processRecurring() {
        LocalDate today = LocalDate.now();
        for (Expense template : expenseRepository.findRecurringTemplates()) {
            if (!isDueToday(template, today)) {
                continue;
            }
            Expense copy = new Expense();
            copy.setUser(template.getUser());
            copy.setCategory(template.getCategory());
            copy.setAmount(template.getAmount());
            String base = template.getComments();
            copy.setComments(base == null || base.isEmpty() ? "[Auto]" : "[Auto] " + base);
            copy.setExpenseDate(today);
            copy.setRecurring(false);
            copy.setRecurrenceType(null);
            copy.setPaymentMethod(template.getPaymentMethod());
            copy.setUpiRefId(null);
            expenseRepository.save(copy);
        }
    }

    private boolean isDueToday(Expense template, LocalDate today) {
        String type = template.getRecurrenceType();
        if (type == null) {
            return false;
        }
        return switch (type) {
            case "MONTHLY" -> template.getExpenseDate().getDayOfMonth() == today.getDayOfMonth();
            case "WEEKLY" -> template.getExpenseDate().getDayOfWeek() == today.getDayOfWeek();
            case "DAILY" -> true;
            default -> false;
        };
    }
}
