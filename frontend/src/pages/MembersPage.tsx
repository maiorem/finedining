import { lazy, Suspense } from "react";
import { useTranslation } from "react-i18next";
import { useCan } from "../hooks/useCan";
import { useNoIndex } from "../hooks/useNoIndex";
import styles from "./MembersPage.module.css";

// 관리자 전용 API 경로가 익명 방문자 번들에 섞이면 안 되므로 React.lazy로만 import한다
// (CLAUDE.md §3.5·§9).
const MemberList = lazy(() => import("../features/editing/MemberList"));

/**
 * 관리자 로그인 상태에서만 실제 내용이 보이는 회원(카카오 로그인) 목록 페이지(2026-09-12).
 * 검색엔진에 노출할 이유가 없어 /login·리뷰와 같이 noindex를 붙인다(§3.5·§10). 관리자가
 * 아니면 빈 화면만 보인다 — 서버가 이미 /api/accounts/manage를 EDITOR 이상으로 막고 있으니
 * (§3.5) 이 조건부 렌더링은 편의일 뿐 보안 장치가 아니다.
 */
export default function MembersPage() {
  const { t } = useTranslation();
  const canView = useCan("account:view");
  useNoIndex();

  return (
    <main className={styles.page}>
      <p className={styles.eyebrow}>{t("members.eyebrow")}</p>
      <h1 className={styles.heading}>{t("members.heading")}</h1>

      {canView && (
        <Suspense fallback={<p className={styles.status}>{t("members.loading")}</p>}>
          <MemberList />
        </Suspense>
      )}
    </main>
  );
}
