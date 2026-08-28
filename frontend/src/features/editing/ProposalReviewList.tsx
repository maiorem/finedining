import { useTranslation } from "react-i18next";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { acceptProposal, declineProposal, listProposalsForAdmin, type ProposalStatus } from "../../api/proposalAdmin";
import { queryKeys } from "../../api/queryKeys";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import styles from "./ProposalReviewList.module.css";

function formatAdminTime(iso: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

const OPEN_STATUSES: ProposalStatus[] = ["SENT", "READ"];

/**
 * 협업 제안을 게시글처럼 목록으로 보여주는 관리자 전용 뷰. ProposalPage에서 React.lazy로만
 * import된다 — 관리자 전용 API 경로가 익명 방문자 번들에 섞이면 안 된다(CLAUDE.md §3.5·§9).
 */
export default function ProposalReviewList() {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const queryClient = useQueryClient();

  const { data: proposals, isLoading } = useQuery({
    queryKey: queryKeys.proposals.adminList,
    queryFn: () => listProposalsForAdmin(session!.accessToken),
    enabled: Boolean(session),
  });

  function invalidate() {
    void queryClient.invalidateQueries({ queryKey: queryKeys.proposals.all });
  }

  async function handleAccept(id: number) {
    if (!session) return;
    await acceptProposal(session.accessToken, id);
    invalidate();
  }

  async function handleDecline(id: number) {
    if (!session) return;
    await declineProposal(session.accessToken, id);
    invalidate();
  }

  if (isLoading) {
    return <p className={styles.status}>{t("proposal.admin.loading")}</p>;
  }

  if (proposals?.length === 0) {
    return <p className={styles.status}>{t("proposal.admin.empty")}</p>;
  }

  return (
    <ul className={styles.list}>
      {proposals?.map((proposal) => (
        <li key={proposal.id} className={styles.item}>
          <div className={styles.row}>
            <h3 className={styles.title}>{proposal.title}</h3>
            <span className={styles.statusBadge}>{t(`proposal.admin.status.${proposal.status}`)}</span>
          </div>
          <p className={styles.meta}>
            {proposal.name} · {proposal.contactEmail} · {formatAdminTime(proposal.createdAt)}
          </p>
          <p className={styles.body}>{proposal.body}</p>

          {OPEN_STATUSES.includes(proposal.status) && (
            <div className={styles.actions}>
              <button type="button" onClick={() => void handleAccept(proposal.id)}>
                {t("proposal.admin.accept")}
              </button>
              <button type="button" onClick={() => void handleDecline(proposal.id)}>
                {t("proposal.admin.decline")}
              </button>
            </div>
          )}
        </li>
      ))}
    </ul>
  );
}
