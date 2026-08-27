import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useArtist } from "../api/artists";
import { ApiError } from "../api/http";
import styles from "./ArtistDetailPage.module.css";

export default function ArtistDetailPage() {
  const { slug = "" } = useParams();
  const { t, i18n } = useTranslation();
  const { data: artist, isLoading, error } = useArtist(slug, i18n.language);

  if (isLoading) {
    return (
      <main className={styles.page}>
        <p className={styles.status}>{t("artists.loading")}</p>
      </main>
    );
  }

  if (error || !artist) {
    const notFound = error instanceof ApiError && error.code === "ENTITY_NOT_FOUND";
    return (
      <main className={styles.page}>
        <p className={styles.status}>{notFound ? t("artists.notFound") : t("artists.loadError")}</p>
      </main>
    );
  }

  return (
    <main className={styles.page}>
      {artist.role && <p className={styles.eyebrow}>{artist.role}</p>}
      <h1 className={styles.name}>{artist.name}</h1>

      {artist.bio && <p className={styles.bio}>{artist.bio}</p>}

      {artist.linkUrl && (
        <a
          className={styles.link}
          href={artist.linkUrl}
          target="_blank"
          rel="noopener noreferrer"
          aria-label={`${artist.name} (${t("booking.opensNewWindow")})`}
        >
          {artist.linkUrl}
        </a>
      )}

      {artist.productions.length > 0 && (
        <section>
          <h2 className={styles.productionsHeading}>{t("artists.productionsHeading")}</h2>
          <ul className={styles.productionList}>
            {artist.productions.map((production) => (
              <li key={production.id}>{production.title}</li>
            ))}
          </ul>
        </section>
      )}
    </main>
  );
}
