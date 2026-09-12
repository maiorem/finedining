import { useTranslation } from "react-i18next";
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
      <p className={styles.copyright}>{t("footer.copyright")}</p>
    </footer>
  );
}
