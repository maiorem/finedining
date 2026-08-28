import { Link } from "react-router-dom";
import type { ArtistSummary } from "../../api/artists";
import styles from "./ArtistCard.module.css";

export function ArtistCard({ artist }: { artist: ArtistSummary }) {
  const photo = artist.photo;

  return (
    <Link to={`/artists/${artist.slug}`} className={styles.card}>
      <div
        className={styles.thumbnail}
        style={photo?.lqipBase64 ? { backgroundImage: `url(data:image/jpeg;base64,${photo.lqipBase64})` } : undefined}
      >
        {photo?.url640 && (
          <img
            src={photo.url640}
            alt=""
            width={photo.width ?? undefined}
            height={photo.height ?? undefined}
            loading="lazy"
            decoding="async"
          />
        )}
      </div>
      <div>
        <h3 className={styles.name}>{artist.name}</h3>
        {artist.role && <p className={styles.role}>{artist.role}</p>}
      </div>
    </Link>
  );
}
