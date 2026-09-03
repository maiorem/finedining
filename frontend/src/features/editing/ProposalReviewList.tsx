import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import { listProposalsForAdmin } from "../../api/proposalAdmin";
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

/**
 * 협업 제안을 게시글처럼 목록으로 보여주는 관리자 전용 뷰. 열람만 한다 — 수락/거절은 상태
 * 전환 시 발신자에게 보내는 이메일 알림(EmailNotifier)이 아직 no-op라 실질적 효과가 없어
 * 뺐다(2026-08-29 결정). 이메일 연동이 들어오면 accept/decline 버튼을 다시 붙인다 —
 * 백엔드 API(POST /{id}/accept·decline)는 그대로 남아 있다.
 *
 * ProposalPage에서 React.lazy로만 import된다 — 관리자 전용 API 경로가 익명 방문자 번들에
 * 섞이면 안 된다(CLAUDE.md §3.5·§9).
 */
export default function ProposalReviewList() {
  const { t } = useTranslation();
  const { session } = useAdminAuth();

  const { data: proposals, isLoading } = useQuery({
    queryKey: queryKeys.proposals.adminList,
    queryFn: () => listProposalsForAdmin(session!.accessToken),
    enabled: Boolean(session),
  });

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
            {proposal.name} · {proposal.contactEmail}
            {proposal.category && <> · {t(`proposal.category.${proposal.category}`)}</>} ·{" "}
            {formatAdminTime(proposal.createdAt)}
          </p>
          <p className={styles.body}>{proposal.body}</p>
        </li>
      ))}
    </ul>
  );
}
