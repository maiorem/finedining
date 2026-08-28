import { lazy, Suspense, useState } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useReview } from "../api/reviews";
import { ApiError } from "../api/http";
import { useCan } from "../hooks/useCan";
import { useNoIndex } from "../hooks/useNoIndex";
import styles from "./ReviewDetailPage.module.css";

// 관리자 전용 API 경로가 익명 방문자 번들에 섞이면 안 되므로 React.lazy로만 import한다
// (CLAUDE.md §3.5·§9).
const ReviewEditForm = lazy(() => import("../features/editing/ReviewEditForm"));

export default function ReviewDetailPage() {
  const { id } = useParams();
  const reviewId = Number(id);
  const { t } = useTranslation();
  const canModerate = useCan("review:moderate");
  const [moderationMode, setModerationMode] = useState(false);

  useNoIndex();

  const { data: review, isLoading, error } = useReview(reviewId);
  const showAdminView = canModerate && moderationMode;

  return (
    <main className={styles.page}>
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
          <ReviewEditForm reviewId={reviewId} />
        </Suspense>
      ) : (
        renderPublicContent()
      )}
    </main>
  );

  function renderPublicContent() {
    if (isLoading) {
      return <p className={styles.status}>{t("reviews.loading")}</p>;
    }

    if (error || !review) {
      const notFound = error instanceof ApiError && error.code === "ENTITY_NOT_FOUND";
      return <p className={styles.status}>{notFound ? t("reviews.notFound") : t("reviews.loadError")}</p>;
    }

    return (
      <>
        <h1 className={styles.title}>{review.title}</h1>
        <p className={styles.body}>{review.body}</p>

        {review.comments.length > 0 && (
          <section className={styles.comments}>
            <h2 className={styles.commentsHeading}>{t("reviews.commentsHeading")}</h2>
            <ul className={styles.commentList}>
              {review.comments.map((comment) => (
                <li key={comment.id} className={styles.comment}>
                  <p className={styles.commentBody}>{comment.body}</p>
                </li>
              ))}
            </ul>
          </section>
        )}
      </>
    );
  }
}
