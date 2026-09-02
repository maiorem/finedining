import { useTranslation } from "react-i18next";
import type { Program } from "../../api/programs";
import styles from "./ProgramCard.module.css";

export function ProgramCard({ program }: { program: Program }) {
  const { t } = useTranslation();

  return (
    <article className={styles.card}>
      <h3 className={styles.title}>{program.title}</h3>
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
