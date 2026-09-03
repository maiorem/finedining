import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { ApiError } from "../api/http";
import { changeAdminPassword, setAdminPin, type AdminSession } from "../api/auth";
import styles from "./LoginPage.module.css";

const PIN_ERROR_CODES = ["INVALID_CREDENTIALS", "WEAK_PIN", "VALIDATION_ERROR"] as const;

function pinErrorMessageKey(code: string): string {
  return (PIN_ERROR_CODES as readonly string[]).includes(code)
    ? `login.pin.error.${code}`
    : "login.pin.error.generic";
}

const PASSWORD_ERROR_CODES = ["INVALID_CREDENTIALS", "VALIDATION_ERROR"] as const;

function passwordErrorMessageKey(code: string): string {
  return (PASSWORD_ERROR_CODES as readonly string[]).includes(code)
    ? `login.password.error.${code}`
    : "login.password.error.generic";
}

type AdminSessionPanelProps = {
  session: AdminSession;
  onLogout: () => void;
};

/**
 * 로그인 후 보이는 관리자 세션 카드. 로그아웃 외에 PIN·비밀번호를 직접 바꿀 수 있는 통로다 —
 * 둘 다 시드로 심은 초기값을 영구히 쓰지 않게 하기 위해서다(CLAUDE.md §3.1·§3.4). 옛 값을 몰라도
 * 되고 현재 비밀번호만 재확인하면 되는 것도 두 폼이 동일하다.
 */
export function AdminSessionPanel({ session, onLogout }: AdminSessionPanelProps) {
  const { t } = useTranslation();

  const [pinFormOpen, setPinFormOpen] = useState(false);
  const [currentPasswordForPin, setCurrentPasswordForPin] = useState("");
  const [newPin, setNewPin] = useState("");
  const [pinError, setPinError] = useState<string | null>(null);
  const [pinSuccess, setPinSuccess] = useState(false);
  const [pinSubmitting, setPinSubmitting] = useState(false);

  const [passwordFormOpen, setPasswordFormOpen] = useState(false);
  const [currentPasswordForPassword, setCurrentPasswordForPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);

  async function handleSetPin(e: FormEvent) {
    e.preventDefault();
    setPinError(null);
    setPinSuccess(false);
    setPinSubmitting(true);
    try {
      await setAdminPin(session.accessToken, currentPasswordForPin, newPin);
      setCurrentPasswordForPin("");
      setNewPin("");
      setPinSuccess(true);
    } catch (err) {
      const code = err instanceof ApiError ? err.code : "UNKNOWN";
      setPinError(t(pinErrorMessageKey(code)));
    } finally {
      setPinSubmitting(false);
    }
  }

  async function handleChangePassword(e: FormEvent) {
    e.preventDefault();
    setPasswordError(null);
    setPasswordSuccess(false);
    setPasswordSubmitting(true);
    try {
      await changeAdminPassword(session.accessToken, currentPasswordForPassword, newPassword);
      setCurrentPasswordForPassword("");
      setNewPassword("");
      setPasswordSuccess(true);
    } catch (err) {
      const code = err instanceof ApiError ? err.code : "UNKNOWN";
      setPasswordError(t(passwordErrorMessageKey(code)));
    } finally {
      setPasswordSubmitting(false);
    }
  }

  return (
    <div className={styles.sessionCard}>
      <p className={styles.sessionText}>
        {t("login.loggedInAs", { username: session.username, role: session.role })}
      </p>
      <button type="button" className={styles.logoutButton} onClick={onLogout}>
        {t("login.logout")}
      </button>

      <button
        type="button"
        className={styles.pinToggle}
        aria-expanded={pinFormOpen}
        onClick={() => {
          setPinFormOpen((open) => !open);
          setPinError(null);
          setPinSuccess(false);
        }}
      >
        {t("login.pin.toggle")}
      </button>

      {pinFormOpen && (
        <form className={styles.adminForm} onSubmit={handleSetPin}>
          <div className={styles.field}>
            <label htmlFor="pin-current-password">{t("login.pin.currentPasswordLabel")}</label>
            <input
              id="pin-current-password"
              type="password"
              autoComplete="current-password"
              value={currentPasswordForPin}
              onChange={(e) => setCurrentPasswordForPin(e.target.value)}
              required
            />
          </div>
          <div className={styles.field}>
            <label htmlFor="pin-new-value">{t("login.pin.newPinLabel")}</label>
            <input
              id="pin-new-value"
              type="password"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={6}
              autoComplete="off"
              value={newPin}
              onChange={(e) => setNewPin(e.target.value)}
              required
            />
          </div>

          {pinError && (
            <p className={styles.error} role="alert">
              {pinError}
            </p>
          )}
          {pinSuccess && (
            <p className={styles.success} role="status">
              {t("login.pin.success")}
            </p>
          )}

          <button type="submit" className={styles.submit} disabled={pinSubmitting || newPin.length !== 6}>
            {t("login.pin.submit")}
          </button>
        </form>
      )}

      <button
        type="button"
        className={styles.pinToggle}
        aria-expanded={passwordFormOpen}
        onClick={() => {
          setPasswordFormOpen((open) => !open);
          setPasswordError(null);
          setPasswordSuccess(false);
        }}
      >
        {t("login.password.toggle")}
      </button>

      {passwordFormOpen && (
        <form className={styles.adminForm} onSubmit={handleChangePassword}>
          <div className={styles.field}>
            <label htmlFor="password-current-password">{t("login.password.currentPasswordLabel")}</label>
            <input
              id="password-current-password"
              type="password"
              autoComplete="current-password"
              value={currentPasswordForPassword}
              onChange={(e) => setCurrentPasswordForPassword(e.target.value)}
              required
            />
          </div>
          <div className={styles.field}>
            <label htmlFor="password-new-value">{t("login.password.newPasswordLabel")}</label>
            <input
              id="password-new-value"
              type="password"
              autoComplete="new-password"
              minLength={10}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
            />
          </div>

          {passwordError && (
            <p className={styles.error} role="alert">
              {passwordError}
            </p>
          )}
          {passwordSuccess && (
            <p className={styles.success} role="status">
              {t("login.password.success")}
            </p>
          )}

          <button
            type="submit"
            className={styles.submit}
            disabled={passwordSubmitting || newPassword.length < 10}
          >
            {t("login.password.submit")}
          </button>
        </form>
      )}
    </div>
  );
}
