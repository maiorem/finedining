/** i18next의 "ko"/"en"과 백엔드 SiteLocale("KO"/"EN")을 잇는다. */
export function toApiLocale(i18nLanguage: string): "KO" | "EN" {
  return i18nLanguage.toLowerCase().startsWith("en") ? "EN" : "KO";
}
