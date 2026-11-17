# Index Web Platform

```
index-web/
  frontend/   React 18 + Vite + Tailwind (web app)
  backend/    Spring Boot 3 + PostgreSQL (REST API, JWT, Flyway)
  docker-compose.yml
```

## Run everything with Docker
```
docker compose up --build
```
- Web app: http://localhost:5173
- API docs: http://localhost:8080/swagger-ui.html
- First admin: `admin@index.rw` / `ChangeMe123!` (set `ADMIN_EMAIL` / `ADMIN_PASSWORD`)

## Run for development
Backend (Java 17, Maven, PostgreSQL):
```
createdb index_db
cd backend && mvn spring-boot:run
```
Frontend (Node 18+):
```
cd frontend
cp .env.example .env
npm install
npm run dev
```

## Deploy (free tiers)
1. Database: Neon. Build `DB_URL` as `jdbc:postgresql://<host>/<db>?sslmode=require`.
2. Backend: Render, Root Directory `backend`, Runtime Docker. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `CORS_ORIGINS`.
3. Frontend: Vercel, Root Directory `frontend`, env `VITE_API_URL=<Render URL>` (no trailing slash).
4. Set `CORS_ORIGINS` on Render to your Vercel URL and redeploy.

Endpoints and the order flow are documented in `backend/README.md`.


## New features
See `CHANGES.md` for everything added in this version (alerts, Kinyarwanda, password reset, account approval,
payment status, partial fulfilment, photos, ratings, team accounts, USSD, offline use, exports and price trends).


Version 3 added problem reports, delivery codes, the marketplace, repeating requests, KPIs, a support inbox, alerts, tests and backups. See the top of `CHANGES.md`.
