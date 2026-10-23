package com.agenticai.expensesagenticai.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ChatResponse(
        Long conversationId,
        String conversationTitle,
        MessageItem userMessage,
        MessageItem assistantMessage,
        ExpenseIntent intent,
        Map<String, Object> details
) {
    public record MessageItem(
            Long id,
            String sender,
            String message,
            LocalDateTime createdAt
    ) {}
}
