package com.example.smartBiz.service.impl;

import com.example.smartBiz.config.OpenAiConfig;
import com.example.smartBiz.dto.AiReportResponseDto;
import com.example.smartBiz.dto.AnalyticsResponseDto;
import com.example.smartBiz.dto.UnpaidInvoiceDto;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.exception.AiException;
import com.example.smartBiz.repository.BusinessRepo;
import com.example.smartBiz.repository.ExpenseRepo;
import com.example.smartBiz.repository.InvoiceItemRepo;
import com.example.smartBiz.repository.InvoiceRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.repository.projection.TopProductRow;
import com.example.smartBiz.service.AiService;
import com.example.smartBiz.service.AuditLogService;
import com.example.smartBiz.service.UsageCounterService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_TOKENS = 800;
    private static final int MAX_INPUT_LENGTH = 500;

    private final OpenAiConfig openAiConfig;
    private final RestTemplate restTemplate;
    private final InvoiceRepo invoiceRepo;
    private final ProductRepo productRepo;
    private final ExpenseRepo expenseRepo;
    private final UsageCounterService usageCounterService;
    private final AuditLogService auditLogService;
    private final BusinessRepo businessRepo;
    private final InvoiceItemRepo invoiceItemRepo;

    public AiServiceImpl(RestTemplate restTemplate,
            InvoiceRepo invoiceRepo,
            ProductRepo productRepo,
            ExpenseRepo expenseRepo,
            UsageCounterService usageCounterService,
            AuditLogService auditLogService,
            OpenAiConfig openAiConfig,
            BusinessRepo businessRepo,
            InvoiceItemRepo invoiceItemRepo) {
        this.restTemplate = restTemplate;
        this.invoiceRepo = invoiceRepo;
        this.productRepo = productRepo;
        this.expenseRepo = expenseRepo;
        this.usageCounterService = usageCounterService;
        this.auditLogService = auditLogService;
        this.openAiConfig = openAiConfig;
        this.businessRepo = businessRepo;
        this.invoiceItemRepo = invoiceItemRepo;
    }

    // ─────────────────────────────────────────────
    // CORE OPENAI CALLER
    // ─────────────────────────────────────────────
    private String callOpenAi(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiConfig.getApiKey());

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "messages", messages,
                "max_tokens", MAX_TOKENS);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    OPENAI_URL,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractContent(response.getBody());
            }

            String errorMsg = "OpenAI returned " + response.getStatusCode();
            if (response.getBody() != null) {
                errorMsg += " - " + response.getBody().toString();
            }
            auditLogService.error(errorMsg);
            System.err.println("[AI ERROR] " + errorMsg);

        } catch (HttpClientErrorException e) {
            String errorMsg = "OpenAI API Client Error: " + e.getStatusCode().value() + " "
                    + e.getResponseBodyAsString();
            auditLogService.error(errorMsg);
            System.err.println("[AI ERROR] " + errorMsg);
        } catch (Exception e) {
            auditLogService.error("OpenAI API System Error: " + e.getMessage());
            e.printStackTrace();
        }

        return "AI response unavailable. Please try again later.";
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            auditLogService.error("Failed to parse OpenAI response: " + e.getMessage());
            return "Failed to parse AI response.";
        }
    }

    private String sanitize(String input) {
        if (input == null)
            return "";
        String cleaned = input
                .replace("\"", "'")
                .replace("\n", " ")
                .replace("\r", "")
                .trim();
        return cleaned.length() > MAX_INPUT_LENGTH
                ? cleaned.substring(0, MAX_INPUT_LENGTH)
                : cleaned;
    }

    // ─────────────────────────────────────────────
    // ANALYTICS
    // ─────────────────────────────────────────────
    @Override
    public AnalyticsResponseDto getAnalytics(Long businessId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);

        long invoiceCount = invoiceRepo.countByBusinessIdAndInvoiceDateBetween(businessId, start, end);
        double totalSales = invoiceRepo.sumTotalAmountByBusinessAndStatusAndDateRange(businessId,
                InvoiceStatus.PAID, start, end);
        double totalExpenses = expenseRepo.sumExpensesByBusinessBetween(businessId, start, end);
        long activeProducts = productRepo.countByBusinessIdAndDeletedAtIsNull(businessId);
        long lowStockProducts = productRepo.countLowStockProductsByBusinessId(businessId);
        double profit = totalSales - totalExpenses;

        List<TopProductRow> topRows = invoiceItemRepo.findTopSellingProducts(
                businessId, start, end, PageRequest.of(0, 3));

        List<AnalyticsResponseDto.TopProductDto> topProducts = topRows.stream()
                .map(r -> new AnalyticsResponseDto.TopProductDto(
                        r.getName(),
                        r.getQtySold() != null ? r.getQtySold() : 0,
                        r.getRevenue() != null ? r.getRevenue() : 0.0))
                .toList();

        // 3) Unpaid Invoices (recent 10)
        List<UnpaidInvoiceDto> unpaidInvoices = invoiceRepo.findInvoicesByStatusAsDtoAndBusinessId(
                InvoiceStatus.UNPAID,
                start, end, businessId,
                PageRequest.of(0, 10));

        return AnalyticsResponseDto.builder()
                .metrics(AnalyticsResponseDto.Metrics.builder()
                        .invoiceCount(invoiceCount)
                        .totalSales(totalSales)
                        .totalExpenses(totalExpenses)
                        .profit(profit)
                        .activeProductsCount(activeProducts)
                        .lowStockProductsCount(lowStockProducts)
                        .build())
                .topProducts(topProducts)
                .unpaidInvoices(unpaidInvoices)
                .build();
    }

    // ─────────────────────────────────────────────
    // AI REPORT — UPDATED PROMPTS
    // ─────────────────────────────────────────────
    @Override
    public AiReportResponseDto generateReport(Long businessId, LocalDate from, LocalDate to, String prompt) {

        String businessName = businessRepo.findNameById(businessId)
                .orElseThrow(() -> new AiException("Business not found: " + businessId));

        AnalyticsResponseDto analytics = getAnalytics(businessId, from, to);
        AnalyticsResponseDto.Metrics metrics = analytics.getMetrics();
        List<AnalyticsResponseDto.TopProductDto> topProductsList = analytics.getTopProducts();

        // Format top products — one per line for better GPT readability
        String topProductsText = topProductsList.isEmpty()
                ? "No sales data found for this period."
                : String.join("\n", topProductsList.stream()
                        .map(p -> String.format("  - %s: Qty Sold = %d, Revenue = %.2f",
                                p.getName(), p.getQtySold(), p.getRevenue()))
                        .toList());

        // Calculate profit margin
        double profitMargin = metrics.getTotalSales() > 0
                ? (metrics.getProfit() / metrics.getTotalSales() * 100)
                : 0.0;

        // ── SYSTEM PROMPT ──────────────────────────────────────────
        String systemPrompt = """
                You are SmartBiz AI, a professional business performance analyst.

                STRICT OUTPUT RULES:
                - Plain text only. No JSON. No markdown. No symbols like * or **.
                - Use a dash (-) for bullet points under Key Insights only.
                - Do NOT restate raw numbers verbatim — interpret and analyze them.
                - Write in a confident, business-friendly tone.
                - Total report must be between 130 and 200 words.

                Use EXACTLY these 4 section headings on their own line:

                Summary
                Write 2 sentences summarizing overall business performance for the period.

                Key Insights
                - Insight about invoice and sales volume
                - Insight about the top-selling product and its revenue share
                - Insight about expense efficiency and profit margin
                - Insight about product diversity or stock situation

                Risks
                Write 2 sentences identifying the biggest risks visible in the data.

                Recommendations
                Write 2 to 3 sentences with specific, actionable steps the business owner should take.
                """;

        // ── USER PROMPT ────────────────────────────────────────────
        String userPrompt = String.format("""
                Analyze the following business data and generate a professional report.

                Business: %s
                Period: %s to %s

                Financial Summary:
                - Total Invoices Issued: %d
                - Total Sales Revenue: %.2f
                - Total Expenses: %.2f
                - Net Profit: %.2f
                - Profit Margin: %.1f%%

                Top Selling Products:
                %s

                Generate the report now using the 4 sections: Summary, Key Insights, Risks, Recommendations.
                """,
                businessName,
                from, to,
                metrics.getInvoiceCount(),
                metrics.getTotalSales(),
                metrics.getTotalExpenses(),
                metrics.getProfit(),
                profitMargin,
                topProductsText);

        auditLogService.info("Generating professional AI business analysis for business: " + businessId);
        usageCounterService.incrementAiCount(businessId);

        String aiReport = callOpenAi(systemPrompt, userPrompt);

        return AiReportResponseDto.builder()
                .aiReport(aiReport)
                .build();
    }

    // ─────────────────────────────────────────────
    // MARKETING POST
    // ─────────────────────────────────────────────
    @Override
    public String generateMarketingPost(Long businessId, String topic) {
        String systemPrompt = "You are a social media copywriter. Write engaging, emoji-rich posts " +
                "for Facebook and Instagram. Keep it under 150 words.";

        String userPrompt = "Write a post about: " + sanitize(topic);

        auditLogService.info("Generating AI marketing post for business: " + businessId);
        usageCounterService.incrementAiCount(businessId);
        return callOpenAi(systemPrompt, userPrompt);
    }

    // ─────────────────────────────────────────────
    // EMAIL DRAFT
    // ─────────────────────────────────────────────
    @Override
    public String generateEmailDraft(Long businessId, String category, String tone, String context) {

        String systemPrompt = """
            You are SmartBiz AI, a professional business email writer for an ERP system.

            IMPORTANT RULES:
            - Output must be plain readable text.
            - Do NOT return JSON.
            - Do NOT use markdown.
            - Only return two sections exactly:

            Subject:
            Body:

            - Email must be professional and concise (80–150 words).
            - Always use placeholders instead of real data.

            Allowed placeholders:

            Customer:
            {{customer.name}}

            Invoice:
            {{invoice.number}}
            {{invoice.amount}}
            {{invoice.dueDate}}

            Promotion:
            {{promo.title}}
            {{promo.discount}}
            {{promo.validUntil}}

            Company:
            {{company.name}}

            Do NOT include company address, phone, or email because the system footer
            will automatically add company details.

            Tone must match the requested tone.
            """;

        String userPrompt = """
            Email Category: %s
            Tone: %s

            Context:
            %s

            Write a professional email draft based on this information.
            """.formatted(category, tone, sanitize(context));

        auditLogService.info("Generating AI email draft for business: " + businessId +
                " | category=" + category + " | tone=" + tone);

        usageCounterService.incrementAiCount(businessId);

        return callOpenAi(systemPrompt, userPrompt);
    }
}