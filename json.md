# SmartBiz API Endpoints & DTOs — New/Updated Reference

> **Last updated:** May 8, 2026
> **Scope:** PayHere integration, payment history, subscription cancellation, **Invoice PDF dynamic branding, line/total discounts, and full invoice update logic**

---

## Table of Contents
- [New API Endpoints](#new-api-endpoints)
  - [Payments](#payments--payhere)
  - [Subscriptions](#subscriptions--cancel)
- [New DTOs](#new-dtos)
- [Updated Controllers (Swagger)](#updated-controllers--swagger)
- [Updated Entities](#updated-entities)
- [Updated Enums](#updated-enums)

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
| `InvoicePdfController` | Invoice PDF | `download/{id}`, `preview/{id}` |
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
| **Discounts** | Added "Discount" column to items table and total savings breakdown. |

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

