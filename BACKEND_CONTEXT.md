# Project Context: Mono Studio E-Commerce Backend

This document provides a comprehensive overview of the backend project for the "Mono Studio" fashion brand e-commerce website. This context is intended to assist in writing a graduation project report.

---

## 1. Project Overview
- **Project Name:** Mono Studio E-Commerce Backend
- **Domain:** Fashion Retail (B2C E-commerce)
- **Objective:** To build a robust, scalable, and secure monolithic backend for a fashion brand, supporting full customer lifecycle and back-office operations.

---

## 2. Technology Stack
- **Core Framework:** Java 21 with Spring Boot 3.2.10
- **Persistence:** Spring Data JPA (supporting PostgreSQL, MariaDB, and H2)
- **Database Querying:** QueryDSL (for type-safe dynamic queries)
- **Security:** Spring Security with JWT (Stateless) and OAuth2 (Google Login)
- **Messaging:** Apache Kafka (used for data synchronization and background tasks)
- **Search Engine:** Elasticsearch (integrated for high-performance product searching)
- **Object Storage:** Minio (S3-compatible storage for media assets)
- **Payment Gateways:** Integrated with PayOS, VNPay, MoMo, and supports Cash on Delivery (COD)
- **Shipping Integration:** Giao Hang Nhanh (GHN) API
- **Documentation:** Springdoc OpenAPI (Swagger UI)
- **Utilities:** Lombok, Apache Commons, Unirest, Bucket4j (Rate Limiting)
- **Testing & Coverage:** JUnit 5, Spring Security Test, Jacoco

---

## 3. System Architecture
- **Pattern:** Layered Architecture (Controller -> Service -> Repository)
- **Data Transfer:** Uses Pojo/DTO objects for API communication with a custom conversion layer (`ConversionService`).
- **Generic CRUD Engine:** Implements a powerful `DataCrudGenericController` to handle standard CRUD operations for all entities, significantly reducing boilerplate code.
- **Stateless Authentication:** Uses JWT tokens for both registered users and guest sessions.
- **Role-Based Access Control (RBAC):** Fine-grained permission system with Roles and Permissions enforced via `@PreAuthorize` annotations.

---

## 4. Key Modules & Features

### A. Product & Inventory Management
- **Product Catalog:** Multi-level categories, product variants (size/color), and high-quality image management.
- **Stock Reservation System:** Automatically reserves stock when items are added to the cart to prevent overselling, with automatic release on expiration or confirmation on payment.
- **Stock Adjustments:** Supports manual stock corrections, transfers, and goods receipt tracking.

### B. Cart & Checkout
- **Flexible Cart:** Supports both guest and registered user carts, with persistence across sessions for logged-in users.
- **Checkout Process:** Comprehensive checkout flow including address selection, shipping method calculation, and discount application.
- **Promotion Engine:** Supports various discount types (Percentage, Fixed Amount, Free Shipping) with usage limits and validity periods.

### C. Order Management
- **State Machine:** Orders follow a robust state machine with 8 statuses (e.g., Pending, Paid, Shipped, Delivered, Cancelled).
- **History Tracking:** Customers can view their order history and status updates in real-time.

### D. Payment & Shipping
- **Multi-Gateway Payment:** Integrated with popular Vietnamese payment providers (VNPay, MoMo, PayOS).
- **Shipping Integration:** Real-time shipping fee calculation and tracking via GHN.

### E. Search & Social
- **Elasticsearch Integration:** Full-text search for products with Kafka-based real-time synchronization between the main DB and the search index.
- **Review System:** Customers can rate and review products, with an admin moderation flow.
- **Blog Management:** Built-in blog post system for brand storytelling.

### F. Loyalty & Engagement
- **Loyalty Points:** Ledger-based system to track customer points earned from purchases.
- **Mailing System:** Automated email notifications for registration and order updates.

---

## 5. Core Business Workflows

### A. Checkout & Order Lifecycle
1.  **Cart Validation:** Checks stock availability and validates session.
2.  **Pricing Calculation:** `CartPricingService` calculates subtotal, applies discounts, and computes shipping fees.
3.  **Order Creation:** `CheckoutService` persists the order in `PENDING` status.
4.  **Payment Integration:** Initiates payment with selected gateway (PayOS/VNPay/MoMo).
5.  **Stock Confirmation:** Upon payment success, `OrdersProcessService` confirms stock reservations and deducts inventory.
6.  **Fulfillment:** Transitions order to `SHIPPING` via `ShipmentOrchestrator` which calls GHN API.
7.  **Completion:** Automatically updates status to `DELIVERED` based on carrier webhooks.

### B. Intelligent Stock Reservation
- **Pre-allocation:** Reserves stock as soon as an item is added to the cart (valid for a specific time window).
- **Atomicity:** Uses native SQL queries with atomic updates to prevent race conditions during high-traffic sales.
- **Auto-Release:** A background task or session expiry trigger releases reserved stock back to the pool if the checkout is not completed.

### C. Search Synchronization (Kafka + Elasticsearch)
- **Producer:** DB changes (Product/Variant updates) trigger `IndexEvent` messages to Kafka.
- **Consumer:** `ElasticsearchSyncConsumer` listens to Kafka and updates the Elasticsearch index near real-time.
- **Search Query:** `PublicProductsController` utilizes the search index for fuzzy and full-text search capabilities.

---

## 6. Infrastructure & Deployment
- **Dockerized Environment:** The system includes Dockerfiles and Compose files for Redis and Minio.
- **Object Storage (Minio):** Used for storing product images and media, providing an S3-compatible interface.
- **Caching (Redis):** Used for caching frequently accessed data like product catalogs and session tokens to reduce DB load.
- **Database Migrations:** Managed through a combination of Hibernate auto-update (for development) and manual migration scripts (`database-migrations.sql`) for production schema refinements.

---

## 7. Project Structure (Package Breakdown)
- `org.monostudio.api`: REST Controllers, DTOs (Pojo), and core Service interfaces/implementations.
- `org.monostudio.jpa`: Entities, Repositories, and specialized JPA services (Predicates, Patching, Converters).
- `org.monostudio.security`: Security configurations, JWT filters, and authentication providers.
- `org.monostudio.search`: Elasticsearch integration logic and Kafka consumers.
- `org.monostudio.shipping`: Carrier integration (GHN) and shipping rate logic.
- `org.monostudio.payment`: Payment gateway adapters (PayOS, VNPay, MoMo, COD).
- `org.monostudio.pricing`: Discount and promotion engine rules.
- `org.monostudio.mailing`: Email templates and mail sending services.

---

## 8. Database Schema Highlights
The system manages over 60 entities, including:
- **Identity:** `User`, `Role`, `Permission`, `Customer`, `Person`.
- **Products:** `Product`, `ProductVariant`, `ProductCategory`, `ProductImage`.
- **Commerce:** `Order`, `OrderDetail`, `CartSession`, `CartItem`.
- **Inventory:** `StockReservation`, `StockAdjustment`, `GoodsReceipt`.
- **Marketing:** `DiscountCode`, `PromotionRule`, `BlogPost`.
- **Logistics:** `ShipmentTracking`, `Shipper`, `ShippingMethod`, `AddressBook`.

---

## 6. Security & Performance
- **Authentication:** JWT-based stateless auth with unique guest tokens.
- **Rate Limiting:** Implemented via Bucket4j on critical endpoints (Login, Registration, Checkout) to prevent brute-force and DDoS attacks.
- **Security Best Practices:** Password hashing with BCrypt, CORS configuration, clickjacking protection, and no PII logging in application logs.
- **Caching:** Integrated Redis for performance optimization.

---

## 7. Project Status (as of May 2026)
- **Implemented:** Generic CRUD, Product/Category management, Cart & Reservation, RBAC, JWT Auth, Order State Machine, Discount Engine, GHN & PayOS/VNPay/MoMo integrations.
- **In Progress/Planned:** Enhanced email verification flow, full-text Vietnamese search optimization, and advanced analytics for the admin dashboard.

---
*This context was extracted from the backend codebase of the Mono Studio graduation project.*
