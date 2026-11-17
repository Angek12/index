import { useAuth } from '../auth.jsx';
import { useT } from '../i18n.jsx';
import { PageHeader } from '../ui.jsx';

const GUIDE = {
  SHOP: [
    'Open Sales and record what you sold. Use quick entry to fill in many crops at once.',
    'Open Produce requests and ask for the crop, quantity and date you need.',
    'The Index team matches your request with a farmer. You get a notification and can see the farmer on the order.',
    'When the goods arrive, open Orders and press Confirm received. Mark the order as paid when you pay.',
    'After a few weeks of sales, open Forecast to see the expected demand for next week.',
    'You can also order straight from a farmer: open Marketplace, choose a listing and send an order. The farmer confirms it.',
    'Your order shows a 4-digit delivery code. Give it to the farmer only when you receive the goods.',
    'For goods you need every week, open Repeating requests and set it once.',
  ],
  FARMER: [
    'Open My supply and list what you can deliver: crop, quantity, price and dates. Add a photo and a quality grade to stand out.',
    'The Dashboard shows what shops are asking for right now.',
    'When an order is matched to you, open Orders and press Confirm.',
    'After you deliver, press Mark delivered and enter the transport cost you paid.',
    'When the shop pays you, press Confirm payment received.',
    'When you hand over the goods, ask the shop for its 4-digit delivery code and enter it when you press Mark delivered.',
  ],
  ADMIN: [
    'Review new accounts under Users and approve the ones you know.',
    'Open Match requests. For each request press Find supply, compare farmers, then create the order.',
    'One request can be filled by several farmers. The remaining amount stays open.',
    'Follow Orders and chase anything stuck in Matched or Confirmed.',
    'Read Reports every week and download them for your records.',
    'Check Problems, the KPI dashboard and the Support inbox every day. The dashboard lists what needs your attention.',
  ],
};

const FAQ = [
  ['Does Index handle payments?', 'No. Index only records whether an order was paid and received. Payment is arranged directly between the shop and the farmer.'],
  ['I forgot my password.', 'On the sign-in page press Forgot your password. You get a code by SMS, or the Index team can give you a code.'],
  ['What if I have no internet?', 'Sales you save are kept on your device and sent when you are online again.'],
  ['Why can I not see the other side\'s phone number?', 'Phone numbers are shown only after an order is matched, and hidden again if the order is cancelled or fails.'],
  ['How do stars and reliability work?', 'After an order is completed or failed, both sides can give 1 to 5 stars. Reliability is the share of finished orders that were completed.'],
  ['Can I use Index in Kinyarwanda?', 'Yes. Use the language box at the top of the page. English stays the default until you choose.'],
  ['What is the delivery code?', 'It is a 4-digit code the shop sees on its order. The farmer must enter it to mark the order as delivered, which proves the goods reached the shop.'],
  ['How do I report a problem with an order?', 'Open Orders, press Problems on the order, and describe what went wrong. The other side and the Index team are told and the team will decide.'],
  ['How do I contact the Index team?', 'Open Support in the menu and write a message. You get a notification when the team replies.'],
  ['How do repeating requests work?', 'You choose the crop, quantity and how often. Index sends the request automatically a few days before each date, and you can pause or delete it at any time.'],
  ['I have no smartphone.', 'Farmers can use Index by USSD from any phone, when the Index team has switched it on for your network.'],
];

export default function Help() {
  const { user } = useAuth();
  const { t } = useT();
  return (
    <>
      <PageHeader title={t('Help')} subtitle={t('Quick guide for your role')} />
      <div className="card mb-6">
        <ol className="list-decimal space-y-2 pl-5 text-sm">
          {GUIDE[user.role].map((s) => <li key={s}>{t(s)}</li>)}
        </ol>
      </div>
      <h2 className="mb-2 font-semibold">{t('Common questions')}</h2>
      <div className="divide-y divide-stone-200 rounded-xl border border-stone-200 bg-white">
        {FAQ.map(([q, a]) => (
          <details key={q} className="group p-4">
            <summary className="cursor-pointer text-sm font-medium">{t(q)}</summary>
            <p className="mt-2 text-sm text-stone-600">{t(a)}</p>
          </details>
        ))}
      </div>
    </>
  );
}
