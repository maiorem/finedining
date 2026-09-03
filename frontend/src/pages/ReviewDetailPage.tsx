import { lazy, Suspense, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { useReview } from "../api/reviews";
import { deleteOwnReview, updateOwnReview } from "../api/reviewMember";
import { queryKeys } from "../api/queryKeys";
import { ApiError } from "../api/http";
import { useCan } from "../hooks/useCan";
import { useMemberAuth } from "../contexts/MemberAuthContext";
import { useNoIndex } from "../hooks/useNoIndex";
import styles from "./ReviewDetailPage.module.css";

// 관리자 전용 API 경로가 익명 방문자 번들에 섞이면 안 되므로 React.lazy로만 import한다
// (CLAUDE.md §3.5·§9).
const ReviewEditForm = lazy(() => import("../features/editing/ReviewEditForm"));

const KNOWN_ERROR_CODES = ["VALIDATION_ERROR", "RATE_LIMITED", "POST_NOT_OWNED"] as const;

function errorMessageKey(code: string): string {
  return (KNOWN_ERROR_CODES as readonly string[]).includes(code) ? `reviews.error.${code}` : "reviews.error.generic";
}

export default function ReviewDetailPage() {
  const { id } = useParams();
  const reviewId = Number(id);
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const canModerate = useCan("review:moderate");
  const [moderationMode, setModerationMode] = useState(false);
  const { session: memberSession } = useMemberAuth();

  useNoIndex();

  const { data: review, isLoading, error } = useReview(reviewId);
  const showAdminView = canModerate && moderationMode;

  const [selfEditing, setSelfEditing] = useState(false);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  function startSelfEdit() {
    if (!review) return;
    setTitle(review.title);
    setBody(review.body);
    setFormError(null);
    setSelfEditing(true);
  }

  async function handleSelfEditSubmit(e: FormEvent) {
    e.preventDefault();
    if (!memberSession) return;
    setSubmitting(true);
    setFormError(null);
    try {
      await updateOwnReview(memberSession.accessToken, reviewId, title, body);
      setSelfEditing(false);
      void queryClient.invalidateQueries({ queryKey: queryKeys.reviews.all });
    } catch (err) {
      const code = err instanceof ApiError ? err.code : "UNKNOWN";
      setFormError(t(errorMessageKey(code)));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSelfDelete() {
    if (!memberSession) return;
    await deleteOwnReview(memberSession.accessToken, reviewId);
    void queryClient.invalidateQueries({ queryKey: queryKeys.reviews.all });
    navigate("/reviews");
  }

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

    const isOwner = memberSession?.accountId === review.accountId;

    if (selfEditing) {
      return (
        <form className={styles.selfEditForm} onSubmit={handleSelfEditSubmit}>
          <div className={styles.field}>
            <label htmlFor="review-title">{t("editing.panel.titleLabel")}</label>
            <input id="review-title" type="text" value={title} onChange={(e) => setTitle(e.target.value)} required />
          </div>
          <div className={styles.field}>
            <label htmlFor="review-body">{t("reviews.bodyLabel")}</label>
            <textarea id="review-body" rows={8} value={body} onChange={(e) => setBody(e.target.value)} required />
          </div>

          {formError && (
            <p className={styles.error} role="alert">
              {formError}
            </p>
          )}

          <div className={styles.selfActions}>
            <button type="submit" className={styles.submit} disabled={submitting}>
              {t("reviews.save")}
            </button>
            <button type="button" onClick={() => setSelfEditing(false)}>
              {t("reviews.cancel")}
            </button>
          </div>
        </form>
      );
    }

    return (
      <>
        <h1 className={styles.title}>{review.title}</h1>
        <p className={styles.body}>{review.body}</p>

        {isOwner && (
          <div className={styles.selfActions}>
            <button type="button" onClick={startSelfEdit}>
              {t("reviews.edit")}
            </button>
            <button type="button" onClick={() => void handleSelfDelete()}>
              {t("reviews.delete")}
            </button>
          </div>
        )}

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
