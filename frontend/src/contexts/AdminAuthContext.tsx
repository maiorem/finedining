import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { loginAdmin, logoutAdmin, refreshAdminSession, type AdminSession } from "../api/auth";

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
