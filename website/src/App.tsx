import { useEffect, useRef, useState } from 'react';
import FeatureCatalog from './FeatureCatalog';

const REPO = 'https://github.com/fuckramochka/miogram';
type IconName = 'heart' | 'download' | 'github' | 'arrow' | 'shield' | 'sparkles' | 'palette' | 'send' | 'check' | 'menu' | 'close' | 'chevron' | 'plug' | 'star';
function Icon({ name, size = 20, className = '' }: { name: IconName; size?: number; className?: string }) {
  const paths: Record<IconName, React.ReactNode> = {
    heart: <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z" />,
    download: <><path d="M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5" /></>,
    github: <><path d="M9 19c-4 1-4-2-6-2m13 5v-4a3.5 3.5 0 0 0-1-3c3-.4 6-1.5 6-6a5 5 0 0 0-1.4-3.5A4.5 4.5 0 0 0 19.5 2S18 1.6 15 3a13 13 0 0 0-6 0C6 1.6 4.5 2 4.5 2a4.5 4.5 0 0 0-.1 3.5A5 5 0 0 0 3 9c0 4.5 3 5.6 6 6a3.5 3.5 0 0 0-1 3v4" /></>,
    arrow: <path d="M5 12h14m-6-6 6 6-6 6" />,
    shield: <><path d="m12 2 9 4v6c0 5-9 10-9 10S3 17 3 12V6l9-4Z" /><path d="m8 12 3 3 5-6" /></>,
    sparkles: <><path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3Z" /><path d="M21 2v4m-2-2h4" /></>,
    palette: <><path d="M12 3a9 9 0 1 0 0 18h1a2 2 0 0 0 1-3.7 1.7 1.7 0 0 1 1-3.3h2a4 4 0 0 0 4-4c0-4-4-7-9-7Z" /><path d="M7 10h.01M10 6h.01M15 7h.01M6 15h.01" /></>,
    send: <><path d="m22 2-7 20-4-9-9-4 20-7ZM22 2 11 13" /></>,
    check: <path d="m5 12 4 4L19 6" />,
    menu: <path d="M4 6h16M4 12h16M4 18h16" />,
    close: <path d="m6 6 12 12M18 6 6 18" />,
    chevron: <path d="m6 9 6 6 6-6" />,
    plug: <><path d="M8 2v5m8-5v5M5 7h14v3a7 7 0 0 1-14 0V7Zm7 10v5" /></>,
    star: <path d="m12 2 3 6.5 7 .9-5 5 1.2 7L12 18l-6.2 3.4 1.2-7-5-5 7-.9L12 2Z" />,
  };
  return <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}
function WingHeart({ className = '' }: { className?: string }) {
  return <svg viewBox="0 0 100 64" className={`wing-heart ${className}`} aria-hidden="true" shapeRendering="crispEdges"><path d="M4 13h8v6h8v6h15v23H23v-6H15v-7H8v-9H4Zm92 0h-8v6h-8v6H65v23h12v-6h8v-7h7v-9h4Z" fill="#fff9fd" stroke="#db8bab" strokeWidth="3"/><path d="M34 21h12v6h8v-6h12v6h6v16h-6v6h-7v6h-7v6h-5v-6h-7v-6h-7v-6h-6V27h7Z" fill="#f16fa4" stroke="#ae477c" strokeWidth="3"/><path d="M36 28h9v5h-9Z" fill="#ffcfe2"/><path d="M13 25h8v6h10m56-6h-8v6H69M18 35h13m51 0H69" fill="none" stroke="#e9b8cf" strokeWidth="3"/><path d="M44 42h3m8 0h3" stroke="#873b64" strokeWidth="3"/><path d="M39 5h22v4H39Z" fill="#edb969"/></svg>;
}
const features: { icon: IconName; title: string; text: string; tag: string; detail: string }[] = [
  { icon: 'palette', title: 'Твой стиль. Без компромиссов.', text: 'Discord, iOS, современный или классический ТГ. Темы, кастомные иконки и твои бейджики.', tag: 'make it yours', detail: 'MioGram — это не одна розовая тема. Выбирай визуал Discord, iOS, современного Telegram или старого доброго ТГ. Настраивай оформление, меняй иконку приложения и открывай пиксельные бейджи сообщества. Это стили интерфейса Android-клиента. На сайте ниже можно попробовать три палитры в демо.' },
  { icon: 'shield', title: 'Двойное дно. Личное пространство.', text: 'Двойное хранилище, отдельный PIN, режим призрака и сохранение удалённых сообщений.', tag: 'your private corner', detail: 'Двойное хранилище разделяет основное пространство и нейтральный экран, доступный по Duress PIN. Режим призрака добавляет контроль видимости активности, а сохранение удалённых помогает удержать уже полученную историю. Эти функции не обещают абсолютную анонимность или восстановление сообщений, которых клиент не получал. Настройки и ограничения зависят от сборки.' },
  { icon: 'plug', title: 'Больше фич. Больше свободы.', text: 'Плагины MioHook и exteraGram, уникальный музыкальный плеер, встроенный ИИ и умные новости.', tag: 'a little bit of magic', detail: 'Расширяй клиент плагинами систем MioHook и exteraGram, слушай музыку во встроенном плеере и используй ИИ-инструменты. Умные ИИ-новости, мультичат, подпапки, локализатор и встроенный поиск обновлений дополняют привычный Telegram. Совместимость расширений и доступность возможностей проверяй для своей версии.' },
];
const themes = [
  { name: 'Strawberry milk', note: 'Клубничное молоко и немного любви.', color: '#ed80ad', bg: '#fff0f6', bubble: '#f9cee0', slug: 'pink' },
  { name: 'Lavender dream', note: 'Для нежных снов и ночных разговоров.', color: '#a592d3', bg: '#f3efff', bubble: '#e1d6f7', slug: 'lavender' },
  { name: 'Minty angel', note: 'Глоток свежести в любимых чатах.', color: '#78b8a6', bg: '#eefaf5', bubble: '#ceeade', slug: 'mint' },
];
const faq = [
  ['Что такое MioGram?', 'MioGram — независимый Android-клиент на основе Telegram: двойное хранилище, плагины MioHook и exteraGram, глубокая кастомизация, собственный музыкальный плеер и встроенный ИИ. А ещё подпапки, мультичат, бейджики и много других возможностей. Это не официальный продукт Telegram.'],
  ['Это бесплатно?', 'Исходный код проекта открыт под лицензией GPL-3.0. Доступные сборки можно найти бесплатно на странице GitHub Releases. Подключаемые сторонние ИИ-сервисы могут иметь собственные тарифы и условия.'],
  ['Как установить MioGram?', 'Открой GitHub Releases, выбери актуальный релиз и скачай APK из раздела Assets. На Android разреши установку из выбранного источника, установи приложение и войди в свой аккаунт Telegram. Загружай файлы только из официального репозитория проекта.'],
  ['Мои чаты и стикеры останутся?', 'Облачные чаты, контакты и стикеры привязаны к аккаунту Telegram и синхронизируются при входе. Секретные чаты привязаны к устройству и не переносятся.'],
  ['Где предложить идею или сообщить об ошибке?', 'Создай обращение в разделе Issues репозитория. Для ошибки укажи версию клиента, модель устройства и шаги воспроизведения. Не прикладывай личные данные и коды входа.'],
  ['Какие плагины поддерживаются?', 'MioGram поддерживает плагины систем MioHook и exteraGram. Совместимость конкретного расширения зависит от версии плагина и клиента — поддержка системы не означает, что любой плагин будет работать без изменений. Проверяй источник, разрешения и документацию расширения.'],
  ['Как получать новые версии?', 'В клиенте есть поиск и установка обновлений. При установке Android может запросить подтверждение или разрешение. Историю изменений и сборки также можно найти в GitHub Releases.'],
  ['Будут ли ещё новые фичи?', 'Да! MioGram продолжит получать новые возможности — этот каталог не финальная точка. Следи за релизами и предлагай идеи в GitHub. Конкретные функции и сроки будут появляться по мере готовности, без обещаний неподтверждённых дат.'],
];

function App() {
  const [menu, setMenu] = useState(false);
  const [modal, setModal] = useState<'download' | number | null>(null);
  const [theme, setTheme] = useState(0);
  const [applied, setApplied] = useState(false);
  const [openFaq, setOpenFaq] = useState<number | null>(0);
  const [chat, setChat] = useState(0);
  const [message, setMessage] = useState('');
  const [sent, setSent] = useState<string[]>([]);
  const [liked, setLiked] = useState(false);
  const [toast, setToast] = useState('');
  const modalRef = useRef<HTMLDivElement>(null);
  const previousFocus = useRef<HTMLElement | null>(null);
  useEffect(() => { const stored = localStorage.getItem('miogram-demo-theme'); if (stored) { const n = Number(stored); if (n >= 0 && n < 3) setTheme(n); } }, []);
  useEffect(() => {
    if (modal === null) return;
    previousFocus.current = document.activeElement as HTMLElement;
    const old = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    modalRef.current?.querySelector<HTMLButtonElement>('button')?.focus();
    const listener = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setModal(null);
      if (e.key === 'Tab') {
        const nodes = modalRef.current?.querySelectorAll<HTMLElement>('button, a[href]');
        if (!nodes?.length) return;
        const first = nodes[0], last = nodes[nodes.length - 1];
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
      }
    };
    window.addEventListener('keydown', listener);
    return () => { document.body.style.overflow = old; window.removeEventListener('keydown', listener); previousFocus.current?.focus(); };
  }, [modal]);
  useEffect(() => { if (toast) { const timer = setTimeout(() => setToast(''), 3500); return () => clearTimeout(timer); } }, [toast]);
  const [stats, setStats] = useState<{ users: number; badges: number } | null>(null);
  useEffect(() => { let live = true; fetch('https://dbxsnjoeyiqvqtrluvwu.supabase.co/rest/v1/rpc/miogram_community_stats', { method: 'POST', headers: { apikey: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRieHNuam9leWlxdnF0cmx1dnd1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg1NDI1MzEsImV4cCI6MjEwNDExODUzMX0.KJ0kvON1HXZu4MzlZjapSJEhEzWYlEqQoNEstWCgIjA', 'Content-Type': 'application/json' }, body: '{}' }).then(r => r.json()).then(j => { if (live && j && typeof j.users_count === 'number') setStats({ users: j.users_count, badges: j.badges_count || 0 }); }).catch(() => {}); return () => { live = false; }; }, []);
  const sendMessage = (e: React.FormEvent) => { e.preventDefault(); if (!message.trim()) return; setSent(s => [...s.slice(-1), message.trim()]); setMessage(''); };
  return (
    <>
      <div className="announcement"><span>✧</span> маленький апгрейд твоего цифрового мира <span>♡</span> <a href={REPO + '/releases'} target="_blank" rel="noreferrer">встречай MioGram <span>↗</span></a></div>
      <header className="header container">
        <a className="brand" href="#" aria-label="MioGram — главная"><WingHeart /><span>mio<span>gram</span><i>✦</i></span></a>
        <nav className={menu ? 'nav open' : 'nav'} aria-label="Основная навигация">
          <a href="#features" onClick={() => setMenu(false)}>Возможности</a><a href="#themes" onClick={() => setMenu(false)}>Темы <span className="tiny-heart">♡</span></a><a href="#faq" onClick={() => setMenu(false)}>FAQ</a><a href={REPO} target="_blank" rel="noreferrer">GitHub <span className="external-arrow">↗</span></a>
        </nav>
        <button className="button small header-download" onClick={() => setModal('download')}><Icon name="download" size={16} /> Скачать <span>♡</span></button>
        <button className="mobile-menu icon-button" onClick={() => setMenu(!menu)} aria-label="Меню" aria-expanded={menu}><Icon name={menu ? 'close' : 'menu'} /></button>
      </header>
      <main>
        <section className="hero container">
          <div className="hero-copy">
            <div className="eyebrow"><span className="status-dot" /> TELEGRAM, BUT MAKE IT CUTE <span>✧</span></div>
            <h1>Твой Telegram.<br />Только <span className="sweet-word">няшнее.<svg viewBox="0 0 365 14" preserveAspectRatio="none" aria-hidden="true"><path d="M3 9Q150-2 359 7M25 13Q190 5 320 12" fill="none" stroke="currentColor" strokeWidth="3" /></svg></span><span className="heading-sparkle">✧</span></h1>
            <p className="hero-description">Знакомься, <strong>MioGram</strong> — милый снаружи,<br className="desktop-break" /> мощный внутри. Твой стиль, музыка, плагины и ИИ.<br className="desktop-break" /> Целый мир возможностей. И это только начало. <span>୨୧</span></p>
            <div className="hero-feature-chips"><a href="#all-features">▣ двойное хранилище</a><a href="#all-features">⌘ MioHook + exteraGram</a><a href="#all-features">все фичи ↓</a></div>
            <div className="hero-actions"><button className="button primary" onClick={() => setModal('download')}><Icon name="download" /> Скачать MioGram <span>♡</span></button><a className="button secondary" href={REPO} target="_blank" rel="noreferrer"><Icon name="github" /> Исходный код <span>↗</span></a></div>
            <div className="hero-notes"><span><Icon name="check" size={14} /> Бесплатно навсегда</span><span className="note-dot">·</span><span><Icon name="check" size={14} /> Открытый код</span><span className="note-dot">·</span><span>Для Android</span></div>
            {stats && <div className="hero-notes community-count" role="status"><span className="status-dot" /><span>{stats.users.toLocaleString()} members alive - {stats.badges.toLocaleString()} badges in the cloud</span></div>}
            <div className="love-note"><span className="tiny-avatars"><span>♡</span><span>✿</span><span>✧</span></span><p>Сделано с любовью. Для таких, как ты.</p><span className="hand-heart">↗ ♡</span></div>
          </div>
          <div className="hero-visual">
            <span className="floating-star star-one">✧</span><span className="floating-star star-two">✦</span><span className="floating-plus">+</span>
            <div className="sticker top-sticker">интернет, но уютный <span>♡</span></div>
            <div className="app-window" style={{ '--chat-bg': themes[theme].bg, '--chat-bubble': themes[theme].bubble } as React.CSSProperties}>
              <div className="window-bar"><span><WingHeart /> miogram.exe</span><div className="window-controls" aria-hidden="true"><span>−</span><span>□</span><span>×</span></div></div>
              <div className="app-toolbar"><span><Icon name="menu" size={15} /> MioGram <span className="app-version">♡</span></span><span className="demo-tag">live demo <span className="status-dot" /></span></div>
              <div className="app-body">
                <aside className="chat-sidebar"><div className="search-decoration">⌕ <span>Твои чатики...</span></div>{[['✿', 'Мио-чан', 'отправляет любовь ♡'], ['♡', 'Избранное', 'маленькие радости'], ['✦', 'уютный уголок', 'ты дома!'], ['☁', 'dream team', 'до завтра ~']].map((c, i) => <button className={`contact ${chat === i ? 'active' : ''}`} key={c[1]} onClick={() => { setChat(i); setSent([]); }}><span className={`avatar avatar-${i}`}>{c[0]}</span><span><strong>{c[1]}</strong><small>{c[2]}</small></span>{i === 0 && <i>2</i>}</button>)}<div className="sidebar-bottom">made with <span>♥</span></div></aside>
                <div className="chat-main"><div className="chat-title"><span><strong>{['Мио-чан ୨୧', 'Избранное ♡', 'уютный уголок ✦', 'dream team ☁'][chat]}</strong><small>{chat === 0 ? 'в сети и на твоей стороне' : 'твоё маленькое безопасное место'}</small></span><span>⋮</span></div><div className="chat-conversation"><div className="chat-date">сегодня</div>{sent.length === 0 ? <><div className="message received">{['хей! я так рада тебя видеть ♡', 'сохрани здесь что-то хорошее ♡', 'добро пожаловать домой ♡', 'у нас есть ты. значит, всё получится ♡'][chat]}<small>14:28</small></div><div className="mascot-message"><img src="/images/mio-chan.png" alt="Мио-чан — розововолосый аниме-ангел с наушниками" /><button className={`heart-reaction ${liked ? 'liked' : ''}`} onClick={() => setLiked(!liked)} aria-label={liked ? 'Убрать сердечко' : 'Отправить сердечко'} aria-pressed={liked}>♥ {liked ? '2' : '1'}</button></div><div className="message outgoing">здесь так уютно... остаюсь! <span>♡</span><small>14:29 ✓✓</small></div></> : <><div className="message received">Это демо твоего уютного чата ♡<small>сейчас</small></div><img className="mini-mascot" src="/images/mio-chan.png" alt="Мио-чан подмигивает" />{sent.map((m, i) => <div className="message outgoing" key={i}>{m}<small>сейчас ✓✓</small></div>)}</>}</div><form className="message-input" onSubmit={sendMessage}><span>☺</span><input aria-label="Демо-сообщение" placeholder="Напиши что-нибудь милое..." value={message} onChange={e => setMessage(e.target.value)} maxLength={100} /><button type="submit" aria-label="Отправить сообщение"><Icon name="send" size={17} /></button></form></div>
              </div>
              <div className="app-status"><span><span className="status-dot" /> всё хорошо. ты в MioGram.</span><span>♡ connected</span></div>
            </div>
            <div className="floating-card"><div className="floating-card-icon"><WingHeart /></div><div><strong>Твой маленький safe space</strong><span>никакого шума. только ты и твои люди.</span></div><span className="floating-card-sparkle">✧</span></div>
            <span className="bottom-doodle">be yourself, angel ♡</span><span className="pixel-flower">✿</span>
          </div>
        </section>
        <div className="marquee" aria-label="Создан для тебя"><div><span>♡</span> НЕЖНЫЙ ДИЗАЙН <span>✧</span> ТВОЯ ПРИВАТНОСТЬ <span>♡</span> НИКАКОЙ ЛИШНЕЙ РЕКЛАМЫ <span>✧</span> OPEN SOURCE <span>♡</span> СДЕЛАНО С ЛЮБОВЬЮ <span>✧</span> 100% ТВОЙ ВАЙБ <span>♡</span></div></div>
        <section className="features-section container section" id="features"><div className="section-heading"><div><div className="section-kicker">НЕ ПРОСТО ЕЩЁ ОДИН КЛИЕНТ</div><h2>Мягкий визуал. <span>Серьёзные возможности.</span></h2></div><a className="section-doodle" href="#all-features">все суперсилы ниже ↙</a></div><div className="feature-grid">{features.map((f, i) => <button className={`feature-card feature-${i}`} key={f.title} onClick={() => setModal(i)}><div className="feature-top"><span className="feature-icon"><Icon name={f.icon} size={25} /></span><span className="feature-tag">{f.tag}</span><span className="feature-sparkle">{i === 0 ? '✧' : i === 1 ? '♡' : '✦'}</span></div><h3>{f.title}</h3><p>{f.text}</p><span className="feature-link">Узнать больше <Icon name="arrow" size={16} /></span></button>)}</div></section>
        <FeatureCatalog />
        <section className="themes-section container section" id="themes"><div className="theme-copy"><div className="section-kicker">PERSONALITY.EXE ЗАПУЩЕН ♡</div><h2>Какой у тебя<br /><span>сегодня вайб?</span></h2><p>Начни с интерфейса: Discord, iOS, современный Telegram или старый добрый ТГ. Добавь любимую тему, свою иконку и бейджик — получится твой MioGram.</p><div className="layout-labels" aria-label="Варианты интерфейса клиента"><span>Discord</span><span>iOS</span><span>Telegram</span><span>Классический ТГ</span></div><div className="theme-options" aria-label="Темы оформления">{themes.map((t, i) => <button key={t.slug} className={`theme-option ${theme === i ? 'selected' : ''}`} onClick={() => { setTheme(i); setApplied(false); }} aria-pressed={theme === i}><span style={{ background: t.color }}>{theme === i && <Icon name="check" size={18} />}</span><div><strong>{t.name}</strong><small>{i === 0 ? 'классика нашего сердечка' : i === 1 ? 'мечтательный лавандовый' : 'свежий мятный'}</small></div>{theme === i && <span className="selected-heart">♡</span>}</button>)}</div><button className="button secondary apply-theme" onClick={() => { localStorage.setItem('miogram-demo-theme', String(theme)); setApplied(true); setToast('Тема сохранена для демо на этом устройстве ♡'); }}><Icon name={applied ? 'check' : 'palette'} size={17} />{applied ? 'Тема сохранена ♡' : 'Сохранить тему демо'}</button></div><div className="theme-preview-wrap"><span className="theme-preview-note">маленькие детали, большая любовь ✧</span><div className="theme-preview" style={{ '--preview-color': themes[theme].color, '--preview-bg': themes[theme].bg, '--preview-bubble': themes[theme].bubble } as React.CSSProperties}><div className="window-bar"><span>♡ theme_preview.exe</span><span>− &nbsp; □ &nbsp; ×</span></div><div className="preview-heading"><span className="preview-avatar">୨୧</span><div><strong>твой любимый человек</strong><small>печатает что-то хорошее...</small></div><Icon name="heart" size={21} /></div><div className="preview-chat"><span className="preview-date">немного нежности</span><div className="preview-message">как прошёл твой день? <span>♡</span><small>18:42</small></div><div className="preview-message right">теперь точно лучше!<small>18:42 ✓✓</small></div><div className="preview-heart">ʚ<span>♥</span>ɞ</div><div className="preview-message">{themes[theme].note}<small>18:43</small></div></div><div className="preview-bottom"><span>☺</span> Здесь начинается что-то хорошее... <Icon name="send" size={18} /></div></div><span className="preview-label"><span style={{ background: themes[theme].color }} />{themes[theme].name} <span>— preview</span></span></div></section>
        <section className="community container"><div className="community-art"><WingHeart /><span>✧</span></div><div><div className="section-kicker">ОТКРЫТЫЙ КОД. ОТКРЫТОЕ СЕРДЦЕ.</div><h2>Хорошие вещи создают вместе.</h2><p>Есть идея, нашёл баг или просто хочешь сказать «ня»?<br />Заглядывай. Здесь рады каждому.</p></div><a className="button secondary" href={REPO} target="_blank" rel="noreferrer"><Icon name="github" /> Мы на GitHub <span>↗</span></a></section>
        <section className="faq-section container section" id="faq"><div className="faq-intro"><div className="section-kicker">FAQ.TXT</div><h2>Есть вопрос?<br /><span>Сейчас обнимем.<br />И ответим.</span></h2><span className="faq-face">(づ｡◕‿‿◕｡)づ ♡</span></div><div className="faq-list">{faq.map(([q, a], i) => <div className={`faq-item ${openFaq === i ? 'expanded' : ''}`} key={q}><button onClick={() => setOpenFaq(openFaq === i ? null : i)} aria-expanded={openFaq === i} aria-controls={`faq-${i}`}><span>{q}</span><Icon name={openFaq === i ? 'close' : 'chevron'} size={18} /></button><div id={`faq-${i}`} hidden={openFaq !== i}><p>{a}</p>{i === 4 && <a href={REPO + '/issues'} target="_blank" rel="noreferrer">Открыть GitHub Issues ↗</a>}</div></div>)}</div></section>
        <section className="final-cta container"><span className="cta-star">✧</span><div className="section-kicker">ТВОЙ НОВЫЙ УЮТНЫЙ УГОЛОК</div><h2>Ну что, <span>будем на связи?</span> ♡</h2><p>Те же любимые люди. Совсем другое настроение.</p><button className="button primary" onClick={() => setModal('download')}><Icon name="download" /> Забрать MioGram <span>♡</span></button><span className="cta-flower">✿</span><small>Android · бесплатно · с любовью</small></section>
      </main>
      <footer className="footer container"><a className="brand" href="#"><WingHeart /><span>mio<span>gram</span></span></a><p>Независимый Telegram-клиент.<br /><span>Сделано с ♡ и щепоткой интернет-магии.</span></p><div><a href={REPO} target="_blank" rel="noreferrer">GitHub ↗</a><a href={REPO + '/blob/main/LICENSE'} target="_blank" rel="noreferrer">GPL-3.0 ↗</a><span>stay soft. stay you. ✧</span></div></footer>
      {toast && <div className="toast" role="status"><Icon name="check" size={18} />{toast}<button onClick={() => setToast('')} aria-label="Закрыть уведомление"><Icon name="close" size={15} /></button></div>}
      {modal !== null && <div className="modal-overlay" onClick={e => { if (e.target === e.currentTarget) setModal(null); }}><div className="modal app-window" ref={modalRef} role="dialog" aria-modal="true" aria-labelledby="modal-title"><div className="window-bar"><span>{modal === 'download' ? '♡ download_miogram.exe' : '✧ more_about_miogram.txt'}</span><button className="icon-button" onClick={() => setModal(null)} aria-label="Закрыть"><Icon name="close" size={18} /></button></div><div className="modal-content">{modal === 'download' ? <><WingHeart /><div className="section-kicker">НОВЫЙ УЮТ — В ОДНОМ КЛИКЕ</div><h2 id="modal-title">Забирай MioGram ♡</h2><p>Скачай актуальную Android-сборку из официального репозитория проекта.</p><div className="download-steps"><span><b>01</b> Открой последний релиз на GitHub.</span><span><b>02</b> В разделе Assets выбери файл .apk.</span><span><b>03</b> Установи и войди в свой Telegram.</span></div><a className="button primary" href={REPO + '/releases/latest'} target="_blank" rel="noreferrer"><Icon name="download" /> Открыть GitHub Releases ↗</a><small>Доступность APK и требования к Android указаны в релизе.<br />Никогда не передавай никому код входа.</small></> : <><span className="modal-feature-icon"><Icon name={features[modal].icon} size={32} /></span><div className="section-kicker">{features[modal].tag}</div><h2 id="modal-title">{features[modal].title}</h2><p>{features[modal].detail}</p><a className="button secondary" href={REPO + '#-key-features'} target="_blank" rel="noreferrer">Подробнее в репозитории <Icon name="arrow" size={18} /></a></>}</div></div></div>}
    </>
  );
}
export default App;
