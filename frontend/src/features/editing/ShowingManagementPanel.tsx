import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/http";
import { queryKeys } from "../../api/queryKeys";
import {
  changeBookingUrl,
  changeSalesStatus,
  listShowingsForAdmin,
  publishShowing,
  unpublishShowing,
} from "../../api/showingAdmin";
import type { SalesStatus } from "../../api/showings";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import ShowingForm from "./ShowingForm";
import { PinModal } from "./PinModal";
import styles from "./ShowingManagementPanel.module.css";

const SALES_STATUSES: SalesStatus[] = ["OPEN", "CLOSING_SOON", "SOLD_OUT", "ENDED"];

type PendingPinAction = { type: "publish" | "unpublish" | "booking-url"; id: number };

function formatAdminTime(iso: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

/**
 * 예약(회차) 관리자 패널. BookingPage에서 React.lazy로만 import된다 — 관리자 전용 API 경로가
 * 익명 방문자 번들에 섞이면 안 된다(CLAUDE.md §3.5·§9).
 */
export default function ShowingManagementPanel() {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const queryClient = useQueryClient();

  const [creating, setCreating] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [urlEditingId, setUrlEditingId] = useState<number | null>(null);
  const [urlDraft, setUrlDraft] = useState("");
  const [pinAction, setPinAction] = useState<PendingPinAction | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const { data: showings, isLoading } = useQuery({
    queryKey: queryKeys.showings.adminList,
    queryFn: () => listShowingsForAdmin(session!.accessToken),
    enabled: Boolean(session),
  });

  function invalidate() {
    void queryClient.invalidateQueries({ queryKey: queryKeys.showings.all });
  }

  async function handleSalesStatus(id: number, status: SalesStatus) {
    if (!session) return;
    setActionError(null);
    await changeSalesStatus(session.accessToken, id, status);
    invalidate();
  }

  async function handlePublish(id: number) {
    if (!session) return;
    try {
      await publishShowing(session.accessToken, id);
      setActionError(null);
      invalidate();
    } catch (err) {
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPinAction({ type: "publish", id });
        return;
      }
      setActionError(err instanceof ApiError ? err.message : t("showings.error.generic"));
    }
  }

  async function handleUnpublish(id: number) {
    if (!session) return;
    try {
      await unpublishShowing(session.accessToken, id);
      setActionError(null);
      invalidate();
    } catch (err) {
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPinAction({ type: "unpublish", id });
        return;
      }
      setActionError(err instanceof ApiError ? err.message : t("showings.error.generic"));
    }
  }

  async function handleSaveBookingUrl(id: number, url: string) {
    if (!session) return;
    try {
      await changeBookingUrl(session.accessToken, id, url);
      setActionError(null);
      setUrlEditingId(null);
      invalidate();
    } catch (err) {
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPinAction({ type: "booking-url", id });
        return;
      }
      setActionError(err instanceof ApiError ? err.message : t("showings.error.generic"));
    }
  }

  function handlePinVerified() {
    if (!pinAction) return;
    const action = pinAction;
    setPinAction(null);
    if (action.type === "publish") void handlePublish(action.id);
    if (action.type === "unpublish") void handleUnpublish(action.id);
    if (action.type === "booking-url") void handleSaveBookingUrl(action.id, urlDraft);
  }

  return (
    <div className={styles.panel}>
      <button type="button" className={styles.addToggle} onClick={() => setCreating((prev) => !prev)}>
        {creating ? t("showings.form.cancel") : t("showings.addNew")}
      </button>

      {creating && (
        <ShowingForm
          mode="create"
          onSaved={() => {
            setCreating(false);
            invalidate();
          }}
          onCancel={() => setCreating(false)}
        />
      )}

      {actionError && (
        <p className={styles.error} role="alert">
          {actionError}
        </p>
      )}

      {isLoading && <p className={styles.status}>{t("showings.loading")}</p>}

      <ul className={styles.list}>
        {showings?.map((showing) =>
          editingId === showing.id ? (
            <li key={showing.id} className={styles.item}>
              <ShowingForm
                mode="edit"
                showingId={showing.id}
                initial={{
                  startsAt: showing.startsAt,
                  durationMinutes: showing.durationMinutes,
                  venueName: showing.venueName,
                  venueAddress: showing.venueAddress,
                  spokenLanguage: showing.spokenLanguage,
                  interpretationAvailable: showing.interpretationAvailable,
                }}
                onSaved={() => {
                  setEditingId(null);
                  invalidate();
                }}
                onCancel={() => setEditingId(null)}
              />
            </li>
          ) : (
            <li key={showing.id} className={styles.item}>
              <div className={styles.row}>
                <div>
                  <p className={styles.production}>{showing.productionTitle ?? showing.productionSlug}</p>
                  <p className={styles.meta}>
                    {formatAdminTime(showing.startsAt)} · {showing.venueName}
                  </p>
                </div>
                <span className={styles.statusBadge}>{showing.status}</span>
              </div>

              <div className={styles.salesStatusRow}>
                {SALES_STATUSES.map((status) => (
                  <button
                    key={status}
                    type="button"
                    aria-pressed={showing.salesStatus === status}
                    className={
                      showing.salesStatus === status ? `${styles.statusButton} ${styles.statusButtonActive}` : styles.statusButton
                    }
                    onClick={() => void handleSalesStatus(showing.id, status)}
                  >
                    {t(`booking.status.${statusI18nKey(status)}`)}
                  </button>
                ))}
              </div>

              {urlEditingId === showing.id ? (
                <div className={styles.urlRow}>
                  <input
                    type="text"
                    className={styles.urlInput}
                    value={urlDraft}
                    onChange={(e) => setUrlDraft(e.target.value)}
                    placeholder="https://booking.naver.com/..."
                  />
                  <button type="button" onClick={() => void handleSaveBookingUrl(showing.id, urlDraft)}>
                    {t("showings.form.save")}
                  </button>
                  <button type="button" onClick={() => setUrlEditingId(null)}>
                    {t("showings.form.cancel")}
                  </button>
                </div>
              ) : (
                <div className={styles.urlRow}>
                  <span className={styles.urlDisplay}>{showing.bookingUrl ?? t("showings.noBookingUrl")}</span>
                  <button
                    type="button"
                    onClick={() => {
                      setUrlEditingId(showing.id);
                      setUrlDraft(showing.bookingUrl ?? "");
                    }}
                  >
                    {t("showings.changeBookingUrl")}
                  </button>
                </div>
              )}

              <div className={styles.actions}>
                <button type="button" onClick={() => setEditingId(showing.id)}>
                  {t("showings.edit")}
                </button>
                {showing.status === "PUBLISHED" ? (
                  <button type="button" onClick={() => void handleUnpublish(showing.id)}>
                    {t("editing.panel.unpublish")}
                  </button>
                ) : (
                  <button type="button" onClick={() => void handlePublish(showing.id)}>
                    {t("editing.panel.publish")}
                  </button>
                )}
              </div>
            </li>
          ),
        )}
      </ul>

      {pinAction && <PinModal onClose={() => setPinAction(null)} onVerified={handlePinVerified} />}
    </div>
  );
}

function statusI18nKey(status: SalesStatus): string {
  return { OPEN: "open", CLOSING_SOON: "closingSoon", SOLD_OUT: "soldOut", ENDED: "ended" }[status];
}
