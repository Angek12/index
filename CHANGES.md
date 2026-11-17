# Version 3: what changed

Added on top of version 2. Existing data is kept (the database updates itself with `V3__trust_ops_market_kpi.sql`).

## 1. Trust and safety
- **Problem reports.** Shops and farmers report a problem on an order (short delivery, spoiled goods, wrong quality, late delivery, payment not received). The other side and the admins are told. Admins decide on the new **Problems** page and both sides get the decision.
- **Delivery confirmation code.** Each new order gets a 4-digit code. The shop sees it (and gets it by SMS when the farmer confirms). The farmer must enter it to mark the order delivered. After 5 wrong tries only an admin can confirm. Switch off with `REQUIRE_DELIVERY_CODE=false`.
- **Phone verification.** Optional SMS code at sign-up (`REQUIRE_PHONE_VERIFICATION=true`). Admins can also confirm a phone by hand on the Users page.
- **Terms and privacy notice** with a checkbox at sign-up (draft text, have a lawyer check it).
- **Activity log** of admin actions, **Add an admin** screen, and **login protection** (5 wrong passwords block the account for 15 minutes).

## 2. Reliability
- **Automatic tests.** New unit tests, a GitHub check on every push (`.github/workflows/ci.yml`), and a live smoke test (`tests/smoke-test.mjs`) that runs a full trade against your real backend.
- **Weekly backups.** `.github/workflows/backup.yml` saves the database every Sunday for 30 days (needs the `DATABASE_URL` secret). See `tests/README.md`.
- **Alerts.** A daily morning message to admins listing what needs attention (stuck orders, overdue deliveries, unpaid orders, open problems, waiting accounts, unanswered support). The admin dashboard shows the same list. Unexpected server errors also notify admins, at most once every 10 minutes.

## 3. Marketplace and repeating requests
- **Marketplace.** Shops browse available listings (photo, grade, price, stars, pickup point) and order directly. The stock is held and the farmer confirms or declines. Switch off with `MARKETPLACE_ENABLED=false`.
- **Repeating requests.** A shop sets crop, quantity and how often (weekly, every 2 weeks, monthly). Index sends the request automatically a few days before each date. Pause, resume or delete any time.

## 4. KPIs and support
- **KPI dashboard** (admin): time to match, share of requests matched, on-time delivery, fulfilment, spoilage, payments confirmed, active and repeat shops and farmers, problem rate, marketplace share, orders per week, and a table by district showing supply gaps. Print or save as PDF.
- **Support inbox.** Every user can write to the Index team from Support and gets a notification on the reply. Admins see all conversations and answer them.

## How to apply it
1. Replace `backend`, `frontend`, `tests` and `.github` in your project with the ones in this zip, plus `CHANGES.md` and `README.md`. Commit and push.
2. Render rebuilds. In the logs look for `now at version v3` and `Started IndexApplication`.
3. For backups, add the GitHub secret `DATABASE_URL` (see `tests/README.md`).
4. Run the smoke test once the new version is live.

# What changed in this version

All of these are added on top of the previous Index web platform. Existing data is kept.

## Before real users
1. **Contact details on matched orders.** Phone, name and location of the other side, with Call and WhatsApp buttons. Hidden again if the order is cancelled or fails. Admins see both sides.
2. **Alerts.** A bell in the app for everyone, plus optional SMS (Africa's Talking) and WhatsApp (Twilio). Messages go out in each person's own language. Without provider keys the app only logs the message, so nothing breaks.
3. **Kinyarwanda.** English stays the default. A language box (top of every page, and on the sign-in page) lets people choose Kinyarwanda. The choice is saved on the device and in the account, so SMS use the same language. Please have a native speaker review `frontend/src/rw.js`.
4. **Password reset.** By SMS code (needs SMS keys), or the admin presses Reset code on the Users page and gives the one-time code by phone. People can also change their password in Profile.
5. **Admin approval of new accounts.** New sign-ups wait on the Users page. Set `REQUIRE_APPROVAL=false` to switch this off.
6. **Payment status.** The shop marks an order as paid (cash, Mobile Money, bank, other, with a reference). The farmer confirms receipt. Index records this only and never moves money. Reports show payments confirmed and completed orders not yet paid.
7. **Edit.** Requests (while open) and sales can be edited.

## During the pilot
8. **Partial fulfilment.** One request can be filled by several farmers. Requests show matched and remaining quantity, and the admin sees only what is still needed.
9. **Quick sales entry and repeat requests.** One screen for many crops, prices pre-filled from the last sale. One tap to repeat a past request on a new date.
10. **Delivery method.** Farmer delivers, shop collects, shared transport, who pays transport, and a pickup point.
11. **Photos and quality.** Photo (shrunk in the browser), quality grade A to C, harvest date, minimum order and pickup point on listings.
12. **Ratings and reliability.** 1 to 5 stars after completed or failed orders, and a completion rate. Shown to the admin when matching.
13. **Help page.** A short guide and common questions for each role.

## Later items (first versions)
14. **Offline use and USSD.** The web app installs on a phone and opens without a connection. Sales saved offline are sent when the connection returns. A USSD menu lets farmers list produce, see demand and act on orders from any phone (needs a USSD code from a provider).
15. **Price trends and exports.** Weekly price charts per crop. Download orders, sales and reports as CSV files that open in Excel. Reports can be printed or saved as PDF.
16. **Team accounts.** A shop or cooperative owner can add team members who share the same records and orders.

## How to apply it
1. Replace the `backend` and `frontend` folders in your project with these, commit and push.
2. Render rebuilds. On start, the database is updated automatically with `V2__features.sql` and keeps all existing data.
3. Optional new Render variables are listed in `backend/.env.example`.
4. Vercel rebuilds the website. No new variables are needed.

## Not included
- Mobile Money payments (waiting for the legal review, as planned).
- The Android app screens for the new features. The existing app still works with the new backend, but new accounts it creates wait for approval.
