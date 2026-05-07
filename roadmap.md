# SmartBiz Business Suite — Improvement Roadmap

> **Current State:** Spring Boot 4.0.2 backend with JWT auth, role-based access (ADMIN / OWNER), JPA/MySQL, OpenAI integration, PDF generation, subscription billing, rate limiting, and Swagger docs.
> **Tech Stack:** Java 17, Spring Boot 4.0.2, Spring Security, Spring Data JPA, MySQL, OpenPDF, Bucket4j, Caffeine, JJWT, OpenAI API.

---

## Phase 1 — Stability & Foundation (Week 1–2)

| Priority | Task | Motivation |
|----------|------|------------|
| **P0** | Fix all build/compiler warnings & errors | `build_err.txt` / `build_top_errors.txt` indicate active issues |
| **P0** | Add unit tests for `AuthService`, `JwtUtil`, `JwtFilter` | Currently only default `SmartBizApplicationTests` exists |
| **P0** | Add integration tests for `AuthController` | `@WebMvcTest` + `MockMvc` for login, register, refresh, logout |
| **P1** | Replace hardcoded admin credentials in `AdminSeeder` | Security risk — load from environment or encrypted config |
| **P1** | Standardize endpoint security | Some use `@PreAuthorize`, others manual `CustomUserPrincipal.getCurrent()` — pick one pattern |
| **P1** | Introduce Flyway or Liquibase for DB migrations | `migration_plan_status.sql` exists but no migration runner configured |
| **P1** | Externalize all hardcoded config values | Cookie `secure=false`, rate-limit capacities (5, 20), AI credit defaults should be in `application.yml` |

**Files to touch:**
- `AdminSeeder.java`, `SecurityConfig.java`, `RateLimitFilter.java`, `AuthController.java`, `AiController.java`
- Add `src/test/java/.../service/*Test.java`, `controller/*Test.java`

---

## Phase 2 — Architecture & Quality (Week 3–4)

| Priority | Task | Motivation |
|----------|------|------------|
| **P1** | Implement full audit logging | `AuditLogService` exists but appears skeletal; log every CREATE / UPDATE / DELETE with user + timestamp |
| **P1** | Add request/response logging filter | Debuggability — especially for AI endpoint failures |
| **P1** | Introduce DTO-to-Entity MapStruct mappers | Reduce boilerplate conversion code in services |
| **P1** | Add Bean Validation on *all* incoming DTOs | Currently only `AuthController` uses `@Valid`; expand to `InvoiceCreateRequestDto`, `CustomerDto`, etc. |
| **P1** | Add pagination & sorting to all list endpoints | `InvoiceListService`, `CustomerService`, `ProductService` likely return unbounded lists |
| **P2** | Add global `@Transactional` boundaries in services | Prevents partial writes on multi-step operations |
| **P2** | Add `@SQLDelete` / `@SQLRestriction` soft-delete pattern to remaining entities | Already on `Invoice`; add to `Customer`, `Product`, `Supplier` |
| **P2** | Add database indexes via JPA for hot query paths | `DashboardService`, analytics, and invoice lookups |

**Files to touch:**
- `service/impl/*.java`, `dto/*.java`, all `*Controller.java`, `entity/*.java`

---

## Phase 3 — Security & Compliance (Week 5–6)

| Priority | Task | Motivation |
|----------|------|------------|
| **P0** | Encrypt sensitive DB columns (`email`, `phone`) | Business/customer PII compliance |
| **P0** | Store `openai.api.key` in external secrets manager | Currently plain `@Value` — rotate key immediately |
| **P1** | Add refresh token rotation & revocation store | Current refresh tokens are stateless and cannot be revoked on logout |
| **P1** | Add `X-Request-Id` trace ID to all responses | Distributed tracing & debugging |
| **P1** | Implement IP allowlist / blocklist for admin endpoints | Admin endpoints (`/v1/api/admin/**`) should have extra geo/IP checks |
| **P1** | Add OWASP ZAP or basic security headers | `X-Content-Type-Options`, `X-Frame-Options`, CSP via `SecurityConfig` |
| **P2** | Add method-level `@PreAuthorize` to *all* admin endpoints | Some admin controllers may be missing explicit authorization |
| **P2** | Add brute-force lockout for repeated bad passwords | Extend existing rate limit to temporarily lock accounts |

**Files to touch:**
- `SecurityConfig.java`, `JwtFilter.java`, `RateLimitFilter.java`, `AuthService.java`, `OpenAiConfig.java`

---

## Phase 4 — Performance & Scalability (Week 7–8)

| Priority | Task | Motivation |
|----------|------|------------|
| **P1** | Introduce Spring Cache (Caffeine/Redis) for dashboard & analytics | Dashboard calls aggregate heavy SQL — cache for 5–15 min |
| **P1** | Add connection pooling config (HikariCP) | MySQL connections likely on defaults |
| **P1** | Optimize N+1 queries in `Invoice` + `InvoiceItem` | Use `@EntityGraph` or `JOIN FETCH` in repositories |
| **P1** | Add async processing for AI & PDF generation | `@Async` + `CompletableFuture` to prevent blocking HTTP threads |
| **P2** | Add read-replica routing for analytics/report queries | Offload heavy `AdminAnalyticsController` reads |
| **P2** | Compress PDF output & add caching layer for generated PDFs | Repeated invoice PDF generation is expensive |
| **P2** | Implement API response compression (`Gzip`) | Reduce payload size for large invoice/product lists |

**Files to touch:**
- `InvoiceService.java`, `AdminAnalyticsService.java`, `AiService.java`, `InvoicePdfService.java`, `application.yml`

---

## Phase 5 — DevOps & Observability (Week 9–10)

| Priority | Task | Motivation |
|----------|------|------------|
| **P1** | Add Spring Boot Actuator + health endpoints | `/actuator/health`, `/actuator/info`, `/actuator/metrics` |
| **P1** | Add Micrometer + Prometheus metrics | Track AI usage, invoice creation rate, auth failures, DB connection pool |
| **P1** | Add structured logging (JSON) with trace/span IDs | ELK / Loki ingestion ready |
| **P1** | Add `Dockerfile` + `docker-compose.yml` (App + MySQL + Redis) | Containerization for local dev & deployment |
| **P2** | Add GitHub Actions CI pipeline | Build, test, SonarQube scan, Docker push |
| **P2** | Add API versioning strategy document | Currently `/v1/api/` — plan for `/v2/api/` breaking changes |
| **P2** | Add environment-specific configs | `application-dev.yml`, `application-staging.yml`, `application-prod.yml` |
| **P2** | Add graceful shutdown + lifecycle hooks | Ensure in-flight AI/PDF jobs complete before termination |

**New files:**
- `Dockerfile`, `docker-compose.yml`, `.github/workflows/ci.yml`, `src/main/resources/application-{env}.yml`

---

## Phase 6 — Feature Expansion (Week 11–12)

| Priority | Task | Motivation |
|----------|------|------------|
| **P2** | Add notification system (email/SMS) | Invoice due reminders, subscription expiry alerts |
| **P2** | Add multi-currency support | Store `currency` on `Business` + `Invoice`; exchange rate integration |
| **P2** | Add inventory low-stock alerts | `ProductBatch` should trigger notifications when `quantity < threshold` |
| **P2** | Add recurring invoices | Auto-generate monthly/quarterly invoices for subscriptions |
| **P2** | Add two-factor authentication (TOTP) | Security enhancement for owner/admin accounts |
| **P2** | Add file attachment support (S3/minio) | Attach receipts to expenses, logos to invoices |
| **P3** | Add data export (CSV/Excel) for all entities | GDPR / backup / migration utility |
| **P3** | Add webhooks for invoice status changes | Allow external integrations (accounting tools) |

---

## Quick Wins (Do This Week)

1. **Move `secure=false` in `AuthController` to env-based config** — production must be `true`.
2. **Add `@Valid` to all `@RequestBody` controller methods** — prevents garbage data.
3. **Delete `build_err.txt`, `build_errors_notif.txt`, `build_output.txt`, `build_top_errors.txt`, `hs_err_pid*.log`** from repo — these are build artifacts and JVM crash logs, not source code.
4. **Add `.gitignore` entries** for build logs, crash dumps, and IDE files.
5. **Add a `README.md` update** with setup instructions, env variables list, and API base URL.

---

## Appendix — Entity Inventory

| Entity | Soft Delete | Audit Log | Notes |
|--------|-------------|-----------|-------|
| `AppUser` | ❌ | ❌ | Needs audit |
| `Business` | ❌ | ❌ | Needs audit |
| `Customer` | ❌ | ❌ | Needs soft delete + PII encryption |
| `Invoice` | ✅ `@SQLDelete` | Partial | Good pattern — replicate to others |
| `Products` | ❌ | ❌ | Needs stock alerts |
| `Subscription` | ❌ | ✅ (implied) | Critical for billing |
| `Expense` | ❌ | ❌ | Needs receipt attachments |
| `Supplier` | ❌ | ❌ | Needs audit |
| `AiUsageLog` | N/A | ✅ | Already tracking — good |
| `ActivityLog` | N/A | ✅ | Already tracking — good |

---

## Appendix — Controller Inventory (Security Audit Needed)

| Controller | Auth Pattern | `@PreAuthorize`? | Notes |
|------------|-------------|------------------|-------|
| `AuthController` | Public | N/A | OK |
| `DashboardController` | JWT | ❌ | Add `@PreAuthorize("hasAnyRole('OWNER','ADMIN')")` |
| `AiController` | Mixed | Partial | Standardize to annotations |
| `Admin*Controller` | JWT | Some | Audit all for `@PreAuthorize` |
| `InvoiceController` | JWT | ❌ | Add ownership checks |
| `ProductsController` | JWT | ❌ | Add ownership checks |
| `PublicPlanController` | Public | N/A | OK |

---

*Last updated: April 2026*
