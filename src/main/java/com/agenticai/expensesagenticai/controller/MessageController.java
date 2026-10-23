package com.agenticai.expensesagenticai.controller;

import com.agenticai.expensesagenticai.dto.ChatResponse;
import com.agenticai.expensesagenticai.entity.Message;
import com.agenticai.expensesagenticai.service.MessageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public record CreateMessageRequest(
            @NotBlank(message = "Sender is required")
            @Pattern(regexp = "USER|ASSISTANT|AI",
                    message = "Sender must be USER, ASSISTANT, or AI")
            String sender,

            @NotBlank(message = "Message cannot be empty")
            @Size(max = 10000, message = "Message is too long")
            String message
    ) {
    }

    @PostMapping
    public ResponseEntity<?> createMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody CreateMessageRequest request,
            Authentication authentication) {

        ChatResponse response = messageService.processUserMessage(
                authentication.getName(),
                conversationId,
                request.sender(),
                request.message()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getMessages(
            @PathVariable Long conversationId,
            Authentication authentication) {

        List<Message> messages = messageService.getMessages(
                authentication.getName(),
                conversationId
        );

        List<Map<String, Object>> response = new ArrayList<>();

        for (Message message : messages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", message.getId());
            item.put("sender", message.getSender());
            item.put("message", message.getMessage());
            item.put("createdAt", message.getCreatedAt());

            response.add(item);
        }

        return ResponseEntity.ok(response);
    }
}