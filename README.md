# SupaSpring (SpringBase)

SupaSpring is a high-performance, open-source alternative to Supabase, built entirely using the latest Spring Boot (Java) ecosystem.

## Project Status

- [x] Project Initialization (Spring Boot 3.5.10, Java 21)
- [x] Auth Module (Email/Password, JWT, Multi-tenancy)
- [x] Dynamic DB Engine (Auto-CRUD)
- [x] Ownership-based Security
- [x] AI-First Workflow (SupaShell & JSON-to-DDL)

## Technical Specifications

- **Framework**: Spring Boot 3.5+
- **Database**: H2 (Postgres Mode)
- **Cache**: Caffeine
- **Port**: 1890

## Documentation

- [BRD](BRD_SUPA_SPRING.md)
- [SRS](SRS_SUPA_SPRING.md)
- **Interactive API Docs**: Enhanced Swagger (SpringDoc) UI is available at `http://localhost:1890/swagger-ui.html`. 
  - Exhaustive details for Auth, Dynamic DB, and Admin APIs.
  - Comprehensive parameter, response, and error code (400, 401, 403, 404, 500) documentation.
  - Example payloads for AI-assisted schema generation and CRUD operations.
