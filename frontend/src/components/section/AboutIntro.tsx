import { useTranslation } from "react-i18next";
import aboutImage from "../../assets/about/about.jpg";
import history1 from "../../assets/about/history-1.png";
import history2 from "../../assets/about/history-2.jpg";
import history3 from "../../assets/about/history-3.jpg";
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

// 기업 연혁·소개 슬라이드 3장(2026-09-19 요청). 글이 그림에 박혀 있어서 같은 내용을 숨김 캡션으로 함께
// 싣는다 — 스크린리더와 검색엔진이 읽을 수 있고, 영문 페이지에서도 번역된 내용을 전달한다(§8.8).
const HISTORY = [
  { key: "1", src: history1 },
  { key: "2", src: history2 },
  { key: "3", src: history3 },
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

      <section className={styles.history} aria-label={t("about.historyLabel")}>
        {HISTORY.map(({ key, src }) => (
          <figure key={key} className={styles.historyItem}>
            <img
              src={src}
              alt=""
              className={styles.historyImage}
              width={1920}
              height={1080}
              loading="lazy"
              decoding="async"
            />
            <figcaption className={styles.srOnly}>{t(`about.history.${key}`)}</figcaption>
          </figure>
        ))}
      </section>

      <p className={styles.closing}>
        {t("about.closing")}
        <span className={styles.closingBrand}>FINEDINING THEATER</span>
      </p>
    </>
  );
}
