package com.expensetracker.repository;

import com.expensetracker.model.entity.Expense;
import com.expensetracker.repository.projection.CategorySpendSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Page<Expense> findByUserId(Long userId, Pageable pageable);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    List<Expense> findByRecurringTrue();

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
            WHERE e.user.id = :userId
              AND e.expenseDate >= :startDate
              AND e.expenseDate <= :endDate
            """)
    BigDecimal sumAmountByUserIdAndExpenseDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    default BigDecimal sumByUserAndMonth(Long userId, int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return sumAmountByUserIdAndExpenseDateBetween(userId, start, end);
    }

    @Query("""
            SELECT e.category.id AS categoryId,
                   e.category.name AS categoryName,
                   e.category.icon AS categoryIcon,
                   SUM(e.amount) AS totalAmount
            FROM Expense e
            WHERE e.user.id = :userId
              AND e.expenseDate >= :startDate
              AND e.expenseDate <= :endDate
            GROUP BY e.category.id, e.category.name, e.category.icon
            ORDER BY SUM(e.amount) DESC
            """)
    List<CategorySpendSummary> sumByCategoryForUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
            WHERE e.user.id = :userId
              AND e.category.id = :categoryId
              AND e.expenseDate >= :startDate
              AND e.expenseDate <= :endDate
            """)
    BigDecimal sumAmountByUserIdAndCategoryIdAndExpenseDateBetween(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT e.expenseDate AS expenseDate, SUM(e.amount) AS totalAmount
            FROM Expense e
            WHERE e.user.id = :userId
              AND e.expenseDate >= :startDate
              AND e.expenseDate <= :endDate
            GROUP BY e.expenseDate
            ORDER BY e.expenseDate ASC
            """)
    List<DailySpendSummary> sumDailyForUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT YEAR(e.expenseDate) AS year, MONTH(e.expenseDate) AS month, SUM(e.amount) AS totalAmount
            FROM Expense e
            WHERE e.user.id = :userId
            GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate)
            ORDER BY YEAR(e.expenseDate) ASC, MONTH(e.expenseDate) ASC
            """)
    List<MonthlySpendSummary> sumMonthlyForUser(@Param("userId") Long userId);

    interface DailySpendSummary {
        LocalDate getExpenseDate();

        BigDecimal getTotalAmount();
    }

    interface MonthlySpendSummary {
        int getYear();

        int getMonth();

        BigDecimal getTotalAmount();
    }
}
