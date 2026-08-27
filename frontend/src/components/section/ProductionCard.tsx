import { Link } from "react-router-dom";
import type { ProductionSummary } from "../../api/productions";
import styles from "./ProductionCard.module.css";

export function ProductionCard({ production }: { production: ProductionSummary }) {
  const thumbnail = production.thumbnail;

  return (
    <Link to={`/productions/${production.slug}`} className={styles.card}>
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
  );
}
