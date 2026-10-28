package com.agenticai.expensesagenticai.repository;

import com.agenticai.expensesagenticai.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdOrderByIdDesc(Long userId);

    List<Expense> findByUserIdAndExpenseDateBetweenOrderByExpenseDateDesc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Expense> findByUserIdAndCategoryIgnoreCaseAndExpenseDateBetweenOrderByExpenseDateDesc(
            Long userId,
            String category,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalSpending(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND LOWER(e.category) = LOWER(:category) AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalSpendingByCategory(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
