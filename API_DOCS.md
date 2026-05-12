# Mono Studio E-Commerce — Backend API Documentation

> **Base URL:** `http://localhost:8080/api`
> **Content-Type:** `application/json`
> **Authentication:** JWT Bearer token in `Authorization` header
> **Cart Session:** `X-Session-Token` header (UUID, optional — auto-created if absent)

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Account & Profile](#2-account--profile)
3. [My Orders (Customer)](#3-my-orders-customer)
4. [Public Product Data](#4-public-product-data)
5. [Reviews (Public)](#5-reviews-public)
6. [Cart](#6-cart)
7. [Stock Reservations](#7-stock-reservations)
8. [Checkout & Payment](#8-checkout--payment)
9. [Receipt](#9-receipt)
10. [Discount Codes (Public)](#10-discount-codes-public)
11. [Shipping Methods (Public)](#11-shipping-methods-public)
12. [Address Book](#12-address-book)
13. [Admin — Dashboard](#13-admin--dashboard)
14. [Admin — Products](#14-admin--products)
15. [Admin — Product Categories](#15-admin--product-categories)
16. [Admin — Product Variants](#16-admin--product-variants)
17. [Admin — Product Lists](#17-admin--product-lists)
18. [Admin — Product Reviews](#18-admin--product-reviews)
19. [Admin — Images](#19-admin--images)
20. [Admin — Billing Types](#20-admin--billing-types)
21. [Admin — Order Statuses](#21-admin--order-statuses)
22. [Admin — Shipping Methods](#22-admin--shipping-methods)
23. [Admin — Shippers](#23-admin--shippers)
24. [Admin — Orders](#24-admin--orders)
25. [Admin — Return Requests](#25-admin--return-requests)
26. [Admin — Discount Codes](#26-admin--discount-codes)
27. [Admin — Cart Sessions](#27-admin--cart-sessions)
28. [Admin — Users & Roles](#28-admin--users--roles)
29. [Admin — People & Customers](#29-admin--people--customers)
30. [Admin — Salespeople](#30-admin--salespeople)
31. [Error Reference](#31-error-reference)
32. [Pagination & Filtering](#32-pagination--filtering)

---

## 1. Authentication

All authenticated endpoints require: `Authorization: Bearer <jwt_token>`

### `POST /public/auth/login` — Login

**Security:** permitAll (no auth required)

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | ✅ | Username |
| `password` | string | ✅ | Password |

```json
// Request
{ "name": "johndoe", "password": "Secret123!" }

// Response 200 — JWT token set as Bearer in response header
// Body: (empty)
```

**Errors:**
- `AUTH_01` (401) — invalid credentials

---

### `POST /public/auth/register` — Register Account

**Security:** permitAll

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | ✅ | Username (3–50 chars, unique) |
| `password` | string | ✅ | Password (min 8, upper+lower+digit+special) |
| `profile.firstName` | string | ✅ | First name |
| `profile.lastName` | string | ✅ | Last name |
| `profile.email` | string | ✅ | Valid email address |
| `profile.phone1` | string | ❌ | Primary phone |
| `profile.phone2` | string | ❌ | Secondary phone |
| `profile.idNumber` | string | ❌ | ID number |

```json
// Request
{
  "name": "johndoe",
  "password": "Secret123!",
  "profile": {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com"
  }
}

// Response 201 — (empty body)
```

**Errors:**
- `EXISTS_01` (400) — username already taken
- `REJECTED_02` (400) — validation failure

---

### `POST /public/guest` — Guest Session

**Security:** permitAll

Creates a guest JWT token. No request body required.

```json
// Response 200 — JWT token set as Bearer
```

---

## 2. Account & Profile

### `GET /account/profile` — View Profile

**Security:** authenticated

```json
// Response 200
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "phone1": "0912345678",
  "phone2": null,
  "idNumber": null
}
```

---

### `PUT /account/profile` — Update Profile

**Security:** authenticated

```json
// Request — full PersonPojo
{
  "firstName": "John",
  "lastName": "Smith",
  "email": "john.smith@example.com",
  "phone1": "0912345678",
  "phone2": "0999999999",
  "idNumber": "001234567890"
}

// Response 204 — (empty body)
```

---

### `GET /access` — List Authorized Routes

**Security:** authenticated

```json
// Response 200
{
  "routes": ["api/account/profile", "api/data/orders", "api/admin/dashboard/stats"]
}
```

---

### `GET /access/{apiRoute}` — Check Permissions for Route

**Security:** authenticated

**Path Params:**
- `apiRoute` — route prefix to check (e.g. `orders`)

```json
// Response 200
{
  "permissions": ["orders:read", "orders:update"]
}
```

---

## 3. My Orders (Customer)

### `GET /account/orders` — List My Orders

**Security:** authenticated

Returns paginated list of all orders belonging to the authenticated customer, sorted by date descending.

**Query Params:**
| Param | Type | Default | Description |
|---|---|---|---|
| `pageIndex` | int | 0 | Zero-based page index |
| `pageSize` | int | 20 | Items per page |

```json
// Response 200 — DataPagePojo<OrderPojo>
{
  "items": [
    {
      "buyOrder": 12345,
      "date": "2026-04-18T14:30:00",
      "totalValue": 672800,
      "totalItems": 2,
      "status": "Delivery Complete",
      "paymentType": "Webpay Plus",
      "shippingAddress": { "city": "Ho Chi Minh" }
    }
  ],
  "totalCount": 5,
  "pageSize": 20
}
```

**Errors:**
- `NOTFOUND_01` (404) — no orders found for this account

---

### `GET /account/orders/{buyOrder}` — Get Order Detail

**Security:** authenticated

Returns full order details. Customers can only view their own orders.

**Path Params:** `buyOrder` — the order ID

```json
// Response 200 — full OrderPojo
{
  "buyOrder": 12345,
  "date": "2026-04-18T14:30:00",
  "details": [
    {
      "productName": "Basic T-Shirt",
      "variant": "Red / S",
      "quantity": 2,
      "unitPrice": 299000,
      "lineTotal": 598000
    }
  ],
  "netValue": 598000,
  "taxValue": 59800,
  "transportValue": 15000,
  "totalValue": 672800,
  "totalItems": 2,
  "discountCode": "SUMMER20",
  "discountValue": 11960,
  "status": "Delivery Complete",
  "billingType": "individual",
  "paymentType": "Webpay Plus",
  "customer": { "firstName": "John", "lastName": "Doe" },
  "shippingAddress": { "recipientName": "John Doe", "city": "Ho Chi Minh" }
}
```

**Errors:**
- `NOTFOUND_01` (404) — order not found or does not belong to the customer

---

## 4. Public Product Data

### `GET /public/products/{barcode}` — Get Product by Barcode

**Security:** permitAll

```json
// Response 200
{
  "name": "Basic T-Shirt",
  "barcode": "123456789",
  "description": "Premium cotton t-shirt",
  "price": 299000,
  "currentStock": 50,
  "criticalStock": 10,
  "category": { ... },
  "images": [{ "url": "...", "alt": "..." }],
  "averageRating": 4.5,
  "totalReviews": 23,
  "variants": [
    {
      "sku": "BT-RED-S",
      "size": "S",
      "color": "Red",
      "price": 299000,
      "stock": 15
    }
  ]
}
```

**Errors:**
- `NOTFOUND_01` (404) — product not found

---

### `GET /public/products` — Search / List Products

**Security:** permitAll

**Query Params:**
| Param | Type | Default | Description |
|---|---|---|---|
| `q` | string | — | Search query (name/description) |
| `category` | string | — | Filter by category code |
| `minPrice` | number | — | Minimum price |
| `maxPrice` | number | — | Maximum price |
| `pageIndex` | int | 0 | Page index |
| `pageSize` | int | 20 | Items per page |
| `sortBy` | string | — | Sort field |
| `order` | string | `asc` | `asc` or `desc` |

```json
// Response 200
{
  "items": [{ ...productPojo }],
  "totalCount": 42,
  "pageSize": 20
}
```

---

### `GET /data/products` — List Products (Admin Read)

**Security:** `products:read` authority (also permitted via config for GET)

Same params as above. Returns full product list including hidden ones.

---

### `GET /data/products/{id}` — Get Product by ID (Admin)

**Security:** `products:read`

```json
// Response 200 — ProductPojo with full details
```

---

### `POST /data/products` — Create Product

**Security:** `products:create`

```json
// Request
{
  "name": "string",
  "barcode": "string",
  "description": "string",
  "price": 299000,
  "currentStock": 50,
  "criticalStock": 10,
  "categoryCode": "tshirts"
}

// Response 201 — created ProductPojo
```

---

### `PUT /data/products/{id}` — Replace Product

**Security:** `products:update`

Full product replacement. All fields required.

---

### `PATCH /data/products/{id}` — Partial Update Product

**Security:** `products:update`

```json
// Request — partial fields
{ "price": 249000, "currentStock": 40 }

// Response 200 — updated ProductPojo
```

---

### `DELETE /data/products/{id}` — Delete Product

**Security:** `products:delete`

```json
// Response 204 — (empty body)
```

---

## 4. Reviews (Public)

### `GET /public/products/{barcode}/reviews` — List Approved Reviews

**Security:** permitAll

Returns only `approved: true` reviews, sorted by `createdAt desc`.

```json
// Response 200 — List<ProductReviewPojo>
[
  {
    "id": 1,
    "rating": 5,
    "title": "Great quality!",
    "body": "Fabric is soft and comfortable.",
    "productBarcode": "123456789",
    "productName": "Basic T-Shirt",
    "reviewerName": "John D.",
    "createdAt": "2026-04-10T14:00:00",
    "approved": true,
    "verifiedPurchase": true
  }
]
```

---

### `GET /public/products/{barcode}/reviews/stats` — Review Statistics

**Security:** permitAll

```json
// Response 200
{
  "averageRating": 4.3,
  "totalReviews": 47,
  "distribution": { "5": 20, "4": 15, "3": 8, "2": 3, "1": 1 }
}
```

**Errors:**
- `NOTFOUND_01` (404) — product not found

---

### `POST /account/reviews` — Submit Review

**Security:** authenticated

| Field | Type | Required | Description |
|---|---|---|---|
| `productBarcode` | string | ✅ | Product to review |
| `rating` | int | ✅ | 1–5 stars |
| `title` | string | ❌ | Review title |
| `body` | string | ❌ | Review content |

```json
// Request
{
  "productBarcode": "123456789",
  "rating": 5,
  "title": "Great quality!",
  "body": "Fabric is soft and comfortable."
}

// Response 201 — created ProductReviewPojo
{
  "id": 10,
  "rating": 5,
  "title": "Great quality!",
  "body": "Fabric is soft and comfortable.",
  "approved": false,
  "verifiedPurchase": true,
  "productBarcode": "123456789",
  "reviewerName": "John D.",
  "createdAt": "2026-04-18T10:00:00"
}
```

**Notes:**
- `verifiedPurchase` is auto-set based on whether the customer purchased the product
- New reviews start with `approved: false` (pending, not visible publicly)

---

### `GET /account/reviews` — List My Reviews

**Security:** authenticated

Returns all reviews written by the authenticated customer, including pending ones.

```json
// Response 200 — List<ProductReviewPojo>
```

---

## 5. Cart

### `GET /public/cart` — Get Cart State

**Security:** permitAll

**Headers:** `X-Session-Token` (optional — creates new session if absent)

```json
// Response 200 — CartSessionPojo
{
  "id": 1,
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "items": [
    {
      "variantSku": "BT-RED-S",
      "variantSize": "S",
      "variantColor": "Red",
      "productName": "Basic T-Shirt",
      "productBarcode": "123456789",
      "price": 299000,
      "quantity": 2,
      "lineTotal": 598000,
      "imageUrl": "https://..."
    }
  ],
  "subtotal": 598000,
  "itemCount": 1,
  "totalUnits": 2,
  "appliedDiscountCode": null,
  "discountAmount": 0,
  "totalAfterDiscount": 598000,
  "createdAt": "2026-04-18T10:00:00",
  "updatedAt": "2026-04-18T10:05:00",
  "expiresAt": "2026-04-25T10:00:00",
  "expired": false
}
```

---

### `POST /public/cart/items` — Add Item to Cart

**Security:** permitAll

**Headers:** `X-Session-Token` (optional)

| Field | Type | Required | Description |
|---|---|---|---|
| `variantSku` | string | ✅ | SKU of the product variant |
| `quantity` | int | ✅ | Quantity to add (> 0) |

```json
// Request
{ "variantSku": "BT-RED-S", "quantity": 2 }

// Response 200 — updated CartSessionPojo
```

**Errors:**
- `REJECTED_01` (400) — invalid SKU or quantity ≤ 0

---

### `PATCH /public/cart/items/{variantSku}` — Update Item Quantity

**Security:** permitAll

**Path Params:** `variantSku` — SKU of the cart item

```json
// Request
{ "quantity": 3 }

// Response 200 — updated CartSessionPojo
```

**Errors:**
- `REJECTED_01` (400) — quantity ≤ 0 or item not in cart

---

### `DELETE /public/cart/items/{variantSku}` — Remove Item

**Security:** permitAll

**Path Params:** `variantSku`

```json
// Response 200 — updated CartSessionPojo (item removed)
```

---

### `DELETE /public/cart` — Clear Cart

**Security:** permitAll

```json
// Response 204 — (empty body)
```

---

### `GET /public/cart/validate` — Validate Cart Stock

**Security:** permitAll

Checks stock availability for all items in the cart.

```json
// Response 200 — all available
{ "valid": true, "message": "All items available" }

// Response 200 — some unavailable
{
  "valid": false,
  "message": "Some items are unavailable",
  "unavailableItems": [
    { "variantSku": "BT-RED-S", "requested": 5, "available": 2 }
  ]
}
```

---

## 6. Stock Reservations

All stock reservation endpoints require the `X-Session-Token` header (except `/confirm`).

### `GET /public/cart/reservations` — List Active Reservations

**Security:** permitAll

**Headers:** `X-Session-Token` (required)

```json
// Response 200 — List<StockReservationPojo>
[
  {
    "id": 1,
    "sessionId": "550e8400-...",
    "variantSku": "BT-RED-S",
    "variantSkuResolved": "BT-RED-S",
    "variantSize": "S",
    "variantColor": "Red",
    "productName": "Basic T-Shirt",
    "productBarcode": "123456789",
    "quantity": 2,
    "status": "RESERVED",
    "expiresAt": "2026-04-18T12:00:00",
    "createdAt": "2026-04-18T10:00:00",
    "updatedAt": "2026-04-18T10:00:00"
  }
]
```

**Status values:** `RESERVED` | `CONFIRMED` | `RELEASED`

---

### `POST /public/cart/reservations` — Reserve Stock

**Security:** permitAll

**Headers:** `X-Session-Token` (required)

| Field | Type | Required | Description |
|---|---|---|---|
| `variantSku` | string | ✅ | Variant SKU |
| `quantity` | int | ✅ | Quantity to reserve (> 0) |

```json
// Request
{ "variantSku": "BT-RED-S", "quantity": 2 }

// Response 201 — StockReservationPojo
```

**Errors:**
- `REJECTED_01` (400) — insufficient stock or invalid SKU

---

### `PATCH /public/cart/reservations` — Update Reservation Quantity

**Security:** permitAll

**Headers:** `X-Session-Token` (required)

```json
// Request
{ "variantSku": "BT-RED-S", "quantity": 3 }

// Response 200 — updated StockReservationPojo
```

---

### `DELETE /public/cart/reservations` — Release All Reservations

**Security:** permitAll

**Headers:** `X-Session-Token` (required)

```json
// Response 200 — List<StockReservationPojo> (released)
```

---

### `DELETE /public/cart/reservations/{variantSku}` — Release Single Reservation

**Security:** permitAll

**Headers:** `X-Session-Token` (required)

```json
// Response 200 — StockReservationPojo (released)
// or 204 — if no reservation existed for this SKU
```

---

### `GET /public/cart/reservations/availability?variantSku=X` — Check Stock Availability

**Security:** permitAll

**Query Params:** `variantSku` (required)

```json
// Response 200
{ "variantSku": "BT-RED-S", "availableStock": 42 }

// Response 404 — variant not found
```

---

### `POST /public/cart/reservations/confirm` — Confirm Reservations (Payment Success)

**Security:** permitAll

Confirms all RESERVED items as CONFIRMED (stock is deducted).

**Headers:** `X-Session-Token` (optional — also accepts body field `sessionId`)

```json
// Request body (optional — header takes precedence)
{ "sessionId": "550e8400-..." }

// Response 200 — List<StockReservationPojo> (status = CONFIRMED)
```

---

## 7. Checkout & Payment

> **Payment Gateway:** Webpay Plus (Transbank Chile) — `paymentType` value: `"Webpay Plus"`

### `POST /public/checkout` — Start Checkout

**Security:** requires `checkout` authority (authenticated user)

| Field | Type | Required | Description |
|---|---|---|---|
| `sessionToken` | string | ❌ | Cart session token (required in body OR `X-Session-Token` header) |
| `shippingMethodId` | long | ✅ | Shipping method ID |
| `discountCode` | string | ❌ | Discount code |
| `customer` | PersonPojo | ❌ | Override customer info |
| `shippingAddress` | AddressPojo | ✅ | Delivery address |
| `paymentType` | string | ✅ | `"Webpay Plus"` |
| `billingType` | string | ✅ | `"enterprise"` or `"individual"` |
| `billingCompany` | BillingCompanyPojo | ❌ | Required if `billingType=enterprise` |
| `billingAddress` | AddressPojo | ❌ | Required if `billingType=enterprise` |

**AddressPojo fields:**
```json
{
  "recipientName": "John Doe",
  "phone": "0912345678",
  "addressLine1": "123 Nguyen Hue",
  "addressLine2": "Floor 2",
  "ward": "Ben Thanh",
  "district": "District 1",
  "city": "Ho Chi Minh",
  "postalCode": "700000",
  "country": "Vietnam"
}
```

**BillingCompanyPojo fields:**
```json
{
  "name": "Mono Studio Co.",
  "taxCode": "0123456789",
  "email": "billing@monostudio.vn"
}
```

```json
// Request
{
  "sessionToken": "550e8400-...",
  "shippingMethodId": 1,
  "discountCode": "SUMMER20",
  "shippingAddress": {
    "recipientName": "John Doe",
    "phone": "0912345678",
    "addressLine1": "123 Nguyen Hue",
    "city": "Ho Chi Minh"
  },
  "paymentType": "Webpay Plus",
  "billingType": "enterprise",
  "billingCompany": {
    "name": "Mono Studio Co.",
    "taxCode": "0123456789",
    "email": "billing@monostudio.vn"
  },
  "billingAddress": {
    "recipientName": "Mono Studio",
    "addressLine1": "456 Le Duan",
    "city": "Ho Chi Minh"
  }
}

// Response 200 — PaymentRedirectionDetailsPojo
{
  "url": "https://webpay.example.com/...",
  "token": "xxx"
}
```

**Errors:**
- `REJECTED_01` (400) — bad input
- `PAYMENT_01` (400) — payment gateway failure

---

### `GET /public/checkout/validate?token_ws=X` — Payment Success Callback (WebPay)

**Security:** permitAll

Called by WebPay after successful payment. Redirects client to receipt page.

**Query Params:** `token_ws` (WebPay success token — required)

```json
// Response 303 → /receipt?token=<order_token>
// or error redirect to error page
```

**Errors:**
- `REJECTED_01` (400) — missing token
- `NOTFOUND_01` (404) — order not found
- `PAYMENT_01` (400) — payment verification failed

---

### `POST /public/checkout/validate` — Payment Aborted Callback (WebPay)

**Security:** permitAll

Called by WebPay when customer cancels. Uses `TBK_TOKEN` param.

**Query Params:** `TBK_TOKEN` (WebPay abortion token — required)

```json
// Response 303 → /result?status=aborted&token=<token>
// or error redirect to error page
```

---

## 8. Receipt

### `GET /public/receipt/{token}` — Fetch Receipt

**Security:** permitAll

**Path Params:** `token` — transaction/order token

```json
// Response 200 — ReceiptPojo
{
  "buyOrder": 12345,
  "token": "550e8400-...",
  "date": "2026-04-18T14:30:00",
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "totalValue": 124000,
  "status": "Delivery Complete",
  "paymentType": "Webpay Plus",
  "details": [
    {
      "productName": "Basic T-Shirt",
      "variant": "Red / S",
      "quantity": 2,
      "unitPrice": 299000,
      "lineTotal": 598000
    }
  ],
  "shippingAddress": { ... },
  "billingAddress": { ... }
}
```

**Errors:**
- `REJECTED_01` (400) — blank token
- `NOTFOUND_01` (404) — no matching transaction

---

## 9. Discount Codes (Public)

### `GET /public/discount/validate?code=X&subtotal=Y` — Validate Discount Code

**Security:** permitAll

| Param | Type | Required | Description |
|---|---|---|---|
| `code` | string | ✅ | Discount code to validate |
| `subtotal` | number | ✅ | Cart subtotal in VND (without formatting) |

```json
// Response 200 — DiscountValidationResult
{
  "valid": true,
  "code": "SUMMER20",
  "type": "PERCENTAGE",
  "value": 20,
  "discountAmount": 11960,
  "message": "20% discount applied"
}

// Response 200 — invalid code
{
  "valid": false,
  "code": "EXPIRED",
  "discountAmount": 0,
  "message": "This discount code has expired"
}
```

**Discount Types:** `PERCENTAGE` | `FIXED_AMOUNT` | `FREE_SHIPPING`

---

## 10. Shipping Methods (Public)

### `GET /public/shipping/methods?subtotal=X` — List Active Shipping Methods

**Security:** permitAll

**Query Params:** `subtotal` (optional) — cart subtotal in VND; if provided, fee is waived when subtotal ≥ method's freeShippingThreshold

```json
// Response 200 — List<ShippingRatePojo>
[
  {
    "id": 1,
    "name": "Standard Delivery",
    "description": "3–5 business days",
    "fee": 15000,
    "freeShippingThreshold": 200000,
    "estimatedDays": "3-5"
  },
  {
    "id": 2,
    "name": "Express Delivery",
    "description": "1–2 business days",
    "fee": 35000,
    "freeShippingThreshold": null,
    "estimatedDays": "1-2"
  }
]
```

---

## 11. Address Book

### `GET /public/address-book` — List My Addresses

**Security:** authenticated

```json
// Response 200 — List<AddressBookPojo>
[
  {
    "id": 1,
    "label": "Home",
    "defaultShipping": true,
    "defaultBilling": false,
    "address": {
      "recipientName": "John Doe",
      "phone": "0912345678",
      "addressLine1": "123 Nguyen Hue",
      "city": "Ho Chi Minh"
    }
  }
]
```

---

### `GET /public/address-book/{id}` — Get Address Entry

**Security:** authenticated

```json
// Response 200 — AddressBookPojo
```

---

### `POST /public/address-book` — Create Address Entry

**Security:** authenticated

```json
// Request
{
  "label": "Office",
  "defaultShipping": false,
  "defaultBilling": false,
  "address": { ...AddressPojo }
}

// Response 201 — created AddressBookPojo
```

---

### `PUT /public/address-book/{id}` — Replace Address Entry

**Security:** authenticated

Full replacement — all fields required.

---

### `PATCH /public/address-book/{id}` — Partial Update Address

**Security:** authenticated

```json
// Request — partial fields
{ "label": "Home 2", "defaultShipping": true }

// Response 200 — updated AddressBookPojo
```

---

### `DELETE /public/address-book/{id}` — Delete Address Entry

**Security:** authenticated

```json
// Response 204 — (empty body)
```

---

## 12. Admin — Dashboard

> **Auth:** requires `dashboard:read` authority

### `GET /admin/dashboard/stats` — Full Dashboard Stats

**Query Params:**
| Param | Type | Required | Description |
|---|---|---|---|
| `from` | date | ❌ | Start of date range (inclusive), format `YYYY-MM-DD` |
| `to` | date | ❌ | End of date range (inclusive), format `YYYY-MM-DD` |

```json
// Response 200 — AdminDashboardStatsPojo
{
  "totalRevenue": 15000000,
  "totalOrders": 120,
  "orderStatusBreakdown": [
    { "status": "Pending", "count": 15 },
    { "status": "Confirmed", "count": 30 },
    { "status": "Delivery Complete", "count": 75 }
  ],
  "revenueByPeriod": [
    { "period": "2026-04-01", "revenue": 1200000 },
    { "period": "2026-04-02", "revenue": 980000 }
  ],
  "topProducts": [
    { "barcode": "123456789", "name": "Basic T-Shirt", "unitsSold": 150, "revenue": 44850000 }
  ],
  "lowStockAlerts": [
    { "sku": "BT-RED-S", "name": "Basic T-Shirt Red S", "stock": 3, "criticalLevel": 10 }
  ]
}
```

---

### `GET /admin/dashboard/stats/revenue` — Revenue by Period

**Query Params:** `from`, `to`, `groupBy` (`day` | `week` | `month`, default `day`)

```json
// Response 200 — Collection<RevenueStatPojo>
[
  { "period": "2026-04-01", "revenue": 1200000 },
  { "period": "2026-04-02", "revenue": 980000 }
]
```

---

### `GET /admin/dashboard/stats/top-products` — Top Selling Products

**Query Params:** `from`, `to`, `limit` (default 10)

```json
// Response 200 — Collection<TopProductPojo>
[
  { "barcode": "123456789", "name": "Basic T-Shirt", "unitsSold": 150, "revenue": 44850000 }
]
```

---

### `GET /admin/dashboard/stats/low-stock` — Low Stock Alerts

Returns all variants where `stockCurrent <= criticalStock`.

```json
// Response 200 — Collection<LowStockAlertPojo>
[
  { "sku": "BT-RED-S", "name": "Basic T-Shirt Red S", "stock": 3, "criticalLevel": 10 }
]
```

---

### `GET /admin/dashboard/stats/order-statuses` — Order Status Breakdown

**Query Params:** `from`, `to`

```json
// Response 200 — Collection<OrderStatusCountPojo>
[
  { "status": "Pending", "count": 15 },
  { "status": "Confirmed", "count": 30 }
]
```

---

## 14. Admin — Products

`DataCrudGenericController<ProductPojo, Product>`

**CRUD Authority:** `products:create` | `products:read` | `products:update` | `products:delete`

### Standard CRUD

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/data/products` | read | List products (paginated) |
| `GET` | `/data/products/{id}` | read | Get product by ID |
| `POST` | `/data/products` | create | Create product |
| `PUT` | `/data/products/{id}` | update | Replace product |
| `PATCH` | `/data/products/{id}` | update | Partial update |
| `DELETE` | `/data/products/{id}` | delete | Delete product |

### `GET /data/products?barcode=X` — Filter Products by Barcode

> Note: `barcode` is a **filter query parameter** on the standard list endpoint, not a separate endpoint.

```json
// GET /api/data/products?barcode=123456789
// Response 200 — DataPagePojo with matching ProductPojo
```

### Product Visibility Control

All products have a `status` field with lifecycle values. Newly created products default to `DRAFT`.

| Status | Description |
|---|---|
| `DRAFT` | Not visible to customers (pre-launch preparation) |
| `PUBLISHED` | Live and visible to customers |
| `UNLISTED` | Was available, now hidden (discontinued, seasonal) |

| Method | Path | Auth | Description |
|---|---|---|---|
| `PATCH` | `/data/products/{id}/publish` | `products:update` | DRAFT → PUBLISHED |
| `PATCH` | `/data/products/{id}/unpublish` | `products:update` | PUBLISHED → UNLISTED |
| `PATCH` | `/data/products/{id}/revert-to-draft` | `products:update` | any → DRAFT |
| `GET` | `/data/products?status=DRAFT` | read | Admin filter by status |

```json
// PATCH /data/products/5/publish
// Response 200 — ProductPojo with status: "PUBLISHED"

// GET /data/products?status=DRAFT
// Response 200 — all draft products (admin review)
```

---

## 15. Admin — Product Categories

`DataCrudGenericController<ProductCategoryPojo, ProductCategory>`

**CRUD Authority:** `product_categories:create` | `product_categories:read` | `product_categories:update` | `product_categories:delete`

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/product_categories` | read |
| `GET` | `/data/product_categories/{id}` | read |
| `POST` | `/data/product_categories` | create |
| `PUT` | `/data/product_categories/{id}` | update |
| `PATCH` | `/data/product_categories/{id}` | update |
| `DELETE` | `/data/product_categories/{id}` | delete |

### `GET /data/product_categories/{code:[a-zA-Z0-9\\-]+}` — Get by Code

**Auth:** none (also permitted via config)

```json
// Response 200 — ProductCategoryPojo
{
  "id": 1,
  "code": "tshirts",
  "name": "T-Shirts",
  "description": "All t-shirt products",
  "parentCode": null,
  "active": true
}
```

---

### `GET /data/product_categories/{code}/products` — List Products in Category

**Auth:** none

**Query Params:** standard pagination + filter params

```json
// Response 200 — DataPagePojo<ProductPojo>
```

---

## 16. Admin — Product Variants

`DataCrudGenericController<ProductVariantPojo, ProductVariant>`

**CRUD Authority:** `productVariants:create` | `productVariants:read` | `productVariants:update` | `productVariants:delete`

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/product-variants` | read |
| `GET` | `/data/product-variants/{id}` | read |
| `POST` | `/data/product-variants` | create |
| `PUT` | `/data/product-variants/{id}` | update |
| `PATCH` | `/data/product-variants/{id}` | update |
| `DELETE` | `/data/product-variants/{id}` | delete |

---

## 17. Admin — Product Lists

`DataCrudGenericController<ProductListPojo, ProductList>`

**CRUD Authority:** `product_lists:create` | `product_lists:read` | `product_lists:update` | `product_lists:delete`

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/product_lists` | read |
| `GET` | `/data/product_lists/{id}` | read |
| `POST` | `/data/product_lists` | create |
| `PUT` | `/data/product_lists/{id}` | update |
| `PATCH` | `/data/product_lists/{id}` | update |
| `DELETE` | `/data/product_lists/{id}` | delete |

---

### Product List Contents (Standalone Controller)

**Auth:** `product_lists:contents` (add/remove/replace), none for read

| Method | Path | Auth | Operation |
|---|---|---|---|
| `GET` | `/data/product_list_contents?listCode=X` | read | View list contents |
| `POST` | `/data/product_list_contents?listCode=X` | contents | Add products |
| `PUT` | `/data/product_list_contents?listCode=X` | contents | Replace all contents |
| `DELETE` | `/data/product_list_contents?listCode=X` | contents | Remove products |

**POST body:**
```json
[{ "barcode": "123456789" }, { "barcode": "987654321" }]
```

**DELETE query params:** `listCode` (required) + `barcode` (optional filter)

---

## 18. Admin — Product Reviews

`DataCrudGenericController<ProductReviewPojo, ProductReview>` + custom actions

**CRUD Authority:** `productReviews:create` | `productReviews:read` | `productReviews:update` | `productReviews:delete`

### Standard CRUD (admin sees ALL reviews including pending)

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/product-reviews` | read |
| `GET` | `/data/product-reviews/{id}` | read |
| `POST` | `/data/product-reviews?customerId=X` | create |
| `PUT` | `/data/product-reviews/{id}` | update |
| `PATCH` | `/data/product-reviews/{id}` | update |
| `DELETE` | `/data/product-reviews/{id}` | delete |

**POST notes:** `customerId` is a required **query parameter**.

### Custom Action Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `PATCH` | `/data/product-reviews/{id}/approve` | `productReviews:update` | Approve review (make public) |
| `PATCH` | `/data/product-reviews/{id}/reject` | `productReviews:update` | Reject review |

```json
// PATCH /data/product-reviews/10/approve
// Response 204 — (empty body)
```

---

## 19. Admin — Images

`DataCrudGenericController<ImagePojo, Image>`

**CRUD Authority:** `images:create` | `images:read` | `images:update` | `images:delete`

> ⚠️ No multipart file upload endpoint — CRUD only (create/update via image URL/URL references).

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/images` | read (also permitted via config) |
| `GET` | `/data/images/{id}` | read |
| `POST` | `/data/images` | create |
| `PUT` | `/data/images/{id}` | update |
| `PATCH` | `/data/images/{id}` | update |
| `DELETE` | `/data/images/{id}` | delete |

---

## 20. Admin — Billing Types

`DataCrudGenericController<BillingTypePojo, BillingType>`

**Security:** class-level `@PreAuthorize("isAuthenticated()")`; method-level:
- `GET` — `billing_types:read`
- `POST/PUT/PATCH/DELETE` — `billing_types:create` / `billing_types:update` / `billing_types:delete`

Also permitted via config for GET.

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/billing_types` | read |
| `GET` | `/data/billing_types/{id}` | read |
| `POST` | `/data/billing_types` | create |
| `PUT` | `/data/billing_types/{id}` | update |
| `PATCH` | `/data/billing_types/{id}` | update |
| `DELETE` | `/data/billing_types/{id}` | delete |

---

## 21. Admin — Order Statuses

`DataGenericController<OrderStatusPojo, OrderStatus>` — **read-only**

**CRUD Authority:** `order_statuses:read` (GET only; no create/update/delete)

### Read-Only Endpoints

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/order_statuses` | `order_statuses:read` |
| `GET` | `/data/order_statuses/{id}` | `order_statuses:read` |

---

## 22. Admin — Shipping Methods

`DataCrudGenericController<ShippingMethodPojo, ShippingMethod>`

**CRUD Authority:** `shipping-methods:create` | `shipping-methods:read` | `shipping-methods:update` | `shipping-methods:delete`

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/shipping-methods` | read |
| `GET` | `/data/shipping-methods/{id}` | read |
| `POST` | `/data/shipping-methods` | create |
| `PUT` | `/data/shipping-methods/{id}` | update |
| `PATCH` | `/data/shipping-methods/{id}` | update |
| `DELETE` | `/data/shipping-methods/{id}` | delete |

---

## 23. Admin — Shippers

`DataCrudGenericController<ShipperPojo, Shipper>`

**CRUD Authority:** `shippers:create` | `shippers:read` | `shippers:update` | `shippers:delete`

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/shippers` | read |
| `GET` | `/data/shippers/{id}` | read |
| `POST` | `/data/shippers` | create |
| `PUT` | `/data/shippers/{id}` | update |
| `PATCH` | `/data/shippers/{id}` | update |
| `DELETE` | `/data/shippers/{id}` | delete |

---

## 24. Admin — Orders

`DataCrudGenericController<OrderPojo, Order>` + custom action endpoints

**CRUD Authority:** `orders:create` | `orders:read` | `orders:update` | `orders:delete`

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/orders` | read |
| `GET` | `/data/orders/{id}` | read |
| `POST` | `/data/orders` | create |
| `PUT` | `/data/orders/{id}` | update |
| `PATCH` | `/data/orders/{id}` | update |
| `DELETE` | `/data/orders/{id}` | delete |

**Special GET behavior:** If `buyOrder` query param is present, returns single-item page sorted by `buyOrder desc`.

### Order Status State Machine Actions

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/data/orders/confirmation` | `orders:update` | Confirm a pending order |
| `POST` | `/data/orders/rejection` | `orders:update` | Reject a pending order |
| `POST` | `/data/orders/completion` | `orders:update` | Mark as completed |
| `POST` | `/data/orders/cancellation?orderId=X&reason=Y` | `orders:update` | Admin cancel — releases stock + refund if paid |

```json
// POST /data/orders/confirmation
// Request — OrderPojo
// Response 200 — updated OrderPojo

// POST /data/orders/cancellation?orderId=123&reason=Wrong+size
// Response 200 — updated OrderPojo (status = Cancelled)
```

---

## 25. Admin — Return Requests

`DataCrudGenericController<ReturnRequestPojo, ReturnRequest>` + custom action endpoints

**CRUD Authority:** `returnRequests:create` | `returnRequests:read` | `returnRequests:update` | `returnRequests:delete`

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/return-requests` | read |
| `GET` | `/data/return-requests/{id}` | read |
| `POST` | `/data/return-requests` | create |
| `PUT` | `/data/return-requests/{id}` | update |
| `PATCH` | `/data/return-requests/{id}` | update |
| `DELETE` | `/data/return-requests/{id}` | delete |

**Special GET behavior:** If `id` query param present, returns single-item page; defaults `sortBy=id&order=desc`.

### Return Request Lifecycle Actions

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/data/return-requests/approve/{id}` | `returnRequests:update` | Approve — releases stock, initiates refund |
| `POST` | `/data/return-requests/reject/{id}` | `returnRequests:update` | Reject return request |
| `POST` | `/data/return-requests/receive/{id}` | `returnRequests:update` | Mark items received at warehouse |
| `POST` | `/data/return-requests/complete-refund/{id}` | `returnRequests:update` | Complete refund processing |
| `POST` | `/data/return-requests/tracking/{id}` | `returnRequests:update` | Add tracking number |
| `POST` | `/data/return-requests/cancel/{id}` | `returnRequests:update` | Cancel pending return |

**Optional body for approve/receive/complete-refund:**
```json
{ "adminNotes": "Refund processed via bank transfer", "refundAmount": 50000 }
```

---

## 26. Admin — Discount Codes

`DataCrudGenericController<DiscountCodePojo, DiscountCode>`

**CRUD Authority:** `discountCodes:create` | `discountCodes:read` | `discountCodes:update` | `discountCodes:delete`

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/discount-codes` | read |
| `GET` | `/data/discount-codes/{id}` | read |
| `POST` | `/data/discount-codes` | create |
| `PUT` | `/data/discount-codes/{id}` | update |
| `PATCH` | `/data/discount-codes/{id}` | update |
| `DELETE` | `/data/discount-codes/{id}` | delete |

---

## 27. Admin — Cart Sessions

`DataGenericController<CartSessionPojo, CartSession>` — **read-only**

> ⚠️ `create()` and `update()` are overridden to throw `UnsupportedOperationException`. Admin cannot create carts.

**CRUD Authority:** `cartSessions:read` (GET), `cartSessions:update` (PATCH), `cartSessions:delete` (DELETE)

### Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/data/cart-sessions` | read | List cart sessions |
| `GET` | `/data/cart-sessions/{id}` | read | Get session details |
| `PATCH` | `/data/cart-sessions/{id}` | update | Refresh cart TTL |
| `DELETE` | `/data/cart-sessions/{id}` | delete | Delete session |

**PATCH body (refresh TTL):**
```json
{ "refreshExpiry": true, "expiresAt": "2026-04-25T00:00:00" }
```

> ⚠️ `DELETE` does NOT release stock reservations — use `DELETE /public/cart/reservations` first.

---

## 28. Admin — Users & Roles

### Users

`DataCrudGenericController<UserPojo, User>`

**CRUD Authority:** `users:create` | `users:read` | `users:update` | `users:delete`

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/users` | read |
| `GET` | `/data/users/{id}` | read |
| `POST` | `/data/users` | create |
| `PUT` | `/data/users/{id}` | update |
| `PATCH` | `/data/users/{id}` | update |
| `DELETE` | `/data/users/{id}` | delete |

### User Roles

`DataCrudGenericController<UserRolePojo, UserRole>`

**CRUD Authority:** `user_roles:create` | `user_roles:read` | `user_roles:update` | `user_roles:delete`

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/user_roles` | read |
| `GET` | `/data/user_roles/{id}` | read |
| `POST` | `/data/user_roles` | create |
| `PUT` | `/data/user_roles/{id}` | update |
| `PATCH` | `/data/user_roles/{id}` | update |
| `DELETE` | `/data/user_roles/{id}` | delete |

---

## 29. Admin — People & Customers

### People — Read Only

`DataGenericController<PersonPojo, Person>`

**CRUD Authority:** `people:read` (GET only — no create/update/delete)

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/people` | `people:read` |
| `GET` | `/data/people/{id}` | `people:read` |

---

### Customers — Full CRUD

`DataCrudGenericController<PersonPojo, Customer>`

**CRUD Authority:** `customers:create` | `customers:read` | `customers:update` | `customers:delete`

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/customers` | read |
| `GET` | `/data/customers/{id}` | read (returns `PersonPojo` with read-only `customerId`) |
| `GET` | `/data/customers/{id}/detail` | read — admin profile: `CustomerAdminPojo` (orders, addresses, aggregates) |
| `POST` | `/data/customers` | create — response body: `PersonPojo` with `customerId` |
| `PUT` | `/data/customers/{id}` | update |
| `PATCH` | `/data/customers/{id}` | partial update (`person.*` keys in JSON map) |
| `DELETE` | `/data/customers/{id}` | delete — rejected (`400`) if customer has orders or a linked user account |

**List filtering (customers predicate):** `q` (matches first/last name, email, phones, id number), plus `nameLike`, `emailLike`, `phoneLike`, and exact-match keys such as `email`, `name`.

**Manual QA (admin):** list pagination (`pageIndex`/`pageSize`), search box (`q`), open detail (`GET .../detail`), edit (`PATCH` with `person.*`), create (`POST` → redirect), delete blocked when orders exist or user linked.

---

## 30. Admin — Salespeople

`DataCrudGenericController<PersonPojo, Salesperson>`

**CRUD Authority:** `salespeople:create` | `salespeople:read` | `salespeople:update` | `salespeople:delete`

### Standard CRUD

| Method | Path | Auth |
|---|---|---|
| `GET` | `/data/salespeople` | read |
| `GET` | `/data/salespeople/{id}` | read |
| `POST` | `/data/salespeople` | create |
| `PUT` | `/data/salespeople/{id}` | update |
| `PATCH` | `/data/salespeople/{id}` | update |
| `DELETE` | `/data/salespeople/{id}` | delete |

---

## 31. Error Reference

All errors follow this structure:

```json
{
  "code": "NOTFOUND_01",
  "message": "Entity not found",
  "detail": "Optional additional context"
}
```

### Exception → HTTP Status Mapping

| Exception | HTTP Status | Code | Description |
|---|---|---|---|
| `EntityNotFoundException` | 404 | `NOTFOUND_01` | Resource not found |
| `EntityExistsException` | 400 | `EXISTS_01` | Resource already exists |
| `BadInputException` | 400 | `REJECTED_01` | Invalid input / business rule violation |
| `MethodArgumentNotValidException` | 400 | `REJECTED_02` | Validation annotation failure |
| `PaymentServiceException` | 400 | `PAYMENT_01` | Payment gateway error |
| `AuthenticationException` / `AccessDeniedException` | 401 | `AUTH_01` | Auth failure |

---

## 32. Pagination & Filtering

All `DataGenericController` `GET /` list endpoints accept:

| Param | Type | Default | Description |
|---|---|---|---|
| `pageIndex` | int | `0` | Zero-based page index |
| `pageSize` | int | configurable (20) | Items per page |
| `sortBy` | string | varies | Field to sort by (e.g. `id`, `name`, `price`) |
| `order` | string | `asc` | `asc` or `desc` |
| `{field}` | string | — | Filter by entity field (via PredicateService — exact or partial match depending on field type) |

### Response Wrapper — DataPagePojo

```json
{
  "items": [ ... ],
  "totalCount": 100,
  "pageSize": 20
}
```

### Date Range Filtering

For date fields, use `from` and `to` params with format `YYYY-MM-DD`.

---

## Appendix: Pojo Reference

### ProductPojo
```json
{
  "id": 1,
  "name": "Basic T-Shirt",
  "barcode": "123456789",
  "description": "Premium cotton t-shirt",
  "price": 299000,
  "currentStock": 50,
  "criticalStock": 10,
  "status": "PUBLISHED",
  "category": { "id": 1, "code": "tshirts", "name": "T-Shirts" },
  "images": [{ "id": 1, "url": "https://...", "alt": "Product image" }],
  "averageRating": 4.5,
  "totalReviews": 23,
  "variants": [{ "sku": "BT-RED-S", "size": "S", "color": "Red", "price": 299000, "stock": 15 }]
}
```

### CartSessionPojo
```json
{
  "id": 1,
  "token": "550e8400-...",
  "items": [{ "variantSku": "BT-RED-S", "productName": "Basic T-Shirt", "price": 299000, "quantity": 2, "lineTotal": 598000, "imageUrl": "https://..." }],
  "subtotal": 598000,
  "itemCount": 1,
  "totalUnits": 2,
  "appliedDiscountCode": null,
  "discountAmount": 0,
  "totalAfterDiscount": 598000,
  "createdAt": "2026-04-18T10:00:00",
  "updatedAt": "2026-04-18T10:05:00",
  "expiresAt": "2026-04-25T10:00:00",
  "expired": false
}
```

### StockReservationPojo
```json
{
  "id": 1,
  "sessionId": "550e8400-...",
  "variantSku": "BT-RED-S",
  "variantSkuResolved": "BT-RED-S",
  "variantSize": "S",
  "variantColor": "Red",
  "productName": "Basic T-Shirt",
  "productBarcode": "123456789",
  "quantity": 2,
  "status": "RESERVED",
  "expiresAt": "2026-04-18T12:00:00",
  "createdAt": "2026-04-18T10:00:00",
  "updatedAt": "2026-04-18T10:00:00"
}
```

### OrderPojo
```json
{
  "id": 1,
  "buyOrder": 12345,
  "token": "550e8400-...",
  "cartSessionToken": "...",
  "date": "2026-04-18T14:30:00",
  "details": [{ "productName": "Basic T-Shirt", "variant": "Red / S", "quantity": 2, "unitPrice": 299000, "lineTotal": 598000 }],
  "netValue": 598000,
  "taxValue": 59800,
  "transportValue": 15000,
  "totalValue": 672800,
  "totalItems": 2,
  "totalRefundedAmount": 0,
  "discountCode": "SUMMER20",
  "discountValue": 11960,
  "status": "Confirmed",
  "billingType": "enterprise",
  "paymentType": "Webpay Plus",
  "customer": { "firstName": "John", "lastName": "Doe", "email": "john@example.com" },
  "salesperson": { "firstName": "Jane", "lastName": "Smith" },
  "shipper": "FastShip Co.",
  "billingCompany": { "name": "Mono Studio", "taxCode": "0123456789" },
  "billingAddress": { "recipientName": "Mono Studio", "addressLine1": "456 Le Duan", "city": "Ho Chi Minh" },
  "shippingAddress": { "recipientName": "John Doe", "phone": "0912345678", "addressLine1": "123 Nguyen Hue", "city": "Ho Chi Minh" }
}
```

### ProductReviewPojo
```json
{
  "id": 1,
  "rating": 5,
  "title": "Great quality!",
  "body": "Fabric is soft and comfortable.",
  "productBarcode": "123456789",
  "productName": "Basic T-Shirt",
  "reviewerName": "John D.",
  "createdAt": "2026-04-10T14:00:00",
  "updatedAt": "2026-04-10T14:00:00",
  "approved": false,
  "verifiedPurchase": true
}
```

### ReturnRequestPojo
```json
{
  "id": 1,
  "date": "2026-04-15T10:00:00",
  "lastModified": "2026-04-18T09:00:00",
  "reason": "Wrong size",
  "adminNotes": null,
  "status": "PENDING",
  "refundMethod": null,
  "refundAmount": 0,
  "trackingNumber": null,
  "orderId": 123,
  "items": [{ "productName": "Basic T-Shirt", "variant": "Red / S", "quantity": 1, "unitPrice": 299000 }]
}
```

### AddressPojo
```json
{
  "recipientName": "John Doe",
  "phone": "0912345678",
  "addressLine1": "123 Nguyen Hue",
  "addressLine2": "Floor 2",
  "ward": "Ben Thanh",
  "district": "District 1",
  "city": "Ho Chi Minh",
  "postalCode": "700000",
  "country": "Vietnam"
}
```

### ShippingRatePojo
```json
{
  "id": 1,
  "name": "Standard Delivery",
  "description": "3–5 business days",
  "fee": 15000,
  "freeShippingThreshold": 200000,
  "estimatedDays": "3-5"
}
```

### DiscountValidationResult
```json
{
  "valid": true,
  "code": "SUMMER20",
  "type": "PERCENTAGE",
  "value": 20,
  "discountAmount": 11960,
  "message": "20% discount applied"
}
```