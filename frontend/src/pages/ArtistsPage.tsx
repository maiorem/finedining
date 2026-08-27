import { useTranslation } from "react-i18next";

export default function ArtistsPage() {
  const { t } = useTranslation();

  return (
    <main style={{ padding: "var(--section-y-mobile) var(--gutter-mobile)" }}>
      <h1 style={{ fontFamily: "var(--f-display)", fontSize: "var(--t-h1)" }}>
        {t("nav.artists")}
      </h1>
      <p style={{ color: "var(--c-ink-60)" }}>{t("page.artists.placeholder")}</p>
    </main>
  );
}
