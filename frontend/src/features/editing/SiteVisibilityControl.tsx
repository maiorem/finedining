import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/http";
import { queryKeys } from "../../api/queryKeys";
import { getSiteStatus, setSiteOpen } from "../../api/site";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import { PinModal } from "./PinModal";
import styles from "./SiteVisibilityControl.module.css";

/**
 * 오픈 전 비공개 ↔ 공개 전환 버튼(2026-09-19). 공개는 사이트 전체를 외부에 여는 동작이라
 * PIN 확인을 거치고, 공개할 때는 한 번 더 묻는다. AdminSessionPanel에서 SUPER_ADMIN에게만
 * React.lazy로 불러온다(§3.5·§9).
 */
export default function SiteVisibilityControl() {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const queryClient = useQueryClient();
  const { data: status } = useQuery({ queryKey: queryKeys.site.status, queryFn: getSiteStatus });

  const [pendingOpen, setPendingOpen] = useState<boolean | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!status) return null;

  async function change(open: boolean) {
    if (!session) return;
    setError(null);
    setSubmitting(true);
    try {
      const result = await setSiteOpen(session.accessToken, open);
      queryClient.setQueryData(queryKeys.site.status, result);
    } catch (err) {
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPendingOpen(open);
      } else {
        setError(t("site.visibility.error"));
      }
    } finally {
      setSubmitting(false);
    }
  }

  function handleClick() {
    const nextOpen = !status!.open;
    if (nextOpen && !window.confirm(t("site.visibility.confirmOpen"))) return;
    void change(nextOpen);
  }

  return (
    <div className={styles.wrapper}>
      <p className={styles.heading}>{t("site.visibility.heading")}</p>
      <p className={styles.state}>{status.open ? t("site.visibility.open") : t("site.visibility.closed")}</p>
      <button type="button" className={styles.button} disabled={submitting} onClick={handleClick}>
        {status.open ? t("site.visibility.closeButton") : t("site.visibility.openButton")}
      </button>
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
      {pendingOpen !== null && (
        <PinModal
          onClose={() => setPendingOpen(null)}
          onVerified={() => {
            const open = pendingOpen;
            setPendingOpen(null);
            void change(open);
          }}
        />
      )}
    </div>
  );
}
