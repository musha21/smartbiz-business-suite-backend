package com.example.smartBiz.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiDocConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Bean
    public OpenAPI smartBizOpenAPI() {
        final String bearerAuth = "bearerAuth";

        return new OpenAPI()
                // ── Server list ──
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local dev"),
                        new Server().url("https://api.smartbiz.app").description("Production")
                ))

                // ── Global security (JWT) ──
                .addSecurityItem(new SecurityRequirement().addList(bearerAuth))
                .components(new Components()
                        .addSecuritySchemes(bearerAuth, new SecurityScheme()
                                .name(bearerAuth)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT access token here (without 'Bearer ' prefix)")))

                // ── API metadata ──
                .info(new Info()
                        .title("SmartBiz Business Suite API")
                        .version(appVersion)
                        .description("""
                                **SmartBiz** is a multi-tenant business management platform.\s
                                It provides invoicing, inventory, customer management,\s
                                expense tracking, AI-powered analytics, subscription billing,\s
                                and a full admin panel.

                                ### Authentication
                                - `POST /v1/api/auth/register` — register a new business owner
                                - `POST /v1/api/auth/login` — obtain access + refresh tokens
                                - `POST /v1/api/auth/refresh` — rotate refresh token (cookie-based)
                                - All other endpoints require a valid **Bearer JWT** in the `Authorization` header.

                                ### Rate Limits
                                | Endpoint | Limit |
                                |----------|-------|
                                | Login | 5 requests / minute / IP |
                                | AI endpoints | 20 requests / hour / user |
                                """)
                        .contact(new Contact()
                                .name("SmartBiz Team")
                                .email("support@smartbiz.app")
                                .url("https://smartbiz.app"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))

                // ── Ordered tag list (controls sidebar order in Swagger UI) ──
                .tags(List.of(
                        new Tag().name("Auth").description("Registration, login, token refresh & logout"),
                        new Tag().name("Dashboard").description("Business owner dashboard summary"),
                        new Tag().name("Invoices").description("Create, update, list, archive & restore invoices"),
                        new Tag().name("Invoice PDF").description("Download & preview invoice PDFs"),
                        new Tag().name("Products").description("CRUD + archive/restore for products"),
                        new Tag().name("Product Batches").description("Stock batch management for products"),
                        new Tag().name("Categories").description("Product category management"),
                        new Tag().name("Customers").description("CRUD + archive/restore for customers"),
                        new Tag().name("Suppliers").description("CRUD + archive/restore for suppliers"),
                        new Tag().name("Expenses").description("CRUD for business expenses"),
                        new Tag().name("Business Profile").description("Get/create/update business profile"),
                        new Tag().name("AI").description("AI-powered reports, marketing posts & email drafts"),
                        new Tag().name("AI Image").description("AI image generation"),
                        new Tag().name("Reports").description("Analytics reports with date range"),
                        new Tag().name("Owner Plan").description("Owner's current plan & usage counters"),
                        new Tag().name("Owner Subscription").description("Owner's subscription details"),
                        new Tag().name("Public Plans").description("Publicly visible active plans"),
                        new Tag().name("Payments").description("PayHere payment gateway integration for subscription billing"),
                        new Tag().name("Admin - Dashboard").description("Admin stats overview, logs & expiring subscriptions"),
                        new Tag().name("Admin - Analytics").description("Platform-wide analytics for admins"),
                        new Tag().name("Admin - Plans").description("CRUD for subscription plans (admin only)"),
                        new Tag().name("Admin - Plan Limits").description("Manage limits per plan (admin only)"),
                        new Tag().name("Admin - Subscriptions").description("Assign plans to businesses (admin only)")
                ));
    }
}
