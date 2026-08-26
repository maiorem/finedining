import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import ko from "./locales/ko/common.json";
import en from "./locales/en/common.json";

// URL 경로 프리픽스(/en/...)로 언어를 결정한다 (CLAUDE.md §7.6).
// 실제 라우트 연동은 Phase 2(다국어 라우팅)에서 붙인다 — 지금은 ko 고정 초기화만 한다.
void i18n.use(initReactI18next).init({
  resources: {
    ko: { common: ko },
    en: { common: en },
  },
  lng: "ko",
  fallbackLng: "ko",
  defaultNS: "common",
  interpolation: { escapeValue: false },
});

export default i18n;
