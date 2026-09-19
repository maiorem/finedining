import { useTranslation } from "react-i18next";
import aboutImage from "../../assets/about/about.jpg";
import styles from "./AboutIntro.module.css";

// 라벨은 영문 그대로 노출하는 디자인 문구라 로케일과 무관하다 — 번역 대상은 제목·설명뿐이다.
const VALUES = [
  { key: "story", label: "STORY" },
  { key: "food", label: "FOOD" },
  { key: "together", label: "TOGETHER" },
] as const;

const PRODUCTS = [
  { key: "experience", label: "EXPERIENCE CONTENT" },
  { key: "cultural", label: "CULTURAL EVENT" },
  { key: "local", label: "LOCAL CONTENT" },
  { key: "kfood", label: "K-FOOD CONTENT" },
  { key: "custom", label: "CUSTOM CONTENT" },
] as const;

/**
 * 소개 페이지 "소개" 탭 — 거의 안 바뀌는 정적 카피라 번역 리소스에서 그대로 읽는다(§17).
 * 이미지 왼쪽/텍스트 오른쪽 구성에서 MISSION 박스는 이미지 맨 아래 끝에 맞춘다(2026-09-19 요청).
 */
export function AboutIntro() {
  const { t } = useTranslation();

  return (
    <>
      <div className={styles.top}>
        <img
          src={aboutImage}
          alt=""
          className={styles.image}
          width={960}
          height={1440}
          loading="lazy"
          decoding="async"
        />
        <div className={styles.text}>
          <p className={styles.headline}>{t("about.headline")}</p>
          <p className={styles.headline}>{t("about.lead")}</p>
          <p className={styles.body}>{t("about.body")}</p>
          <div className={styles.mission}>
            <p className={styles.missionLabel}>MISSION</p>
            <p className={styles.missionText}>{t("about.mission")}</p>
          </div>
        </div>
      </div>

      <section className={styles.values} aria-label="STORY · FOOD · TOGETHER">
        <ul className={styles.valueList}>
          {VALUES.map(({ key, label }) => (
            <li key={key} className={styles.value}>
              <p className={styles.valueLabel}>{label}</p>
              <p className={styles.valueTitle}>{t(`about.values.${key}.title`)}</p>
              <p className={styles.valueBody}>{t(`about.values.${key}.body`)}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className={styles.products}>
        <h2 className={styles.productsHeading}>{t("about.productsHeading")}</h2>
        <ul className={styles.productList}>
          {PRODUCTS.map(({ key, label }) => (
            <li key={key} className={styles.product}>
              <p className={styles.productLabel}>{label}</p>
              <p className={styles.productTitle}>{t(`about.products.${key}.title`)}</p>
              <p className={styles.productBody}>{t(`about.products.${key}.body`)}</p>
            </li>
          ))}
        </ul>
      </section>

      <p className={styles.closing}>
        {t("about.closing")}
        <span className={styles.closingBrand}>FINEDINING THEATER</span>
      </p>
    </>
  );
}
