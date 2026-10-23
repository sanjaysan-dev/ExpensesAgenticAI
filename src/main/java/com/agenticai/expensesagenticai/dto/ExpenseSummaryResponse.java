package com.agenticai.expensesagenticai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record ExpenseSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalAmount,
        long transactionCount,
        Map<String, BigDecimal> categoryBreakdown
) {
}
