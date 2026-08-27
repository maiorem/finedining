import { Link } from "react-router-dom";
import type { ArtistSummary } from "../../api/artists";
import styles from "./ArtistCard.module.css";

// 사진(이미지 파이프라인)은 다음 단계다 — 지금은 이름·역할 텍스트만 보여준다.
export function ArtistCard({ artist }: { artist: ArtistSummary }) {
  return (
    <Link to={`/artists/${artist.slug}`} className={styles.card}>
      <h3 className={styles.name}>{artist.name}</h3>
      {artist.role && <p className={styles.role}>{artist.role}</p>}
    </Link>
  );
}
