import { useState } from "react";
import { useTranslation } from "react-i18next";
import styles from "./TrailerVideo.module.css";

// 클릭하기 전에는 iframe을 만들지 않는다 — 자동재생 없이, 유튜브 스크립트도 재생 전에는 불러오지 않는다.
export function TrailerVideo({ videoId, title }: { videoId: string; title: string }) {
  const { t } = useTranslation();
  const [playing, setPlaying] = useState(false);

  return (
    <div className={styles.frame}>
      {playing ? (
        <iframe
          className={styles.media}
          src={`https://www.youtube-nocookie.com/embed/${encodeURIComponent(videoId)}?autoplay=1&rel=0`}
          title={title}
          allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
          allowFullScreen
        />
      ) : (
        <button type="button" className={styles.poster} onClick={() => setPlaying(true)} aria-label={`${t("trailer.play")}: ${title}`}>
          <img
            className={styles.media}
            src={`https://i.ytimg.com/vi/${encodeURIComponent(videoId)}/hqdefault.jpg`}
            alt=""
            loading="lazy"
            decoding="async"
          />
          <span className={styles.playIcon} aria-hidden="true">
            <svg width="28" height="28" viewBox="0 0 24 24">
              <path d="M8 5.5v13l11-6.5z" fill="currentColor" />
            </svg>
          </span>
        </button>
      )}
    </div>
  );
}
