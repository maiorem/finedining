import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { logoutMember, refreshMemberSession, type MemberSession } from "../api/memberAuth";
import { registerMemberSessionHandlers } from "../api/memberHttp";

type MemberAuthContextValue = {
  session: MemberSession | null;
  isInitializing: boolean;
  /** 카카오 리다이렉트 콜백 페이지가 URL 프래그먼트에서 토큰을 읽은 뒤 호출한다. */
  setSession: (session: MemberSession) => void;
  logout: () => Promise<void>;
};

const MemberAuthContext = createContext<MemberAuthContextValue | null>(null);

/**
 * 일반 회원(카카오) 세션. 관리자(AdminAuthContext)와 완전히 별개다 — 로그인 수단도, 권한도
 * 다르다(CLAUDE.md §1·§3.1). 로그인 자체는 이 컨텍스트에 없다 — 카카오 리다이렉트 흐름이
 * 로그인이고, 성공하면 /oauth/callback이 setSession을 호출한다.
 */
export function MemberAuthProvider({ children }: { children: ReactNode }) {
  const [session, setSessionState] = useState<MemberSession | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);

  // 새로고침해도 로그인 상태를 유지하되 access token은 localStorage에 두지 않는다(§7.4) —
  // 대신 HttpOnly refresh 쿠키로 조용히 재발급받는다. 쿠키가 없거나 만료됐으면 그냥 로그아웃 상태.
  useEffect(() => {
    let cancelled = false;
    refreshMemberSession()
      .then((restored) => {
        if (!cancelled) setSessionState(restored);
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setIsInitializing(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // memberHttp의 401 재시도 로직이 조용히 재발급한 토큰을 이 컨텍스트에도 반영한다 — 그래야
  // 다음 호출부터 새 토큰을 쓴다(adminHttp와 동일한 이유, §7.4).
  useEffect(() => {
    registerMemberSessionHandlers({
      onRefreshed: (refreshed) => setSessionState(refreshed),
      onExpired: () => setSessionState(null),
    });
  }, []);

  async function logout() {
    await logoutMember();
    setSessionState(null);
  }

  return (
    <MemberAuthContext.Provider value={{ session, isInitializing, setSession: setSessionState, logout }}>
      {children}
    </MemberAuthContext.Provider>
  );
}

export function useMemberAuth(): MemberAuthContextValue {
  const context = useContext(MemberAuthContext);
  if (!context) {
    throw new Error("useMemberAuth는 MemberAuthProvider 안에서만 쓸 수 있다.");
  }
  return context;
}
