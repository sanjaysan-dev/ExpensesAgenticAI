package com.agenticai.expensesagenticai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "Expense Agentic AI Backend is running successfully!";
    }
}
