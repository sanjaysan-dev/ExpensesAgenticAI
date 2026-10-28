package com.agenticai.expensesagenticai;

import com.agenticai.expensesagenticai.dto.ChatResponse;
import com.agenticai.expensesagenticai.dto.ExpenseIntent;
import com.agenticai.expensesagenticai.dto.ExpenseParseResult;
import com.agenticai.expensesagenticai.entity.Conversation;
import com.agenticai.expensesagenticai.entity.Expense;
import com.agenticai.expensesagenticai.entity.Message;
import com.agenticai.expensesagenticai.entity.User;
import com.agenticai.expensesagenticai.repository.ConversationRepository;
import com.agenticai.expensesagenticai.repository.ExpenseRepository;
import com.agenticai.expensesagenticai.repository.MessageRepository;
import com.agenticai.expensesagenticai.service.ExpenseService;
import com.agenticai.expensesagenticai.service.GeminiAiService;
import com.agenticai.expensesagenticai.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private GeminiAiService geminiAiService;

    @InjectMocks
    private MessageService messageService;

    private User testUser;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("sanjay@example.com");

        conversation = new Conversation();
        conversation.setId(10L);
        conversation.setTitle("New Chat");
        conversation.setUser(testUser);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testProcessDeleteExpenseMessageSuccessfully() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        Message userMsg = new Message();
        userMsg.setId(100L);
        userMsg.setConversation(conversation);
        userMsg.setSender("USER");
        userMsg.setMessage("remove that amount");
        when(messageRepository.save(any(Message.class))).thenReturn(userMsg);

        ExpenseParseResult parseResult = ExpenseParseResult.deleteExpense(null, null, null, LocalDate.now(), false);
        when(geminiAiService.parseMessage("remove that amount")).thenReturn(parseResult);

        Expense deleted = new Expense();
        deleted.setId(50L);
        deleted.setUser(testUser);
        deleted.setAmount(new BigDecimal("300.00"));
        deleted.setDescription("dinner");
        deleted.setCategory("Food");
        deleted.setExpenseDate(LocalDate.now());

        when(expenseService.deleteMatchingExpense(
                eq("sanjay@example.com"), isNull(), isNull(), isNull(), any(LocalDate.class), eq(false)
        )).thenReturn(Optional.of(deleted));

        ChatResponse response = messageService.processUserMessage("sanjay@example.com", 10L, "USER", "remove that amount");

        assertNotNull(response);
        assertEquals(ExpenseIntent.DELETE_EXPENSE, response.intent());
        assertNotNull(response.assistantMessage());
        assertTrue(response.assistantMessage().message().contains("Successfully removed expense"));
        assertTrue(response.assistantMessage().message().contains("300.00"));
        assertTrue(response.assistantMessage().message().contains("dinner"));
    }

    @Test
    void testProcessDeleteExpenseNotFound() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        Message userMsg = new Message();
        userMsg.setId(101L);
        userMsg.setConversation(conversation);
        userMsg.setSender("USER");
        userMsg.setMessage("delete 9999");
        when(messageRepository.save(any(Message.class))).thenReturn(userMsg);

        ExpenseParseResult parseResult = ExpenseParseResult.deleteExpense(new BigDecimal("9999"), null, null, LocalDate.now(), false);
        when(geminiAiService.parseMessage("delete 9999")).thenReturn(parseResult);

        when(expenseService.deleteMatchingExpense(
                eq("sanjay@example.com"), eq(new BigDecimal("9999")), isNull(), isNull(), any(LocalDate.class), eq(false)
        )).thenReturn(Optional.empty());

        ChatResponse response = messageService.processUserMessage("sanjay@example.com", 10L, "USER", "delete 9999");

        assertNotNull(response);
        assertEquals(ExpenseIntent.DELETE_EXPENSE, response.intent());
        assertTrue(response.assistantMessage().message().contains("Could not find an expense"));
    }
}
