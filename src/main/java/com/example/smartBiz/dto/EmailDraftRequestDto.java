package com.example.smartBiz.dto;// EmailDraftRequestDto.java
import lombok.Data;
import java.util.List;

@Data
public class EmailDraftRequestDto {
    private String category;     // INVOICE_REMINDER or PROMO_OFFER
    private String tone;         // FRIENDLY / SECOND / URGENT / DISCOUNT / NEW_PRODUCT / SEASONAL_SALE
    private String context;      // optional extra notes

    // Targets (one of these will be used depending on category)
    private List<Long> invoiceIds;   // for INVOICE_REMINDER
    private List<Long> customerIds;  // for PROMO_OFFER

    // Promo object (only for PROMO_OFFER)
    private PromoDto promo;

    @Data
    public static class PromoDto {
        private String title;
        private String discount;
        private String validUntil;
        private String ctaUrl;
    }
}