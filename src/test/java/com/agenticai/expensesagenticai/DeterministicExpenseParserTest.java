package com.agenticai.expensesagenticai;

import com.agenticai.expensesagenticai.dto.ExpenseIntent;
import com.agenticai.expensesagenticai.dto.ExpenseParseResult;
import com.agenticai.expensesagenticai.service.DeterministicExpenseParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicExpenseParserTest {

    private DeterministicExpenseParser parser;

    @BeforeEach
    void setUp() {
        parser = new DeterministicExpenseParser();
    }

    @Test
    void testRecordExpense() {
        ExpenseParseResult res = parser.parse("i spend 300 for today dinner");
        assertEquals(ExpenseIntent.RECORD_EXPENSE, res.intent());
        assertEquals(new BigDecimal("300"), res.amount());
        assertEquals("Food", res.category());
        assertTrue(res.description().toLowerCase().contains("dinner"));
    }

    @Test
    void testDeleteLastExpense() {
        ExpenseParseResult res = parser.parse("remove that amount");
        assertEquals(ExpenseIntent.DELETE_EXPENSE, res.intent());
        assertNull(res.amount());
        assertNull(res.description());
    }

    @Test
    void testDeleteParticularAmountRough() {
        ExpenseParseResult res = parser.parse("delete the particular amount");
        assertEquals(ExpenseIntent.DELETE_EXPENSE, res.intent());
        assertNull(res.amount());
    }

    @Test
    void testDeleteRoughMessage() {
        ExpenseParseResult res = parser.parse("i told extra money but i spend less amount for that then and after the next message i told to remove or delete the particular amount");
        assertEquals(ExpenseIntent.DELETE_EXPENSE, res.intent());
    }

    @Test
    void testDeleteAmountOnly() {
        ExpenseParseResult res = parser.parse("remove 500");
        assertEquals(ExpenseIntent.DELETE_EXPENSE, res.intent());
        assertEquals(new BigDecimal("500"), res.amount());
    }

    @Test
    void testDeleteDinnerExpense() {
        ExpenseParseResult res = parser.parse("delete dinner");
        assertEquals(ExpenseIntent.DELETE_EXPENSE, res.intent());
        assertNotNull(res.description());
        assertTrue(res.description().toLowerCase().contains("dinner"));
        assertEquals("Food", res.category());
    }

    @Test
    void testDeleteAmountAndItem() {
        ExpenseParseResult res = parser.parse("delete 300 for dinner");
        assertEquals(ExpenseIntent.DELETE_EXPENSE, res.intent());
        assertEquals(new BigDecimal("300"), res.amount());
        assertEquals("Food", res.category());
        assertTrue(res.description().toLowerCase().contains("dinner"));
    }

    @Test
    void testDeleteDinnerAmount() {
        ExpenseParseResult res = parser.parse("delete dinner 500");
        assertEquals(ExpenseIntent.DELETE_EXPENSE, res.intent());
        assertEquals(new BigDecimal("500"), res.amount());
        assertEquals("Food", res.category());
    }

    @Test
    void testUndoLastExpense() {
        ExpenseParseResult res = parser.parse("undo last expense");
        assertEquals(ExpenseIntent.DELETE_EXPENSE, res.intent());
    }
}
