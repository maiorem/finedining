import { lazy, Suspense, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useReviews } from "../api/reviews";
import { useCan } from "../hooks/useCan";
import { useNoIndex } from "../hooks/useNoIndex";
import styles from "./ReviewsPage.module.css";

// 숨김·복구·삭제 API 경로가 익명 방문자 번들에 섞이면 안 되므로 React.lazy로만 import한다
// (CLAUDE.md §3.5·§9).
const ReviewModerationList = lazy(() => import("../features/editing/ReviewModerationList"));

export default function ReviewsPage() {
  const { t } = useTranslation();
  const canModerate = useCan("review:moderate");
  const [moderationMode, setModerationMode] = useState(false);

  // 회원 글이 검색에 노출되면 개인정보·저작권 문제가 사이트 책임이 된다 (CLAUDE.md §3.6).
  useNoIndex();

  const publicQuery = useReviews();
  const showAdminView = canModerate && moderationMode;

  return (
    <main className={styles.page}>
      <h1 className={styles.heading}>{t("nav.reviews")}</h1>
      <p className={styles.writeNotice}>{t("reviews.writeComingSoon")}</p>

      {canModerate && (
        <button
          type="button"
          className={styles.moderationToggle}
          aria-pressed={moderationMode}
          onClick={() => setModerationMode((prev) => !prev)}
        >
          {moderationMode ? t("editing.exitEditMode") : t("reviews.moderationToggle")}
        </button>
      )}

      {showAdminView ? (
        <Suspense fallback={<p className={styles.status}>{t("reviews.loading")}</p>}>
          <ReviewModerationList />
        </Suspense>
      ) : (
        <>
          {publicQuery.isLoading && <p className={styles.status}>{t("reviews.loading")}</p>}
          {!publicQuery.isLoading && publicQuery.data?.length === 0 && (
            <p className={styles.status}>{t("reviews.empty")}</p>
          )}
          <ul className={styles.list}>
            {publicQuery.data?.map((review) => (
              <li key={review.id} className={styles.item}>
                <Link to={`/reviews/${review.id}`} className={styles.itemLink}>
                  <h3 className={styles.itemTitle}>{review.title}</h3>
                </Link>
              </li>
            ))}
          </ul>
        </>
      )}
    </main>
  );
}
