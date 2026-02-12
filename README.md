# SupaSpring (SpringBase)

SupaSpring is a high-performance, open-source alternative to Supabase, built entirely using the latest Spring Boot (Java) ecosystem.

## Features Showcase

*   **⚡ Instant REST APIs**: Automatically generate CRUD endpoints for any table with zero code.
*   **🔒 Smart-Policy Security**: Row-level security that just works. No complex SQL policies required.
*   **⛓️ Atomic Action Engine**: Execute complex multi-step workflows in a single ACID-compliant transaction.
*   **📦 Object Storage**: S3-compatible storage with project-level isolation and stakeholder-based access.
*   **🛠️ SupaShell Console**: A built-in SQL terminal for advanced migrations and data exploration.
*   **🎨 Premium Dashboard**: A sleek, high-density interface for managing your entire backend.

## Atomic Actions

The **Atomic Action Engine** is a core capability of SpringBase that allows developers to group multiple database operations into a single transaction. This ensures that either all operations succeed or none of them do, preventing partial data updates.

### Why use Atomic Actions?
- **Financial Transfers**: Ensure money is deducted from one account and added to another simultaneously.
- **Order Processing**: Reduce inventory stock and create an order record in one go.
- **Complex Updates**: Update multiple related tables while maintaining referential integrity.

Access it via `POST /rest/v1/actions/execute`.

## Multi-party Stakeholder Security

SpringBase features a built-in **Smart-Policy** engine that automatically applies Row-Level Security (RLS) based on specific column names. This enables complex multi-party workflows without writing any security rules.

### Industry Examples
*   **Healthcare (Doctor/Patient)**: A record with `user_id` (Patient) and `merchant_id` (Doctor) is automatically visible to both parties, but invisible to other patients or doctors.
*   **Ride-Hailing (Driver/Rider)**: A `ride` record containing `client_id` (Rider) and `owner_id` (Driver) ensures both can track the trip status securely.
*   **Marketplaces (Seller/Buyer)**: An `order` with `customer_id` and `vendor_id` allows both to manage the transaction.

### Stakeholder Columns
- `user_id`: The primary owner of the record.
- `owner_id`: Alternative owner identifier.
- `merchant_id`: The service provider or merchant associated with the record.
- `client_id`: The client or customer associated with the record.
- `shared_with_id`: An additional collaborator who has access.
- `company_id`: The organization or company the record belongs to.
- `tenant_id`: The tenant or workspace the record belongs to.

The engine uses an `OR` logic: `WHERE user_id = :uid OR owner_id = :uid OR ...`. 
General ID columns like `product_id` or `category_id` are **ignored** by the security engine to prevent false-positive filters.

## Documentation

- [**SDK Guide (Dashboard)**](src/main/resources/static/index.html) - Interactive guide built into the app.
- [**Interactive API Docs (Swagger)**](http://localhost:1890/swagger-ui.html) - Full OpenAPI specification.
- [BRD](BRD_SUPA_SPRING.md)
- [SRS](SRS_SUPA_SPRING.md)
- [Platform Comparison (Springbase vs. Supabase)](PLATFORM_COMPARISON.md)

## Technical Specifications

- **Framework**: Spring Boot 3.5+
- **Database**: H2 (Postgres Mode)
- **Cache**: Caffeine
- **Port**: 1890
