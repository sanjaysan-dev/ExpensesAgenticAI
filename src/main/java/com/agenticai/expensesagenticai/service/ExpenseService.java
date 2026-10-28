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
import java.util.Optional;

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

    @Transactional
    public Optional<Expense> deleteMatchingExpense(
            String email,
            BigDecimal amount,
            String category,
            String description,
            LocalDate expenseDate,
            boolean isDateExplicit) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        List<Expense> expenses = expenseRepository.findByUserIdOrderByIdDesc(user.getId());
        if (expenses.isEmpty()) {
            return Optional.empty();
        }

        // Pass 1: Try strict match
        for (Expense exp : expenses) {
            if (matchesExpense(exp, amount, category, description, expenseDate, isDateExplicit, true)) {
                expenseRepository.delete(exp);
                return Optional.of(exp);
            }
        }

        // Pass 2: Relaxed match (if description or category partially matched, or amount matched)
        if (amount != null || description != null || (category != null && !"Other".equalsIgnoreCase(category))) {
            for (Expense exp : expenses) {
                if (matchesExpense(exp, amount, category, description, expenseDate, false, false)) {
                    expenseRepository.delete(exp);
                    return Optional.of(exp);
                }
            }
        }

        // Pass 3: If no amount and no specific description was requested (e.g. "delete last expense" / "remove that amount")
        if (amount == null && (description == null || description.isBlank())
                && (category == null || category.isBlank() || "Other".equalsIgnoreCase(category))) {
            Expense latest = expenses.get(0);
            expenseRepository.delete(latest);
            return Optional.of(latest);
        }

        return Optional.empty();
    }

    private boolean matchesExpense(
            Expense exp,
            BigDecimal amount,
            String category,
            String description,
            LocalDate expenseDate,
            boolean isDateExplicit,
            boolean strict) {

        // Check date if explicit
        if (isDateExplicit && expenseDate != null) {
            if (!expenseDate.equals(exp.getExpenseDate())) {
                return false;
            }
        }

        // Check amount
        if (amount != null) {
            if (exp.getAmount().compareTo(amount) != 0) {
                return false;
            }
        }

        // Check description / category
        if (description != null && !description.isBlank()) {
            String targetDesc = description.trim().toLowerCase();
            String expDesc = exp.getDescription() != null ? exp.getDescription().toLowerCase() : "";
            String expCat = exp.getCategory() != null ? exp.getCategory().toLowerCase() : "";

            boolean descMatches = expDesc.contains(targetDesc) || targetDesc.contains(expDesc);
            boolean catMatches = category != null && expCat.equalsIgnoreCase(category);

            if (strict) {
                if (!descMatches && !catMatches) {
                    return false;
                }
            } else {
                if (amount == null && !descMatches && !catMatches) {
                    return false;
                }
            }
        } else if (category != null && !category.isBlank() && !"Other".equalsIgnoreCase(category)) {
            if (!exp.getCategory().equalsIgnoreCase(category)) {
                return false;
            }
        }

        return true;
    }

    @Transactional
    public void deleteExpenseById(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Expense not found"));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot delete this expense");
        }

        expenseRepository.delete(expense);
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
