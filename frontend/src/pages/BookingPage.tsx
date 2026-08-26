import { useTranslation } from "react-i18next";

export default function BookingPage() {
  const { t } = useTranslation();

  return (
    <main style={{ padding: "var(--section-y-mobile) var(--gutter-mobile)" }}>
      <h1 style={{ fontFamily: "var(--f-display)", fontSize: "var(--t-h1)" }}>
        {t("nav.booking")}
      </h1>
      <p style={{ color: "var(--c-ink-60)" }}>{t("page.booking.placeholder")}</p>
    </main>
  );
}
