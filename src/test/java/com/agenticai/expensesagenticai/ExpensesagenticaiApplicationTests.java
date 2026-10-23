package com.agenticai.expensesagenticai;

import com.agenticai.expensesagenticai.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ExpensesagenticaiApplicationTests {

	@Autowired
	private MessageService messageService;

	@Test
	void contextLoads() {
	}

	@Test
	void testProcessUserMessageQuery() {
		messageService.processUserMessage("sanjay.test@example.com", 1L, "USER", "How much did I spend yesterday?");
	}

	@Test
	void testProcessUserMessageRecord() {
		messageService.processUserMessage("sanjay.test@example.com", 1L, "USER", "Bought groceries for Rs. 800 yesterday.");
	}

	@Test
	void testProcessUserMessageSpendDinner() {
		var res = messageService.processUserMessage("sanjay.test@example.com", 1L, "USER", "i spend 300 for today dinner");
		org.junit.jupiter.api.Assertions.assertEquals(com.agenticai.expensesagenticai.dto.ExpenseIntent.RECORD_EXPENSE, res.intent());
	}
}
