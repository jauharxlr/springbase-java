# Platform Comparison: Springbase vs. Supabase

This document provides a comparative audit of the current **Springbase** implementation against **Supabase** features. The goal is to evaluate developer experience parity and identify architectural differences.

## 1. Feature Parity Matrix

| Feature Category | Supabase | Springbase | Status |
| :--- | :--- | :--- | :--- |
| **Database** | PostgreSQL | PostgreSQL (via JDBC) | ✅ Matched |
| **Auto-CRUD API** | PostgREST | `RestApiController` | ✅ Matched |
| **Auth** | GoTrue (OAuth, etc.) | Custom JWT Auth | ⚠️ Basic |
| **Security** | Database RLS | App-level "Smart-Policy" | 🔄 Emulated |
| **SQL Editor** | Supabase Dashboard | SupaShell Admin API | ✅ Matched |
| **Filtering** | Extensive (PostgREST) | Basic (eq, gt, like, etc.) | ⚠️ Partial |
| **Realtime** | Realtime Server (Walrus) | Not implemented | ❌ Missing |
| **Storage** | Object Storage | Not implemented | ❌ Missing |
| **Edge Functions** | Deno-based | Not implemented | ❌ Missing |
| **RPC** | `/rest/v1/rpc/...` | Not implemented | ❌ Missing |
| **AI Workflows** | Supabase AI | JSON-to-DDL Engine | ✨ Unique |

---

## 2. Architectural Differences

### Security Model
- **Supabase**: Uses native PostgreSQL **Row Level Security (RLS)**. Policies are defined in SQL and enforced by the database engine.
- **Springbase**: Uses an application-layer **"Smart-Policy"** engine. It intercepts requests and automatically appends `WHERE` clauses (e.g., `user_id = :auth_uid`) based on the authenticated user's context. 
  - *Advantage*: Easier to debug for Java developers; works on databases without native RLS support.
  - *Disadvantage*: Bypassed if accessing the database directly via SQL.

### Multi-Tenancy
- **Supabase**: Typically isolates tenants into separate PostgreSQL instances/projects.
- **Springbase**: Implements **Shared-Database Multi-Tenancy**. Projects are isolated via a `project_ref` attribute in metadata and table-level ownership.

### API Layer
- **Supabase**: Exposes PostgREST directly. Highly performant but rigid in terms of custom logic without writing PL/pgSQL.
- **Springbase**: Wraps database operations in a Spring Boot controller. 
  - *Advantage*: Allows for easy extension with Java-based middleware, logging, and complex business logic.

---

## 3. API Surface Comparison

### Supported Filtering Operators
Springbase currently supports a subset of PostgREST operators:

| Operator | PostgREST | Springbase | Description |
| :--- | :--- | :--- | :--- |
| `eq` | `?id=eq.1` | `?id=eq.1` | Equals |
| `neq` | `?id=neq.1` | `?id=neq.1` | Not Equals |
| `gt` | `?age=gt.20` | `?age=gt.20` | Greater Than |
| `gte` | `?age=gte.20` | `?age=gte.20` | Greater Than or Equal |
| `lt` | `?age=lt.20` | `?age=lt.20` | Less Than |
| `lte` | `?age=lte.20` | `?age=lte.20` | Less Than or Equal |
| `like` | `?name=like.*j*` | `?name=like.%j%` | Case-sensitive pattern |
| `ilike` | `?name=ilike.*j*` | `?name=ilike.%j%` | Case-insensitive pattern |

### Missing API Capabilities (Pending)
- **Advanced Select**: `?select=column1,nested(column2)` (Joining tables).
- **Ordering**: `?order=id.desc`.
- **Pagination**: `?limit=10&offset=20`.
- **Search**: `?q=search_term`.
- **In-Filter**: `?id=in.(1,2,3)`.

---

## 4. Audit Findings

### Strengths
- **Developer Experience**: The `RestApiController` provides a near-identical experience to the Supabase JS client's REST calls.
- **AI Readiness**: The `/admin/v1/schema/apply` endpoint is a unique differentiator, allowing AI agents to define backends using high-level JSON rather than raw SQL.
- **Hybrid Auth**: Support for both `apikey` (anon/service_role) and Bearer JWT matches the Supabase header convention.

### Gaps
- **Realtime**: The lack of a WebSocket/Event-based system limits use cases for chat or live-dashboards.
- **Storage**: No built-in way to handle file uploads, forcing developers to use external services.
- **Database Schema**: All user tables currently reside in the `public` schema. Adding schema isolation (e.g., `project_ref` as schema name) would improve security.
