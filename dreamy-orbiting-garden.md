# Plan: Business Logic — Mono Studio E-Commerce Production Readiness

## Context

The Spring Boot backend lacks several core e-commerce business logic features. Without these, the system cannot handle real checkout flows, inventory control, promotions, or post-sale operations. This plan adds 12 feature modules across 4 tiers, sequenced by revenue impact and dependency order.

---

## Tier 1 — Revenue-Blocking (Must implement first)

### 1.1 ProductVariant Entity
**Why first:** Every other Tier 1 feature depends on variants.

**New files:**
- `jpa/entities/ProductVariant.java` — `id`, `product` (FK), `sku` (unique), `size`, `color`, `priceOverride` (nullable), `stockCurrent`, `stockReserved` (int, default 0)
- `jpa/repositories/ProductVariantsRepository.java`
- `jpa/repositories/ProductVariantStockReservationsRepository.java`
- `api/models/ProductVariantPojo.java`, `CartItemPojo.java`
- `jpa/services/crud/ProductVariantsCrudService.java` + `impl/`
- `jpa/services/conversion/ProductVariantsConverterService.java` + `impl/` — `effectivePrice = priceOverride ?? product.price`
- `jpa/services/patch/ProductVariantsPatchService.java` + `impl/`
- `jpa/services/predicates/ProductVariantsPredicateService.java` + `impl/`
- `api/controllers/DataProductVariantsController.java`

**Modifications:**
- `Product.java`: add `@OneToMany List<ProductVariant> variants` (LAZY, CascadeType.ALL, orphanRemoval)
- `OrderDetail.java`: add `@ManyToOne ProductVariant productVariant` (nullable) + FK column `product_variant_id`
- `ProductsConverterServiceImpl.java`: include variants list in `convertToPojo(Product)`

---

### 1.2 CartSession + CartItem Entities
**Why:** Backend must own cart state before checkout can convert it into an order.

**New files:**
- `jpa/entities/CartSession.java` — `id`, `sessionToken` (UUID, unique), `user` (ManyToOne, nullable), `customer` (ManyToOne, nullable), `expiresAt`, `ONE_TO_MANY CartItem`
- `jpa/entities/CartItem.java` — `id`, `cartSession`, `productVariant` (ManyToOne), `quantity`. Unique: `(cartSession, productVariant)`
- `jpa/repositories/CartSessionsRepository.java`, `CartItemsRepository.java`
- `api/models/CartSessionPojo.java`, `CartItemPojo.java`
- `api/services/CartService.java` + `impl/CartServiceImpl.java`
- `api/controllers/PublicCartController.java`

**PublicCartController endpoints:**
```
GET    /public/cart              → getOrCreateCart
POST   /public/cart/items        → addItem
PUT    /public/cart/items        → updateItem
DELETE /public/cart/items/{variantId} → removeItem
DELETE /public/cart              → clearCart
GET    /public/cart/summary      → itemCount + subtotal
```

**CartService key methods:** `getOrCreateCart`, `addItem` (calls `reserveStock`), `updateItem` (delta-based reserve/release), `removeItem` (releases stock), `clearCart`, `getCart`. Guest = `X-Session-Token` UUID header; logged-in = JWT + userId. Login merges guest cart into user cart.

**Modification:**
- `SecurityConfig.java`: add `.antMatchers("/public/cart/**").permitAll()`

---

### 1.3 Stock Reservation System
**Why:** Prevents overselling. Sits between cart and payment.

**New files:**
- `api/services/StockReservationService.java` + `impl/`

**`StockReservationServiceImpl` key methods:**
```java
int getAvailableStock(variantId);     // = stockCurrent - stockReserved
void reserveStock(variantId, qty);    // throws if insufficient; stockReserved += qty
void releaseStock(variantId, qty);   // stockReserved = max(0, stockReserved - qty)
void commitReservation(variantId, qty); // called on markAsPaid — stockCurrent -= qty, stockReserved -= qty
```
**Scheduled cleanup:** `CartExpirationCleanupServiceImpl` — `@Scheduled(fixedRate = 900_000)` (15 min), releases stock for expired `CartSession`.

---

### 1.4 DiscountCode Entity
**New files:**
- `jpa/entities/DiscountCode.java` — `id`, `code` (unique), `type` (PERCENT | FIXED_AMOUNT), `value` (int), `minOrderValue`, `maxUsages`, `currentUsages`, `startsAt`, `expiresAt`, `active`
- `jpa/repositories/DiscountCodesRepository.java`
- `api/models/DiscountCodePojo.java`, `DiscountValidationResultPojo.java`
- CRUD + converter + patch + predicate services (4-service pattern)
- `api/services/DiscountService.java` + `impl/`
- `api/controllers/DataDiscountCodesController.java` (admin), `PublicDiscountController.java`

**PublicDiscountController:**
```
POST /public/discount/validate   → validate code + cart subtotal, return discount value
```

**Modifications:**
- `Order.java`: add `discountCode` (String), `discountValue` (int)
- `OrderPojo.java`: add same fields
- `OrdersConverterServiceImpl.java`: modify `updateTotals()` — `netValue = subtotal - discount + taxes`

---

### 1.5 ShippingMethod Entity + Checkout Integration
**New files:**
- `jpa/entities/ShippingMethod.java` — `id`, `name`, `baseFee`, `freeShippingThreshold` (nullable), `estimatedDaysMin`, `estimatedDaysMax`, `active`
- CRUD + converter + patch + predicate services (4-service pattern)
- `api/models/ShippingMethodPojo.java`, `ShippingRatePojo.java`
- `api/controllers/DataShippingMethodsController.java`, `PublicShippingMethodsController.java`

**PublicShippingMethodsController:**
```
GET /public/shipping/methods?subtotal={vnd} → returns fee (0 if free threshold met)
```

**Modifications:**
- `Order.java`: replace `@ManyToOne Shipper shipper` with `@ManyToOne ShippingMethod shippingMethod`
- `CheckoutServiceImpl.java`: inject `ShippingMethodsCrudService` + `DiscountService`; refactor `requestTransactionStart()`:
  1. Validate cart variant stocks
  2. Reserve stock per item
  3. Compute shipping fee (0 if free threshold met)
  4. Validate discount code if provided
  5. Build Order with correct `transportValue` = shipping fee

**New checkout endpoint:**
```
POST /public/checkout/start   → validates cart, reserves stock, creates order in PENDING, returns payment URL
```

---

### 1.6 Stock Deduction on Payment
**Code modification only — no new entities.**

**Modifications:**
- `OrdersProcessServiceImpl.java`: inject `StockReservationService`; modify `markAsPaid()`:
  ```java
  for each OrderDetail:
    variantId = detail.productVariant?.id
    if variantId != null → stockReservationService.commitReservation(variantId, detail.units)
    else → productsRepository.decrementStock(detail.product.id, detail.units)
  ```
- `OrdersProcessServiceImpl.java`: modify `markAsAborted()`, `markAsFailed()` — call `releaseStock(variantId, units)`
- `ProductsRepository.java`: add `@Modifying @Query` `decrementStock(id, qty)` — returns 0 if insufficient

---

## Tier 2 — Conversion Boosters

### 2.1 AddressBook
**New files:**
- `jpa/entities/UserAddress.java` — `id`, `user` (ManyToOne), `address` (ManyToOne), `label`, `isDefault`, `createdAt`
- `jpa/repositories/UserAddressesRepository.java`
- `api/models/UserAddressPojo.java`
- `api/services/AddressBookService.java` + `impl/`
- `api/controllers/AccountAddressBookController.java`

**Endpoints:** `GET/POST/PUT/DELETE /account/addresses`, `PUT /account/addresses/{id}/default`

**Modification:** `CheckoutServiceImpl.java` — accept `shippingAddressId` (Long) to look up `UserAddress` at checkout time.

---

### 2.2 ProductReview
**New files:**
- `jpa/entities/ProductReview.java` — `id`, `product` (ManyToOne), `customer` (ManyToOne), `rating` (1–5), `reviewText`, `approved` (default false), timestamps. Unique: `(product, customer)`
- 4-service CRUD pattern
- `api/models/ProductReviewPojo.java`
- `api/controllers/PublicProductReviewsController.java`, `DataProductReviewsController.java`

**Endpoints:**
```
GET  /public/products/{productId}/reviews        → approved reviews + avg rating
POST /public/products/{productId}/reviews        → submit (auth required, one per customer)
GET  /admin/product-reviews                      → all (including unapproved)
PUT  /admin/product-reviews/{id}/approve         → set approved=true
DELETE /admin/product-reviews/{id}               → admin delete
```

**Modifications:**
- `Product.java`: add `@OneToMany List<ProductReview> reviews`
- `ProductPojo.java`: add `averageRating` (Double), `reviewCount` (int)
- `ProductsConverterServiceImpl.java`: populate avg + count from approved reviews

---

## Tier 3 — Operations

### 3.1 ReturnRequest + Refund
**New files:**
- `jpa/entities/ReturnRequest.java` — `id`, `order`, `customer`, `reason`, `type` (FULL|PARTIAL), `status` (PENDING|APPROVED|REJECTED|REFUND_INITIATED|REFUND_COMPLETED), `adminNotes`, timestamps
- `jpa/entities/ReturnRequestItem.java` — links to `orderDetail`, variantId, return quantity
- 4-service CRUD + predicate services
- `api/models/ReturnRequestPojo.java`, `ReturnRequestItemPojo.java`
- `api/services/ReturnRequestService.java` + `impl/`
- `api/controllers/AccountReturnRequestsController.java`, `AdminReturnRequestsController.java`

**ReturnRequestService.approve() logic:**
1. Set `status = APPROVED`
2. For each item: `stockReservationService.releaseStock(variantId, qty)` — restores stock
3. Send email to customer

**Endpoints:**
```
POST /account/orders/{orderId}/return-request
GET  /account/return-requests
GET  /admin/return-requests
PUT  /admin/return-requests/{id}/approve
PUT  /admin/return-requests/{id}/reject
PUT  /admin/return-requests/{id}/refund-complete
```

---

### 3.2 Email Notifications
**Extends existing `MailgunMailingServiceImpl`.**

**New interface methods on `MailingService`:**
```java
void notifyReturnRequestStatusToClient(ReturnRequestPojo request);
void notifyReturnRequestToOwners(ReturnRequestPojo request);
void notifyLowStockAlert(String productName, int currentStock);
```

**Trigger points:**
| Event | Location | Method |
|---|---|---|
| Order created | `OrdersCrudServiceImpl.create()` | `notifyOrderStatusToClient` (Pending) |
| Payment started | `OrdersProcessServiceImpl.markAsStarted()` | `notifyOrderStatusToClient` |
| Payment aborted | `markAsAborted()` | `notifyOrderStatusToClient` |
| Payment failed | `markAsFailed()` | `notifyOrderStatusToClient` |
| Payment confirmed | `PublicCheckoutController.validateSuccessfulTransaction()` | `notifyOrderStatusToClient` |
| Order rejected | `markAsRejected()` | `notifyOrderStatusToClient` + `notifyOrderStatusToOwners` |
| Order completed | `markAsCompleted()` | `notifyOrderStatusToClient` |
| Return requested | `ReturnRequestServiceImpl.create()` | `notifyReturnRequestToOwners` |
| Return approved | `approveReturnRequest()` | `notifyReturnRequestStatusToClient` |
| Refund complete | `markRefundComplete()` | `notifyReturnRequestStatusToClient` |
| Low stock | `StockReservationServiceImpl.reserveStock()` | `notifyLowStockAlert` |

---

### 3.3 StockAdjustment Audit Log
**New files:**
- `jpa/entities/StockAdjustment.java` — `id`, `type` (SALE|RETURN|MANUAL|DAMAGED|EXPIRED|INITIAL_STOCK), `reason`, `adjustedBy` (User), `notes`, `createdAt`
- `jpa/entities/StockAdjustmentItem.java` — `id`, `stockAdjustment`, `variant` (nullable), `product` (nullable), `quantityBefore`, `quantityChange`, `quantityAfter`
- 4-service CRUD + predicate services
- `api/models/StockAdjustmentPojo.java`, `StockAdjustmentItemPojo.java`
- `api/services/StockAdjustmentService.java` + `impl/`
- `api/controllers/DataStockAdjustmentsController.java`

**Auto-creation triggers:** `StockReservationServiceImpl.commitReservation()` → type=SALE; `ReturnRequestServiceImpl.approveReturnRequest()` → type=RETURN.

**Endpoints:**
```
GET  /admin/stock-adjustments
POST /admin/stock-adjustments  (manual adjustment)
GET  /admin/stock-adjustments/{id}
GET  /admin/stock-adjustments/variant/{variantId}
```

---

## Tier 4 — Analytics

### 4.1 Admin Dashboard Stats
**New files:**
- `api/models/AdminDashboardStatsPojo.java` — container DTO
- `api/models/RevenueStatPojo.java`, `TopProductPojo.java`, `LowStockAlertPojo.java`, `OrderStatusCountPojo.java`
- `api/services/AdminDashboardService.java` + `impl/`
- `api/controllers/AdminDashboardController.java`

**`AdminDashboardServiceImpl` queries:**
- Revenue + order count: `OrdersRepository.countByStatusAndDateBetween()`
- Top products by units: custom JPQL grouping `order_details` + `orders` by product
- Low stock alerts: `ProductVariantsRepository.findByStockCurrentLessThanEqual(threshold)`
- Order status breakdown: `OrdersRepository.countByStatus()`
- Revenue by day: JPQL `SELECT DATE(order_date), SUM(total_value) ... GROUP BY DATE`

**Endpoints:**
```
GET /admin/dashboard/stats?from=&to=
GET /admin/dashboard/stats/revenue?from=&to=&groupBy=day|week|month
GET /admin/dashboard/stats/top-products?from=&to=&limit=10
GET /admin/dashboard/stats/low-stock
GET /admin/dashboard/stats/order-statuses
```

---

## Dependency Order (Critical Path)

```
Tier 1:
│
├─ 1.1 ProductVariant          ← all SKU/stock features start here
│   ├─ 1.3 StockReservation    ← needs ProductVariant.stockReserved
│   │   └─ 1.6 StockDeduction   ← needs commitReservation()
│   └─ 1.2 CartSession          ← CartItem → productVariant FK
│
├─ 1.4 DiscountCode             ← needs Order.discountCode field
│
└─ 1.5 ShippingMethod           ← needs Order.shippingMethod FK + CheckoutService refactor
    └─ 1.6 StockDeduction also wired here

Tier 2: AddressBook + ProductReview (independent of each other)
Tier 3: ReturnRequest → triggers Email + StockAdjustment
Tier 4: AdminDashboardStats (queries all tables above)
```

---

## Critical Files to Modify

| File | Changes |
|---|---|
| `src/main/java/org/monostudio/jpa/entities/Product.java` | + variants field |
| `src/main/java/org/monostudio/jpa/entities/OrderDetail.java` | + productVariant FK |
| `src/main/java/org/monostudio/jpa/entities/Order.java` | + discountCode, discountValue; replace shipper with shippingMethod |
| `src/main/java/org/monostudio/jpa/entities/ProductPojo.java` | + averageRating, reviewCount |
| `src/main/java/org/monostudio/api/services/impl/OrdersProcessServiceImpl.java` | + StockReservationService injection; modify markAsPaid/Aborted/Failed |
| `src/main/java/org/monostudio/api/services/impl/CheckoutServiceImpl.java` | Full refactor: validate cart, reserve stock, compute shipping, validate discount |
| `src/main/java/org/monostudio/jpa/services/conversion/impl/OrdersConverterServiceImpl.java` | + discount application in updateTotals() |
| `src/main/java/org/monostudio/jpa/services/conversion/impl/ProductsConverterServiceImpl.java` | + variants + review stats |
| `src/main/java/org/monostudio/jpa/repositories/ProductsRepository.java` | + decrementStock() |
| `src/main/java/org/monostudio/config/SecurityConfig.java` | + /public/cart/** permitAll |

---

## Naming Convention Compliance

All new files follow existing 4-service pattern:
- Entity: `Xxx.java`
- Repository: `XxxRepository.java`
- Pojo: `XxxPojo.java`
- CRUD: `XxxCrudService.java` + `XxxCrudServiceImpl.java`
- Converter: `XxxConverterService.java` + `XxxConverterServiceImpl.java`
- Patch: `XxxPatchService.java` + `XxxPatchServiceImpl.java`
- Predicate: `XxxPredicateService.java` + `XxxPredicateServiceImpl.java`
- Controller: `DataXxxController.java` (admin) or `PublicXxxController.java` (public)

All entities implement `DBEntity`, use `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Getter`, `@Setter`, `@CreationTimestamp`.

---

## Verification

1. **Backend compiles:** `mvn clean compile` in `graduation-ecommerce-backend/`
2. **Cart flow:** POST `/public/cart/items` → GET `/public/cart` → verify stockReserved incremented
3. **Checkout:** POST `/public/checkout/start` with session token + shipping method → verify order created, stock reserved
4. **Payment confirmation:** Trigger `markAsPaid()` → verify stockCurrent decremented, stockReserved decremented
5. **Discount code:** POST `/public/discount/validate` → apply in checkout → verify `discountValue` on Order
6. **Free shipping:** GET `/public/shipping/methods?subtotal=500000` (above threshold) → verify fee=0
7. **Email trigger:** Check logs or Mailgun dashboard for email sent on order creation
8. **Return flow:** Create return request → approve → verify stock restored
9. **Admin stats:** GET `/admin/dashboard/stats?from=2026-01-01&to=2026-04-03` → verify revenue, orders, top products, low stock
10. **Swagger docs:** Visit `http://localhost:8080/swagger-ui.html` — all new endpoints should appear
