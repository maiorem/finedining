import { useTranslation } from "react-i18next";
import { useNoIndex } from "../hooks/useNoIndex";
import logo from "../assets/logo.png";
import styles from "./PrivatePage.module.css";

/** 오픈 전 비공개 동안 관리자가 아닌 방문자에게 보이는 유일한 화면이다(SiteGate). */
export default function PrivatePage() {
  const { t } = useTranslation();
  useNoIndex();

  return (
    <main className={styles.page}>
      <img src={logo} alt="" width={56} height={56} className={styles.logo} />
      <h1 className={styles.name}>{t("app.name")}</h1>
      <p className={styles.message}>{t("site.private.message")}</p>
      <p className={styles.sub}>{t("site.private.sub")}</p>
    </main>
  );
}
