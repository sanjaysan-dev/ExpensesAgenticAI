package com.agenticai.expensesagenticai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseParseResult(
        ExpenseIntent intent,
        BigDecimal amount,
        String category,
        String description,
        LocalDate expenseDate,
        LocalDate queryStartDate,
        LocalDate queryEndDate,
        String queryCategory,
        String clarificationPrompt,
        boolean isDateExplicit
) {
    public static ExpenseParseResult recordExpense(
            BigDecimal amount,
            String category,
            String description,
            LocalDate expenseDate) {
        return new ExpenseParseResult(
                ExpenseIntent.RECORD_EXPENSE,
                amount,
                category,
                description,
                expenseDate,
                null,
                null,
                null,
                null,
                true
        );
    }

    public static ExpenseParseResult deleteExpense(
            BigDecimal amount,
            String category,
            String description,
            LocalDate expenseDate,
            boolean isDateExplicit) {
        return new ExpenseParseResult(
                ExpenseIntent.DELETE_EXPENSE,
                amount,
                category,
                description,
                expenseDate,
                null,
                null,
                null,
                null,
                isDateExplicit
        );
    }

    public static ExpenseParseResult query(
            LocalDate startDate,
            LocalDate endDate,
            String category) {
        return new ExpenseParseResult(
                ExpenseIntent.QUERY_EXPENSES,
                null,
                null,
                null,
                null,
                startDate,
                endDate,
                category,
                null,
                false
        );
    }

    public static ExpenseParseResult ambiguous(String prompt) {
        return new ExpenseParseResult(
                ExpenseIntent.AMBIGUOUS,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                prompt,
                false
        );
    }

    public static ExpenseParseResult general() {
        return new ExpenseParseResult(
                ExpenseIntent.GENERAL_CONVERSATION,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }
}
