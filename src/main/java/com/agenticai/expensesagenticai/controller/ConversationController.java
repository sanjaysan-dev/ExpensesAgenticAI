package com.agenticai.expensesagenticai.controller;

import com.agenticai.expensesagenticai.entity.Conversation;
import com.agenticai.expensesagenticai.service.ConversationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(
            ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    public record CreateConversationRequest(
            @Size(max = 200, message = "Title must not exceed 200 characters")
            String title
    ) {
    }

    @PostMapping
    public ResponseEntity<?> createConversation(
            @Valid @RequestBody(required = false) CreateConversationRequest request,
            Authentication authentication) {

        String title = (request != null && request.title() != null && !request.title().isBlank())
                ? request.title()
                : "New Chat";

        Conversation conversation =
                conversationService.createConversation(
                        authentication.getName(),
                        title
                );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", conversation.getId());
        response.put("title", conversation.getTitle());
        response.put("createdAt", conversation.getCreatedAt() != null ? conversation.getCreatedAt().toString() : LocalDateTime.now().toString());
        response.put("updatedAt", conversation.getUpdatedAt() != null ? conversation.getUpdatedAt().toString() : LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getConversations(
            Authentication authentication) {

        List<Map<String, Object>> conversations =
                conversationService.getUserConversations(
                                authentication.getName()
                        ).stream()
                        .map(conversation -> {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("id", conversation.getId());
                            item.put("title", conversation.getTitle());
                            item.put("createdAt", conversation.getCreatedAt() != null ? conversation.getCreatedAt().toString() : "");
                            item.put("updatedAt", conversation.getUpdatedAt() != null ? conversation.getUpdatedAt().toString() : "");
                            return item;
                        })
                        .toList();

        return ResponseEntity.ok(conversations);
    }
}
