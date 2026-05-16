package com.example.smartBiz.service.impl;

import com.example.smartBiz.config.OpenAiConfig;
import com.example.smartBiz.dto.ChatResponseDto;
import com.example.smartBiz.dto.UnpaidInvoiceDto;
import com.example.smartBiz.entity.AiUsageLog;
import com.example.smartBiz.entity.BusinessProfile;
import com.example.smartBiz.entity.Customer;
import com.example.smartBiz.entity.Products;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.dto.TopProductDto;
import com.example.smartBiz.repository.AiUsageLogRepo;
import com.example.smartBiz.repository.BusinessProfileRepo;
import com.example.smartBiz.repository.CustomerRepo;
import com.example.smartBiz.repository.ExpenseRepo;
import com.example.smartBiz.repository.InvoiceItemRepo;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.ChatbotService;
import com.example.smartBiz.service.UsageCounterService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_TOKENS = 600;

    private static final String BUSINESS_ONLY_RULE =
            "CRITICAL RULE: You are SmartBiz Assistant — a business-only AI for an ERP system. " +
            "You ONLY answer questions related to business, finance, sales, marketing, customers, " +
            "inventory, invoices, pricing, or entrepreneurship. " +
            "If the user asks ANYTHING unrelated to business (e.g. sports, cooking, politics, " +
            "entertainment, personal topics, general knowledge), you MUST reply with exactly: " +
            "'I can only help with business-related questions. Try asking about your sales, " +
            "invoices, customers, products, or how to grow your business.' " +
            "Never break this rule under any circumstances. " +
            "IMPORTANT: This assistant supports only English, Tamil, and Sinhala. " +
            "Detect which of these three languages the user wrote in and reply in that SAME language. " +
            "If the user writes in any other language, reply in English only.";

    private final OpenAiConfig openAiConfig;
    private final RestTemplate restTemplate;
    private final InvoiceRepo invoiceRepo;
    private final InvoiceItemRepo invoiceItemRepo;
    private final ProductRepo productRepo;
    private final CustomerRepo customerRepo;
    private final ExpenseRepo expenseRepo;
    private final BusinessProfileRepo businessProfileRepo;
    private final UsageCounterService usageCounterService;
    private final AiUsageLogRepo aiUsageLogRepo;

    public ChatbotServiceImpl(OpenAiConfig openAiConfig,
                              RestTemplate restTemplate,
                              InvoiceRepo invoiceRepo,
                              InvoiceItemRepo invoiceItemRepo,
                              ProductRepo productRepo,
                              CustomerRepo customerRepo,
                              ExpenseRepo expenseRepo,
                              BusinessProfileRepo businessProfileRepo,
                              UsageCounterService usageCounterService,
                              AiUsageLogRepo aiUsageLogRepo) {
        this.openAiConfig = openAiConfig;
        this.restTemplate = restTemplate;
        this.invoiceRepo = invoiceRepo;
        this.invoiceItemRepo = invoiceItemRepo;
        this.productRepo = productRepo;
        this.customerRepo = customerRepo;
        this.expenseRepo = expenseRepo;
        this.businessProfileRepo = businessProfileRepo;
        this.usageCounterService = usageCounterService;
        this.aiUsageLogRepo = aiUsageLogRepo;
    }

    // ─── Intent Detection ────────────────────────────────────────────────────

    private enum Intent {
        INVOICE, SALES, CUSTOMER, STOCK, ADVISOR, GUIDE, GENERAL
    }

    private Intent detectIntent(String message) {
        // Normalize: lowercase, collapse spaces, remove punctuation
        String m = message.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // GUIDE — checked FIRST so "how to add product/customer/invoice" wins over data intents
        if (containsAny(m,
                "how to create", "how to make", "how to add", "how to record",
                "how to use", "how to generate", "how do i create", "how do i make",
                "how do i add", "how do i use", "how do i record",
                "steps to", "step by step", "guide", "tutorial",
                "teach me", "explain how", "show me how", "what are the steps",
                "help me create", "help me add", "help me make",
                "create invoice", "make invoice", "create bill", "make bill",
                "create a bill", "make a bill", "create an invoice",
                "creat invoice", "creat bill", "crete invoice",
                "add customer", "add product", "add expense",
                "ad customer", "ad product", "ad expense",
                "how billing", "billing process", "how bill work",
                "how to bill", "how to invoice"))
            return Intent.GUIDE;

        // INVOICE — common misspellings: invoce, invioce, invoise, bll, unpiad, overdu
        if (containsAny(m,
                "invoice", "invoce", "invioce", "invoise", "invoices",
                "bill", "bll", "bills",
                "unpaid", "unpiad", "un paid",
                "due", "deu",
                "payment pending", "paymet pending", "paymnt",
                "overdue", "overdu", "over due",
                "show invoice", "show bill", "show unpaid", "list invoice",
                "list bill", "list unpaid", "display invoice", "get invoice",
                "view invoice", "view bill", "my invoice", "my bill"))
            return Intent.INVOICE;

        // SALES — common misspellings: sals, proffit, revenu, incme, earings
        if (containsAny(m,
                "sale", "sales", "sals", "saels",
                "revenue", "revenu", "revanue", "revnue",
                "income", "incme", "incom",
                "earning", "earings", "earns",
                "profit", "proffit", "proift", "prfit",
                "loss", "los", "losse",
                "today", "toady", "tody", "todays", "today sales", "today report",
                "today profit", "today income", "today earning", "today revenue",
                "what today", "show today", "give today",
                "this month", "ths month", "this mnth",
                "this year", "ths year", "this yeer",
                "last month", "lst month", "last mnth",
                "last year", "lst year", "last yeer",
                "last week", "lst week", "last wek",
                "yesterday", "yesterdy", "yestrday",
                "how much", "how mch", "how mutch",
                "performance", "performace", "preformance",
                "total sales", "total revenue", "total income", "total profit",
                "monthly sales", "weekly sales", "annual sales", "yearly sales",
                "this week", "current month", "current year",
                "expense", "expenses", "expnse", "expence", "expens",
                "spending", "spendings", "spent", "cost", "costs",
                "today expense", "today cost", "today spending",
                "this month expense", "last month expense",
                "how much spend", "how much spent", "how much cost",
                "january", "february", "march", "april", "may", "june",
                "july", "august", "september", "october", "november", "december",
                "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "oct", "nov", "dec"))
            return Intent.SALES;

        // CUSTOMER — common misspellings: custmer, costumer, cilent, cliet
        if (containsAny(m,
                "customer", "custmer", "cusomer", "costumer", "custommer",
                "client", "cilent", "clinet", "cliet",
                "buyer", "buyr",
                "who bought", "who buy",
                "top customer", "best customer"))
            return Intent.CUSTOMER;

        // STOCK — common misspellings: stok, inventry, prodcut, itm
        if (containsAny(m,
                "stock", "stok", "stcok", "stokc",
                "inventory", "inventry", "invertory", "invetory",
                "product", "prodcut", "produt", "prodcts",
                "low stock", "lo stock", "lowstock",
                "out of stock", "out stock", "no stock",
                "available", "availble", "availble",
                "item", "itm", "itmes"))
            return Intent.STOCK;

        // ADVISOR — common misspellings: improove, stratagy, advise, suggets
        if (containsAny(m,
                "improve", "improove", "imporve", "improvment",
                "grow", "grwo", "growt", "growth",
                "strategy", "stratagy", "stratergy", "strategey",
                "advice", "advise", "advcie", "adivce",
                "suggest", "suggets", "sugest",
                "tips", "tps", "tip",
                "help my business", "help bussiness", "help my bussiness",
                "how to increase", "how to increse",
                "boost", "boast", "bost",
                "better", "betr", "beter",
                "struggling", "strugling", "struglling"))
            return Intent.ADVISOR;

        return Intent.GENERAL;
    }

    // ─── GUIDE Handler ───────────────────────────────────────────────────────

    private enum GuideType {
        CREATE_INVOICE, ADD_CUSTOMER, ADD_PRODUCT, RECORD_EXPENSE, USE_POS, GENERAL_HELP
    }

    private GuideType detectGuideType(String m) {
        if (containsAny(m, "invoice", "invoce", "invioce", "bill", "bll", "billing", "creat invoice", "creat bill"))
            return GuideType.CREATE_INVOICE;
        if (containsAny(m, "customer", "custmer", "cusomer", "client"))
            return GuideType.ADD_CUSTOMER;
        if (containsAny(m, "product", "prodcut", "produt", "item", "itm"))
            return GuideType.ADD_PRODUCT;
        if (containsAny(m, "expense", "expnse", "expence", "expens", "cost"))
            return GuideType.RECORD_EXPENSE;
        if (containsAny(m, "pos", "point of sale", "quick sale", "quick bill", "quick invoice"))
            return GuideType.USE_POS;
        return GuideType.GENERAL_HELP;
    }

    private String handleGuide(String message) {
        String m = message.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        GuideType type = detectGuideType(m);

        return switch (type) {
            case CREATE_INVOICE -> """
                    Here's how to create an invoice in SmartBiz:

                    1. Open Invoices from the sidebar (or press Ctrl+I)
                    2. Click the + New Invoice button
                    3. Select a customer from the dropdown
                    4. Add products or services — type the name and choose from the list
                    5. Set quantity for each item
                    6. Apply a line discount or overall discount if needed
                    7. Click Save Invoice
                    8. On the invoice detail page you can Download PDF or Print

                    Tip: Use the POS screen for faster walk-in billing!"""
                    .stripIndent();

            case ADD_CUSTOMER -> """
                    Here's how to add a new customer:

                    1. Go to Customers from the sidebar
                    2. Click the + button
                    3. Enter the customer's name (required), email, and phone
                    4. Add address if needed
                    5. Click Save

                    Tip: You can also add a customer directly from the invoice creation screen!"""
                    .stripIndent();

            case ADD_PRODUCT -> """
                    Here's how to add a new product:

                    1. Go to Products from the sidebar
                    2. Click the + button
                    3. Enter product name and price
                    4. Select a category (or create one)
                    5. Set stock quantity
                    6. Set a low stock limit to get alerts when running low
                    7. Click Save

                    Tip: Use batches to track stock from multiple suppliers!"""
                    .stripIndent();

            case RECORD_EXPENSE -> """
                    Here's how to record an expense:

                    1. Go to Expenses from the sidebar
                    2. Click the + button
                    3. Enter description of the expense
                    4. Enter the amount
                    5. Select the date
                    6. Click Save

                    Tip: Expenses are included in your profit calculations automatically!"""
                    .stripIndent();

            case USE_POS -> """
                    Here's how to use POS (Point of Sale) for quick billing:

                    1. Go to Invoices → click the POS button (top right)
                    2. Search for products by name or scan barcode
                    3. Click + to add items to the cart
                    4. Select or search for a customer
                    5. Apply any discount if needed
                    6. Click Complete Sale
                    7. Invoice is created and saved automatically

                    Tip: POS is the fastest way to create invoices for walk-in customers!"""
                    .stripIndent();

            case GENERAL_HELP -> """
                    I can guide you through these SmartBiz features. Just ask:

                    📄 "How to create an invoice / bill"
                    👤 "How to add a customer"
                    📦 "How to add a product"
                    💸 "How to record an expense"
                    🖥  "How to use POS"

                    Or ask about your live data:
                    💰 "What are my sales this month?"
                    📊 "Show unpaid invoices"
                    📉 "What is my profit last month?"
                    🔴 "Which products are low on stock?"""
                    .stripIndent();
        };
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    // ─── Main Chat Method ────────────────────────────────────────────────────

    @Override
    public ChatResponseDto chat(Long businessId, String message, List<Map<String, String>> history) {
        Intent intent = detectIntent(message);

        String reply = switch (intent) {
            case INVOICE  -> handleInvoice(businessId, message);
            case SALES    -> handleSales(businessId, message);
            case CUSTOMER -> handleCustomer(businessId, message);
            case STOCK    -> handleStock(businessId, message);
            case ADVISOR  -> handleAdvisor(businessId, message);
            case GUIDE    -> handleGuide(message);
            case GENERAL  -> handleGeneral(businessId, message, history);
        };

        // GUIDE responses are free — no credit consumed
        if (intent != Intent.GUIDE) {
            logAiUsage(businessId, intent.name());
        }

        String actionLink = switch (intent) {
            case INVOICE  -> "/invoices";
            case CUSTOMER -> "/customers";
            case STOCK    -> "/products";
            case SALES, ADVISOR -> "/dashboard";
            default -> null;
        };

        String actionLabel = switch (intent) {
            case INVOICE  -> "View Invoices";
            case CUSTOMER -> "View Customers";
            case STOCK    -> "View Products";
            case SALES    -> "View Dashboard";
            case ADVISOR  -> "View Dashboard";
            default -> null;
        };

        return ChatResponseDto.builder()
                .reply(reply)
                .intent(intent.name())
                .actionLink(actionLink)
                .actionLabel(actionLabel)
                .build();
    }

    // ─── INVOICE Handler ─────────────────────────────────────────────────────

    private String handleInvoice(Long businessId, String message) {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<UnpaidInvoiceDto> unpaid = invoiceRepo.findInvoicesByStatusAsDtoAndBusinessId(
                InvoiceStatus.UNPAID, monthStart, now, businessId, PageRequest.of(0, 10));

        long totalInvoices = invoiceRepo.countByBusinessIdAndArchivedFalse(businessId);
        long unpaidCount = invoiceRepo.countByBusinessIdAndStatusAndArchivedFalse(businessId, InvoiceStatus.UNPAID);
        double unpaidTotal = unpaid.stream().mapToDouble(UnpaidInvoiceDto::getTotalAmount).sum();

        StringBuilder context = new StringBuilder();
        context.append("INVOICE DATA:\n");
        context.append("Total invoices: ").append(totalInvoices).append("\n");
        context.append("Unpaid invoices: ").append(unpaidCount)
               .append(" totalling ").append(currency(businessId)).append(" ").append(String.format("%.2f", unpaidTotal)).append("\n");

        if (!unpaid.isEmpty()) {
            context.append("Recent unpaid invoices:\n");
            for (UnpaidInvoiceDto inv : unpaid) {
                context.append("  - #").append(inv.getInvoiceNumber())
                       .append(" | ").append(inv.getCustomerName())
                       .append(" | ").append(currency(businessId)).append(" ").append(String.format("%.2f", inv.getTotalAmount()))
                       .append("\n");
            }
        }

        String systemPrompt = "You are SmartBiz Assistant, an ERP invoice assistant. " +
                "Answer the user's invoice question using the data below. " +
                "Be concise and clear. Format numbers cleanly. " + BUSINESS_ONLY_RULE;

        return callOpenAi(systemPrompt, context + "\nUser question: " + message);
    }

    // ─── SALES Handler ───────────────────────────────────────────────────────

    private String handleSales(Long businessId, String message) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = LocalDateTime.now();

        LocalDateTime thisMonthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime thisYearStart = today.withDayOfYear(1).atStartOfDay();

        LocalDate lastMonthDate = today.minusMonths(1);
        LocalDateTime lastMonthStart = lastMonthDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthEnd = lastMonthDate.withDayOfMonth(lastMonthDate.lengthOfMonth()).atTime(23, 59, 59);

        LocalDateTime lastWeekStart = today.minusWeeks(1).atStartOfDay();
        LocalDateTime lastYearStart = today.minusYears(1).withDayOfYear(1).atStartOfDay();
        LocalDateTime lastYearEnd = today.minusYears(1).withDayOfYear(today.minusYears(1).lengthOfYear()).atTime(23, 59, 59);

        double todaySales = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, todayStart, todayEnd);
        double thisMonthSales = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, thisMonthStart, todayEnd);
        double thisYearSales = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, thisYearStart, todayEnd);
        double lastMonthSales = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, lastMonthStart, lastMonthEnd);
        double lastWeekSales = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, lastWeekStart, todayEnd);
        double lastYearSales = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, lastYearStart, lastYearEnd);

        double todayExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, todayStart, todayEnd);
        double lastWeekExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, lastWeekStart, todayEnd);
        double thisMonthExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, thisMonthStart, todayEnd);
        double lastMonthExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, lastMonthStart, lastMonthEnd);
        double thisYearExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, thisYearStart, todayEnd);
        double todayProfit = todaySales - todayExpenses;
        double thisMonthProfit = thisMonthSales - thisMonthExpenses;
        double lastMonthProfit = lastMonthSales - lastMonthExpenses;

        // Month-over-month growth percentages (B3)
        double salesMoM = lastMonthSales > 0 ? ((thisMonthSales - lastMonthSales) / lastMonthSales * 100) : 0;
        double profitMoM = lastMonthProfit != 0 ? ((thisMonthProfit - lastMonthProfit) / Math.abs(lastMonthProfit) * 100) : 0;

        String cur = currency(businessId);

        // Named-month lookup: detect if user asked about a specific month by name
        java.util.Map<String, Integer> monthMap = new java.util.LinkedHashMap<>();
        monthMap.put("january", 1); monthMap.put("jan", 1);
        monthMap.put("february", 2); monthMap.put("feb", 2);
        monthMap.put("march", 3); monthMap.put("mar", 3);
        monthMap.put("april", 4); monthMap.put("apr", 4);
        monthMap.put("may", 5);
        monthMap.put("june", 6); monthMap.put("jun", 6);
        monthMap.put("july", 7); monthMap.put("jul", 7);
        monthMap.put("august", 8); monthMap.put("aug", 8);
        monthMap.put("september", 9); monthMap.put("sep", 9); monthMap.put("sept", 9);
        monthMap.put("october", 10); monthMap.put("oct", 10);
        monthMap.put("november", 11); monthMap.put("nov", 11);
        monthMap.put("december", 12); monthMap.put("dec", 12);

        String normalizedMsg = message.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        String namedMonthContext = "";
        for (java.util.Map.Entry<String, Integer> entry : monthMap.entrySet()) {
            if (normalizedMsg.contains(entry.getKey())) {
                int monthNum = entry.getValue();
                int year = today.getYear();
                // If the month hasn't occurred yet this year, look at last year
                if (monthNum > today.getMonthValue()) year = year - 1;
                LocalDateTime nmStart = LocalDate.of(year, monthNum, 1).atStartOfDay();
                LocalDateTime nmEnd = LocalDate.of(year, monthNum, 1).withDayOfMonth(
                        LocalDate.of(year, monthNum, 1).lengthOfMonth()).atTime(23, 59, 59);
                double nmSales = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                        businessId, InvoiceStatus.PAID, nmStart, nmEnd);
                double nmExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, nmStart, nmEnd);
                double nmProfit = nmSales - nmExpenses;
                String monthLabel = entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1) + " " + year;
                namedMonthContext = monthLabel + " sales: " + cur + " " + String.format("%.2f", nmSales) + "\n" +
                        monthLabel + " expenses: " + cur + " " + String.format("%.2f", nmExpenses) + "\n" +
                        monthLabel + " profit: " + cur + " " + String.format("%.2f", nmProfit) + "\n";
                break;
            }
        }

        String context = "SALES, EXPENSES & PROFIT DATA:\n" +
                (!namedMonthContext.isEmpty() ? namedMonthContext : "") +
                "Today's sales: " + cur + " " + String.format("%.2f", todaySales) + "\n" +
                "Today's expenses: " + cur + " " + String.format("%.2f", todayExpenses) + "\n" +
                "Today's profit: " + cur + " " + String.format("%.2f", todayProfit) + "\n" +
                "This month's sales: " + cur + " " + String.format("%.2f", thisMonthSales) +
                    " (" + (salesMoM >= 0 ? "+" : "") + String.format("%.1f", salesMoM) + "% vs last month)\n" +
                "This month's expenses: " + cur + " " + String.format("%.2f", thisMonthExpenses) + "\n" +
                "This month's profit: " + cur + " " + String.format("%.2f", thisMonthProfit) +
                    " (" + (profitMoM >= 0 ? "+" : "") + String.format("%.1f", profitMoM) + "% vs last month)\n" +
                "Last 7 days sales: " + cur + " " + String.format("%.2f", lastWeekSales) + "\n" +
                "Last 7 days expenses: " + cur + " " + String.format("%.2f", lastWeekExpenses) + "\n" +
                "Last month's sales: " + cur + " " + String.format("%.2f", lastMonthSales) + "\n" +
                "Last month's expenses: " + cur + " " + String.format("%.2f", lastMonthExpenses) + "\n" +
                "Last month's profit: " + cur + " " + String.format("%.2f", lastMonthProfit) + "\n" +
                "This year's sales: " + cur + " " + String.format("%.2f", thisYearSales) + "\n" +
                "This year's expenses: " + cur + " " + String.format("%.2f", thisYearExpenses) + "\n" +
                "Last year's sales: " + cur + " " + String.format("%.2f", lastYearSales) + "\n";

        String systemPrompt = "You are SmartBiz Assistant, an ERP sales analyst. " +
                "The user is asking about their business sales or financial performance. " +
                "You MUST answer using the real data provided below — never refuse or say you cannot help. " +
                "If the user asks about 'today', use Today's sales, expenses, and profit. " +
                "If they ask about 'this month', use This month's data. " +
                "If they ask about 'last month', use Last month's data. " +
                "If they ask about 'expense' or 'expenses', use the expenses figures from the data. " +
                "If they just say a short phrase, summarize the most relevant numbers. " +
                "Include month-over-month growth percentages when relevant. " +
                "Be concise and direct. State the exact numbers from the data. " + BUSINESS_ONLY_RULE;

        return callOpenAi(systemPrompt, context + "\nUser question: " + message);
    }

    // ─── CUSTOMER Handler ────────────────────────────────────────────────────

    private String handleCustomer(Long businessId, String message) {
        List<Customer> customers = customerRepo.findByBusinessIdAndArchivedFalse(businessId);
        long total = customers.size();
        String cur = currency(businessId);

        StringBuilder context = new StringBuilder("CUSTOMER DATA:\n");
        context.append("Total active customers: ").append(total).append("\n");

        // Get customer-level revenue by summing paid invoices per customer
        List<Object[]> customerRevenue = invoiceRepo.findByBusinessIdAndArchivedFalse(businessId)
                .stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID && i.getCustomer() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        i -> i.getCustomer().getName(),
                        java.util.stream.Collectors.summingDouble(i -> i.getTotalAmount() != null ? i.getTotalAmount() : 0.0)))
                .entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> new Object[]{e.getKey(), e.getValue()})
                .toList();

        if (!customerRevenue.isEmpty()) {
            context.append("Top customers by revenue (all time):\n");
            customerRevenue.forEach(row ->
                context.append("  - ").append(row[0])
                       .append(": ").append(cur).append(" ").append(String.format("%.2f", row[1]))
                       .append("\n"));
        }

        if (!customers.isEmpty()) {
            context.append("Customer list (first 10):\n");
            customers.stream().limit(10).forEach(c -> {
                context.append("  - ").append(c.getName());
                if (c.getEmail() != null) context.append(" | ").append(c.getEmail());
                if (c.getPhone() != null) context.append(" | ").append(c.getPhone());
                context.append("\n");
            });
        }

        String systemPrompt = "You are SmartBiz Assistant, an ERP customer support tool. " +
                "Answer the user's customer question using the data below. " +
                "Be concise and helpful. " + BUSINESS_ONLY_RULE;

        return callOpenAi(systemPrompt, context + "\nUser question: " + message);
    }

    // ─── STOCK Handler ───────────────────────────────────────────────────────

    private String handleStock(Long businessId, String message) {
        List<Products> products = productRepo.findByBusinessIdAndDeletedAtIsNull(businessId);
        long lowStockCount = productRepo.countLowStockProductsByBusinessId(businessId);
        long activeCount = productRepo.countByBusinessIdAndDeletedAtIsNull(businessId);
        String cur = currency(businessId);

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        StringBuilder context = new StringBuilder("STOCK / INVENTORY DATA:\n");
        context.append("Total active products: ").append(activeCount).append("\n");
        context.append("Low stock products: ").append(lowStockCount).append("\n");

        List<Products> lowStock = products.stream()
                .filter(p -> p.getStock_qty() != null && p.getLow_stock_limit() != null
                        && p.getStock_qty() <= p.getLow_stock_limit())
                .toList();

        if (!lowStock.isEmpty()) {
            context.append("Low stock items:\n");
            lowStock.forEach(p -> context.append("  - ").append(p.getName())
                    .append(": ").append(p.getStock_qty()).append(" units left")
                    .append(" (limit: ").append(p.getLow_stock_limit()).append(")\n"));
        }

        // Top 5 best-selling products this month
        List<TopProductDto> topProducts = invoiceItemRepo.findTopProductsByBusiness(
                InvoiceStatus.PAID, monthStart, now, businessId, PageRequest.of(0, 5));
        if (!topProducts.isEmpty()) {
            context.append("Top 5 best-selling products this month:\n");
            topProducts.forEach(p -> context.append("  - ").append(p.getProductName())
                    .append(": ").append(p.getTotalQty()).append(" sold")
                    .append(" | revenue: ").append(cur).append(" ").append(String.format("%.2f", p.getTotalRevenue()))
                    .append("\n"));
        }

        String systemPrompt = "You are SmartBiz Assistant, an ERP inventory manager. " +
                "Answer the user's stock/inventory question using the data below. " +
                "Be clear and list specific products when relevant. " + BUSINESS_ONLY_RULE;

        return callOpenAi(systemPrompt, context + "\nUser question: " + message);
    }

    // ─── ADVISOR Handler ─────────────────────────────────────────────────────

    private String handleAdvisor(Long businessId, String message) {
        BusinessProfile profile = businessProfileRepo.findByBusinessId(businessId).orElse(null);
        String bizName = profile != null && profile.getBusinessName() != null ? profile.getBusinessName() : "Your Business";
        String industry = profile != null && profile.getIndustry() != null ? profile.getIndustry() : "business";
        String country = profile != null && profile.getCountry() != null ? profile.getCountry() : "";
        String currency = profile != null && profile.getCurrency() != null ? profile.getCurrency() : "LKR";

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        double monthSales = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, monthStart, now);
        double monthExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, monthStart, now);
        double monthProfit = monthSales - monthExpenses;
        double profitMargin = monthSales > 0 ? (monthProfit / monthSales * 100) : 0;
        long unpaidCount = invoiceRepo.countByBusinessIdAndStatusAndArchivedFalse(businessId, InvoiceStatus.UNPAID);
        long lowStockCount = productRepo.countLowStockProductsByBusinessId(businessId);
        long totalCustomers = customerRepo.countByBusinessIdAndArchivedFalse(businessId);

        List<UnpaidInvoiceDto> unpaidList = invoiceRepo.findInvoicesByStatusAsDtoAndBusinessId(
                InvoiceStatus.UNPAID, monthStart, now, businessId, PageRequest.of(0, 5));
        double unpaidTotal = unpaidList.stream().mapToDouble(UnpaidInvoiceDto::getTotalAmount).sum();

        String systemPrompt = "You are a senior business advisor for " + bizName + ", a " + industry + " business" +
                (country.isBlank() ? "" : " in " + country) + ". " +
                "You have access to the owner's real business data for this month. " +
                "Give specific, actionable, honest advice based on the actual numbers. " +
                "Be direct — like a real consultant. Do not give generic tips. " +
                "Reference the actual figures in your advice. " +
                "Keep your response under 200 words. Use plain text, no markdown. " +
                BUSINESS_ONLY_RULE;

        // Last month data for MoM comparison
        LocalDate lastMonthDate = LocalDate.now().minusMonths(1);
        LocalDateTime lastMonthStart = lastMonthDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthEnd = lastMonthDate.withDayOfMonth(lastMonthDate.lengthOfMonth()).atTime(23, 59, 59);
        double lastMonthSales = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(
                businessId, InvoiceStatus.PAID, lastMonthStart, lastMonthEnd);
        double lastMonthExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, lastMonthStart, lastMonthEnd);
        double lastMonthProfit = lastMonthSales - lastMonthExpenses;
        double salesGrowth = lastMonthSales > 0 ? ((monthSales - lastMonthSales) / lastMonthSales * 100) : 0;
        double profitGrowth = lastMonthProfit != 0 ? ((monthProfit - lastMonthProfit) / Math.abs(lastMonthProfit) * 100) : 0;

        // Cash flow warning flag
        boolean cashFlowWarning = monthSales > 0 && (unpaidTotal / monthSales) > 0.30;

        // Top 5 best-selling products this month
        List<TopProductDto> topProducts = invoiceItemRepo.findTopProductsByBusiness(
                InvoiceStatus.PAID, monthStart, now, businessId, PageRequest.of(0, 5));

        StringBuilder dataContext = new StringBuilder("CURRENT BUSINESS DATA (this month):\n");
        dataContext.append("Business: ").append(bizName).append(" | Industry: ").append(industry).append("\n");
        dataContext.append("Currency: ").append(currency).append("\n");
        dataContext.append("Sales: ").append(currency).append(" ").append(String.format("%.2f", monthSales)).append("\n");
        dataContext.append("Expenses: ").append(currency).append(" ").append(String.format("%.2f", monthExpenses)).append("\n");
        dataContext.append("Profit: ").append(currency).append(" ").append(String.format("%.2f", monthProfit)).append("\n");
        dataContext.append("Profit Margin: ").append(String.format("%.1f", profitMargin)).append("%\n");
        dataContext.append("Sales vs last month: ").append(String.format("%.1f", salesGrowth)).append("% ").append(salesGrowth >= 0 ? "growth" : "decline").append("\n");
        dataContext.append("Profit vs last month: ").append(String.format("%.1f", profitGrowth)).append("% ").append(profitGrowth >= 0 ? "growth" : "decline").append("\n");
        dataContext.append("Unpaid Invoices: ").append(unpaidCount).append(" totalling ").append(currency).append(" ").append(String.format("%.2f", unpaidTotal)).append("\n");
        if (cashFlowWarning) {
            dataContext.append("CASH FLOW WARNING: Unpaid invoices represent ").append(String.format("%.0f", (unpaidTotal / monthSales * 100))).append("% of monthly sales — this is a significant risk.\n");
        }
        dataContext.append("Low Stock Products: ").append(lowStockCount).append("\n");
        dataContext.append("Total Customers: ").append(totalCustomers).append("\n");
        if (!topProducts.isEmpty()) {
            dataContext.append("Top selling products this month:\n");
            topProducts.forEach(p -> dataContext.append("  - ").append(p.getProductName())
                    .append(": ").append(p.getTotalQty()).append(" units sold\n"));
        }

        return callOpenAi(systemPrompt, dataContext + "\nOwner's question: " + message);
    }

    // ─── GENERAL Handler ─────────────────────────────────────────────────────

    private String handleGeneral(Long businessId, String message, List<Map<String, String>> history) {
        String systemPrompt = "You are SmartBiz Assistant — a helpful business AI for an ERP system. " +
                "You help business owners with questions about sales, marketing, finance, customers, " +
                "inventory, invoices, and general business management. " +
                BUSINESS_ONLY_RULE;

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        if (history != null) {
            int start = Math.max(0, history.size() - 5);
            for (Map<String, String> h : history.subList(start, history.size())) {
                String role = h.getOrDefault("role", "user");
                String content = h.getOrDefault("content", "");
                if (!content.isBlank() && (role.equals("user") || role.equals("assistant"))) {
                    messages.add(Map.of("role", role, "content", content));
                }
            }
        }

        messages.add(Map.of("role", "user", "content", message));

        return callOpenAiWithMessages(messages);
    }

    // ─── OpenAI Helpers ──────────────────────────────────────────────────────

    private String callOpenAi(String systemPrompt, String userContent) {
        return callOpenAiWithMessages(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ));
    }

    private String callOpenAiWithMessages(List<Map<String, String>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiConfig.getApiKey());

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "messages", messages,
                "max_tokens", MAX_TOKENS
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    OPENAI_URL, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String result = extractContent(response.getBody());
                // E1: Retry if response is suspiciously short or a refusal
                if (result != null && result.trim().length() < 20) {
                    System.err.println("[CHATBOT] Short response detected, retrying...");
                    ResponseEntity<Map<String, Object>> retry = restTemplate.exchange(
                            OPENAI_URL, HttpMethod.POST, entity,
                            new ParameterizedTypeReference<Map<String, Object>>() {});
                    if (retry.getStatusCode() == HttpStatus.OK && retry.getBody() != null) {
                        return extractContent(retry.getBody());
                    }
                }
                return result;
            }
        } catch (Exception e) {
            System.err.println("[CHATBOT ERROR] " + e.getMessage());
        }

        return "I'm having trouble connecting right now. Please try again in a moment.";
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            return (String) msg.get("content");
        } catch (Exception e) {
            return "Sorry, I could not parse the response. Please try again.";
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String currency(Long businessId) {
        return businessProfileRepo.findByBusinessId(businessId)
                .map(p -> p.getCurrency() != null ? p.getCurrency() : "LKR")
                .orElse("LKR");
    }

    private void logAiUsage(Long businessId, String intent) {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        Long userId = (principal != null) ? principal.getUserId() : 0L;
        usageCounterService.incrementAiCount(businessId);
        aiUsageLogRepo.save(AiUsageLog.builder()
                .businessId(businessId)
                .userId(userId)
                .feature("CHAT_" + intent)
                .creditsUsed(1L)
                .build());
    }
}
