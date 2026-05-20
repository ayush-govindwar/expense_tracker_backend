# Expense Tracker API

Backend for a personal expense tracker. Spring Boot REST API with JWT auth, **Supabase** (PostgreSQL), and a React-friendly JSON contract.

**Base URL (local):** `http://localhost:8081`

---

## Tech stack

- **Java 17**, **Spring Boot 3.3** (Web, Security, Data JPA, Validation)
- **Supabase** — hosted **PostgreSQL** in production (`supabase` profile); local Postgres optional for dev
- **JWT** (jjwt) — stateless auth, BCrypt passwords
- **OpenPDF** — monthly PDF export
- **Maven** — build and run

Profiles: `local`, `supabase`, `test`. Config can load from `.env` (see `application.yml`).

---

## What it does

**Auth** — Signup/login returns an access token (~15 min) and a refresh token (~7 days). Refresh without logging in again; logout blacklists the current access token.

**Expenses** — CRUD with pagination. List filters by category, date range, and amount. Search adds text on comments, category name, and payment method (JPA Specifications under the hood).

**Categories** — Global defaults (Food, Transport, etc.) plus user-created categories. Defaults can’t be edited or deleted.

**Budgets** — Per-category or overall monthly caps. Responses include spent amount and % used. `/alerts` returns MEDIUM (60%+), HIGH (80%+), EXCEEDED (100%+).

**Reports** — Category breakdown with percentages, monthly totals, daily trend. **PDF export** is a 3-page record (summary, budget vs spend by category, transaction list) branded **Iauro Finance**.

**Recurring** — Mark an expense as recurring (`MONTHLY` / `WEEKLY` / `DAILY`). A nightly job copies templates into real expenses for that day (`[Auto]` in comments).

---

## Run locally

```bash
# needs JWT_SECRET and DB vars in .env
mvn spring-boot:run
```

Default port: **8081** (or `PORT` in production).

Docker:

```bash
docker build -t expense-tracker .
docker run -p 8081:8081 --env-file .env expense-tracker
```

---

## Auth

Public: `POST /api/auth/signup`, `login`, `refresh`.

Everything else needs:

```http
Authorization: Bearer <accessToken>
```

Refresh body: `{ "refreshToken": "..." }`.

---

## API routes

### Auth — `/api/auth`

| Method | Path | Auth | Notes |
|--------|------|------|--------|
| POST | `/signup` | No | Body: `name`, `email`, `password`, optional `currency` → **201**, tokens |
| POST | `/login` | No | Body: `email`, `password` → tokens |
| POST | `/refresh` | No | Body: `refreshToken` → new tokens |
| POST | `/logout` | Yes | **204**, invalidates access token |

**Token response:** `accessToken`, `refreshToken`, `tokenType` (`Bearer`), `expiresIn` (ms).

---

### User — `/api/user`

| Method | Path | Auth | Notes |
|--------|------|------|--------|
| GET | `/profile` | Yes | `id`, `name`, `email`, `currency` |

---

### Categories — `/api/categories`

| Method | Path | Auth | Notes |
|--------|------|------|--------|
| GET | `/` | Yes | All defaults + user categories |
| POST | `/` | Yes | Body: `name`, optional `icon` → **201** |
| PUT | `/{id}` | Yes | Update name/icon |
| DELETE | `/{id}` | Yes | **204** (not allowed for defaults) |

---

### Expenses — `/api/expenses`

| Method | Path | Auth | Notes |
|--------|------|------|--------|
| GET | `/` | Yes | Paginated list. Query: `categoryId`, `from`, `to` (ISO date), `minAmount`, `maxAmount`, `page`, `size`, `sort` (default `createdAt,desc`) |
| GET | `/search` | Yes | Same pagination + `q` (comments), `category` (name), `from`, `to`, `minAmount`, `maxAmount`, `paymentMethod` |
| GET | `/{id}` | Yes | Single expense |
| POST | `/` | Yes | **201**. Body: `categoryId`, `amount`, optional `comments`, `expenseDate`, `paymentMethod`, `upiRefId`, `recurring`, `recurrenceType` (`MONTHLY` \| `WEEKLY` \| `DAILY`) |
| PUT | `/{id}` | Yes | Same body shape as create |
| DELETE | `/{id}` | Yes | **204** |

**Paged response:** `content`, `page`, `size`, `totalElements`, `totalPages`, `last`.

---

### Budgets — `/api/budgets`

| Method | Path | Auth | Notes |
|--------|------|------|--------|
| GET | `/` | Yes | Query: `month`, `year` (default: current). List with `spentAmount`, `percentageUsed`, `overall` |
| GET | `/alerts` | Yes | Same `month`/`year`. Alert levels when spend ≥ 60% |
| POST | `/` | Yes | **201**. Body: `amount`, optional `categoryId` (null = overall budget), `month`, `year` |
| PUT | `/{id}` | Yes | Update budget |
| DELETE | `/{id}` | Yes | **204** |

---

### Reports — `/api/reports`

| Method | Path | Auth | Notes |
|--------|------|------|--------|
| GET | `/category-summary` | Yes | Query: `month`, `year`. `totalSpend` + per-category `amount`, `percentage` |
| GET | `/monthly-summary` | Yes | All months with totals |
| GET | `/daily-trend` | Yes | Query: `month`, `year`. Per-day totals (only days with spend) |
| GET | `/export/pdf` | Yes | Query: `month`, `year`. **PDF** download (`application/pdf`) |

---

## Errors

Most failures return JSON:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Human-readable message"
}
```

Common codes: **400** validation, **401** auth, **404** not found, **409** conflict (e.g. duplicate email or budget).

---

## Environment (typical)

| Variable | Purpose |
|----------|---------|
| `JWT_SECRET` | Required. 32+ chars |
| `SPRING_PROFILES_ACTIVE` | `local` or `supabase` |
| `SUPABASE_DB_HOST` / `SUPABASE_DB_PASS` | Supabase Postgres |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated frontend URLs |
| `PORT` | Set by Render/Heroku; app binds to it |

---

