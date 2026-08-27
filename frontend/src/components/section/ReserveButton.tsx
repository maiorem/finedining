import { useTranslation } from "react-i18next";
import { trackBookingClick } from "../../api/showings";
import type { SalesStatus } from "../../api/showings";
import styles from "./ReserveButton.module.css";

type ReserveButtonProps = {
  showingId: number;
  bookingUrl: string | null;
  bookingAvailable: boolean;
  salesStatus: SalesStatus;
  channel: string;
};

export function ReserveButton({
  showingId,
  bookingUrl,
  bookingAvailable,
  salesStatus,
  channel,
}: ReserveButtonProps) {
  const { t, i18n } = useTranslation();

  if (bookingAvailable && bookingUrl) {
    return (
      <a
        className={styles.reserve}
        href={bookingUrl}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`${t("booking.reserve")} (${t("booking.opensNewWindow")})`}
        onClick={() => trackBookingClick(showingId, channel, i18n.language)}
      >
        {t("booking.reserve")}
      </a>
    );
  }

  const disabledLabel =
    salesStatus === "SOLD_OUT"
      ? t("booking.status.soldOut")
      : salesStatus === "ENDED"
        ? t("booking.status.ended")
        : t("booking.unavailable");

  return (
    <button type="button" className={styles.disabled} disabled>
      {disabledLabel}
    </button>
  );
}
