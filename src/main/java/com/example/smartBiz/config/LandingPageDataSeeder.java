package com.example.smartBiz.config;

import com.example.smartBiz.entity.*;
import com.example.smartBiz.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LandingPageDataSeeder implements CommandLineRunner {

    private final FAQRepo faqRepo;
    private final LandingStatRepo landingStatRepo;
    private final HeroContentRepo heroContentRepo;
    private final TrustIntegrationRepo trustIntegrationRepo;

    @Override
    @Transactional
    public void run(String... args) {
        seedFAQs();
        seedLandingStats();
        seedHeroContent();
        seedTrustIntegrations();
    }

    private void seedFAQs() {
        if (faqRepo.count() == 0) {
            List<FAQ> faqs = List.of(
                FAQ.builder()
                    .question("Is there a free trial available?")
                    .answer("Yes! We offer a 7-day full-featured free trial. You get access to all premium features including unlimited invoices, customers, and products. No credit card required to start.")
                    .category("Billing")
                    .displayOrder(1)
                    .active(true)
                    .build(),
                FAQ.builder()
                    .question("How secure is my business data?")
                    .answer("We take security seriously. All data is encrypted with AES-256 encryption, stored in secure cloud servers with daily backups. We are SOC 2 compliant and never share your data with third parties.")
                    .category("Security")
                    .displayOrder(2)
                    .active(true)
                    .build(),
                FAQ.builder()
                    .question("Can I import data from my existing system?")
                    .answer("Absolutely! We support CSV imports for customers, products, and suppliers. Our team can also help with custom data migration from popular systems like Excel, QuickBooks, or other POS software.")
                    .category("Usage")
                    .displayOrder(3)
                    .active(true)
                    .build(),
                FAQ.builder()
                    .question("What payment methods do you accept?")
                    .answer("We accept all major credit cards, PayPal, and local payment methods. For Sri Lankan customers, we also support PayHere for secure local payments.")
                    .category("Billing")
                    .displayOrder(4)
                    .active(true)
                    .build(),
                FAQ.builder()
                    .question("How do I get support if I need help?")
                    .answer("We offer 24/7 email support and live chat during business hours. Pro and Enterprise plans also get priority phone support and dedicated account managers.")
                    .category("Support")
                    .displayOrder(5)
                    .active(true)
                    .build(),
                FAQ.builder()
                    .question("Can I use SmartBiz on mobile devices?")
                    .answer("Yes! SmartBiz is fully responsive and works on all devices. We also have native mobile apps for iOS and Android coming soon for even better mobile experience.")
                    .category("Usage")
                    .displayOrder(6)
                    .active(true)
                    .build()
            );
            faqRepo.saveAll(faqs);
            log.info("Seeded {} default FAQs", faqs.size());
        }
    }

    private void seedLandingStats() {
        if (landingStatRepo.count() == 0) {
            List<LandingStat> stats = List.of(
                LandingStat.builder()
                    .label("Active Businesses")
                    .value("2,000+")
                    .icon("Users")
                    .displayOrder(1)
                    .active(true)
                    .build(),
                LandingStat.builder()
                    .label("Uptime Guaranteed")
                    .value("99.9%")
                    .icon("Zap")
                    .displayOrder(2)
                    .active(true)
                    .build(),
                LandingStat.builder()
                    .label("Revenue Processed")
                    .value("$50M+")
                    .icon("TrendingUp")
                    .displayOrder(3)
                    .active(true)
                    .build(),
                LandingStat.builder()
                    .label("Security Certified")
                    .value("SOC 2")
                    .icon("Shield")
                    .displayOrder(4)
                    .active(true)
                    .build()
            );
            landingStatRepo.saveAll(stats);
            log.info("Seeded {} default landing stats", stats.size());
        }
    }

    private void seedHeroContent() {
        if (heroContentRepo.count() == 0) {
            HeroContent hero = HeroContent.builder()
                .badgeText("Your Smart POS Software Solution")
                .headline("The Future of Business Management")
                .subheadline("Everything you need to run your store smoothly, in one smart platform. Streamline operations, boost sales, and delight customers with our AI-powered business suite.")
                .ctaText("Get Free Demo")
                .build();
            heroContentRepo.save(hero);
            log.info("Seeded default hero content");
        }
    }

    private void seedTrustIntegrations() {
        if (trustIntegrationRepo.count() == 0) {
            List<TrustIntegration> integrations = List.of(
                TrustIntegration.builder()
                    .label("Zapier")
                    .icon("Zap")
                    .displayOrder(1)
                    .active(true)
                    .build(),
                TrustIntegration.builder()
                    .label("Stripe")
                    .icon("Shield")
                    .displayOrder(2)
                    .active(true)
                    .build(),
                TrustIntegration.builder()
                    .label("AWS")
                    .icon("Cloud")
                    .displayOrder(3)
                    .active(true)
                    .build(),
                TrustIntegration.builder()
                    .label("Shopify")
                    .icon("Box")
                    .displayOrder(4)
                    .active(true)
                    .build(),
                TrustIntegration.builder()
                    .label("PayPal")
                    .icon("CreditCard")
                    .displayOrder(5)
                    .active(true)
                    .build(),
                TrustIntegration.builder()
                    .label("Analytics")
                    .icon("BarChart3")
                    .displayOrder(6)
                    .active(true)
                    .build()
            );
            trustIntegrationRepo.saveAll(integrations);
            log.info("Seeded {} default trust integrations", integrations.size());
        }
    }
}
