import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { withdrawMember } from "../api/memberAuth";
import { useMemberAuth } from "../contexts/MemberAuthContext";
import { useNoIndex } from "../hooks/useNoIndex";
import styles from "./AccountPage.module.css";

// 개인정보처리방침이 안내하는 "마이페이지/회원정보" 자리다 — 지금은 회원 탈퇴만 있다(§3.2).
export default function AccountPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { session, isInitializing, logout } = useMemberAuth();
  const [confirmed, setConfirmed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(false);
  useNoIndex();

  if (isInitializing) return null;

  if (!session) {
    return (
      <main className={styles.page}>
        <h1 className={styles.title}>{t("account.title")}</h1>
        <p className={styles.text}>
          {t("account.needLogin")} <Link to="/login">{t("nav.login")}</Link>
        </p>
      </main>
    );
  }

  async function handleWithdraw() {
    if (!session) return;
    setSubmitting(true);
    setError(false);
    try {
      await withdrawMember(session.accessToken);
      // 서버가 이미 refresh 쿠키를 지웠다 — 화면의 로그인 상태만 비운다.
      await logout().catch(() => undefined);
      void queryClient.invalidateQueries();
      navigate("/");
    } catch {
      setError(true);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className={styles.page}>
      <h1 className={styles.title}>{t("account.title")}</h1>
      <p className={styles.loggedIn}>{t("account.loggedInAs", { nickname: session.nickname })}</p>
      <button
        type="button"
        className={styles.logout}
        onClick={() => {
          if (window.confirm(t("nav.logoutConfirm"))) {
            void logout().then(() => navigate("/"));
          }
        }}
      >
        {t("login.logout")}
      </button>

      <section className={styles.withdraw}>
        <h2 className={styles.heading}>{t("account.withdraw.heading")}</h2>
        <ul className={styles.info}>
          {[1, 2, 3, 4].map((n) => (
            <li key={n}>{t(`account.withdraw.info${n}`)}</li>
          ))}
        </ul>
        <label className={styles.confirm}>
          <input type="checkbox" checked={confirmed} onChange={(e) => setConfirmed(e.target.checked)} />
          <span>{t("account.withdraw.confirm")}</span>
        </label>
        {error && (
          <p className={styles.error} role="alert">
            {t("account.withdraw.error")}
          </p>
        )}
        <button
          type="button"
          className={styles.submit}
          disabled={!confirmed || submitting}
          onClick={() => void handleWithdraw()}
        >
          {t("account.withdraw.submit")}
        </button>
      </section>
    </main>
  );
}
