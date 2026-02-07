# SupaSpring (SpringBase)

SupaSpring is a high-performance, open-source alternative to Supabase, built entirely using the latest Spring Boot (Java) ecosystem.

## Project Status

- [x] Project Initialization (Spring Boot 3.5.10, Java 21)
- [x] Auth Module (Email/Password, JWT, Multi-tenancy)
- [x] Dynamic DB Engine (Auto-CRUD)
- [x] Smart-Policy Security & Stakeholder Columns
- [x] AI-First Workflow (SupaShell & JSON-to-DDL)

## Smart-Policy Security

SpringBase features a built-in **Smart-Policy** engine that automatically applies Row-Level Security (RLS) based on specific column names. When a table contains any of the following "Stakeholder Columns", the engine automatically filters queries so that users can only see records where they are a stakeholder.

### Stakeholder Columns
- `user_id`: The primary owner of the record.
- `owner_id`: Alternative owner identifier.
- `merchant_id`: The service provider or merchant associated with the record.
- `client_id`: The client or customer associated with the record.
- `shared_with_id`: An additional collaborator who has access.

The engine uses an `OR` logic: `WHERE user_id = :uid OR owner_id = :uid OR ...`. 
General ID columns like `product_id` or `category_id` are **ignored** by the security engine to prevent false-positive filters, ensuring that global resources remain accessible.

## Public (Read-Only) Access

Tables can be marked as **Public (Read-Only)** in the Dashboard or via the Schema Editor. When enabled:
- Anonymous users (using the `anon` key) can read all data from the table.
- Ownership-based security filters are bypassed for read operations.
- Write operations (Insert/Update/Delete) still require authentication and follow ownership rules.

## Technical Specifications

- **Framework**: Spring Boot 3.5+
- **Database**: H2 (Postgres Mode)
- **Cache**: Caffeine
- **Port**: 1890

## Dashboard

- **Web Dashboard**: A no-code-friendly visual interface is available at the root URL: `http://localhost:1890/`.
  - **Landing Page**: Overview and links to documentation.
  - **Authentication**: Easy login and signup.
  - **Table Browser**: View and manage your data visually.
  - **Table Creator**: Define new tables without writing SQL.
  - **SQL Console**: Run raw SQL queries via SupaShell.

## Documentation

- [BRD](BRD_SUPA_SPRING.md)
- [SRS](SRS_SUPA_SPRING.md)
- **Interactive API Docs**: Enhanced Swagger (SpringDoc) UI is available at `http://localhost:1890/swagger-ui.html`. 
  - Exhaustive details for Auth, Dynamic DB, and Admin APIs.
  - Comprehensive parameter, response, and error code (400, 401, 403, 404, 500) documentation.
  - Example payloads for AI-assisted schema generation and CRUD operations.
