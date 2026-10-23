package com.agenticai.expensesagenticai.service;

import com.agenticai.expensesagenticai.dto.ExpenseSummaryResponse;
import com.agenticai.expensesagenticai.entity.Expense;
import com.agenticai.expensesagenticai.entity.User;
import com.agenticai.expensesagenticai.repository.ExpenseRepository;
import com.agenticai.expensesagenticai.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Expense createExpense(
            String email,
            BigDecimal amount,
            String category,
            String description,
            LocalDate expenseDate) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException(
                    "Category is required");
        }

        if (category.trim().length() > 50) {
            throw new IllegalArgumentException(
                    "Category cannot exceed 50 characters");
        }

        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException(
                    "Description cannot exceed 255 characters");
        }

        if (expenseDate == null) {
            throw new IllegalArgumentException(
                    "Expense date is required");
        }

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setAmount(amount);
        expense.setCategory(category.trim());
        expense.setDescription(
                description == null || description.isBlank()
                        ? null : description.trim());
        expense.setExpenseDate(expenseDate);

        return expenseRepository.save(expense);
    }

    @Transactional(readOnly = true)
    public List<Expense> getExpensesBetweenDates(
            String email,
            LocalDate startDate,
            LocalDate endDate) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (startDate == null || endDate == null
                || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Please provide a valid date range");
        }

        return expenseRepository
                .findByUserIdAndExpenseDateBetweenOrderByExpenseDateDesc(
                        user.getId(), startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Expense> getExpensesByCategoryAndDates(
            String email,
            String category,
            LocalDate startDate,
            LocalDate endDate) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Please provide a valid date range");
        }

        return expenseRepository
                .findByUserIdAndCategoryIgnoreCaseAndExpenseDateBetweenOrderByExpenseDateDesc(
                        user.getId(), category.trim(), startDate, endDate);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSpending(
            String email,
            LocalDate startDate,
            LocalDate endDate) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Please provide a valid date range");
        }

        return expenseRepository.calculateTotalSpending(user.getId(), startDate, endDate);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalSpendingByCategory(
            String email,
            String category,
            LocalDate startDate,
            LocalDate endDate) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Please provide a valid date range");
        }

        return expenseRepository.calculateTotalSpendingByCategory(
                user.getId(), category.trim(), startDate, endDate);
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getExpenseSummary(
            String email,
            LocalDate startDate,
            LocalDate endDate) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Please provide a valid date range");
        }

        List<Expense> expenses = expenseRepository
                .findByUserIdAndExpenseDateBetweenOrderByExpenseDateDesc(
                        user.getId(), startDate, endDate);

        BigDecimal total = expenseRepository.calculateTotalSpending(user.getId(), startDate, endDate);

        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            String cat = expense.getCategory();
            breakdown.put(cat, breakdown.getOrDefault(cat, BigDecimal.ZERO).add(expense.getAmount()));
        }

        return new ExpenseSummaryResponse(
                startDate,
                endDate,
                total,
                expenses.size(),
                breakdown
        );
    }
}