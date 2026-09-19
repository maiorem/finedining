import { Link } from "react-router-dom";
import type { ArtistSummary } from "../../api/artists";
import styles from "./ArtistCard.module.css";

export function ArtistCard({ artist }: { artist: ArtistSummary }) {
  const photo = artist.photo;

  return (
    <article className={styles.card}>
      <Link to={`/artists/${artist.slug}`} className={styles.link}>
        <div
          className={styles.thumbnail}
          style={photo?.lqipBase64 ? { backgroundImage: `url(data:image/jpeg;base64,${photo.lqipBase64})` } : undefined}
        >
          {(photo?.url960 ?? photo?.url640) && (
            <img
              src={photo.url960 ?? photo.url640 ?? undefined}
              alt=""
              width={photo.width ?? undefined}
              height={photo.height ?? undefined}
              loading="lazy"
              decoding="async"
            />
          )}
        </div>
        {artist.role && <p className={styles.role}>{artist.role}</p>}
        <h3 className={styles.name}>{artist.name}</h3>
        {artist.quote && <p className={styles.quote}>“{artist.quote}”</p>}
      </Link>
      {artist.email && (
        <a className={styles.email} href={`mailto:${artist.email}`}>
          {artist.email}
        </a>
      )}
    </article>
  );
}
