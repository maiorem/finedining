import { useState } from "react";
import { useTranslation } from "react-i18next";
import styles from "./GallerySlideshow.module.css";

// frontend/src/assets/gallery/ 에 넣은 이미지가 파일명 순으로 슬라이드가 된다. 코드를 고칠
// 필요 없이 파일만 추가/삭제하면 된다. 사진이 아직 없어도 아래 카피 두 줄은 그대로 보여준다
// — 요청서 구성(작품 목록 → 음식·무대 사진 → 카피)에서 카피는 사진과 별개로 이미 확정된
// 콘텐츠라 사진을 기다릴 이유가 없다.
const images = import.meta.glob<{ default: string }>(
  "../../assets/gallery/*.{jpg,jpeg,png,webp,JPG,JPEG,PNG,WEBP}",
  { eager: true },
);
const slides = Object.keys(images)
  .sort()
  .map((path) => images[path].default);

/**
 * 히어로와 달리 자동재생하지 않는다 — 클릭(화살표/점)으로만 넘어간다. CLAUDE.md §8.2의
 * 자동재생 캐러셀 금지 원칙을 그대로 지키는 두 번째 갤러리 섹션이다(히어로 예외를 확대하지
 * 않는다).
 */
export function GallerySlideshow() {
  const { t } = useTranslation();
  const [index, setIndex] = useState(0);

  function goTo(next: number) {
    setIndex(((next % slides.length) + slides.length) % slides.length);
  }

  return (
    <div className={styles.gallery}>
      {slides.length > 0 && (
        <>
          <div
            className={styles.frame}
            role="region"
            aria-roledescription="carousel"
            aria-label={t("home.galleryTagline")}
          >
            <img src={slides[index]} alt="" className={styles.image} />

            {slides.length > 1 && (
              <>
                <button type="button" className={`${styles.arrow} ${styles.prev}`} onClick={() => goTo(index - 1)}>
                  <span aria-hidden="true">‹</span>
                  <span className={styles.srOnly}>{t("home.galleryPrev")}</span>
                </button>
                <button type="button" className={`${styles.arrow} ${styles.next}`} onClick={() => goTo(index + 1)}>
                  <span aria-hidden="true">›</span>
                  <span className={styles.srOnly}>{t("home.galleryNext")}</span>
                </button>
              </>
            )}
          </div>

          {slides.length > 1 && (
            <div className={styles.dots} role="tablist" aria-label={t("home.gallerySelect")}>
              {slides.map((src, i) => (
                <button
                  key={src}
                  type="button"
                  role="tab"
                  aria-selected={i === index}
                  aria-label={t("home.galleryGoTo", { number: i + 1 })}
                  className={i === index ? `${styles.dot} ${styles.dotActive}` : styles.dot}
                  onClick={() => goTo(i)}
                />
              ))}
            </div>
          )}
        </>
      )}

      <p className={styles.tagline}>{t("home.galleryTagline")}</p>
      <p className={styles.brand}>{t("home.galleryBrand")}</p>
    </div>
  );
}
