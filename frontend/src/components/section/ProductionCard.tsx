import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { ProductionSummary } from "../../api/productions";
import styles from "./ProductionCard.module.css";

export function ProductionCard({ production }: { production: ProductionSummary }) {
  const { t } = useTranslation();
  const thumbnail = production.thumbnail;

  return (
    <article className={styles.card}>
      <Link to={`/productions/${production.slug}`} className={styles.link}>
        <div
          className={styles.thumbnail}
          style={thumbnail?.lqipBase64 ? { backgroundImage: `url(data:image/jpeg;base64,${thumbnail.lqipBase64})` } : undefined}
        >
          {thumbnail?.url640 && (
            <img
              src={thumbnail.url640}
              alt=""
              width={thumbnail.width ?? undefined}
              height={thumbnail.height ?? undefined}
              loading="lazy"
              decoding="async"
            />
          )}
        </div>
        <h3 className={styles.title}>{production.title}</h3>
      </Link>

      {(production.bookingUrl || production.locationUrl) && (
        <div className={styles.actions}>
          {production.bookingUrl && (
            <a
              className={styles.actionLink}
              href={production.bookingUrl}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`${t("booking.reserve")} (${t("booking.opensNewWindow")})`}
            >
              {t("booking.reserve")}
            </a>
          )}
          {production.locationUrl && (
            <a
              className={styles.actionLink}
              href={production.locationUrl}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`${t("booking.location")} (${t("booking.opensNewWindow")})`}
            >
              {t("booking.location")}
            </a>
          )}
        </div>
      )}
    </article>
  );
}
