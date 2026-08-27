import { useTranslation } from "react-i18next";
import type { SalesStatus } from "../../api/showings";
import styles from "./SalesStatusBadge.module.css";

const STATUS_LABEL_KEY: Record<SalesStatus, string> = {
  OPEN: "booking.status.open",
  CLOSING_SOON: "booking.status.closingSoon",
  SOLD_OUT: "booking.status.soldOut",
  ENDED: "booking.status.ended",
};

export function SalesStatusBadge({ status }: { status: SalesStatus }) {
  const { t } = useTranslation();

  return (
    <span className={status === "OPEN" ? `${styles.badge} ${styles.open}` : styles.badge}>
      {t(STATUS_LABEL_KEY[status])}
    </span>
  );
}
