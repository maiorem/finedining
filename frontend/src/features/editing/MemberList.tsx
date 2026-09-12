import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import { listAccountsForAdmin } from "../../api/accountAdmin";
import { queryKeys } from "../../api/queryKeys";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import styles from "./MemberList.module.css";

function formatJoinedDate(iso: string) {
  return new Intl.DateTimeFormat("ko-KR", { timeZone: "Asia/Seoul", dateStyle: "medium" }).format(new Date(iso));
}

/**
 * 카카오로 로그인한 일반 회원 정보를 보여주는 관리자 전용 목록(2026-09-12) — 조회
 * 전용이다. 카카오 내부 식별자(providerUserId)는 서버가 애초에 내려주지 않는다(§3.2·§7.7).
 * email은 노출하지만 개인정보처리방침에 이 열람 목적이 아직 명시돼 있지 않다 — 오픈 전
 * 별도 문서 작업이 필요하다(§15).
 * MembersPage에서 React.lazy로만 import된다(§3.5·§9).
 */
export default function MemberList() {
  const { t } = useTranslation();
  const { session } = useAdminAuth();

  const { data: accounts, isLoading } = useQuery({
    queryKey: queryKeys.accounts.adminList,
    queryFn: () => listAccountsForAdmin(session!.accessToken),
    enabled: Boolean(session),
  });

  if (isLoading) {
    return <p className={styles.status}>{t("members.loading")}</p>;
  }

  if (accounts?.length === 0) {
    return <p className={styles.status}>{t("members.empty")}</p>;
  }

  return (
    <div className={styles.tableWrapper}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th scope="col">{t("members.nicknameLabel")}</th>
            <th scope="col">{t("members.emailLabel")}</th>
            <th scope="col">{t("members.providerLabel")}</th>
            <th scope="col">{t("members.localeLabel")}</th>
            <th scope="col">{t("members.statusLabel")}</th>
            <th scope="col">{t("members.joinedAtLabel")}</th>
          </tr>
        </thead>
        <tbody>
          {accounts?.map((account) => (
            <tr key={account.id}>
              <td>{account.nickname}</td>
              <td>{account.email ?? t("members.noEmail")}</td>
              <td>{account.provider}</td>
              <td>{account.locale}</td>
              <td>
                <span className={account.status === "WITHDRAWN" ? styles.withdrawn : undefined}>
                  {t(`members.status.${account.status}`)}
                </span>
              </td>
              <td>{formatJoinedDate(account.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
