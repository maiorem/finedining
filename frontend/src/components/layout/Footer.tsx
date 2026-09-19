import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { FACEBOOK_URL, INSTAGRAM_URL, YOUTUBE_URL } from "../../constants/social";
import styles from "./Footer.module.css";

// 사업자 정보(§8.5)는 상호·주소 등 사실 값이라 로케일에 따라 달라지지 않는다 — 라벨만
// i18n을 타고, 값 자체는 여기 상수로 둔다(번역할 대상이 아니다).
const BUSINESS_INFO = {
  name: "파인다이닝 씨어터",
  owner: "김미란",
  registrationNumber: "571-28-01830",
  address: "서울특별시 성북구 삼선교로 8-1, B1(삼선동 1가)",
  phoneDisplay: "010-6776-1801",
  phoneHref: "tel:01067761801",
  email: "finediningtheater@naver.com",
};

// 소셜 채널 링크 — 전부 새 창으로 연다(2026-09-19 요청). 유튜브 핸들은 한글이라 인코딩해서 쓴다.
const SOCIAL_LINKS = [
  {
    key: "youtube",
    href: YOUTUBE_URL,
    icon: (
      <>
        <rect x="2" y="5" width="20" height="14" rx="4" fill="none" stroke="currentColor" strokeWidth="1.6" />
        <path d="M10 9.2l5 2.8-5 2.8z" fill="currentColor" />
      </>
    ),
  },
  {
    key: "instagram",
    href: INSTAGRAM_URL,
    icon: (
      <>
        <rect x="3" y="3" width="18" height="18" rx="5" fill="none" stroke="currentColor" strokeWidth="1.6" />
        <circle cx="12" cy="12" r="4" fill="none" stroke="currentColor" strokeWidth="1.6" />
        <circle cx="17.2" cy="6.8" r="1" fill="currentColor" />
      </>
    ),
  },
  {
    key: "facebook",
    href: FACEBOOK_URL,
    icon: (
      <path
        d="M13.5 21v-7.5h2.6l.4-3h-3V8.6c0-.9.3-1.5 1.6-1.5h1.6V4.4c-.3 0-1.2-.1-2.3-.1-2.3 0-3.9 1.4-3.9 4v2.2H7.9v3h2.6V21z"
        fill="currentColor"
      />
    ),
  },
] as const;

// 로그인/로그아웃 진입점은 헤더로 옮겼다 — 푸터에 숨어 있으면 너무 안 보인다는 피드백.
export function Footer() {
  const { t } = useTranslation();

  return (
    <footer className={styles.footer}>
      <div className={styles.bizInfo}>
        <p>
          {BUSINESS_INFO.name} · {t("footer.owner")} {BUSINESS_INFO.owner} · {t("footer.registrationNumber")}{" "}
          {BUSINESS_INFO.registrationNumber}
        </p>
        <p>
          {t("footer.address")} {BUSINESS_INFO.address}
        </p>
        <p>
          {t("footer.phone")} <a href={BUSINESS_INFO.phoneHref}>{BUSINESS_INFO.phoneDisplay}</a> ·{" "}
          {t("footer.email")} <a href={`mailto:${BUSINESS_INFO.email}`}>{BUSINESS_INFO.email}</a>
        </p>
      </div>
      <nav aria-label={t("footer.legalNav")} className={styles.legalLinks}>
        <Link to="/privacy">{t("footer.privacyPolicy")}</Link>
        <Link to="/terms">{t("footer.terms")}</Link>
      </nav>
      <div className={styles.bottomRow}>
        <p className={styles.copyright}>{t("footer.copyright")}</p>
        <nav aria-label={t("footer.socialNav")} className={styles.social}>
          {SOCIAL_LINKS.map(({ key, href, icon }) => (
            <a
              key={key}
              href={href}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.socialLink}
              aria-label={`${t(`footer.social.${key}`)} (${t("booking.opensNewWindow")})`}
            >
              <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
                {icon}
              </svg>
            </a>
          ))}
        </nav>
      </div>
    </footer>
  );
}
