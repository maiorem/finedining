import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import {
  changeProgramApplyUrl,
  changeProgramLocationUrl,
  getProgramForAdmin,
  publishProgram,
  saveProgramDraftTranslation,
  unpublishProgram,
} from "../../api/programAdmin";
import { ApiError } from "../../api/http";
import { queryKeys } from "../../api/queryKeys";
import { PinModal } from "./PinModal";
import styles from "./ProgramForm.module.css";

type Locale = "KO" | "EN";
const LOCALES: Locale[] = ["KO", "EN"];

type DraftState = Record<Locale, { title: string; description: string }>;

const EMPTY_DRAFTS: DraftState = {
  KO: { title: "", description: "" },
  EN: { title: "", description: "" },
};

type ProgramFormProps = {
  programId: number;
  onClose: () => void;
};

/**
 * 프로그램 하나의 인라인 편집 폼. KO/EN 탭으로 제목·설명을 임시저장하고(§3.9), 참가/위치
 * 링크는 즉시 반영된다(Artist.linkUrl과 같은 취급). ProgramsPage에서 React.lazy로만 import되는
 * ProgramManagementPanel 안에서만 쓰인다 — 관리자 전용 쓰기 경로다(CLAUDE.md §3.5·§9).
 */
export default function ProgramForm({ programId, onClose }: ProgramFormProps) {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const queryClient = useQueryClient();
  const queryKey = queryKeys.programs.adminDetail(programId);

  const { data } = useQuery({
    queryKey,
    queryFn: () => getProgramForAdmin(session!.accessToken, programId),
    enabled: Boolean(session),
    staleTime: 0, // 편집 모드는 방금 저장한 값이 바로 보여야 한다(CLAUDE.md §9).
  });

  const [activeLocale, setActiveLocale] = useState<Locale>("KO");
  const [drafts, setDrafts] = useState<DraftState>(EMPTY_DRAFTS);
  const [applyUrlDraft, setApplyUrlDraft] = useState("");
  const [locationUrlDraft, setLocationUrlDraft] = useState("");
  const [pinAction, setPinAction] = useState<"publish" | "unpublish" | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);

  useEffect(() => {
    if (!data) return;
    setDrafts((prev) => {
      const next = { ...prev };
      for (const translation of data.translations) {
        next[translation.locale] = {
          title: translation.draftTitle ?? translation.title ?? "",
          description: translation.draftDescription ?? translation.description ?? "",
        };
      }
      return next;
    });
    setApplyUrlDraft(data.applyUrl ?? "");
    setLocationUrlDraft(data.locationUrl ?? "");
  }, [data]);

  function invalidate() {
    void queryClient.invalidateQueries({ queryKey });
    void queryClient.invalidateQueries({ queryKey: queryKeys.programs.all });
    void queryClient.invalidateQueries({ queryKey: queryKeys.programs.adminList });
  }

  const saveDraftMutation = useMutation({
    mutationFn: () =>
      saveProgramDraftTranslation(
        session!.accessToken,
        programId,
        activeLocale,
        drafts[activeLocale].title,
        drafts[activeLocale].description || null,
      ),
    onSuccess: () => {
      setActionError(null);
      setSaveNotice(t("editing.panel.saved"));
      invalidate();
    },
    onError: (err: unknown) => {
      setSaveNotice(null);
      setActionError(err instanceof ApiError ? err.message : t("editing.panel.saveFailed"));
    },
  });

  const applyUrlMutation = useMutation({
    mutationFn: () => changeProgramApplyUrl(session!.accessToken, programId, applyUrlDraft || null),
    onSuccess: () => {
      setActionError(null);
      setSaveNotice(t("editing.panel.saved"));
      invalidate();
    },
    onError: (err: unknown) => {
      setSaveNotice(null);
      setActionError(err instanceof ApiError ? err.message : t("editing.panel.saveFailed"));
    },
  });

  const locationUrlMutation = useMutation({
    mutationFn: () => changeProgramLocationUrl(session!.accessToken, programId, locationUrlDraft || null),
    onSuccess: () => {
      setActionError(null);
      setSaveNotice(t("editing.panel.saved"));
      invalidate();
    },
    onError: (err: unknown) => {
      setSaveNotice(null);
      setActionError(err instanceof ApiError ? err.message : t("editing.panel.saveFailed"));
    },
  });

  const publishMutation = useMutation({
    mutationFn: () => publishProgram(session!.accessToken, programId),
    onSuccess: () => {
      setActionError(null);
      invalidate();
    },
    onError: (err: unknown) => {
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPinAction("publish");
        return;
      }
      setActionError(err instanceof ApiError ? err.message : t("editing.panel.publishFailed"));
    },
  });

  const unpublishMutation = useMutation({
    mutationFn: () => unpublishProgram(session!.accessToken, programId),
    onSuccess: () => {
      setActionError(null);
      invalidate();
    },
    onError: (err: unknown) => {
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPinAction("unpublish");
        return;
      }
      setActionError(err instanceof ApiError ? err.message : t("editing.panel.publishFailed"));
    },
  });

  if (!data) {
    return <div className={styles.form}>{t("editing.panel.loading")}</div>;
  }

  const koTranslation = data.translations.find((tr) => tr.locale === "KO");
  const hasKoTitle = Boolean(koTranslation?.title ?? koTranslation?.draftTitle);

  return (
    <div className={styles.form}>
      <div className={styles.tabs} role="tablist">
        {LOCALES.map((locale) => (
          <button
            key={locale}
            type="button"
            role="tab"
            aria-selected={activeLocale === locale}
            className={activeLocale === locale ? `${styles.tab} ${styles.tabActive}` : styles.tab}
            onClick={() => setActiveLocale(locale)}
          >
            {locale}
          </button>
        ))}
      </div>

      <label className={styles.field}>
        <span>{t("programs.form.titleLabel")}</span>
        <input
          type="text"
          value={drafts[activeLocale].title}
          onChange={(e) =>
            setDrafts((prev) => ({ ...prev, [activeLocale]: { ...prev[activeLocale], title: e.target.value } }))
          }
        />
      </label>

      <label className={styles.field}>
        <span>{t("programs.form.descriptionLabel")}</span>
        <textarea
          rows={4}
          value={drafts[activeLocale].description}
          onChange={(e) =>
            setDrafts((prev) => ({
              ...prev,
              [activeLocale]: { ...prev[activeLocale], description: e.target.value },
            }))
          }
        />
      </label>

      {/* 제목·설명과 별개 저장 버튼으로 나눠뒀더니 운영자가 링크 저장 버튼을 놓치고 값이 비는
          사고가 실제로 있었다 — 아래 저장 버튼 하나로 전부 같이 저장한다. */}
      <label className={styles.field}>
        <span>{t("programs.form.applyUrlLabel")}</span>
        <input
          type="url"
          value={applyUrlDraft}
          onChange={(e) => setApplyUrlDraft(e.target.value)}
          placeholder="https://forms.gle/..."
        />
      </label>

      <label className={styles.field}>
        <span>{t("programs.form.locationUrlLabel")}</span>
        <input
          type="url"
          value={locationUrlDraft}
          onChange={(e) => setLocationUrlDraft(e.target.value)}
          placeholder="https://map.naver.com/..."
        />
      </label>

      {saveNotice && <p className={styles.notice}>{saveNotice}</p>}
      {actionError && (
        <p className={styles.error} role="alert">
          {actionError}
        </p>
      )}

      <button
        type="button"
        className={styles.saveButton}
        disabled={saveDraftMutation.isPending || applyUrlMutation.isPending || locationUrlMutation.isPending}
        onClick={() => {
          setSaveNotice(null);
          setActionError(null);
          saveDraftMutation.mutate();
          if (applyUrlDraft !== (data.applyUrl ?? "")) applyUrlMutation.mutate();
          if (locationUrlDraft !== (data.locationUrl ?? "")) locationUrlMutation.mutate();
        }}
      >
        {t("editing.panel.saveDraft")}
      </button>

      <hr className={styles.divider} />

      <div className={styles.publishRow}>
        <span className={styles.statusBadge}>{data.status}</span>
        <div className={styles.publishActions}>
          <button
            type="button"
            className={styles.publishButton}
            disabled={publishMutation.isPending || !hasKoTitle}
            onClick={() => publishMutation.mutate()}
          >
            {t("editing.panel.publish")}
          </button>
          {data.status === "PUBLISHED" && (
            <button
              type="button"
              className={styles.unpublishButton}
              disabled={unpublishMutation.isPending}
              onClick={() => unpublishMutation.mutate()}
            >
              {t("editing.panel.unpublish")}
            </button>
          )}
          <button type="button" className={styles.closeButton} onClick={onClose}>
            {t("programs.form.cancel")}
          </button>
        </div>
      </div>

      {pinAction && (
        <PinModal
          onClose={() => setPinAction(null)}
          onVerified={() => {
            const action = pinAction;
            setPinAction(null);
            if (action === "publish") publishMutation.mutate();
            if (action === "unpublish") unpublishMutation.mutate();
          }}
        />
      )}
    </div>
  );
}
