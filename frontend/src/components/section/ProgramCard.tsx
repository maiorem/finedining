import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import type { ProgramSummary } from "../../api/programs";
import styles from "./ProgramCard.module.css";

export function ProgramCard({ program }: { program: ProgramSummary }) {
  const { t } = useTranslation();
  const thumbnail = program.thumbnail;

  return (
    <article className={styles.card}>
      <Link to={`/programs/${program.slug}`} className={styles.link}>
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
        <h3 className={styles.title}>{program.title}</h3>
      </Link>
      {program.description && <p className={styles.description}>{program.description}</p>}

      {(program.applyUrl || program.locationUrl) && (
        <div className={styles.actions}>
          {program.applyUrl && (
            <a
              className={styles.actionLink}
              href={program.applyUrl}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`${t("programs.apply")} (${t("booking.opensNewWindow")})`}
            >
              {t("programs.apply")}
            </a>
          )}
          {program.locationUrl && (
            <a
              className={styles.actionLink}
              href={program.locationUrl}
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
