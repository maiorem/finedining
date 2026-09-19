import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { loginAdmin, logoutAdmin, refreshAdminSession, type AdminSession } from "../api/auth";
import { registerAdminSessionHandlers } from "../api/adminHttp";
import { registerPublicAuthorizer } from "../api/http";

type AdminAuthContextValue = {
  session: AdminSession | null;
  isInitializing: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
};

const AdminAuthContext = createContext<AdminAuthContextValue | null>(null);

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AdminSession | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);
  // 공개 조회(apiGet)가 렌더 직후 바로 최신 토큰을 읽어야 해서 ref에 렌더 시점 값을 둔다.
  const tokenRef = useRef<string | null>(null);
  tokenRef.current = session?.accessToken ?? null;

  // 새로고침해도 로그인 상태를 유지하되 access token은 localStorage에 두지 않는다(§7.4) —
  // 대신 HttpOnly refresh 쿠키로 조용히 재발급받는다. 쿠키가 없거나 만료됐으면 그냥 로그아웃 상태.
  useEffect(() => {
    let cancelled = false;
    refreshAdminSession()
      .then((restored) => {
        if (!cancelled) setSession(restored);
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setIsInitializing(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // adminHttp의 401 재시도 로직이 조용히 재발급한 토큰을 이 컨텍스트에도 반영한다 — 그래야
  // 다음 호출부터 새 토큰을 쓴다. refresh 쿠키마저 만료되면 세션을 비워 로그인 화면으로
  // 돌려보낸다(§7.4 — 편집 패널이 무한 로딩에 멈추던 버그).
  useEffect(() => {
    registerAdminSessionHandlers({
      onRefreshed: (refreshed) => setSession(refreshed),
      onExpired: () => setSession(null),
    });
  }, []);

  useEffect(() => {
    registerPublicAuthorizer({
      getToken: () => tokenRef.current,
      refreshToken: () =>
        refreshAdminSession()
          .then((refreshed) => {
            setSession(refreshed);
            return refreshed.accessToken;
          })
          .catch(() => {
            setSession(null);
            return null;
          }),
    });
  }, []);

  async function login(username: string, password: string) {
    const next = await loginAdmin(username, password);
    setSession(next);
  }

  async function logout() {
    await logoutAdmin();
    setSession(null);
  }

  return (
    <AdminAuthContext.Provider value={{ session, isInitializing, login, logout }}>
      {children}
    </AdminAuthContext.Provider>
  );
}

export function useAdminAuth(): AdminAuthContextValue {
  const context = useContext(AdminAuthContext);
  if (!context) {
    throw new Error("useAdminAuth는 AdminAuthProvider 안에서만 쓸 수 있다.");
  }
  return context;
}
