import { useEffect, useRef, useState } from "react";
import { NavLink, Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useScrolled } from "../../hooks/useScrolled";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import { useMemberAuth } from "../../contexts/MemberAuthContext";
import logo from "../../assets/logo.png";
import styles from "./Header.module.css";

const NAV_ITEMS = [
  { to: "/about", labelKey: "nav.about" },
  { to: "/productions", labelKey: "nav.productions" },
  { to: "/programs", labelKey: "nav.programs" },
  { to: "/artists", labelKey: "nav.artists" },
  { to: "/reviews", labelKey: "nav.reviews" },
  { to: "/proposal", labelKey: "nav.proposal" },
] as const;

// 사람 아이콘 하나로 로그인/로그아웃을 함께 표현한다 — 로그인 상태면 채워진 아이콘,
// 아니면 윤곽선만. 텍스트 메뉴 항목을 늘리지 않으면서도 스크롤 없이 항상 보인다.
function AccountIcon({ active }: { active: boolean }) {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
      <circle cx="9" cy="5.5" r="3.25" fill={active ? "currentColor" : "none"} stroke="currentColor" strokeWidth="1.4" />
      <path
        d="M2.5 16c.9-3.2 3.6-5 6.5-5s5.6 1.8 6.5 5"
        fill={active ? "currentColor" : "none"}
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinecap="round"
      />
    </svg>
  );
}

export function Header() {
  const { t, i18n } = useTranslation();
  const scrolled = useScrolled();
  const [menuOpen, setMenuOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const menuButtonRef = useRef<HTMLButtonElement>(null);
  const { session: adminSession, logout: adminLogout } = useAdminAuth();
  const { session: memberSession, logout: memberLogout } = useMemberAuth();

  const nextLocale = i18n.language === "en" ? "ko" : "en";
  const loggedIn = Boolean(adminSession || memberSession);
  const accountLabel = adminSession ? t("nav.adminLogout") : memberSession ? t("login.logout") : t("nav.login");

  function toggleLanguage() {
    void i18n.changeLanguage(nextLocale);
  }

  async function handleAccountClick() {
    // 아이콘 하나로 로그인/로그아웃을 겸하다 보니 실수로 눌러 로그아웃되기 쉽다 — 한 번 더 확인한다.
    if (!window.confirm(t("nav.logoutConfirm"))) return;

    if (adminSession) {
      await adminLogout();
    } else if (memberSession) {
      await memberLogout();
    }
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
          {/* 바로 옆 텍스트가 같은 이름을 전달하므로 이미지는 장식용이다(CLAUDE.md §8.8 alt="") */}
          <img src={logo} alt="" className={styles.logoImage} width={28} height={28} />
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

        {/* 스크롤이나 메뉴를 열지 않아도 항상 보이도록 모바일·데스크톱 공통 헤더 줄에 둔다. */}
        {loggedIn ? (
          <button
            type="button"
            className={styles.accountButton}
            aria-label={accountLabel}
            onClick={() => void handleAccountClick()}
          >
            <AccountIcon active />
          </button>
        ) : (
          <Link
            to="/login"
            className={styles.accountButton}
            aria-label={accountLabel}
            onClick={() => setMenuOpen(false)}
          >
            <AccountIcon active={false} />
          </Link>
        )}

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
