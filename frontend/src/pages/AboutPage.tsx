import { lazy, Suspense, useState } from "react";
import { useTranslation } from "react-i18next";
import { usePressClippings } from "../api/pressClippings";
import { useCan } from "../hooks/useCan";
import styles from "./AboutPage.module.css";

// 관리자 전용 API 경로가 익명 방문자 번들에 섞이면 안 되므로 React.lazy로만 import한다
// (CLAUDE.md §3.5·§9).
const PressClippingManagementPanel = lazy(() => import("../features/editing/PressClippingManagementPanel"));

type Tab = "about" | "press";

/**
 * 소개 페이지는 두 탭으로 구성된다(2026-09-04 결정) — "소개"는 거의 안 바뀌는 정적 카피라
 * 번역 리소스에 직접 둔다(PIN·발행 흐름까지 갖춘 CMS를 문장 하나 위해 유지할 이유가 없다).
 * "보도자료"는 반대로 운영자가 계속 추가하는 콘텐츠라 실제 백엔드(PressClipping)로 관리한다
 * — 정적 페이지 안에 부분적으로 동적인 탭 하나가 얹힌 구조다.
 */
export default function AboutPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>("about");
  const canEditPress = useCan("pressClipping:edit");
  const [managing, setManaging] = useState(false);
  const { data: clippings, isLoading } = usePressClippings();

  const showPanel = canEditPress && managing;

  return (
    <main className={styles.page}>
      <h1 className={styles.srOnly}>{t("nav.about")}</h1>

      <div className={styles.tabs} role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={tab === "about"}
          className={tab === "about" ? `${styles.tab} ${styles.tabActive}` : styles.tab}
          onClick={() => setTab("about")}
        >
          {t("about.tabAbout")}
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === "press"}
          className={tab === "press" ? `${styles.tab} ${styles.tabActive}` : styles.tab}
          onClick={() => setTab("press")}
        >
          {t("about.tabPress")}
        </button>
      </div>

      {tab === "about" && <p className={styles.intro}>{t("about.introText")}</p>}

      {tab === "press" && (
        <div className={styles.pressSection}>
          {canEditPress && (
            <button
              type="button"
              className={styles.manageToggle}
              aria-pressed={managing}
              onClick={() => setManaging((prev) => !prev)}
            >
              {managing ? t("editing.exitEditMode") : t("press.manageToggle")}
            </button>
          )}

          {showPanel ? (
            <Suspense fallback={<p className={styles.status}>{t("press.loading")}</p>}>
              <PressClippingManagementPanel />
            </Suspense>
          ) : (
            <>
              {isLoading && <p className={styles.status}>{t("press.loading")}</p>}
              {!isLoading && clippings?.length === 0 && <p className={styles.status}>{t("press.empty")}</p>}
              <ul className={styles.pressGrid}>
                {clippings?.map((clipping) => (
                  <li key={clipping.id}>
                    <a
                      href={clipping.externalUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={styles.pressCard}
                      aria-label={`${clipping.title} (${t("booking.opensNewWindow")})`}
                    >
                      {clipping.imageUrl && (
                        <img
                          src={clipping.imageUrl}
                          alt={clipping.imageAlt ?? ""}
                          className={styles.pressImage}
                          loading="lazy"
                          decoding="async"
                        />
                      )}
                      <span className={styles.pressTitle}>{clipping.title}</span>
                    </a>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </main>
  );
}
