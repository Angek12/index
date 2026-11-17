import { createContext, useCallback, useContext, useState } from 'react';
import { RW } from './rw.js';

const Ctx = createContext({ lang: 'en', setLang: () => {}, t: (s) => s });

/** English is the default. Kinyarwanda is used only when the person chooses it. */
export function LangProvider({ children }) {
  const [lang, setLangState] = useState(() => (localStorage.getItem('index_lang') === 'rw' ? 'rw' : 'en'));

  const setLang = useCallback((l) => {
    const next = l === 'rw' ? 'rw' : 'en';
    localStorage.setItem('index_lang', next);
    document.documentElement.lang = next;
    setLangState(next);
  }, []);

  const t = useCallback(
    (s, vars) => {
      let out = lang === 'rw' && RW[s] ? RW[s] : s;
      if (vars) for (const k of Object.keys(vars)) out = out.split('{' + k + '}').join(vars[k]);
      return out;
    },
    [lang]
  );

  return <Ctx.Provider value={{ lang, setLang, t }}>{children}</Ctx.Provider>;
}

export const useT = () => useContext(Ctx);

export function LangSwitch({ onChange, className = '' }) {
  const { lang, setLang, t } = useT();
  return (
    <select
      aria-label={t('Language')}
      className={`input w-auto py-1.5 text-xs ${className}`}
      value={lang}
      onChange={(e) => {
        setLang(e.target.value);
        if (onChange) onChange(e.target.value);
      }}
    >
      <option value="en">English</option>
      <option value="rw">Ikinyarwanda</option>
    </select>
  );
}
