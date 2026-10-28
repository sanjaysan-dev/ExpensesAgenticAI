package com.agenticai.expensesagenticai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret:ExpenseAISecretKeyForJwtAuthenticationMustBeAtLeast256BitsLong2026!}")
    private String secretKey = "ExpenseAISecretKeyForJwtAuthenticationMustBeAtLeast256BitsLong2026!";

    @Value("${jwt.expiration:2592000000}")
    private long expirationMillis = 2592000000L;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtService() {
    }

    public JwtService(String secretKey, long expirationMillis) {
        this.secretKey = secretKey;
        this.expirationMillis = expirationMillis;
    }

    public JwtService(String secretKey, long expirationMillis, ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(String email, Long userId) {
        long now = System.currentTimeMillis();
        long exp = now + expirationMillis;

        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            String headerJson = objectMapper.writeValueAsString(header);
            String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", email);
            payload.put("userId", userId);
            payload.put("iat", now / 1000);
            payload.put("exp", exp / 1000);
            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            String content = encodedHeader + "." + encodedPayload;
            byte[] signature = sign(content.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = base64UrlEncode(signature);

            return content + "." + encodedSignature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        try {
            String content = parts[0] + "." + parts[1];
            byte[] expectedSignature = sign(content.getBytes(StandardCharsets.UTF_8));
            byte[] actualSignature = base64UrlDecode(parts[2]);

            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                return false;
            }

            Map<String, Object> claims = parsePayload(parts[1]);
            Number exp = (Number) claims.get("exp");
            if (exp == null) {
                return false;
            }
            long expSeconds = exp.longValue();
            long nowSeconds = System.currentTimeMillis() / 1000;
            return expSeconds > nowSeconds;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            Map<String, Object> claims = parsePayload(parts[1]);
            return (String) claims.get("sub");
        } catch (Exception e) {
            return null;
        }
    }

    public Long extractUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            Map<String, Object> claims = parsePayload(parts[1]);
            Number id = (Number) claims.get("userId");
            return id != null ? id.longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parsePayload(String encodedPayload) throws Exception {
        byte[] bytes = base64UrlDecode(encodedPayload);
        return objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
    }

    private byte[] sign(byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        return mac.doFinal(data);
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] base64UrlDecode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }
}
