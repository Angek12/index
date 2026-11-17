# Index Backend (Spring Boot + PostgreSQL)

REST API for the Index platform: shops record sales and request produce, farmers list supply,
an admin matches them into orders, and reports show demand vs supply, forecasts and pilot economics.

## Run locally
1. Install Java 17+, Maven, PostgreSQL. Create the database: `createdb index_db`
2. Copy `.env.example` values into your environment (or edit `application.yml`).
3. `mvn spring-boot:run`  (Flyway creates all tables and seeds 7 crops; an admin is seeded on first start)
4. Swagger UI: http://localhost:8080/swagger-ui.html

Or with Docker: `docker compose up --build`

Default admin: `admin@index.rw` / `ChangeMe123!` (set ADMIN_EMAIL / ADMIN_PASSWORD, and change it after first login).

## Roles
SHOP, FARMER, ADMIN. Send `Authorization: Bearer <token>` from `/api/auth/login`.

## Endpoints
| Method | Path | Who |
|---|---|---|
| POST | /api/auth/register, /api/auth/login | public (register: SHOP or FARMER) |
| GET/PUT | /api/auth/me | any |
| GET | /api/crops | any |
| POST/PUT | /api/crops | ADMIN |
| POST/GET/DELETE | /api/sales | SHOP |
| POST | /api/demands | SHOP |
| GET | /api/demands/mine | SHOP |
| GET | /api/demands?status= | ADMIN |
| PATCH | /api/demands/{id}/cancel | SHOP (own), ADMIN |
| POST | /api/supplies | FARMER |
| GET | /api/supplies/mine | FARMER |
| GET | /api/supplies?status= | ADMIN |
| PUT | /api/supplies/{id}, PATCH /api/supplies/{id}/withdraw | FARMER (own), ADMIN |
| GET | /api/orders/suggestions?demandId= | ADMIN |
| POST | /api/orders | ADMIN (match demand + supply) |
| GET | /api/orders, /api/orders/{id}, /api/orders/{id}/history | participants, ADMIN |
| PATCH | /api/orders/{id}/status | participants (rules below) |
| GET | /api/market/demand | any (aggregated, no shop names) |
| GET | /api/forecast/crops/{cropId}?weeks=12&window=4 | SHOP (own data), ADMIN |
| GET | /api/reports/weekly-summary, /api/reports/pilot-economics | ADMIN |
| GET | /api/admin/users, /api/admin/dashboard | ADMIN |
| PATCH | /api/admin/users/{id}/active?value=false | ADMIN |

## Order flow
`MATCHED -> CONFIRMED -> DELIVERED -> COMPLETED`, or `FAILED` / `CANCELLED` (a reason note is required).
- Farmer or admin: CONFIRMED, DELIVERED, and transportCost
- Shop or admin: COMPLETED, and spoiledQuantity
- On FAILED/CANCELLED the reserved quantity returns to the farmer and the shop's request reopens
- Every change is stored in `order_status_history`

## Deploy (free tiers)
Database: Neon or Supabase (use the JDBC URL as `DB_URL`). Backend: Render or Railway using the Dockerfile.
Set `JWT_SECRET`, `ADMIN_PASSWORD`, `CORS_ORIGINS` (your frontend URL).

## New in this version
- Contact details (phone, location) on matched orders, visible only to the other side and the admin
- In-app notifications, plus optional SMS (Africa's Talking) and WhatsApp (Twilio) in English or Kinyarwanda
- Password reset by SMS code or admin-issued code, and change password
- Admin approval of new accounts (`REQUIRE_APPROVAL=false` switches it off)
- Payment status on orders (marked paid, confirmed received). Index records it and never moves money
- Edit requests and sales, quick sales entry, one-tap repeat requests
- Partial fulfilment: one request can be filled by several farmers
- Delivery method on orders, photo, quality grade, harvest date, minimum order and pickup point on listings
- Star ratings and reliability scores
- Price trends, CSV exports (open in Excel), team members for shops and cooperatives
- USSD menu for farmers without smartphones: point your provider's callback to `/api/ussd/{USSD_SECRET}`

### Optional environment variables
`REQUIRE_APPROVAL`, `AT_USERNAME`, `AT_API_KEY`, `AT_SENDER_ID`, `TWILIO_SID`, `TWILIO_TOKEN`, `TWILIO_WHATSAPP_FROM`, `USSD_SECRET`
