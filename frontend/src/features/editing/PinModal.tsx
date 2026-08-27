import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Modal } from "../../components/primitives/Modal";
import { ApiError } from "../../api/http";
import { verifySudoPin } from "../../api/auth";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import styles from "./PinModal.module.css";

type PinModalProps = {
  onVerified: () => void;
  onClose: () => void;
};

const KNOWN_ERROR_CODES = ["PIN_INVALID", "PIN_LOCKED"] as const;

function errorMessageKey(code: string): string {
  return (KNOWN_ERROR_CODES as readonly string[]).includes(code)
    ? `editing.pin.error.${code}`
    : "editing.pin.error.generic";
}

/** 발행/발행취소 같은 파괴적 동작 전에 다시 요구하는 PIN 재확인 (CLAUDE.md §3.4). */
export function PinModal({ onVerified, onClose }: PinModalProps) {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const [pin, setPin] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!session) return;
    setError(null);
    setSubmitting(true);
    try {
      await verifySudoPin(session.accessToken, pin);
      onVerified();
    } catch (err) {
      const code = err instanceof ApiError ? err.code : "UNKNOWN";
      setError(t(errorMessageKey(code)));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal titleId="pin-modal-title" title={t("editing.pin.title")} closeLabel={t("editing.close")} onClose={onClose}>
      <form className={styles.form} onSubmit={handleSubmit}>
        <p className={styles.lead}>{t("editing.pin.lead")}</p>
        <label className={styles.field}>
          <span>{t("editing.pin.label")}</span>
          <input
            type="password"
            inputMode="numeric"
            pattern="[0-9]*"
            maxLength={6}
            value={pin}
            onChange={(e) => setPin(e.target.value)}
            required
          />
        </label>
        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}
        <button type="submit" className={styles.submit} disabled={submitting || pin.length !== 6}>
          {t("editing.pin.submit")}
        </button>
      </form>
    </Modal>
  );
}
