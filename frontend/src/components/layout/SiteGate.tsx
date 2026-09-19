import type { ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { Route, Routes } from "react-router-dom";
import { getSiteStatus } from "../../api/site";
import { queryKeys } from "../../api/queryKeys";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import LoginPage from "../../pages/LoginPage";
import PrivatePage from "../../pages/PrivatePage";

type SiteGateProps = { children: ReactNode };

/**
 * 오픈 전 비공개 동안(2026-09-19) 관리자가 아니면 "준비 중" 화면만 보여준다. 관리자가 로그인할
 * 수 있게 /login만 열어둔다. 이 게이트는 화면 편의일 뿐 보안 장치가 아니다 — 실제로 막는 건
 * 서버가 비공개 동안 /api/**를 관리자에게만 열어두는 것이다(§3.5).
 */
export function SiteGate({ children }: SiteGateProps) {
  const { session, isInitializing } = useAdminAuth();
  const { data, isLoading } = useQuery({
    queryKey: queryKeys.site.status,
    queryFn: getSiteStatus,
    staleTime: 30 * 1000,
    retry: false,
  });

  if (isLoading || isInitializing) {
    return null;
  }

  // 상태를 못 읽으면(백엔드 없이 프론트만 띄운 로컬 등) 화면은 열어둔다 — 서버가 어차피 막는다.
  const isPrivate = data?.open === false;
  if (!isPrivate || session) {
    return <>{children}</>;
  }

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="*" element={<PrivatePage />} />
    </Routes>
  );
}
