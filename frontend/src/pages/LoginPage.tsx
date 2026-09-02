import { useState, type FormEvent } from "react";
import { useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ApiError } from "../api/http";
import { useAdminAuth } from "../contexts/AdminAuthContext";
import { useMemberAuth } from "../contexts/MemberAuthContext";
import { useNoIndex } from "../hooks/useNoIndex";
import kakaoLoginImage from "../assets/kakao-login.png";
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

const KAKAO_ERROR_CODES = ["SIGNUP_NOT_ALLOWED", "OAUTH_FAILED"] as const;

function kakaoErrorMessageKey(code: string): string {
  return (KAKAO_ERROR_CODES as readonly string[]).includes(code)
    ? `login.kakaoError.${code}`
    : "login.kakaoError.generic";
}

export default function LoginPage() {
  const { t } = useTranslation();
  const { session, isInitializing, login, logout } = useAdminAuth();
  const {
    session: memberSession,
    isInitializing: memberInitializing,
    logout: memberLogout,
  } = useMemberAuth();
  const [searchParams] = useSearchParams();
  const kakaoError = searchParams.get("error");
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

        {kakaoError && (
          <p className={styles.error} role="alert">
            {t(kakaoErrorMessageKey(kakaoError))}
          </p>
        )}

        {!memberInitializing &&
          (memberSession ? (
            <div className={styles.sessionCard}>
              <p className={styles.sessionText}>{t("login.memberLoggedInAs", { nickname: memberSession.nickname })}</p>
              <button type="button" className={styles.logoutButton} onClick={() => void memberLogout()}>
                {t("login.logout")}
              </button>
            </div>
          ) : (
            <a className={styles.kakaoButtonActive} href="/api/oauth2/authorization/kakao">
              {/* 카카오 로그인 버튼 리소스 — 브랜드 가이드상 리사이즈·재채색하지 않고 원본 그대로 쓴다. */}
              <img src={kakaoLoginImage} alt={t("login.kakao")} width={300} height={45} />
            </a>
          ))}

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
