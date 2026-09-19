import { useRef, useState, type FocusEvent, type PointerEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMarqueeScroll } from "../../hooks/useMarqueeScroll";
import styles from "./StoryMarquee.module.css";

// frontend/src/assets/story/ 에 01~06 번호로 넣은 사진이 카드에 순서대로 붙는다(02.jpg, 03.png …).
// 코드를 고칠 필요 없이 파일만 넣으면 된다 — 번호에 사진이 없으면 자리 표시 카드로 남는다
// ("음식으로 기억합니다" 사진은 요청서에서 아직 찾는 중이었다).
const images = import.meta.glob<{ default: string }>(
  "../../assets/story/*.{jpg,jpeg,png,webp,JPG,JPEG,PNG,WEBP}",
  { eager: true },
);
const photoByNumber = new Map<number, string>();
for (const [path, module] of Object.entries(images)) {
  const match = path.match(/(\d+)\.\w+$/);
  if (match) photoByNumber.set(Number(match[1]), module.default);
}

const CARDS = [
  { number: 1, key: "people" },
  { number: 2, key: "discover" },
  { number: 3, key: "food" },
  { number: 4, key: "art" },
  { number: 5, key: "meal" },
  { number: 6, key: "sitAgain" },
] as const;

const TOUCH_RESUME_DELAY_MS = 2000;

function prefersReducedMotion() {
  return (
    typeof window !== "undefined" &&
    typeof window.matchMedia === "function" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

/**
 * 메인 페이지의 "흘러가는 브랜드 문장"(2026-09-19 요청) — 사진 카드가 화면을 오른쪽→왼쪽으로
 * 끝없이 흐른다. 자동으로 움직이는 요소라 §8.2의 조건을 모두 갖춘다: 마우스를 올리거나 포커스가
 * 가면 멈추고, 터치하면 잠시 멈추고, 일시정지 버튼이 있으며, 모션 감소 설정에서는 아예 흐르지 않는다.
 */
export function StoryMarquee() {
  const { t } = useTranslation();
  const trackRef = useRef<HTMLDivElement>(null);
  const [reducedMotion] = useState(prefersReducedMotion);
  const [userPaused, setUserPaused] = useState(false);
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const [touching, setTouching] = useState(false);
  const resumeTimer = useRef<number | undefined>(undefined);

  useMarqueeScroll(trackRef, reducedMotion || userPaused || hovered || focused || touching);

  function handlePointerEnter(e: PointerEvent) {
    if (e.pointerType === "mouse") setHovered(true);
  }

  function handlePointerLeave(e: PointerEvent) {
    if (e.pointerType === "mouse") setHovered(false);
  }

  function handlePointerDown(e: PointerEvent) {
    if (e.pointerType === "mouse") return;
    window.clearTimeout(resumeTimer.current);
    setTouching(true);
  }

  // 손가락을 뗀 뒤에도 관성 스크롤이 이어지므로 잠깐 기다렸다가 다시 흐른다.
  function handlePointerRelease(e: PointerEvent) {
    if (e.pointerType === "mouse") return;
    window.clearTimeout(resumeTimer.current);
    resumeTimer.current = window.setTimeout(() => setTouching(false), TOUCH_RESUME_DELAY_MS);
  }

  function handleBlur(e: FocusEvent<HTMLDivElement>) {
    if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setFocused(false);
  }

  const cards = CARDS.map(({ number, key }) => {
    const photo = photoByNumber.get(number);
    return (
      <li key={key} className={styles.card}>
        <div className={styles.photo}>{photo && <img src={photo} alt="" loading="lazy" decoding="async" />}</div>
        <h3 className={styles.cardTitle}>{t(`story.cards.${key}.title`)}</h3>
        <p className={styles.cardBody}>{t(`story.cards.${key}.body`)}</p>
      </li>
    );
  });

  return (
    <section className={styles.section} aria-labelledby="story-marquee-title">
      <div className={styles.header}>
        <h2 id="story-marquee-title" className={styles.title}>
          {t("story.title")}
          <span className={styles.subtitle}>
            {t("story.subtitle")} <span className={styles.brand}>{t("app.name")}</span>
          </span>
        </h2>
        <p className={styles.keywords}>{t("story.keywords")}</p>
        {!reducedMotion && (
          <button
            type="button"
            className={styles.pauseButton}
            aria-pressed={userPaused}
            onClick={() => setUserPaused((paused) => !paused)}
          >
            {userPaused ? t("story.play") : t("story.pause")}
          </button>
        )}
      </div>

      <div
        ref={trackRef}
        className={styles.viewport}
        tabIndex={0}
        role="group"
        aria-roledescription="carousel"
        aria-label={t("story.regionLabel")}
        onPointerEnter={handlePointerEnter}
        onPointerLeave={handlePointerLeave}
        onPointerDown={handlePointerDown}
        onPointerUp={handlePointerRelease}
        onPointerCancel={handlePointerRelease}
        onFocus={() => setFocused(true)}
        onBlur={handleBlur}
      >
        {/* 끝없이 이어 보이게 카드 묶음을 한 번 더 붙인다 — 두 번째 사본은 보조기기에 다시 읽히지 않게 숨긴다. */}
        <ul className={styles.list}>{cards}</ul>
        <ul className={styles.list} aria-hidden="true">
          {cards}
        </ul>
      </div>
    </section>
  );
}
