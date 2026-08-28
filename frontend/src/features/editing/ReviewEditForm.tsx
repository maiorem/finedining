import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/http";
import { deleteReviewComment, getReviewForAdmin, updateReviewContent } from "../../api/reviewAdmin";
import { queryKeys } from "../../api/queryKeys";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import styles from "./ReviewEditForm.module.css";

type ReviewEditFormProps = {
  reviewId: number;
};

/**
 * 리뷰 상세의 관리자 모더레이션 뷰(원문 수정 + 댓글 삭제). ReviewDetailPage에서 React.lazy로만
 * import된다 — 관리자 전용 API 경로가 익명 방문자 번들에 섞이면 안 된다(CLAUDE.md §3.5·§9).
 */
export default function ReviewEditForm({ reviewId }: ReviewEditFormProps) {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: queryKeys.reviews.adminDetail(reviewId),
    queryFn: () => getReviewForAdmin(session!.accessToken, reviewId),
    enabled: Boolean(session) && Number.isFinite(reviewId),
  });

  const [draftTitle, setDraftTitle] = useState("");
  const [draftBody, setDraftBody] = useState("");
  const [saveNotice, setSaveNotice] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (data) {
      setDraftTitle(data.title);
      setDraftBody(data.body);
    }
  }, [data]);

  function invalidate() {
    void queryClient.invalidateQueries({ queryKey: queryKeys.reviews.all });
  }

  async function handleSave() {
    if (!session) return;
    setSaving(true);
    setSaveError(null);
    setSaveNotice(null);
    try {
      await updateReviewContent(session.accessToken, reviewId, draftTitle, draftBody);
      setSaveNotice(t("editing.panel.saved"));
      invalidate();
    } catch (err) {
      setSaveError(err instanceof ApiError ? err.message : t("editing.panel.saveFailed"));
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteComment(commentId: number) {
    if (!session) return;
    await deleteReviewComment(session.accessToken, commentId);
    invalidate();
  }

  if (isLoading || !data) {
    return <p className={styles.status}>{t("reviews.loading")}</p>;
  }

  return (
    <div>
      <div className={styles.editForm}>
        <label className={styles.field}>
          <span>{t("editing.panel.titleLabel")}</span>
          <input type="text" value={draftTitle} onChange={(e) => setDraftTitle(e.target.value)} />
        </label>
        <label className={styles.field}>
          <span>{t("reviews.bodyLabel")}</span>
          <textarea rows={8} value={draftBody} onChange={(e) => setDraftBody(e.target.value)} />
        </label>
        {saveNotice && <p className={styles.notice}>{saveNotice}</p>}
        {saveError && (
          <p className={styles.error} role="alert">
            {saveError}
          </p>
        )}
        <button type="button" className={styles.saveButton} disabled={saving} onClick={() => void handleSave()}>
          {t("editing.panel.saveDraft")}
        </button>
      </div>

      {data.comments.length > 0 && (
        <section className={styles.comments}>
          <h2 className={styles.commentsHeading}>{t("reviews.commentsHeading")}</h2>
          <ul className={styles.commentList}>
            {data.comments.map((comment) => (
              <li key={comment.id} className={styles.comment}>
                <p className={styles.commentBody}>{comment.body}</p>
                <button type="button" onClick={() => void handleDeleteComment(comment.id)}>
                  {t("editing.image.delete")}
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
