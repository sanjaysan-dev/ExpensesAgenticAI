package com.agenticai.expensesagenticai.service;

import com.agenticai.expensesagenticai.dto.ExpenseIntent;
import com.agenticai.expensesagenticai.dto.ExpenseParseResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeterministicExpenseParser {

    private static final Pattern DELETE_KEYWORD_PATTERN = Pattern.compile(
            "(?i)\\b(delete|del|remove|undo|cancel|erase|drop|discard)\\b"
    );

    private static final Pattern DELETE_LAST_PATTERN = Pattern.compile(
            "(?i)(?:delete|remove|del|cancel|undo|erase|drop)\\s+(?:the\\s+)?(?:last|latest|recent|previous|that|this|the particular|particular)\\s*(?:amount|expense|entry|transaction|record|one)?\\b"
    );

    private static final Pattern DELETE_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:please\\s+)?(?:delete|remove|del|cancel|undo|erase)\\s*(?:the\\s+)?(?:that\\s+)?(?:particular\\s+)?(?:amount\\s+(?:of\\s+)?)?(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:k|grand|rupees?|rs|bucks)?(?:\\s*(?:for|from|on|towards|of|in)?\\s*(.*))?"
    );

    private static final Pattern DELETE_ITEM_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:please\\s+)?(?:delete|remove|del|cancel|undo|erase)\\s*(?:the\\s+)?(.+?)\\s+(?:for|of|at|amounting to|worth)?\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:k|grand|rupees?|rs|bucks)?(?:\\s+(today|yesterday))?$"
    );

    private static final Pattern DELETE_ITEM_PATTERN = Pattern.compile(
            "(?i)(?:please\\s+)?(?:delete|remove|del|cancel|undo|erase)\\s*(?:the\\s+)?(.+?)(?:\\s+expense|\\s+amount)?$"
    );

    private static final Pattern QUERY_PATTERN = Pattern.compile(
            "(?i)\\b(how much|what (did|have) i spen(t|d)|show (me )?(my )?expenses|total (spent|spending|expenses?)|my expenses|summary)\\b"
    );

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile(
            "\\b(\\d{4}-\\d{2}-\\d{2})\\b"
    );

    private static final Pattern DMY_DATE_PATTERN = Pattern.compile(
            "\\b(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})\\b"
    );

    // Pattern 1: Verb + Amount + Preposition? + Description
    // e.g. "i spend 300 for today dinner", "spent 350 on lunch today", "paid ₹120 for bus fare", "bought 800 groceries"
    private static final Pattern VERB_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:i(?:'ve| have)?\\s+)?(?:spent|spend|spending|spends|paid|pay|paying|pays|cost|costs|costing|bought|buy|buying|purchased|purchase|charged|gave|give)\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:k|grand|rupees?|rs|bucks)?(?:\\s*(?:on|for|towards|at)\\s+)?(.+)"
    );

    // Pattern 2: Item + Verb + Amount
    // e.g. "bought groceries for 800 yesterday", "paid for bus fare 50 rs"
    private static final Pattern ITEM_VERB_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:bought|paid for|purchased|got)\\s+(.+?)\\s+(?:for|at|cost(?:ing)?)\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:k|grand|rupees?|rs|bucks)?(.*)"
    );

    // Pattern 3: Currency/Amount + Preposition? + Description
    // e.g. "₹180 on snacks", "300 for dinner", "Rs. 500 petrol today"
    private static final Pattern AMOUNT_FIRST_PATTERN = Pattern.compile(
            "(?i)(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:k|grand|rupees?|rs|bucks)?(?:\\s*(?:on|for|towards|at)\\s+)(.+)"
    );

    // Pattern 4: Item + Amount
    // e.g. "dinner 300", "lunch 250 rs", "petrol ₹500"
    private static final Pattern ITEM_THEN_AMOUNT_PATTERN = Pattern.compile(
            "(?i)^([a-zA-Z\\s]{2,30}?)\\s+(?:(?:for|at|cost(?:ing)?)\\s+)?(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:k|grand|rupees?|rs|bucks)?(?:\\s+(today|yesterday))?$"
    );

    public ExpenseParseResult parse(String text) {
        if (text == null || text.isBlank()) {
            return ExpenseParseResult.ambiguous("Message cannot be empty.");
        }

        String trimmed = text.trim();

        // 1. Check if user is asking to delete / remove an expense
        if (isDelete(trimmed)) {
            ExpenseParseResult deleteResult = parseDelete(trimmed);
            if (deleteResult != null) {
                return deleteResult;
            }
        }

        // 2. Check if user is asking a spending query
        if (isQuery(trimmed)) {
            return parseQuery(trimmed);
        }

        // 3. Check if user is recording an expense
        ExpenseParseResult expenseResult = parseExpenseEntry(trimmed);
        if (expenseResult != null) {
            return expenseResult;
        }

        // 4. Check for general greeting
        if (isGreeting(trimmed)) {
            return ExpenseParseResult.general();
        }

        // 5. Ambiguous input
        return ExpenseParseResult.ambiguous(
                "I couldn't quite understand that. You can record an expense (e.g., 'Spent 350 on lunch today' or 'Dinner 300'), delete one (e.g., 'Delete dinner 300' or 'Remove that amount'), or ask about your spending (e.g., 'How much did I spend on food this month?')."
        );
    }

    private boolean isDelete(String text) {
        return DELETE_KEYWORD_PATTERN.matcher(text).find();
    }

    private ExpenseParseResult parseDelete(String text) {
        LocalDate expenseDate = resolveDate(text);
        boolean isDateExplicit = hasExplicitDate(text);

        // Sub-case A: Matches "delete/remove that amount / last expense / the particular amount" without an explicit amount
        if (DELETE_LAST_PATTERN.matcher(text).find() && !hasAmount(text)) {
            return ExpenseParseResult.deleteExpense(null, null, null, expenseDate, isDateExplicit);
        }

        // Sub-case B: Matches Delete + Amount (+ optional description)
        // e.g. "delete 300 for dinner", "remove 500", "remove ₹500 from petrol", "delete that 500"
        Matcher mAmount = DELETE_AMOUNT_PATTERN.matcher(text);
        if (mAmount.find() && mAmount.group(1) != null) {
            BigDecimal amount = parseAmount(mAmount.group(1), text);
            String rawDesc = mAmount.group(2);
            String cleanDesc = rawDesc != null ? cleanDeleteDescription(rawDesc) : null;
            String category = (cleanDesc != null && !cleanDesc.isBlank()) ? detectCategory(cleanDesc) : null;
            return ExpenseParseResult.deleteExpense(amount, category, cleanDesc, expenseDate, isDateExplicit);
        }

        // Sub-case C: Matches Delete + Description + Amount
        // e.g. "delete dinner 300", "remove petrol of 500"
        Matcher mItemAmount = DELETE_ITEM_AMOUNT_PATTERN.matcher(text);
        if (mItemAmount.find()) {
            String rawDesc = mItemAmount.group(1);
            BigDecimal amount = parseAmount(mItemAmount.group(2), text);
            String cleanDesc = cleanDeleteDescription(rawDesc);
            String category = (!cleanDesc.isBlank()) ? detectCategory(cleanDesc) : null;
            return ExpenseParseResult.deleteExpense(amount, category, cleanDesc, expenseDate, isDateExplicit);
        }

        // Sub-case D: Matches Delete + Description only
        // e.g. "delete dinner", "remove the dinner expense", "cancel groceries"
        Matcher mItem = DELETE_ITEM_PATTERN.matcher(text);
        if (mItem.find()) {
            String rawDesc = mItem.group(1);
            String cleanDesc = cleanDeleteDescription(rawDesc);
            if (!cleanDesc.isBlank()) {
                String category = detectCategory(cleanDesc);
                return ExpenseParseResult.deleteExpense(null, category, cleanDesc, expenseDate, isDateExplicit);
            }
        }

        // Sub-case E: Fallback for any sentence with delete keywords (e.g. rough messages like:
        // "i told extra money but i spend less amount ... remove or delete the particular amount")
        BigDecimal standaloneAmount = extractStandaloneAmount(text);
        String category = detectCategory(text);
        String cleanDesc = cleanDeleteDescription(text);

        return ExpenseParseResult.deleteExpense(
                standaloneAmount,
                !"Other".equalsIgnoreCase(category) ? category : null,
                !cleanDesc.isBlank() ? cleanDesc : null,
                expenseDate,
                isDateExplicit
        );
    }

    private boolean hasAmount(String text) {
        String noDates = stripDates(text);
        return Pattern.compile("(?i)(?:₹|rs\\.?|inr)?\\s*\\d+(?:\\.\\d{1,2})?\\s*(?:k|grand|rupees?|rs|bucks)?").matcher(noDates).find();
    }

    private BigDecimal extractStandaloneAmount(String text) {
        String noDates = stripDates(text);
        Matcher m = Pattern.compile("(?i)(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:k|grand|rupees?|rs|bucks)?").matcher(noDates);
        if (m.find()) {
            return parseAmount(m.group(1), text);
        }
        return null;
    }

    private String stripDates(String text) {
        return text.replaceAll("\\b\\d{4}-\\d{2}-\\d{2}\\b", "")
                .replaceAll("\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{4}\\b", "");
    }

    private boolean hasExplicitDate(String text) {
        String lower = text.toLowerCase();
        return lower.contains("today") || lower.contains("yesterday") || lower.contains("last night")
                || ISO_DATE_PATTERN.matcher(text).find() || DMY_DATE_PATTERN.matcher(text).find();
    }

    private String cleanDeleteDescription(String raw) {
        if (raw == null) return "";
        String cleaned = raw.replaceAll("(?i)\\b(please|delete|del|remove|undo|cancel|erase|drop|discard)\\b", "")
                .replaceAll("(?i)\\b(the|that|this|last|latest|recent|previous|particular|rough|message)\\b", "")
                .replaceAll("(?i)\\b(amount|expense|expenses|entry|transaction|record|one|it)\\b", "")
                .replaceAll("(?i)\\b(today|yesterday|tonight|last\\s+night|this\\s+morning|this\\s+evening)\\b", "")
                .replaceAll("(?i)\\bon\\s+\\d{4}-\\d{2}-\\d{2}\\b", "")
                .replaceAll("(?i)\\bon\\s+\\d{1,2}[-/]\\d{1,2}[-/]\\d{4}\\b", "")
                .replaceAll("(?i)\\b(for|on|at|towards|in|worth|of|from|to|by)\\b", "")
                .replaceAll("(?i)\\b(i|my|we|our|me|told|entered|added|spent|spend|spending)\\b", "")
                .replaceAll("(?i)\\b(extra|money|less)\\b", "")
                .replaceAll("[.,!?;:]+$", "")
                .trim();

        if (cleaned.toLowerCase().startsWith("a ")) {
            cleaned = cleaned.substring(2).trim();
        } else if (cleaned.toLowerCase().startsWith("an ")) {
            cleaned = cleaned.substring(3).trim();
        } else if (cleaned.toLowerCase().startsWith("the ")) {
            cleaned = cleaned.substring(4).trim();
        }

        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private boolean isQuery(String text) {
        return QUERY_PATTERN.matcher(text).find() ||
                text.trim().endsWith("?") ||
                text.toLowerCase().startsWith("how much") ||
                text.toLowerCase().startsWith("show me");
    }

    private ExpenseParseResult parseQuery(String text) {
        String lower = text.toLowerCase();

        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;

        if (lower.contains("last month")) {
            YearMonth prevMonth = YearMonth.from(today.minusMonths(1));
            startDate = prevMonth.atDay(1);
            endDate = prevMonth.atEndOfMonth();
        } else if (lower.contains("this week")) {
            startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else if (lower.contains("last week")) {
            LocalDate lastWeekMonday = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            startDate = lastWeekMonday;
            endDate = lastWeekMonday.plusDays(6);
        } else if (lower.contains("today") || lower.contains("tonight")) {
            startDate = today;
            endDate = today;
        } else if (lower.contains("yesterday")) {
            startDate = today.minusDays(1);
            endDate = today.minusDays(1);
        } else {
            // Default to "this month"
            startDate = today.with(TemporalAdjusters.firstDayOfMonth());
        }

        String category = detectCategoryInQuery(lower);

        return ExpenseParseResult.query(startDate, endDate, category);
    }

    private ExpenseParseResult parseExpenseEntry(String text) {
        // Try Pattern 1: Verb + Amount + Description (e.g. "i spend 300 for today dinner")
        Matcher m1 = VERB_AMOUNT_PATTERN.matcher(text);
        if (m1.find()) {
            BigDecimal amount = parseAmount(m1.group(1), text);
            String rawDesc = m1.group(2);
            return buildExpenseResult(amount, rawDesc, text);
        }

        // Try Pattern 2: Item + Verb + Amount (e.g. "bought groceries for 800 yesterday")
        Matcher m2 = ITEM_VERB_AMOUNT_PATTERN.matcher(text);
        if (m2.find()) {
            String item = m2.group(1);
            BigDecimal amount = parseAmount(m2.group(2), text);
            String trailing = m2.group(3);
            String rawDesc = (item + " " + (trailing != null ? trailing : "")).trim();
            return buildExpenseResult(amount, rawDesc, text);
        }

        // Try Pattern 3: Amount + Description (e.g. "300 for dinner", "₹180 on snacks")
        Matcher m3 = AMOUNT_FIRST_PATTERN.matcher(text);
        if (m3.find()) {
            BigDecimal amount = parseAmount(m3.group(1), text);
            String rawDesc = m3.group(2);
            return buildExpenseResult(amount, rawDesc, text);
        }

        // Try Pattern 4: Item + Amount (e.g. "dinner 300", "petrol 500 today")
        Matcher m4 = ITEM_THEN_AMOUNT_PATTERN.matcher(text);
        if (m4.find()) {
            String item = m4.group(1);
            BigDecimal amount = parseAmount(m4.group(2), text);
            String dateWord = m4.group(3);
            String rawDesc = item + (dateWord != null ? " " + dateWord : "");
            return buildExpenseResult(amount, rawDesc, text);
        }

        return null;
    }

    private BigDecimal parseAmount(String numStr, String fullText) {
        try {
            BigDecimal base = new BigDecimal(numStr);
            String lower = fullText.toLowerCase();
            // Check if user said "2k" or "2 grand"
            if (lower.matches(".*\\b" + numStr + "\\s*(k|grand)\\b.*")) {
                base = base.multiply(new BigDecimal("1000"));
            }
            return base;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private ExpenseParseResult buildExpenseResult(BigDecimal amount, String rawDescription, String fullText) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ExpenseParseResult.ambiguous("Expense amount must be greater than zero.");
        }

        // Resolve date from description or fullText
        LocalDate expenseDate = resolveDate(fullText);

        // Clean description
        String cleanDesc = cleanDescription(rawDescription);
        if (cleanDesc.isBlank()) {
            cleanDesc = "Expense";
        }

        String category = detectCategory(cleanDesc);

        return ExpenseParseResult.recordExpense(amount, category, cleanDesc, expenseDate);
    }

    private LocalDate resolveDate(String text) {
        String lower = text.toLowerCase();

        if (lower.contains("yesterday") || lower.contains("last night")) {
            return LocalDate.now().minusDays(1);
        }

        Matcher isoMatcher = ISO_DATE_PATTERN.matcher(text);
        if (isoMatcher.find()) {
            try {
                return LocalDate.parse(isoMatcher.group(1));
            } catch (Exception ignored) {}
        }

        Matcher dmyMatcher = DMY_DATE_PATTERN.matcher(text);
        if (dmyMatcher.find()) {
            try {
                int day = Integer.parseInt(dmyMatcher.group(1));
                int month = Integer.parseInt(dmyMatcher.group(2));
                int year = Integer.parseInt(dmyMatcher.group(3));
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {}
        }

        return LocalDate.now();
    }

    private String cleanDescription(String raw) {
        // Strip out date words, common prefixes, prepositions, trailing punctuation
        String cleaned = raw.replaceAll("(?i)\\b(today|yesterday|tonight|last\\s+night|this\\s+morning|this\\s+evening)\\b", "")
                .replaceAll("(?i)\\bon\\s+\\d{4}-\\d{2}-\\d{2}\\b", "")
                .replaceAll("(?i)\\bon\\s+\\d{1,2}[-/]\\d{1,2}[-/]\\d{4}\\b", "")
                .replaceAll("(?i)\\b(for|on|at|towards|in|worth|of)\\b", "")
                .replaceAll("(?i)\\b(i|my|we|our|me)\\b", "")
                .replaceAll("[.,!?;:]+$", "")
                .trim();

        // Strip leading "a ", "an ", "the "
        if (cleaned.toLowerCase().startsWith("a ")) {
            cleaned = cleaned.substring(2).trim();
        } else if (cleaned.toLowerCase().startsWith("an ")) {
            cleaned = cleaned.substring(3).trim();
        } else if (cleaned.toLowerCase().startsWith("the ")) {
            cleaned = cleaned.substring(4).trim();
        }

        return cleaned.replaceAll("\\s+", " ").trim();
    }

    public String detectCategory(String text) {
        String lower = text.toLowerCase();

        if (containsAny(lower, "lunch", "dinner", "breakfast", "food", "snacks", "restaurant",
                "groceries", "grocery", "cafe", "coffee", "tea", "burger", "pizza", "meal",
                "milk", "vegetables", "veggies", "fruits", "swiggy", "zomato", "eat", "biryani")) {
            return "Food";
        }

        if (containsAny(lower, "bus", "train", "taxi", "uber", "ola", "auto", "travel",
                "fuel", "petrol", "diesel", "flight", "metro", "fare", "toll", "rapido", "cab")) {
            return "Transport";
        }

        if (containsAny(lower, "movie", "game", "entertainment", "netflix", "cinema",
                "concert", "ott", "party", "ticket", "bookmyshow", "hotstar", "spotify")) {
            return "Entertainment";
        }

        if (containsAny(lower, "medicine", "doctor", "hospital", "pharmacy", "clinic",
                "medical", "health", "tablets", "pharmeasy", "apollo")) {
            return "Healthcare";
        }

        if (containsAny(lower, "electricity", "water", "internet", "wifi", "recharge",
                "gas", "bill", "rent", "maintenance", "jio", "airtel")) {
            return "Bills";
        }

        if (containsAny(lower, "clothes", "shirt", "pant", "shoes", "amazon", "flipkart",
                "shopping", "dress", "myntra", "zara", "jeans")) {
            return "Shopping";
        }

        return "Other";
    }

    private String detectCategoryInQuery(String lower) {
        if (lower.contains("food") || lower.contains("groceries") || lower.contains("dining") || lower.contains("dinner") || lower.contains("lunch")) return "Food";
        if (lower.contains("transport") || lower.contains("travel") || lower.contains("fuel") || lower.contains("petrol")) return "Transport";
        if (lower.contains("entertainment") || lower.contains("movie")) return "Entertainment";
        if (lower.contains("healthcare") || lower.contains("medical") || lower.contains("health")) return "Healthcare";
        if (lower.contains("bills") || lower.contains("utilities") || lower.contains("rent")) return "Bills";
        if (lower.contains("shopping")) return "Shopping";
        return null;
    }

    private boolean isGreeting(String text) {
        String lower = text.toLowerCase();
        return lower.matches("^(hi|hello|hey|greetings|good (morning|afternoon|evening))\\b.*");
    }

    private boolean containsAny(String input, String... words) {
        for (String word : words) {
            if (input.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
