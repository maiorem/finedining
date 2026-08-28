import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { deleteReview, hideReview, listReviewsForAdmin, restoreReview } from "../../api/reviewAdmin";
import { queryKeys } from "../../api/queryKeys";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import styles from "./ReviewModerationList.module.css";

/**
 * 리뷰 목록의 관리자 모더레이션 뷰. ReviewsPage에서 React.lazy로만 import된다 — 숨김·복구·
 * 삭제 API 경로가 익명 방문자 번들에 섞이면 안 된다(CLAUDE.md §3.5·§9).
 */
export default function ReviewModerationList() {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const queryClient = useQueryClient();

  const { data: reviews, isLoading } = useQuery({
    queryKey: queryKeys.reviews.adminList,
    queryFn: () => listReviewsForAdmin(session!.accessToken),
    enabled: Boolean(session),
  });

  function invalidate() {
    void queryClient.invalidateQueries({ queryKey: queryKeys.reviews.all });
  }

  async function handleHide(id: number) {
    if (!session) return;
    await hideReview(session.accessToken, id);
    invalidate();
  }

  async function handleRestore(id: number) {
    if (!session) return;
    await restoreReview(session.accessToken, id);
    invalidate();
  }

  async function handleDelete(id: number) {
    if (!session) return;
    await deleteReview(session.accessToken, id);
    invalidate();
  }

  if (isLoading) {
    return <p className={styles.status}>{t("reviews.loading")}</p>;
  }

  if (reviews?.length === 0) {
    return <p className={styles.status}>{t("reviews.empty")}</p>;
  }

  return (
    <ul className={styles.list}>
      {reviews?.map((review) => (
        <li key={review.id} className={styles.item}>
          <Link to={`/reviews/${review.id}`} className={styles.itemLink}>
            <h3 className={styles.itemTitle}>{review.title}</h3>
          </Link>
          <div className={styles.moderationRow}>
            <span className={styles.statusBadge}>{review.status}</span>
            {review.status === "PUBLISHED" && (
              <button type="button" onClick={() => void handleHide(review.id)}>
                {t("reviews.hide")}
              </button>
            )}
            {review.status === "HIDDEN" && (
              <button type="button" onClick={() => void handleRestore(review.id)}>
                {t("reviews.restore")}
              </button>
            )}
            {review.status !== "DELETED" && (
              <button type="button" onClick={() => void handleDelete(review.id)}>
                {t("reviews.delete")}
              </button>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
