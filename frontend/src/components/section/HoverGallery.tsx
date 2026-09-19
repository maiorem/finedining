import { useState } from "react";
import styles from "./HoverGallery.module.css";

export type HoverGalleryItem = { src: string; alt: string };

// 작은 사진에 마우스를 올리면(키보드는 포커스, 터치는 탭) 왼쪽 큰 사진이 그 이미지로 바뀐다.
export function HoverGallery({ items }: { items: HoverGalleryItem[] }) {
  const [active, setActive] = useState(0);
  const current = items[active] ?? items[0];
  if (!current) return null;

  return (
    <div className={styles.gallery}>
      <img className={styles.large} src={current.src} alt={current.alt} loading="lazy" decoding="async" />
      <ul className={styles.thumbs}>
        {items.map((item, index) => (
          <li key={item.src}>
            <button
              type="button"
              className={index === active ? `${styles.thumb} ${styles.thumbActive}` : styles.thumb}
              aria-label={item.alt}
              aria-pressed={index === active}
              onMouseEnter={() => setActive(index)}
              onFocus={() => setActive(index)}
              onClick={() => setActive(index)}
            >
              <img src={item.src} alt="" loading="lazy" decoding="async" />
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
