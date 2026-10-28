package com.agenticai.expensesagenticai.controller;

import com.agenticai.expensesagenticai.dto.LoginRequest;
import com.agenticai.expensesagenticai.dto.RegisterRequest;
import com.agenticai.expensesagenticai.entity.User;
import com.agenticai.expensesagenticai.service.AuthService;
import com.agenticai.expensesagenticai.service.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            SecurityContextRepository securityContextRepository,
            JwtService jwtService) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {

        try {
            User user = authService.registerUser(
                    request.name(),
                    request.email(),
                    request.password()
            );

            String token = jwtService.generateToken(user.getEmail(), user.getId());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Registration successful");
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("token", token);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            User user = authService.loginUser(
                    request.email(),
                    request.password()
            );

            Authentication authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            user.getEmail(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority("ROLE_USER")
                            )
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                    context,
                    httpRequest,
                    httpResponse
            );

            String token = jwtService.generateToken(user.getEmail(), user.getId());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Login successful");
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("token", token);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            SecurityContextHolder.clearContext();

            return ResponseEntity.status(
                    HttpStatus.UNAUTHORIZED
            ).body(
                    Map.of("message", "Invalid email or password")
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
}
