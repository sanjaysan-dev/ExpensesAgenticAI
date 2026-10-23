package com.agenticai.expensesagenticai.service;

import com.agenticai.expensesagenticai.dto.ChatResponse;
import com.agenticai.expensesagenticai.dto.ExpenseIntent;
import com.agenticai.expensesagenticai.dto.ExpenseParseResult;
import com.agenticai.expensesagenticai.dto.ExpenseSummaryResponse;
import com.agenticai.expensesagenticai.entity.Conversation;
import com.agenticai.expensesagenticai.entity.Expense;
import com.agenticai.expensesagenticai.entity.Message;
import com.agenticai.expensesagenticai.repository.ConversationRepository;
import com.agenticai.expensesagenticai.repository.ExpenseRepository;
import com.agenticai.expensesagenticai.repository.MessageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final GeminiAiService geminiAiService;

    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            ExpenseService expenseService,
            ExpenseRepository expenseRepository,
            GeminiAiService geminiAiService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
        this.geminiAiService = geminiAiService;
    }

    @Transactional
    public ChatResponse processUserMessage(
            String email,
            Long conversationId,
            String sender,
            String messageText) {

        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Conversation not found"));

        if (!conversation.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "You cannot access this conversation");
        }

        String normalizedSender = sender != null ? sender.trim().toUpperCase() : "";
        if (!(normalizedSender.equals("USER") || normalizedSender.equals("AI") || normalizedSender.equals("ASSISTANT"))) {
            throw new IllegalArgumentException(
                    "Sender must be USER or AI");
        }
        if (normalizedSender.equals("ASSISTANT")) {
            normalizedSender = "AI";
        }

        if (messageText == null || messageText.isBlank()) {
            throw new IllegalArgumentException(
                    "Message cannot be empty");
        }

        // 1. Save the incoming message
        Message incomingMessage = new Message();
        incomingMessage.setConversation(conversation);
        incomingMessage.setSender(normalizedSender);
        incomingMessage.setMessage(messageText.trim());
        Message savedUserMessage = messageRepository.save(incomingMessage);

        // If the sender was already AI, return directly
        if ("AI".equals(normalizedSender)) {
            ChatResponse.MessageItem assistantItem = new ChatResponse.MessageItem(
                    savedUserMessage.getId(),
                    savedUserMessage.getSender(),
                    savedUserMessage.getMessage(),
                    savedUserMessage.getCreatedAt()
            );
            return new ChatResponse(conversationId, conversation.getTitle(), null, assistantItem, ExpenseIntent.GENERAL_CONVERSATION, Map.of());
        }

        // 2. Parse the USER message for intent and entities using AI (with fallback)
        ExpenseParseResult parseResult = geminiAiService.parseMessage(messageText.trim());
        String assistantReplyText;
        Map<String, Object> details = new LinkedHashMap<>();

        switch (parseResult.intent()) {
            case RECORD_EXPENSE -> {
                // Duplicate prevention: check if same expense was recorded within last 30 seconds
                boolean isDuplicate = checkRecentDuplicate(
                        conversation.getUser().getId(),
                        parseResult.amount(),
                        parseResult.description(),
                        parseResult.expenseDate()
                );

                if (isDuplicate) {
                    assistantReplyText = String.format(
                            "This expense of ₹%.2f for '%s' was already recorded a moment ago. I did not add a duplicate.",
                            parseResult.amount(), parseResult.description()
                    );
                    details.put("status", "DUPLICATE_IGNORED");
                } else {
                    Expense savedExpense = expenseService.createExpense(
                            email,
                            parseResult.amount(),
                            parseResult.category(),
                            parseResult.description(),
                            parseResult.expenseDate()
                    );
                    assistantReplyText = String.format(
                            "Recorded expense: ₹%.2f for '%s' under %s on %s.",
                            savedExpense.getAmount(),
                            savedExpense.getDescription(),
                            savedExpense.getCategory(),
                            savedExpense.getExpenseDate()
                    );
                    details.put("expenseId", savedExpense.getId());
                    details.put("amount", savedExpense.getAmount());
                    details.put("category", savedExpense.getCategory());
                    details.put("description", savedExpense.getDescription());
                    details.put("expenseDate", savedExpense.getExpenseDate());
                }
            }

            case QUERY_EXPENSES -> {
                if (parseResult.queryCategory() != null) {
                    BigDecimal total = expenseService.getTotalSpendingByCategory(
                            email,
                            parseResult.queryCategory(),
                            parseResult.queryStartDate(),
                            parseResult.queryEndDate()
                    );
                    List<Expense> list = expenseService.getExpensesByCategoryAndDates(
                            email,
                            parseResult.queryCategory(),
                            parseResult.queryStartDate(),
                            parseResult.queryEndDate()
                    );
                    assistantReplyText = String.format(
                            "You spent ₹%.2f on %s between %s and %s across %d transaction(s).",
                            total, parseResult.queryCategory(),
                            parseResult.queryStartDate(), parseResult.queryEndDate(),
                            list.size()
                    );
                    details.put("total", total);
                    details.put("category", parseResult.queryCategory());
                    details.put("transactionCount", list.size());
                } else {
                    ExpenseSummaryResponse summary = expenseService.getExpenseSummary(
                            email,
                            parseResult.queryStartDate(),
                            parseResult.queryEndDate()
                    );
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format(
                            "You spent a total of ₹%.2f between %s and %s across %d transaction(s).",
                            summary.totalAmount(), summary.startDate(), summary.endDate(), summary.transactionCount()
                    ));
                    if (!summary.categoryBreakdown().isEmpty()) {
                        sb.append(" Breakdown: ");
                        summary.categoryBreakdown().forEach((cat, amt) ->
                                sb.append(String.format("%s: ₹%.2f; ", cat, amt)));
                    }
                    assistantReplyText = sb.toString().trim();
                    details.put("total", summary.totalAmount());
                    details.put("transactionCount", summary.transactionCount());
                    details.put("breakdown", summary.categoryBreakdown());
                }
            }

            case GENERAL_CONVERSATION -> {
                assistantReplyText = "Hello! I am your personal Expense Assistant. You can tell me expenses like 'Spent 350 on lunch today' or ask questions like 'How much did I spend on food this month?'.";
            }

            default -> { // AMBIGUOUS
                assistantReplyText = parseResult.clarificationPrompt() != null
                        ? parseResult.clarificationPrompt()
                        : "Could you please clarify? For example, specify: 'Spent 200 on fuel yesterday' or 'Show my expenses this week'.";
            }
        }

        // 3. Save the AI response message in the database
        Message assistantMessage = new Message();
        assistantMessage.setConversation(conversation);
        assistantMessage.setSender("AI");
        assistantMessage.setMessage(assistantReplyText);
        Message savedAssistantMessage = messageRepository.save(assistantMessage);

        // Rename conversation if it still has default title
        if ("New Chat".equalsIgnoreCase(conversation.getTitle()) || conversation.getTitle().isBlank()) {
            String smartTitle = generateSmartTitle(parseResult, messageText);
            conversation.setTitle(smartTitle);
        }

        // Update conversation's updated_at timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // 4. Return the full ChatResponse
        return new ChatResponse(
                conversationId,
                conversation.getTitle(),
                new ChatResponse.MessageItem(
                        savedUserMessage.getId(),
                        savedUserMessage.getSender(),
                        savedUserMessage.getMessage(),
                        savedUserMessage.getCreatedAt()
                ),
                new ChatResponse.MessageItem(
                        savedAssistantMessage.getId(),
                        savedAssistantMessage.getSender(),
                        savedAssistantMessage.getMessage(),
                        savedAssistantMessage.getCreatedAt()
                ),
                parseResult.intent(),
                details
        );
    }

    private String generateSmartTitle(ExpenseParseResult parseResult, String messageText) {
        if (parseResult.intent() == ExpenseIntent.RECORD_EXPENSE && parseResult.description() != null) {
            String desc = parseResult.description().trim();
            if (desc.length() > 20) {
                desc = desc.substring(0, 20).trim();
            }
            return capitalize(desc) + " (" + parseResult.category() + ")";
        }
        if (parseResult.intent() == ExpenseIntent.QUERY_EXPENSES) {
            if (parseResult.queryCategory() != null) {
                return parseResult.queryCategory() + " Spending";
            }
            return "Spending Summary";
        }
        String trimmed = messageText.trim();
        if (trimmed.length() <= 24) {
            return capitalize(trimmed);
        }
        return capitalize(trimmed.substring(0, 21)) + "...";
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return "New Chat";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private boolean checkRecentDuplicate(Long userId, BigDecimal amount, String description, java.time.LocalDate expenseDate) {
        List<Expense> expenses = expenseRepository
                .findByUserIdAndExpenseDateBetweenOrderByExpenseDateDesc(userId, expenseDate, expenseDate);

        LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);

        for (Expense e : expenses) {
            if (e.getAmount().compareTo(amount) == 0 &&
                    e.getDescription() != null &&
                    e.getDescription().equalsIgnoreCase(description) &&
                    e.getCreatedAt() != null &&
                    e.getCreatedAt().isAfter(thirtySecondsAgo)) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<Message> getMessages(
            String email,
            Long conversationId) {

        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Conversation not found"));

        if (!conversation.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "You cannot access this conversation");
        }

        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
}
