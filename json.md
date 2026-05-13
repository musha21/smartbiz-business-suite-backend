# SmartBiz API Endpoints & DTOs — New/Updated Reference

> **Last updated:** May 13, 2026 (Landing Page Admin Controls added)
> **Scope:** PayHere integration, payment history, subscription cancellation, **Partner Logo Management (Base64 uploads)**, Invoice PDF dynamic branding, **Landing Page Admin Controls (FAQ, Stats, Hero, Integrations)**

---

## Table of Contents
- [New API Endpoints](#new-api-endpoints)
  - [Payments](#payments--payhere)
  - [Subscriptions](#subscriptions--cancel)
- [New DTOs](#new-dtos)
- [Updated Controllers (Swagger)](#updated-controllers--swagger)
- [Updated Entities](#updated-entities)
- [Updated Enums](#updated-enums)
- [Partner Logos](#partner-logos--crud--public-apis)
- [Landing Page Admin Controls](#landing-page-admin-controls)

---

## New API Endpoints

### Payments — PayHere

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/v1/api/payments/checkout` | JWT (OWNER) | Initiate PayHere checkout — returns form data + hash |
| `POST` | `/v1/api/payments/notify` | **Public** | PayHere server-to-server callback (MD5 verified) |
| `GET`  | `/v1/api/payments/status/{orderId}` | JWT | Get payment order status |
| `GET`  | `/v1/api/payments/history` | JWT | Get payment history for authenticated business |

#### POST /v1/api/payments/checkout

**Request body (CheckoutRequestDto):**
```json
{
  "planId": 2,
  "billingCycle": "MONTHLY"
}
```

**Response body (CheckoutResponseDto):**
```json
{
  "merchantId": "121XXXX",
  "orderId": "SB-1-A3B7C9D2",
  "itemsDescription": "Pro Plan - MONTHLY Subscription",
  "currency": "LKR",
  "amountFormatted": "2500.00",
  "firstName": "John Doe",
  "lastName": "",
  "email": "john@example.com",
  "phone": "",
  "address": "",
  "city": "",
  "country": "Sri Lanka",
  "notifyUrl": "http://localhost:8080/v1/api/payments/notify",
  "returnUrl": "http://localhost:3000/payment/success?order_id=SB-1-A3B7C9D2",
  "cancelUrl": "http://localhost:3000/payment/cancel",
  "hash": "A1B2C3D4...",
  "checkoutUrl": "https://sandbox.payhere.lk/pay/checkout"
}
```

> **Frontend usage:** Build an auto-submitting HTML form with these fields and POST to `checkoutUrl`.

> **Note:** Checkout only creates a `PaymentOrder` (status=PENDING). No `Subscription` is created at this stage. The subscription is created only when PayHere confirms payment success via the notify callback. This makes checkout faster and avoids orphaned pending subscriptions.

#### POST /v1/api/payments/notify

**Form params (x-www-form-urlencoded from PayHere):**

| Param | Example | Description |
|-------|---------|-------------|
| `merchant_id` | `121XXXX` | Your PayHere merchant ID |
| `order_id` | `SB-1-A3B7C9D2` | Order ID from checkout |
| `payhere_amount` | `2500.00` | Paid amount |
| `payhere_currency` | `LKR` | Currency |
| `status_code` | `2` | `2`=Success, `0`=Pending, `-1`=Canceled, `-2`=Failed |
| `md5sig` | `A1B2C3D4...` | MD5 signature for verification |
| `payment_id` | `3209XXXX...` | PayHere payment ID |
| `method` | `VISA` | Payment method used |

**Response:** Always returns `200 OK` with body `"OK"` (PayHere expects this).

#### GET /v1/api/payments/status/{orderId}

**Response:**
```json
{
  "orderId": "SB-1-A3B7C9D2",
  "status": "COMPLETED"
}
```

#### GET /v1/api/payments/history

**Response body (`List<PaymentHistoryDto>`):**
```json
[
  {
    "orderId": "SB-1-A3B7C9D2",
    "planName": "Pro Plan",
    "amount": 2500.0,
    "currency": "LKR",
    "status": "COMPLETED",
    "paidAt": "2026-05-07T14:30:00",
    "billingCycle": "MONTHLY"
  },
  {
    "orderId": "SB-1-F8E2D1C0",
    "planName": "Starter Plan",
    "amount": 1000.0,
    "currency": "LKR",
    "status": "CANCELED",
    "paidAt": null,
    "billingCycle": "YEARLY"
  }
]
```

> **Frontend usage:** Called by `BillingHistory.jsx` to render the payment history table.

### Invoice PDF — Print Endpoint (NEW)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/v1/api/invoices/{id}/print` | JWT | Print-optimized PDF with page numbers and enhanced discount display |

#### GET /v1/api/invoices/{id}/print

Generates a **print-optimized PDF** with the following enhancements:

**Features:**
- **Page Numbers**: Footer shows `"BusinessName | Page X"` on every page
- **Enhanced Item Table**: 8 columns showing full discount breakdown:
  - No, Product, Qty, Unit Price, **Disc %, Disc Amt, Final Price**, Line Total
- **Discount Summary Box**: Displays Original Subtotal → Total Discounts → calculation
- **Professional Footer**: "Thank you for your business!"

**Use Case:** Physical printing, professional invoices for customers.

**Response:** PDF file with `Content-Disposition: attachment` (filename: `invoice-{id}-print.pdf`)

### Subscriptions — Cancel

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET`  | `/v1/api/subscriptions/my` | JWT (OWNER) | Get current subscription (uses JWT, no query params) |
| `POST` | `/v1/api/subscriptions/cancel` | JWT (OWNER) | Cancel own subscription (grace period until `endAt`) |
| `POST` | `/v1/api/admin/subscriptions/cancel/{businessId}` | JWT (ADMIN) | Admin cancel — `immediate` flag controls grace period |

#### POST /v1/api/subscriptions/cancel

**No request body required.** Uses JWT to identify the business.

**Response body (CancelSubscriptionResponseDto):**
```json
{
  "status": "CANCELING",
  "message": "Subscription will end on 2026-06-07T14:30:00. You retain full access until then.",
  "canceledAt": "2026-05-07T21:00:00",
  "accessUntil": "2026-06-07T14:30:00"
}
```

> **Grace period:** Subscription stays `ACTIVE` until `endAt`. The `canceledAt` field records when the user requested cancellation. Frontend can show "Canceling" badge when `canceling == true` in `MySubscriptionDto`.

> **Free plans** return `400 Bad Request` — free subscriptions cannot be canceled.

#### POST /v1/api/admin/subscriptions/cancel/{businessId}?immediate=true

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `immediate` | `boolean` | `true` | `true` = cancel now, `false` = end-of-period |

**Response body (CancelSubscriptionResponseDto):**
```json
{
  "status": "CANCELED",
  "message": "Subscription canceled immediately by admin.",
  "canceledAt": "2026-05-07T21:00:00",
  "accessUntil": null
}
```

---

## New DTOs

### CheckoutRequestDto
**Package:** `com.example.smartBiz.dto`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `planId` | `Long` | `@NotNull` | Plan to purchase |
| `billingCycle` | `String` | `@NotBlank` | `MONTHLY` or `YEARLY` |

### CheckoutResponseDto
**Package:** `com.example.smartBiz.dto`

| Field | Type | Description |
|-------|------|-------------|
| `merchantId` | `String` | PayHere merchant ID |
| `orderId` | `String` | Unique order ID |
| `itemsDescription` | `String` | Human-readable item description |
| `currency` | `String` | e.g. `LKR` |
| `amountFormatted` | `String` | Amount with 2 decimals |
| `firstName` | `String` | Customer first name |
| `lastName` | `String` | Customer last name |
| `email` | `String` | Customer email |
| `phone` | `String` | Customer phone |
| `address` | `String` | Billing address |
| `city` | `String` | City |
| `country` | `String` | Country |
| `notifyUrl` | `String` | Backend notify URL |
| `returnUrl` | `String` | Success redirect URL |
| `cancelUrl` | `String` | Cancel redirect URL |
| `hash` | `String` | PayHere MD5 hash |
| `checkoutUrl` | `String` | PayHere checkout page URL |

> **Note:** `returnUrl` now includes `?order_id={orderId}` so the frontend's `PaymentSuccess.jsx` can read it from the URL to poll status.

### PaymentHistoryDto
**Package:** `com.example.smartBiz.dto`

| Field | Type | Description |
|-------|------|-----------|
| `orderId` | `String` | Unique payment order reference |
| `planName` | `String` | Name of the purchased plan |
| `amount` | `Double` | Amount charged |
| `currency` | `String` | e.g. `LKR` |
| `status` | `String` | `PENDING`, `COMPLETED`, `CANCELED`, `FAILED` |
| `paidAt` | `LocalDateTime` | When payment completed (null if not paid) |
| `billingCycle` | `String` | `MONTHLY` or `YEARLY` |

### CancelSubscriptionResponseDto
**Package:** `com.example.smartBiz.dto`

| Field | Type | Description |
|-------|------|-------------|
| `status` | `String` | `CANCELING` (grace period) or `CANCELED` (immediate) |
| `message` | `String` | Human-readable explanation |
| `canceledAt` | `LocalDateTime` | When the cancel was requested |
| `accessUntil` | `LocalDateTime` | When access ends (`null` if immediate) |

---

## New Entities

### PaymentOrder
**Package:** `com.example.smartBiz.entity`
**Table:** `payment_orders`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK, auto | Internal ID |
| `orderId` | `String` | Unique, not null | PayHere order reference |
| `business` | `Business` | FK, not null | Purchaser |
| `plan` | `Plan` | FK, not null | Plan being purchased |
| `billingCycle` | `String` | Not null | `MONTHLY` / `YEARLY` |
| `amount` | `Double` | Not null | Price paid |
| `currency` | `String` | Not null | e.g. `LKR` |
| `status` | `String` | Not null | `PENDING`, `COMPLETED`, `CANCELED`, `FAILED`, `HASH_FAILED` |
| `payherePaymentId` | `String` | | PayHere payment ID |
| `payhereStatusCode` | `Integer` | | Raw PayHere status code |
| `payhereMd5sig` | `String` | | Received MD5 signature |
| `paidAt` | `LocalDateTime` | | When payment completed |
| `createdAt` | `LocalDateTime` | | Record creation |
| `updatedAt` | `LocalDateTime` | | Record update |

---

## Updated Controllers (Swagger)

All 22 controllers now have `@Tag` + `@Operation` + `@ApiResponse` annotations for Swagger UI.

| Controller | Tag | Endpoints |
|------------|-----|-----------|
| `AuthController` | Auth | `register`, `login`, `me`, `refresh`, `logout` |
| `DashboardController` | Dashboard | `summary` |
| `InvoiceController` | Invoices | `create`, `getById`, `getByNumber`, `list`, `archived`, `byCustomer`, `update`, `updateStatus`, `archive`, `restore` |
| `InvoicePdfController` | Invoice PDF | `download/{id}`, `preview/{id}`, **`print/{id}`** |
| `ProductsController` | Products | `create`, `update`, `delete`, `getById`, `list`, `archive`, `restore`, `archived` |
| `ProductBatchController` | Product Batches | `addStock`, `byProduct`, `getById`, `delete` |
| `CategoryController` | Categories | `create`, `list` |
| `CustomerController` | Customers | `create`, `update`, `archived`, `archive`, `restore`, `getById`, `list` |
| `SupplierController` | Suppliers | `create`, `update`, `archived`, `archive`, `restore`, `getById`, `list` |
| `ExpenseController` | Expenses | `create`, `update`, `delete`, `getById`, `list` |
| `BusinessProfileController` | Business Profile | `get`, `create`, `update` |
| `AiController` | AI | `report`, `marketing/post`, `email/draft` |
| `AiImageController` | AI Image | `generate-image` |
| `ReportController` | Reports | `analytics` |
| `OwnerPlanController` | Owner Plan | `my-plan`, `usage` |
| `OwnerSubscriptionController` | Owner Subscription | `my`, **`cancel`** |
| `PublicPlanController` | Public Plans | `active` |
| `AdminController` | Admin - Dashboard | `stats/overview`, `logs`, `subscriptions/expiring`, `businesses`, `users`, `businesses/{id}/disable`, `businesses/{id}/enable` |
| `AdminAnalyticsController` | Admin - Analytics | `plan-analytics`, `weekly-activity`, `stats`, `ai-analytics` |
| `AdminPlanController` | Admin - Plans | `create`, `update/{id}`, `list`, `status/{id}` |
| `AdminPlanLimitController` | Admin - Plan Limits | `upsert`, `get` |
| `AdminSubscriptionController` | Admin - Subscriptions | `assign`, **`cancel/{businessId}`** |
| **PaymentController** | **Payments** | **`checkout`**, **`notify`**, **`status/{orderId}`**, **`history`** |
| **`AdminFAQController`** | **Admin - FAQs** | **`GET/POST/PUT/DELETE/PATCH`** |
| **`PublicFAQController`** | **Public - FAQs** | **`GET`** |
| **`AdminLandingStatController`** | **Admin - Stats** | **`GET/POST/PUT/DELETE/PATCH`** |
| **`PublicLandingStatController`** | **Public - Stats** | **`GET`** |
| **`AdminHeroContentController`** | **Admin - Hero** | **`GET/PUT`** |
| **`PublicHeroContentController`** | **Public - Hero** | **`GET`** |
| **`AdminTrustIntegrationController`** | **Admin - Integrations** | **`GET/POST/PUT/DELETE/PATCH/reorder`** |
| **`PublicTrustIntegrationController`** | **Public - Integrations** | **`GET`** |

---

## Updated Entities

### Subscription (new fields)

| Field | Type | Description |
|-------|------|-------------|
| `payhereOrderId` | `String` | PayHere order reference |
| `payherePaymentId` | `String` | PayHere payment ID |
| `canceledAt` | `LocalDateTime` | When cancellation was requested (`null` = not canceled) |

### MySubscriptionDto (new fields)

| Field | Type | Description |
|-------|------|-------------|
| `canceledAt` | `LocalDateTime` | When cancel was requested (`null` = not canceled) |
| `canceling` | `boolean` | `true` = cancel requested but still has access (grace period) |

---

## Updated Enums

### SubscriptionStatus

```java
public enum SubscriptionStatus {
    ACTIVE,
    PENDING_PAYMENT,   // ← NEW: subscription created, awaiting PayHere payment
    EXPIRED,
    CANCELED
}
```

---

## New Services

| Service | Method | Description |
|---------|--------|-------------|
| `PayHereService` | `initiateCheckout(businessId, userId, request)` | Creates payment order + PENDING_PAYMENT subscription, returns checkout data |
| `PayHereService` | `handleNotify(params)` | Verifies MD5 hash, activates subscription on successful payment |
| `PayHereService` | `getOrderStatus(orderId)` | Returns order status string |
| `PayHereService` | `getPaymentHistory(businessId)` | Returns all payment orders for a business, newest first |
| `SubscriptionService` | `cancelSubscription(businessId)` | Owner cancel — grace period until `endAt` |
| `SubscriptionService` | `adminCancelSubscription(businessId, immediate)` | Admin cancel — immediate or end-of-period |
| `AuthServiceImpl` | `assignFreePlan(business)` | Auto-assigns FREE plan on registration (private helper) |

---

## Security Changes

| File | Change |
|------|--------|
| `SecurityConfig.java` | Whitelisted `/v1/api/payments/notify` (no JWT required) |
| `SecurityConfig.java` | Whitelisted `/v1/api/public/**` (no JWT required for landing page content) |
| `JwtFilter.java` | Skips `/v1/api/payments/notify` path |
| `OwnerSubscriptionController.java` | **Fixed:** `GET /my` now uses JWT principal instead of `businessId` query param (was a security hole) |

---

## Configuration Template

**File:** `src/main/resources/payhere-config.properties.template`

```properties
payhere.merchant-id=YOUR_SANDBOX_MERCHANT_ID
payhere.merchant-secret=YOUR_SANDBOX_MERCHANT_SECRET
payhere.sandbox=true
payhere.currency=LKR
payhere.notify-url=http://localhost:8080/v1/api/payments/notify
payhere.return-url=http://localhost:3000/payment/success
payhere.cancel-url=http://localhost:3000/payment/cancel
```

> Copy these into `application.properties` and replace with your PayHere sandbox credentials from https://sandbox.payhere.lk/

---

## Swagger UI

**URL:** http://localhost:8080/swagger-ui.html

All endpoints are documented with:
- `@Tag` — grouped sidebar sections
- `@Operation` — summary + description per endpoint
- `@ApiResponse` — status codes and descriptions
- `@Parameter` — path/query param documentation

---

## Free Tier — 7-Day Free Trial on Registration

**Behavior:** When a new user registers (`POST /v1/api/auth/register`), the system automatically assigns a **7-day free trial** of the FREE plan.

**How it works:**
1. `AuthServiceImpl.register()` creates the Business and User
2. Calls `assignFreePlan(business)` which:
   - Looks up the plan with `code = "FREE"` via `PlanRepo.findByCode("FREE")`
   - Creates an `ACTIVE` subscription with `endAt = now + 7 days`
   - Syncs the Business entity (`planId`, `plan`, `subscriptionStart`, `subscriptionEnd`)
3. If no FREE plan exists in the DB → registration still succeeds, but business has no plan (logged as warning)

**After 7 days:**
- `refreshExpiryIfNeeded()` automatically marks the subscription as `EXPIRED`
- Business `planId` is cleared → user has NO plan
- User must upgrade to a paid plan via PayHere checkout
- No auto-downgrade — expired = blocked

**Trial duration constant:** `FREE_TRIAL_DAYS = 7` in `AuthServiceImpl`

**Prerequisite:** A plan must exist in the DB with `code = "FREE"`, `monthlyPrice = 0.0`, `yearlyPrice = 0.0`.

> **Important:** The plan `code` field must be exactly `"FREE"` (uppercase). Admin-assigned free plans also get a 7-day trial.

**Free trial characteristics:**
- `endAt = now + 7 days` → expires after 7 days
- Cannot be canceled (`POST /subscriptions/cancel` returns 400)
- Cannot be checked out via PayHere (blocked in `PayHereServiceImpl`)
- Plan limits (invoices, customers, products, AI credits) are controlled by the `plan_limits` table

---

## May 8 Updates — Invoice & PDF Enhancements

### Dynamic Branding in PDF
The `InvoicePdfService` now automatically fetches the latest **Business Profile** details for the business associated with the invoice.

| Feature | Description |
|---------|-------------|
| **Dynamic Header** | Shows `businessName` and `ownerName` (labels updated). |
| **Contact Info** | Displays business `address` and `phone` in the sender section. |
| **Brand Color** | Table headers use the `brandColor` from the profile (e.g., `#007bff`). |
| **Logo Support** | Decodes Base64 logo from profile; falls back to static asset if missing. |
| **Discounts** | Line items show: Unit Price → Discount % → Discount Amt → Final Price → Line Total. |
| **Discount Summary Box** | Displays Original Subtotal and Total Discounts before Grand Total. |
| **Page Numbers** | Print-optimized PDF includes page footer: "BusinessName \| Page X". |

### Updated Invoice DTOs

#### InvoiceResponseDto (Updated)
**Package:** `com.example.smartBiz.dto`

| Field | Type | Description |
|-------|------|-------------|
| `customer` | `CustomerDto` | Full details of the customer |
| `businessProfile` | `BusinessProfileDto` | **NEW:** Full details of the business (for automatic fetching) |
| `items` | `List<InvoiceItemResponseDto>` | List of purchased items |
| `totalDiscount` | `Double` | Sum of all line item savings |

#### InvoiceItemResponseDto (Updated)
**Package:** `com.example.smartBiz.dto`

| Field | Type | Description |
|-------|------|-------------|
| `discountPercentage` | `Double` | Percentage discount applied |
| `discountAmount` | `Double` | Flat discount amount applied |
| `lineTotal` | `Double` | Final price for the line (after discounts) |

### Improved Invoice Update Logic
`PUT /v1/api/invoices/{id}` now performs a **Full Stock & Item Sync**:
1. Reverses stock for all old items in the invoice.
2. Clears previous items.
3. Processes new items from request, deducting from product batches.
4. Recalculates `totalAmount` and `totalDiscount`.
5. Syncs `Products.stock_qty` across all batches.

---

## May 13 Updates — Partner Logo Management

### Partner Logos — CRUD & Public APIs

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/v1/api/admin/partner-logos` | JWT (ADMIN) | Create a new partner logo (Base64) |
| `PUT` | `/v1/api/admin/partner-logos/{id}` | JWT (ADMIN) | Update an existing partner logo |
| `DELETE` | `/v1/api/admin/partner-logos/{id}` | JWT (ADMIN) | Delete a partner logo |
| `GET` | `/v1/api/admin/partner-logos` | JWT (ADMIN) | List all partner logos (for admin dashboard) |
| `GET` | `/v1/api/public/partner-logos/active` | **Public** | List all active logos for landing page (ordered) |

### PartnerLogoDto
**Package:** `com.example.smartBiz.dto`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | | Primary key (read-only) |
| `companyName` | `String` | Required | Name of the partner company |
| `logo` | `String` | Required (Base64) | PNG/JPG image encoded as Base64 string (maps to `logo_url` column) |
| `displayOrder` | `Integer` | Default: `0` | Order of appearance on the landing page |
| `active` | `Boolean` | Default: `true` | Whether the logo is visible to the public |

> **Note:** The `logo` field must start with either `data:image/png;base64,` or `data:image/jpeg;base64,`. Max size depends on server `max-http-header-size` and `max-swallow-size`, but typically supports up to 5MB.
>
> **Database Mapping:** The `logo` field in the entity maps to the `logo_url` column in the `partner_logos` table.

---

## Landing Page Admin Controls

Complete backend APIs for managing landing page content: FAQ, Stats, Hero Content, and Trust Integrations.

**Base URLs:**
- Admin: `/v1/api/admin/**` (requires JWT + ADMIN role)
- Public: `/v1/api/public/**` (open access)

### FAQ Management

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/v1/api/admin/faqs` | JWT (ADMIN) | List all FAQs (admin view) |
| `POST` | `/v1/api/admin/faqs` | JWT (ADMIN) | Create FAQ |
| `PUT` | `/v1/api/admin/faqs/{id}` | JWT (ADMIN) | Update FAQ |
| `DELETE` | `/v1/api/admin/faqs/{id}` | JWT (ADMIN) | Delete FAQ |
| `PATCH` | `/v1/api/admin/faqs/{id}/toggle` | JWT (ADMIN) | Toggle active status |
| `GET` | `/v1/api/public/faqs` | **Public** | List active FAQs only |

**Categories:** `Billing`, `Security`, `Usage`, `Support`, `General`

**Default Data (6 FAQs seeded on startup):**
- Is there a free trial available? (Billing)
- How secure is my business data? (Security)
- Can I import data from my existing system? (Usage)
- What payment methods do you accept? (Billing)
- How do I get support if I need help? (Support)
- Can I use SmartBiz on mobile devices? (Usage)

### Landing Stats

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/v1/api/admin/stats` | JWT (ADMIN) | List all stats (admin view) |
| `POST` | `/v1/api/admin/stats` | JWT (ADMIN) | Create stat |
| `PUT` | `/v1/api/admin/stats/{id}` | JWT (ADMIN) | Update stat |
| `DELETE` | `/v1/api/admin/stats/{id}` | JWT (ADMIN) | Delete stat |
| `PATCH` | `/v1/api/admin/stats/{id}/toggle` | JWT (ADMIN) | Toggle visibility |
| `GET` | `/v1/api/public/stats` | **Public** | List active stats only |

**Icon Options:** `Users`, `Zap`, `TrendingUp`, `Shield`, `Box`, `CreditCard`, `BarChart3`, `Cloud`

**Default Data (4 stats seeded on startup):**
- Active Businesses: 2,000+ (Users)
- Uptime Guaranteed: 99.9% (Zap)
- Revenue Processed: $50M+ (TrendingUp)
- Security Certified: SOC 2 (Shield)

### Hero Content (Single Record)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/v1/api/admin/hero` | JWT (ADMIN) | Get current hero content |
| `PUT` | `/v1/api/admin/hero` | JWT (ADMIN) | Update hero content (create if not exists) |
| `GET` | `/v1/api/public/hero` | **Public** | Get public hero content |

**Fields:**
- `badgeText`: Small badge above headline (e.g., "Your Smart POS Software Solution")
- `headline`: Main headline (e.g., "The Future of Business Management")
- `subheadline`: Longer description text
- `ctaText`: Button text (e.g., "Get Free Demo")

**Default Data (seeded on startup):**
```json
{
  "badgeText": "Your Smart POS Software Solution",
  "headline": "The Future of Business Management",
  "subheadline": "Everything you need to run your store smoothly, in one smart platform. Streamline operations, boost sales, and delight customers with our AI-powered business suite.",
  "ctaText": "Get Free Demo"
}
```

### Trust Integrations

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/v1/api/admin/integrations` | JWT (ADMIN) | List all integrations (admin view) |
| `POST` | `/v1/api/admin/integrations` | JWT (ADMIN) | Create integration |
| `PUT` | `/v1/api/admin/integrations/{id}` | JWT (ADMIN) | Update integration |
| `DELETE` | `/v1/api/admin/integrations/{id}` | JWT (ADMIN) | Delete integration |
| `PATCH` | `/v1/api/admin/integrations/{id}/toggle` | JWT (ADMIN) | Toggle visibility |
| `PUT` | `/v1/api/admin/integrations/reorder` | JWT (ADMIN) | Bulk reorder (accepts list of `{id, displayOrder}`) |
| `GET` | `/v1/api/public/integrations` | **Public** | List active integrations only |

**Default Data (6 integrations seeded on startup):**
- Zapier (Zap)
- Stripe (Shield)
- AWS (Cloud)
- Shopify (Box)
- PayPal (CreditCard)
- Analytics (BarChart3)

### Landing Page DTOs

#### FAQDto
| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Primary key |
| `question` | `String` | FAQ question (max 500 chars) |
| `answer` | `String` | FAQ answer (text) |
| `category` | `String` | Billing, Security, Usage, Support, General |
| `displayOrder` | `Integer` | Sort order |
| `active` | `Boolean` | Visible to public |
| `createdAt` | `LocalDateTime` | Auto-set |
| `updatedAt` | `LocalDateTime` | Auto-update |

#### LandingStatDto
| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Primary key |
| `label` | `String` | Stat label (e.g., "Active Businesses") |
| `value` | `String` | Stat value (e.g., "2,000+") |
| `icon` | `String` | Lucide icon name |
| `displayOrder` | `Integer` | Sort order |
| `active` | `Boolean` | Visible to public |
| `createdAt` | `LocalDateTime` | Auto-set |
| `updatedAt` | `LocalDateTime` | Auto-update |

#### HeroContentDto
| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Primary key (single record) |
| `badgeText` | `String` | Badge above headline |
| `headline` | `String` | Main headline |
| `subheadline` | `String` | Description text |
| `ctaText` | `String` | CTA button text |
| `updatedAt` | `LocalDateTime` | Auto-update |

#### TrustIntegrationDto
| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Primary key |
| `label` | `String` | Integration name |
| `icon` | `String` | Lucide icon name |
| `displayOrder` | `Integer` | Sort order |
| `active` | `Boolean` | Visible to public |
| `createdAt` | `LocalDateTime` | Auto-set |
| `updatedAt` | `LocalDateTime` | Auto-update |

#### ReorderDto (for bulk reorder)
| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Integration ID |
| `displayOrder` | `Integer` | New sort order |

### New Controllers

| Controller | Tag | Endpoints |
|------------|-----|-----------|
| `AdminFAQController` | Admin - FAQs | `GET/POST/PUT/DELETE/PATCH` |
| `PublicFAQController` | Public - FAQs | `GET` |
| `AdminLandingStatController` | Admin - Stats | `GET/POST/PUT/DELETE/PATCH` |
| `PublicLandingStatController` | Public - Stats | `GET` |
| `AdminHeroContentController` | Admin - Hero | `GET/PUT` |
| `PublicHeroContentController` | Public - Hero | `GET` |
| `AdminTrustIntegrationController` | Admin - Integrations | `GET/POST/PUT/DELETE/PATCH/reorder` |
| `PublicTrustIntegrationController` | Public - Integrations | `GET` |

### Database Tables

**faqs** — FAQ management
```sql
CREATE TABLE faqs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(50),
    display_order INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**landing_stats** — Landing page statistics
```sql
CREATE TABLE landing_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    label VARCHAR(100) NOT NULL,
    value VARCHAR(50) NOT NULL,
    icon VARCHAR(50) DEFAULT 'TrendingUp',
    display_order INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**hero_content** — Single hero section record
```sql
CREATE TABLE hero_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    badge_text VARCHAR(200),
    headline VARCHAR(300),
    subheadline TEXT,
    cta_text VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**trust_integrations** — Trust bar integrations
```sql
CREATE TABLE trust_integrations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    label VARCHAR(100) NOT NULL,
    icon VARCHAR(50) DEFAULT 'Zap',
    display_order INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Testing Commands

```bash
# Public endpoints (no auth)
curl http://localhost:8080/v1/api/public/faqs
curl http://localhost:8080/v1/api/public/stats
curl http://localhost:8080/v1/api/public/hero
curl http://localhost:8080/v1/api/public/integrations
curl http://localhost:8080/v1/api/public/partner-logos

# Admin endpoints (with JWT)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/v1/api/admin/faqs
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"question":"Test","answer":"Answer","category":"General"}' \
  http://localhost:8080/v1/api/admin/faqs
```

