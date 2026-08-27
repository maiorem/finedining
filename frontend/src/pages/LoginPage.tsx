import { useEffect, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { ApiError } from "../api/http";
import { useAdminAuth } from "../contexts/AdminAuthContext";
import styles from "./LoginPage.module.css";

const KNOWN_ERROR_CODES = [
  "INVALID_CREDENTIALS",
  "ACCOUNT_LOCKED",
  "RATE_LIMITED",
  "VALIDATION_ERROR",
] as const;

function errorMessageKey(code: string): string {
  return (KNOWN_ERROR_CODES as readonly string[]).includes(code)
    ? `login.error.${code}`
    : "login.error.generic";
}

// 로그인 진입점은 /login 하나뿐이고 검색엔진에는 노출하지 않는다 (CLAUDE.md §3.5·§10).
function useNoIndex() {
  useEffect(() => {
    const meta = document.createElement("meta");
    meta.name = "robots";
    meta.content = "noindex, nofollow";
    document.head.appendChild(meta);
    return () => {
      document.head.removeChild(meta);
    };
  }, []);
}

export default function LoginPage() {
  const { t } = useTranslation();
  const { session, isInitializing, login, logout } = useAdminAuth();
  const [adminFormOpen, setAdminFormOpen] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useNoIndex();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username, password);
      setPassword("");
    } catch (err) {
      const code = err instanceof ApiError ? err.code : "UNKNOWN";
      setError(t(errorMessageKey(code)));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <p className={styles.eyebrow}>{t("login.eyebrow")}</p>
        <h1 className={styles.heading}>{t("login.heading")}</h1>

        {isInitializing ? null : session ? (
          <div className={styles.sessionCard}>
            <p className={styles.sessionText}>
              {t("login.loggedInAs", { username: session.username, role: session.role })}
            </p>
            <button type="button" className={styles.logoutButton} onClick={() => void logout()}>
              {t("login.logout")}
            </button>
          </div>
        ) : (
          <>
            <button
              type="button"
              className={styles.kakaoButton}
              disabled
              aria-disabled="true"
              title={t("login.kakaoComingSoon")}
            >
              {t("login.kakao")}
            </button>
            <p className={styles.kakaoNote}>{t("login.kakaoComingSoon")}</p>

            <button
              type="button"
              className={styles.adminToggle}
              aria-expanded={adminFormOpen}
              onClick={() => setAdminFormOpen((open) => !open)}
            >
              {t("login.adminToggle")}
            </button>

            {adminFormOpen && (
              <form className={styles.adminForm} onSubmit={handleSubmit}>
                <div className={styles.field}>
                  <label htmlFor="admin-username">{t("login.adminUsername")}</label>
                  <input
                    id="admin-username"
                    type="text"
                    autoComplete="username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    required
                  />
                </div>
                <div className={styles.field}>
                  <label htmlFor="admin-password">{t("login.adminPassword")}</label>
                  <input
                    id="admin-password"
                    type="password"
                    autoComplete="current-password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                </div>

                {error && (
                  <p className={styles.error} role="alert">
                    {error}
                  </p>
                )}

                <button type="submit" className={styles.submit} disabled={submitting}>
                  {t("login.adminSubmit")}
                </button>
              </form>
            )}
          </>
        )}
      </div>
    </main>
  );
}
