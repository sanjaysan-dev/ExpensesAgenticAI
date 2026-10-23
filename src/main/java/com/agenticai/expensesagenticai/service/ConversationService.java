package com.agenticai.expensesagenticai.service;

import com.agenticai.expensesagenticai.entity.Conversation;
import com.agenticai.expensesagenticai.entity.User;
import com.agenticai.expensesagenticai.repository.ConversationRepository;
import com.agenticai.expensesagenticai.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Conversation createConversation(String email, String title) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Conversation conversation = new Conversation();
        conversation.setUser(user);
        LocalDateTime now = LocalDateTime.now();
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);

        if (title != null && !title.isBlank()) {
            conversation.setTitle(title.trim());
        }

        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return conversationRepository
                .findByUserIdOrderByUpdatedAtDesc(user.getId());
    }
}
