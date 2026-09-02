import { useEffect, useState, type FocusEvent, type ReactNode } from "react";
import styles from "./HeroSlideshow.module.css";

const AUTO_ADVANCE_MS = 6000;

// frontend/src/assets/hero/ 에 넣은 이미지가 파일명 순으로 자동으로 슬라이드가 된다.
// 코드를 고칠 필요 없이 파일만 추가/삭제하면 된다.
const images = import.meta.glob<{ default: string }>(
  "../../assets/hero/*.{jpg,jpeg,png,webp,JPG,JPEG,PNG,WEBP}",
  { eager: true },
);
const slides = Object.keys(images)
  .sort()
  .map((path) => images[path].default);

type HeroSlideshowProps = {
  children?: ReactNode;
};

export function HeroSlideshow({ children }: HeroSlideshowProps) {
  const [index, setIndex] = useState(0);
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const [prefersReducedMotion] = useState(
    () =>
      typeof window !== "undefined" && typeof window.matchMedia === "function" &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches,
  );

  const hasMultiple = slides.length > 1;
  // 화살표·재생 버튼은 뺐지만 자동재생은 계속 돈다 — hover/focus 정지와
  // prefers-reduced-motion은 버튼과 무관하게 그대로 유지한다.
  const playing = hasMultiple && !hovered && !focused && !prefersReducedMotion;

  useEffect(() => {
    if (!playing) return;
    const timer = window.setInterval(() => {
      setIndex((i) => (i + 1) % slides.length);
    }, AUTO_ADVANCE_MS);
    return () => window.clearInterval(timer);
  }, [playing]);

  function goTo(next: number) {
    setIndex(((next % slides.length) + slides.length) % slides.length);
  }

  function handleBlur(e: FocusEvent<HTMLDivElement>) {
    if (!e.currentTarget.contains(e.relatedTarget as Node | null)) {
      setFocused(false);
    }
  }

  if (slides.length === 0) {
    return (
      <div className={styles.hero}>
        <div className={styles.overlay}>{children}</div>
      </div>
    );
  }

  return (
    <div
      className={styles.hero}
      role="region"
      aria-roledescription="carousel"
      aria-label="대표 이미지"
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onFocus={() => setFocused(true)}
      onBlur={handleBlur}
    >
      {slides.map((src, i) => (
        <img
          key={src}
          src={src}
          alt=""
          aria-hidden={i === index ? undefined : true}
          className={i === index ? `${styles.slide} ${styles.slideActive}` : styles.slide}
        />
      ))}

      <div className={styles.scrim} aria-hidden="true" />
      <div className={styles.overlay}>{children}</div>

      {hasMultiple && (
        <div className={styles.dots} role="tablist" aria-label="이미지 선택">
          {slides.map((src, i) => (
            <button
              key={src}
              type="button"
              role="tab"
              aria-selected={i === index}
              aria-label={`${i + 1}번째 이미지로 이동`}
              className={i === index ? `${styles.dot} ${styles.dotActive}` : styles.dot}
              onClick={() => goTo(i)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
