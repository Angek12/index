# Automatic checks

## 1. Unit tests (run by GitHub on every push)
`.github/workflows/ci.yml` runs the backend tests and builds the website. A red cross on GitHub means something broke.
Run them yourself: `cd backend` then `mvn test`.

## 2. Live smoke test
Runs a whole trade against your real backend: sign-up, approval, listing, request, matching, delivery code, payment, rating,
problem report, marketplace order, repeating request, support message, KPIs and exports.

```
node tests/smoke-test.mjs https://YOUR-BACKEND.onrender.com admin@index.rw "YourAdminPassword"
```
It needs Node 18 or newer. It creates accounts named smoke-...@test.rw and deactivates them at the end.

## 3. Weekly database backup
`.github/workflows/backup.yml` saves a copy of the database every Sunday for 30 days.
Setup: GitHub > your repository > Settings > Secrets and variables > Actions > New repository secret.
Name: `DATABASE_URL`. Value: your Neon connection string (the postgresql://... one).
Test it: Actions > Weekly database backup > Run workflow. Download the file from the run page under Artifacts.

To restore, create a NEW empty Neon database, then (with the PostgreSQL tools installed):
```
gunzip -c index-backup-DATE.sql.gz | psql "postgresql://user:password@host/newdb?sslmode=require"
```
