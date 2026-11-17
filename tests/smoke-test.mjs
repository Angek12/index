// End-to-end check of a running Index backend.
// Usage: node tests/smoke-test.mjs https://your-backend.onrender.com admin@example.rw 'AdminPassword'
// It creates test accounts (named smoke-...@test.rw) and runs one full trade from request to payment.

const [base, adminEmail, adminPassword] = process.argv.slice(2);
if (!base || !adminEmail || !adminPassword) {
  console.log("Usage: node tests/smoke-test.mjs <backend-url> <admin-email> <admin-password>");
  process.exit(2);
}
const API = base.replace(/\/$/, '');
const stamp = Date.now();
let passed = 0, failed = 0;

async function call(method, path, token, body) {
  const res = await fetch(API + path, {
    method,
    headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  return { status: res.status, data };
}

function check(name, ok, extra = '') {
  if (ok) { passed++; console.log(`  PASS  ${name}`); }
  else { failed++; console.log(`  FAIL  ${name} ${extra}`); }
}
const must = (r, name) => { check(name, r.status >= 200 && r.status < 300, `(HTTP ${r.status} ${JSON.stringify(r.data)?.slice(0, 160)})`); return r.data; };
const isoDay = (n) => new Date(Date.now() + n * 864e5).toISOString().slice(0, 10);

async function makeUser(admin, role, label) {
  const email = `smoke-${label}-${stamp}@test.rw`;
  const reg = await call('POST', '/api/auth/register', null, {
    email, password: 'Smoke12345!', fullName: `Smoke ${label}`, phone: `+2507${String(stamp).slice(-8)}${role === 'SHOP' ? '1' : '2'}`.slice(0, 13),
    role, businessName: `Smoke ${label} Ltd`, district: 'Gasabo', acceptTerms: true, language: 'en' });
  must(reg, `register ${label}`);
  const id = reg.data?.user?.id;
  if (!reg.data?.token) {
    must(await call('PATCH', `/api/admin/users/${id}/verify-phone`, admin), `confirm phone of ${label}`);
    must(await call('PATCH', `/api/admin/users/${id}/approve`, admin), `approve ${label}`);
  }
  const login = await call('POST', '/api/auth/login', null, { email, password: 'Smoke12345!' });
  must(login, `sign in ${label}`);
  return { id, email, token: login.data?.token };
}

console.log(`Testing ${API}\n`);

const health = await call('GET', '/actuator/health');
check('backend is up', health.status === 200 && health.data?.status === 'UP');

const adminLogin = await call('POST', '/api/auth/login', null, { email: adminEmail, password: adminPassword });
const admin = adminLogin.data?.token;
check('admin signs in', !!admin, `(HTTP ${adminLogin.status})`);
if (!admin) { console.log('\nCannot continue without the admin.'); process.exit(1); }

console.log('\nAccounts');
const shop = await makeUser(admin, 'SHOP', 'shop');
const farm = await makeUser(admin, 'FARMER', 'farm');
check('role protection: shop cannot read admin users', (await call('GET', '/api/admin/users', shop.token)).status === 403);
check('no token gives 401', (await call('GET', '/api/orders')).status === 401);

console.log('\nTrade');
const crops = must(await call('GET', '/api/crops', shop.token), 'list crops') || [];
const crop = crops[0];
const listing = must(await call('POST', '/api/supplies', farm.token, { cropId: crop.id, quantityAvailable: 60, pricePerUnit: 800, availableFrom: isoDay(0), qualityGrade: 'A', pickupPoint: 'Market gate' }), 'farmer lists 60');
const demand = must(await call('POST', '/api/demands', shop.token, { cropId: crop.id, quantity: 100, neededBy: isoDay(5) }), 'shop requests 100');
const sug = must(await call('GET', `/api/orders/suggestions?demandId=${demand.id}`, admin), 'admin sees suggestions');
check('suggestion contains the new listing', (sug || []).some((s) => s.supply.id === listing.id));
const order = must(await call('POST', '/api/orders', admin, { demandId: demand.id, supplyId: listing.id, quantity: 60 }), 'admin matches 60');
const mine = must(await call('GET', '/api/demands/mine', shop.token), 'shop lists requests');
const mineDemand = (mine || []).find((d) => d.id === demand.id);
check('request is partly matched with 40 left', mineDemand?.status === 'PARTIAL' && Number(mineDemand?.remainingQuantity) === 40);
must(await call('PATCH', `/api/orders/${order.id}/status`, farm.token, { status: 'CONFIRMED' }), 'farmer confirms');
const shopView = must(await call('GET', `/api/orders/${order.id}`, shop.token), 'shop opens the order');
check('shop sees farmer phone and delivery code', !!shopView?.farmerPhone && (shopView?.deliveryCode === null || /^\d{4}$/.test(shopView?.deliveryCode || '')));
const farmView = must(await call('GET', `/api/orders/${order.id}`, farm.token), 'farmer opens the order');
check('farmer does not see the delivery code', farmView?.deliveryCode == null);
if (shopView?.deliveryCode) {
  check('wrong delivery code is refused', (await call('PATCH', `/api/orders/${order.id}/status`, farm.token, { status: 'DELIVERED', deliveryCode: '0000x' })).status === 400);
}
must(await call('PATCH', `/api/orders/${order.id}/status`, farm.token, { status: 'DELIVERED', transportCost: 5000, deliveryCode: shopView?.deliveryCode || null }), 'farmer delivers with the code');
must(await call('PATCH', `/api/orders/${order.id}/status`, shop.token, { status: 'COMPLETED', spoiledQuantity: 1 }), 'shop confirms receipt');
must(await call('PATCH', `/api/orders/${order.id}/payment`, shop.token, { action: 'MARK_PAID', method: 'MOBILE_MONEY', reference: 'TEST' }), 'shop marks paid');
check('shop cannot confirm its own payment', (await call('PATCH', `/api/orders/${order.id}/payment`, shop.token, { action: 'CONFIRM_RECEIVED' })).status === 403);
must(await call('PATCH', `/api/orders/${order.id}/payment`, farm.token, { action: 'CONFIRM_RECEIVED' }), 'farmer confirms payment');
must(await call('POST', `/api/orders/${order.id}/rating`, shop.token, { stars: 5, comment: 'Good' }), 'shop rates farmer');
check('second rating is refused', (await call('POST', `/api/orders/${order.id}/rating`, shop.token, { stars: 4 })).status === 409);

console.log('\nProblem report');
const issue = must(await call('POST', `/api/orders/${order.id}/issues`, shop.token, { type: 'SPOILED', description: 'One crate was bad', quantityAffected: 1 }), 'shop reports a problem');
check('admin sees the open problem', ((await call('GET', '/api/admin/issues?status=OPEN', admin)).data || []).some((i) => i.id === issue?.id));
must(await call('PATCH', `/api/admin/issues/${issue.id}`, admin, { status: 'RESOLVED', resolution: 'Agreed a small discount' }), 'admin resolves it');

console.log('\nMarketplace');
const listing2 = must(await call('POST', '/api/supplies', farm.token, { cropId: crop.id, quantityAvailable: 30, pricePerUnit: 700, availableFrom: isoDay(0) }), 'farmer lists 30 more');
const market = must(await call('GET', '/api/marketplace/listings', shop.token), 'shop browses the marketplace');
check('new listing is on the marketplace', (market || []).some((l) => l.id === listing2.id));
const mOrder = must(await call('POST', '/api/marketplace/orders', shop.token, { supplyId: listing2.id, quantity: 20, deliveryDate: isoDay(3) }), 'shop orders directly');
check('marketplace order starts as matched', mOrder?.status === 'MATCHED' && mOrder?.source === 'MARKETPLACE');
must(await call('PATCH', `/api/orders/${mOrder.id}/status`, farm.token, { status: 'CANCELLED', note: 'Smoke test decline' }), 'farmer declines it');
const back = (must(await call('GET', '/api/supplies/mine', farm.token), 'farmer lists own supply') || []).find((l) => l.id === listing2.id);
check('stock returned after the decline', Number(back?.quantityAvailable) === 30);

console.log('\nRepeating request');
const before = (await call('GET', '/api/demands/mine', shop.token)).data?.length ?? 0;
must(await call('POST', '/api/standing-orders', shop.token, { cropId: crop.id, quantity: 10, frequency: 'WEEKLY', firstNeededDate: isoDay(1), leadDays: 2 }), 'shop sets a weekly repeating request');
const after = (await call('GET', '/api/demands/mine', shop.token)).data?.length ?? 0;
check('first request was created straight away', after === before + 1);

console.log('\nSupport');
const t = must(await call('POST', '/api/support/tickets', shop.token, { subject: 'Smoke test', category: 'QUESTION', message: 'Hello' }), 'shop writes to support');
must(await call('POST', `/api/support/tickets/${t.ticket.id}/messages`, admin, { body: 'Hi, this is a test' }), 'admin replies');
const detail = must(await call('GET', `/api/support/tickets/${t.ticket.id}`, shop.token), 'shop reads the reply');
check('conversation has two messages', detail?.messages?.length === 2);

console.log('\nAdmin tools');
const k = must(await call('GET', '/api/admin/kpis?days=30', admin), 'KPI dashboard loads');
check('KPIs count the new order', Number(k?.ordersCreated) >= 1);
must(await call('GET', '/api/admin/attention', admin), 'attention list loads');
check('activity log has entries', ((await call('GET', '/api/admin/audit', admin)).data || []).length > 0);
must(await call('GET', '/api/reports/pilot-economics', admin), 'pilot economics load');
check('CSV export works', (await fetch(API + '/api/export/orders.csv', { headers: { Authorization: `Bearer ${admin}` } })).status === 200);

console.log('\nClean up');
await call('PATCH', `/api/admin/users/${shop.id}/active?value=false`, admin);
await call('PATCH', `/api/admin/users/${farm.id}/active?value=false`, admin);

console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed ? 1 : 0);
