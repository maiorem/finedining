import { useEffect, useRef, useState } from "react";
import { NavLink, Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useScrolled } from "../../hooks/useScrolled";
import styles from "./Header.module.css";

const NAV_ITEMS = [
  { to: "/productions", labelKey: "nav.productions" },
  { to: "/about", labelKey: "nav.about" },
  { to: "/booking", labelKey: "nav.booking" },
  { to: "/proposal", labelKey: "nav.proposal" },
  { to: "/reviews", labelKey: "nav.reviews" },
  { to: "/artists", labelKey: "nav.artists" },
] as const;

export function Header() {
  const { t, i18n } = useTranslation();
  const scrolled = useScrolled();
  const [menuOpen, setMenuOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const menuButtonRef = useRef<HTMLButtonElement>(null);

  const nextLocale = i18n.language === "en" ? "ko" : "en";

  function toggleLanguage() {
    void i18n.changeLanguage(nextLocale);
  }

  // 모바일 메뉴: 포커스 트랩 + Esc 닫기 + 배경 스크롤 락 (CLAUDE.md §8.8)
  useEffect(() => {
    if (!menuOpen) return;

    const panel = panelRef.current;
    const focusable = panel?.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled])',
    );
    focusable?.[0]?.focus();

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        setMenuOpen(false);
        menuButtonRef.current?.focus();
        return;
      }
      if (e.key !== "Tab" || !focusable || focusable.length === 0) return;

      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [menuOpen]);

  return (
    <header className={`${styles.header} ${scrolled ? styles.scrolled : ""}`}>
      <div className={styles.inner}>
        <Link to="/" className={styles.logo} onClick={() => setMenuOpen(false)}>
          {t("app.name")}
        </Link>

        <nav aria-label={t("nav.main")} className={styles.desktopNav}>
          <ul className={styles.navList}>
            {NAV_ITEMS.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  className={({ isActive }) =>
                    isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink
                  }
                >
                  {t(item.labelKey)}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <button
          type="button"
          className={`${styles.langToggle} ${styles.langToggleDesktop}`}
          aria-label={t("nav.language")}
          onClick={toggleLanguage}
        >
          {i18n.language === "en" ? "KO" : "EN"}
        </button>

        <button
          type="button"
          ref={menuButtonRef}
          className={styles.menuButton}
          aria-label={menuOpen ? t("nav.closeMenu") : t("nav.openMenu")}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((open) => !open)}
        >
          <span className={styles.menuIcon} aria-hidden="true" />
        </button>
      </div>

      {menuOpen && (
        <div ref={panelRef} className={styles.mobilePanel}>
          <ul className={styles.mobileNavList}>
            {NAV_ITEMS.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  className={styles.mobileNavLink}
                  onClick={() => setMenuOpen(false)}
                >
                  {t(item.labelKey)}
                </NavLink>
              </li>
            ))}
          </ul>
          <button type="button" className={styles.langToggle} onClick={toggleLanguage}>
            {t("nav.language")}: {i18n.language === "en" ? "KO" : "EN"}
          </button>
        </div>
      )}
    </header>
  );
}
