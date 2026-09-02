import { useTranslation } from "react-i18next";
import styles from "./Footer.module.css";

// 연락처·사업자정보(§8.5)는 소개 콘텐츠가 아직 없어 다음에 채운다.
// 로그인/로그아웃 진입점은 헤더로 옮겼다 — 푸터에 숨어 있으면 너무 안 보인다는 피드백.
export function Footer() {
  const { t } = useTranslation();

  return (
    <footer className={styles.footer}>
      <p className={styles.copyright}>{t("footer.copyright")}</p>
    </footer>
  );
}
