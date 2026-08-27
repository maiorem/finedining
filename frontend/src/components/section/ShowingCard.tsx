import { useTranslation } from "react-i18next";
import type { Showing } from "../../api/showings";
import { ReserveButton } from "./ReserveButton";
import { SalesStatusBadge } from "./SalesStatusBadge";
import styles from "./ShowingCard.module.css";

// 회차 시각은 UTC로 저장되지만 화면은 항상 한국 시간이다 (CLAUDE.md §7.6·§13.4).
function formatShowingTime(iso: string, i18nLanguage: string) {
  const date = new Date(iso);
  const locale = i18nLanguage.startsWith("en") ? "en-US" : "ko-KR";

  const dateLabel = new Intl.DateTimeFormat(locale, {
    timeZone: "Asia/Seoul",
    month: "long",
    day: "numeric",
    weekday: "short",
  }).format(date);

  const timeLabel = new Intl.DateTimeFormat(locale, {
    timeZone: "Asia/Seoul",
    hour: "2-digit",
    minute: "2-digit",
    hour12: locale === "en-US",
  }).format(date);

  return { dateLabel, timeLabel };
}

export function ShowingCard({ showing }: { showing: Showing }) {
  const { t, i18n } = useTranslation();
  const { dateLabel, timeLabel } = formatShowingTime(showing.startsAt, i18n.language);

  return (
    <article className={styles.card}>
      <div className={styles.time}>
        <span className={styles.date}>{dateLabel}</span>
        <span className={styles.clock}>{timeLabel}</span>
      </div>

      <div>
        <h3 className={styles.title}>{showing.productionTitle}</h3>
        <p className={styles.meta}>
          {t("booking.duration", { minutes: showing.durationMinutes })} · {showing.venueName}
        </p>
        <div className={styles.badges}>
          <SalesStatusBadge status={showing.salesStatus} />
          <span className={styles.langNote}>
            {t(`booking.language.${showing.spokenLanguage}`)}
            {showing.interpretationAvailable ? ` · ${t("booking.interpretationAvailable")}` : ""}
          </span>
        </div>
      </div>

      <ReserveButton
        showingId={showing.id}
        bookingUrl={showing.bookingUrl}
        bookingAvailable={showing.bookingAvailable}
        salesStatus={showing.salesStatus}
        channel="booking-calendar"
      />
    </article>
  );
}
