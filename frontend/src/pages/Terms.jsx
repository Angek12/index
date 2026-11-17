import { Link } from 'react-router-dom';
import { LangSwitch, useT } from '../i18n.jsx';

/* Plain-language draft. Have a lawyer check it against Rwanda's data protection law before launch. */
export default function Terms() {
  const { t } = useT();
  return (
    <div className="min-h-screen bg-stone-50 p-4">
      <div className="card mx-auto max-w-2xl space-y-4 p-6 text-sm leading-relaxed text-stone-700">
        <div className="flex justify-end"><LangSwitch /></div>
        <h1 className="text-2xl font-bold text-green-800">{t('Terms and privacy notice')}</h1>

        <h2 className="font-semibold text-stone-900">What Index is</h2>
        <p>Index helps grocery shops and farmers in Rwanda find each other, agree orders and follow deliveries. Index is a tool for coordination. The shop and the farmer make the deal between themselves.</p>

        <h2 className="font-semibold text-stone-900">Payments</h2>
        <p>Index does not hold or move money. It only records whether an order was paid and received. Payment is arranged directly between the shop and the farmer.</p>

        <h2 className="font-semibold text-stone-900">Your information</h2>
        <p>We keep the details you give us: name, email, phone number, business name and place, sales, requests, listings, orders, ratings and messages. We use them to run the service, to match shops with farmers, to send you alerts, and to prepare anonymous summaries for the pilot.</p>

        <h2 className="font-semibold text-stone-900">Who can see what</h2>
        <p>After an order is matched, the shop and the farmer on that order can see each other's name, phone number and place. Index administrators can see all records to help with matching and problems. Other shops and farmers cannot see your phone number or your private records. Produce photos and listings are visible to signed-in shops.</p>

        <h2 className="font-semibold text-stone-900">Your choices</h2>
        <p>You can change your details and your alert settings in Profile. You can ask the Index team to correct or delete your account by writing to us from the Support page.</p>

        <h2 className="font-semibold text-stone-900">Using Index fairly</h2>
        <p>Give true information, deliver what you agree, and report problems honestly. Accounts that cheat or abuse other users can be stopped.</p>

        <h2 className="font-semibold text-stone-900">Pilot stage</h2>
        <p>Index is in a pilot. Features and these terms may change, and we will tell you about important changes.</p>

        <p className="pt-2"><Link to="/register" className="font-medium text-green-700 underline">{t('Back to sign up')}</Link></p>
      </div>
    </div>
  );
}
