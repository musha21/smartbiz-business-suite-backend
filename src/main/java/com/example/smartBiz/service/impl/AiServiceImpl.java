package com.example.smartBiz.service.impl;

import com.example.smartBiz.config.OpenAiConfig;
import com.example.smartBiz.dto.AiReportResponseDto;
import com.example.smartBiz.dto.AnalyticsResponseDto;
import com.example.smartBiz.dto.UnpaidInvoiceDto;
import com.example.smartBiz.enums.InvoiceStatus;
import com.example.smartBiz.exception.AiException;
import com.example.smartBiz.entity.BusinessProfile;
import com.example.smartBiz.repository.BusinessProfileRepo;
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
        private final BusinessProfileRepo businessProfileRepo;
        private final InvoiceItemRepo invoiceItemRepo;

        public AiServiceImpl(RestTemplate restTemplate,
                        InvoiceRepo invoiceRepo,
                        ProductRepo productRepo,
                        ExpenseRepo expenseRepo,
                        UsageCounterService usageCounterService,
                        AuditLogService auditLogService,
                        OpenAiConfig openAiConfig,
                        BusinessRepo businessRepo,
                        BusinessProfileRepo businessProfileRepo,
                        InvoiceItemRepo invoiceItemRepo) {
                this.restTemplate = restTemplate;
                this.invoiceRepo = invoiceRepo;
                this.productRepo = productRepo;
                this.expenseRepo = expenseRepo;
                this.usageCounterService = usageCounterService;
                this.auditLogService = auditLogService;
                this.openAiConfig = openAiConfig;
                this.businessRepo = businessRepo;
                this.businessProfileRepo = businessProfileRepo;
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

                BusinessProfile profile = businessProfileRepo.findByBusinessId(businessId).orElse(null);
                String industryContext = (profile != null && profile.getIndustry() != null)
                                ? "This business operates in the " + profile.getIndustry() + " industry."
                                : "";
                String countryContext = (profile != null && profile.getCountry() != null)
                                ? "The business is located in " + profile.getCountry() + "."
                                : "";
                String currencyContext = (profile != null && profile.getCurrency() != null)
                                ? "All financial values are in " + profile.getCurrency()
                                : "All values are in local currency";

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
        You are SmartBiz AI, the built-in business intelligence engine of SmartBiz ERP.

        Your job is to generate a clean, structured business performance report
        that feels like it came from a real ERP system — not a chatbot.

        STRICT OUTPUT RULES:
        - Plain text only. No JSON. No markdown. No symbols like * or **.
        - Use a dash (-) for bullet points only under Key Insights and Recommendations.
        - Do NOT start with "Here is your report" or any intro sentence.
        - Do NOT end with "Feel free to ask" or any closing line.
        - Write in clear, simple English. Avoid heavy financial jargon.
        - Every section must appear exactly once, in order.
        - Total report must be between 150 and 220 words.

        Use EXACTLY these 6 section headings on their own line, in this order:

        PERFORMANCE OVERVIEW
        Write 2 sentences giving a straight, honest summary of the business performance
        for this period. Mention revenue and profit direction (up/down/stable).

        KEY INSIGHTS
        - One insight about total invoices and sales volume
        - One insight about the best selling product and its contribution
        - One insight about expenses vs profit margin
        - One insight about stock levels or product availability

        FINANCIAL HEALTH
        Write 2 sentences assessing the overall financial position.
        Mention if the business is in a healthy, caution, or critical state.
        Use one of these labels clearly: [ HEALTHY ] [ CAUTION ] [ CRITICAL ]

        RISK FLAGS
        Write 2 sentences highlighting the top risks the owner must watch.
        Be direct and specific — no vague warnings.

        RECOMMENDATIONS
        - One quick action the owner can do this week
        - One medium term improvement for next month
        - One strategic suggestion for long term growth

        OUTLOOK
        Write 1 sentence predicting the short term business direction
        based on the current data trends. Be realistic.
        """;

                // ── USER PROMPT ────────────────────────────────────────────
                String userPrompt = String.format(
                                """
                                                Analyze the following business data and generate a professional report.

                                                Business: %s
                                                Period: %s to %s
                                                %s
                                                %s
                                                %s

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
                                industryContext,
                                countryContext,
                                currencyContext,
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
                BusinessProfile profile = businessProfileRepo.findByBusinessId(businessId).orElse(null);

                // ✅ All null-safe — no crash if field missing
                String businessName = (profile != null && profile.getBusinessName() != null)
                        ? profile.getBusinessName() : "Our Business";
                String industry     = (profile != null && profile.getIndustry() != null)
                        ? profile.getIndustry() : "retail";
                String country      = (profile != null && profile.getCountry() != null)
                        ? " based in " + profile.getCountry() : "";
                String tagline      = (profile != null && profile.getBrandTagline() != null)
                        ? "Tagline: \"" + profile.getBrandTagline() + "\"" : "";
                String brandColor   = (profile != null && profile.getBrandColor() != null)
                        ? "Brand Color: " + profile.getBrandColor() : "";

                // ✅ No .formatted() — using simple string concat, zero %s risk
                String systemPrompt = "You are a professional social media copywriter for "
                        + businessName + ", a " + industry + " business" + country + ".\n\n"
                        + "BRAND IDENTITY:\n"
                        + (tagline.isEmpty()    ? "" : tagline    + "\n")
                        + (brandColor.isEmpty() ? "" : brandColor + "\n")
                        + "\nSTRICT OUTPUT RULES:\n"
                        + "- Write ONE post only. No options, no variations.\n"
                        + "- Do NOT add labels like Caption: or Post:.\n"
                        + "- Do NOT use markdown or asterisks.\n"
                        + "- Use emojis naturally.\n"
                        + "- Maximum 150 words.\n"
                        + "- End with 3 to 5 relevant hashtags on the last line.\n"
                        + "- Write like a real person, not a robot.";

                String userPrompt = "Write a social media post for Facebook and Instagram about:\n"
                        + sanitize(topic);

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

                BusinessProfile profile = businessProfileRepo.findByBusinessId(businessId).orElse(null);
                String companyName = (profile != null && profile.getBusinessName() != null) ? profile.getBusinessName()
                                : "Our Company";
                String country = (profile != null && profile.getCountry() != null) ? profile.getCountry() : "Unknown";
                String industry = (profile != null && profile.getIndustry() != null) ? profile.getIndustry()
                                : "Business";
                String companyEmail = (profile != null && profile.getEmail() != null) ? profile.getEmail()
                                : "{{company.email}}";
                String companyPhone = (profile != null && profile.getPhone() != null) ? profile.getPhone()
                                : "{{company.phone}}";

                String userPrompt = """
        Email Category: %s
        Tone: %s
        Company Name: %s (Industry: %s, Location: %s)
        Contact Info: %s, %s

        Context:
        %s

        Write a professional email draft based on this information.
        """.formatted(
                        category,
                        tone,
                        companyName,
                        industry,        // ✅ was companyEmail — wrong!
                        country,         // ✅ was companyPhone — wrong!
                        companyEmail,    // ✅ Contact Info first
                        companyPhone,    // ✅ Contact Info second
                        sanitize(context) // ✅ 8th — Context
                );

                auditLogService.info("Generating AI email draft for business: " + businessId +
                                " | category=" + category + " | tone=" + tone);

                usageCounterService.incrementAiCount(businessId);

                return callOpenAi(systemPrompt, userPrompt);
        }
}