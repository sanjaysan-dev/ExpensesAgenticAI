package com.agenticai.expensesagenticai;

import com.agenticai.expensesagenticai.entity.Expense;
import com.agenticai.expensesagenticai.entity.User;
import com.agenticai.expensesagenticai.repository.ExpenseRepository;
import com.agenticai.expensesagenticai.repository.UserRepository;
import com.agenticai.expensesagenticai.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private User testUser;
    private Expense dinnerExpense;
    private Expense petrolExpense;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        dinnerExpense = new Expense();
        dinnerExpense.setId(101L);
        dinnerExpense.setUser(testUser);
        dinnerExpense.setAmount(new BigDecimal("500.00"));
        dinnerExpense.setCategory("Food");
        dinnerExpense.setDescription("dinner");
        dinnerExpense.setExpenseDate(LocalDate.now());

        petrolExpense = new Expense();
        petrolExpense.setId(102L);
        petrolExpense.setUser(testUser);
        petrolExpense.setAmount(new BigDecimal("200.00"));
        petrolExpense.setCategory("Transport");
        petrolExpense.setDescription("petrol");
        petrolExpense.setExpenseDate(LocalDate.now());
    }

    @Test
    void testDeleteLastExpenseWhenNoParamsProvided() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findByUserIdOrderByIdDesc(1L))
                .thenReturn(new ArrayList<>(List.of(petrolExpense, dinnerExpense)));

        Optional<Expense> deleted = expenseService.deleteMatchingExpense(
                "test@example.com", null, null, null, LocalDate.now(), false
        );

        assertTrue(deleted.isPresent());
        assertEquals(102L, deleted.get().getId());
        verify(expenseRepository, times(1)).delete(petrolExpense);
    }

    @Test
    void testDeleteMatchingByAmountAndDescription() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findByUserIdOrderByIdDesc(1L))
                .thenReturn(new ArrayList<>(List.of(petrolExpense, dinnerExpense)));

        Optional<Expense> deleted = expenseService.deleteMatchingExpense(
                "test@example.com", new BigDecimal("500.00"), "Food", "dinner", LocalDate.now(), false
        );

        assertTrue(deleted.isPresent());
        assertEquals(101L, deleted.get().getId());
        verify(expenseRepository, times(1)).delete(dinnerExpense);
    }

    @Test
    void testDeleteMatchingByAmountOnly() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findByUserIdOrderByIdDesc(1L))
                .thenReturn(new ArrayList<>(List.of(petrolExpense, dinnerExpense)));

        Optional<Expense> deleted = expenseService.deleteMatchingExpense(
                "test@example.com", new BigDecimal("500.00"), null, null, LocalDate.now(), false
        );

        assertTrue(deleted.isPresent());
        assertEquals(101L, deleted.get().getId());
        verify(expenseRepository, times(1)).delete(dinnerExpense);
    }

    @Test
    void testDeleteMatchingByDescriptionOnly() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findByUserIdOrderByIdDesc(1L))
                .thenReturn(new ArrayList<>(List.of(petrolExpense, dinnerExpense)));

        Optional<Expense> deleted = expenseService.deleteMatchingExpense(
                "test@example.com", null, "Food", "dinner", LocalDate.now(), false
        );

        assertTrue(deleted.isPresent());
        assertEquals(101L, deleted.get().getId());
        verify(expenseRepository, times(1)).delete(dinnerExpense);
    }

    @Test
    void testDeleteMatchingNotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findByUserIdOrderByIdDesc(1L))
                .thenReturn(new ArrayList<>(List.of(petrolExpense, dinnerExpense)));

        Optional<Expense> deleted = expenseService.deleteMatchingExpense(
                "test@example.com", new BigDecimal("9999.00"), null, null, LocalDate.now(), false
        );

        assertTrue(deleted.isEmpty());
        verify(expenseRepository, never()).delete(any());
    }

    @Test
    void testDeleteById() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findById(101L)).thenReturn(Optional.of(dinnerExpense));

        expenseService.deleteExpenseById("test@example.com", 101L);

        verify(expenseRepository, times(1)).delete(dinnerExpense);
    }

    @Test
    void testDeleteByIdUnauthorizedOtherUser() {
        User otherUser = new User();
        otherUser.setId(99L);
        dinnerExpense.setUser(otherUser);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findById(101L)).thenReturn(Optional.of(dinnerExpense));

        assertThrows(IllegalArgumentException.class, () ->
                expenseService.deleteExpenseById("test@example.com", 101L)
        );
        verify(expenseRepository, never()).delete(any());
    }
}
