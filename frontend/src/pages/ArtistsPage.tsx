import { useTranslation } from "react-i18next";
import { useArtists } from "../api/artists";
import { useCastings } from "../api/castings";
import { ArtistCard } from "../components/section/ArtistCard";
import styles from "./ArtistsPage.module.css";

export default function ArtistsPage() {
  const { t, i18n } = useTranslation();
  const { data: artists, isLoading: artistsLoading } = useArtists(i18n.language);
  const { data: castings } = useCastings(i18n.language);

  return (
    <main className={styles.page}>
      <h1 className={styles.heading}>{t("nav.artists")}</h1>

      {artistsLoading && <p className={styles.status}>{t("artists.loading")}</p>}
      {!artistsLoading && artists?.length === 0 && (
        <p className={styles.status}>{t("artists.empty")}</p>
      )}

      <ul className={styles.list}>
        {artists?.map((artist) => (
          <li key={artist.id}>
            <ArtistCard artist={artist} />
          </li>
        ))}
      </ul>

      {castings && castings.length > 0 && (
        <section>
          <h2 className={styles.castingHeading}>{t("artists.castingHeading")}</h2>
          <ul className={styles.castingList}>
            {castings.map((casting) => (
              <li key={casting.id} className={styles.castingItem}>
                <h3 className={styles.castingTitle}>{casting.title}</h3>
                <p className={styles.castingBody}>{casting.body}</p>
              </li>
            ))}
          </ul>
        </section>
      )}
    </main>
  );
}
