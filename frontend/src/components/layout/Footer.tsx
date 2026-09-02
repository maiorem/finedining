import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import styles from "./Footer.module.css";

// 연락처·사업자정보(§8.5)는 소개 콘텐츠가 아직 없어 다음에 채운다 — 지금은 로그인 진입점만.
export function Footer() {
  const { t } = useTranslation();
  const { session, logout } = useAdminAuth();

  return (
    <footer className={styles.footer}>
      <p className={styles.copyright}>{t("footer.copyright")}</p>
      {session ? (
        <button type="button" className={styles.adminLogout} onClick={() => void logout()}>
          {t("footer.adminLogout")}
        </button>
      ) : (
        <Link to="/login" className={styles.loginLink}>
          {t("footer.login")}
        </Link>
      )}
    </footer>
  );
}
