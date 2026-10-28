package com.agenticai.expensesagenticai.controller;

import com.agenticai.expensesagenticai.dto.ExpenseSummaryResponse;
import com.agenticai.expensesagenticai.entity.Expense;
import com.agenticai.expensesagenticai.service.ExpenseService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    public record CreateExpenseRequest(
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.0", inclusive = false,
                    message = "Amount must be greater than zero")
            BigDecimal amount,

            @NotBlank(message = "Category is required")
            @Size(max = 50, message = "Category cannot exceed 50 characters")
            String category,

            @Size(max = 255, message = "Description cannot exceed 255 characters")
            String description,

            @NotNull(message = "Expense date is required")
            LocalDate expenseDate
    ) {
    }

    @PostMapping
    public ResponseEntity<?> createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            Authentication authentication) {

        Expense expense = expenseService.createExpense(
                authentication.getName(),
                request.amount(),
                request.category(),
                request.description(),
                request.expenseDate()
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", expense.getId());
        response.put("amount", expense.getAmount());
        response.put("category", expense.getCategory());
        response.put("description", expense.getDescription());
        response.put("expenseDate", expense.getExpenseDate());
        response.put("createdAt", expense.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getExpenses(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            String category,

            Authentication authentication) {

        List<Expense> expenses;
        if (category != null && !category.isBlank()) {
            expenses = expenseService.getExpensesByCategoryAndDates(
                    authentication.getName(),
                    category,
                    startDate,
                    endDate
            );
        } else {
            expenses = expenseService.getExpensesBetweenDates(
                    authentication.getName(),
                    startDate,
                    endDate
            );
        }

        List<Map<String, Object>> response = new ArrayList<>();

        for (Expense expense : expenses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", expense.getId());
            item.put("amount", expense.getAmount());
            item.put("category", expense.getCategory());
            item.put("description", expense.getDescription());
            item.put("expenseDate", expense.getExpenseDate());
            item.put("createdAt", expense.getCreatedAt());

            response.add(item);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummaryResponse> getExpenseSummary(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            Authentication authentication) {

        ExpenseSummaryResponse summary = expenseService.getExpenseSummary(
                authentication.getName(),
                startDate,
                endDate
        );

        return ResponseEntity.ok(summary);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(
            @PathVariable Long id,
            Authentication authentication) {

        expenseService.deleteExpenseById(authentication.getName(), id);
        return ResponseEntity.ok(Map.of(
                "message", "Expense deleted successfully",
                "id", id
        ));
    }
}