import { useTranslation } from "react-i18next";
import styles from "./AboutPage.module.css";

/**
 * 소개문은 거의 바뀌지 않는 정적 카피라 관리자 편집 모드(DB 편집) 대신 번역 리소스에 직접
 * 둔다(2026-09-04 결정) — PIN·발행 흐름까지 갖춘 CMS를 문장 하나 위해 유지할 이유가 없다.
 * 나중에 자주 바뀌는 콘텐츠가 필요해지면 그때 다시 동적으로 만든다.
 */
export default function AboutPage() {
  const { t } = useTranslation();

  return (
    <main className={styles.page}>
      <h1 className={styles.srOnly}>{t("nav.about")}</h1>
      <p className={styles.intro}>{t("about.introText")}</p>
    </main>
  );
}
