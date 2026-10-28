package com.agenticai.expensesagenticai;

import com.agenticai.expensesagenticai.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "MyTestSecretKeyForJwtAuthenticationMustBeAtLeast256BitsLong!",
                3600000L,
                new ObjectMapper()
        );
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String email = "sanjay@example.com";
        Long userId = 42L;

        String token = jwtService.generateToken(email, userId);
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        assertTrue(jwtService.validateToken(token));
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals(userId, jwtService.extractUserId(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.token.here"));
        assertFalse(jwtService.validateToken(null));
        assertFalse(jwtService.validateToken(""));
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken("test@example.com", 1L);
        String tampered = token.substring(0, token.length() - 5) + "ABCDE";
        assertFalse(jwtService.validateToken(tampered));
    }
}
