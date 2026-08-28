import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import { getAboutForAdmin, publishAbout, saveAboutDraftTranslation, unpublishAbout } from "../../api/aboutAdmin";
import { ApiError } from "../../api/http";
import { queryKeys } from "../../api/queryKeys";
import { PinModal } from "./PinModal";
import styles from "./AboutEditPanel.module.css";

type Locale = "KO" | "EN";
const LOCALES: Locale[] = ["KO", "EN"];

type DraftState = Record<Locale, string>;

const EMPTY_DRAFTS: DraftState = { KO: "", EN: "" };

/**
 * §3.9의 "같은 페이지, 편집 패널" — Production/Artist와 같은 패턴. 소개문은 싱글턴이라
 * artistId 같은 prop이 없다 — 항상 그 하나의 행만 조회·편집한다. `features/editing/`에
 * 있으므로 React.lazy로만 import된다(§3.5·§9).
 */
export default function AboutEditPanel() {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const queryClient = useQueryClient();
  const queryKey = queryKeys.about.admin;

  const { data } = useQuery({
    queryKey,
    queryFn: () => getAboutForAdmin(session!.accessToken),
    enabled: Boolean(session),
    staleTime: 0, // 편집 모드는 방금 저장한 값이 바로 보여야 한다 (CLAUDE.md §9).
  });

  const [activeLocale, setActiveLocale] = useState<Locale>("KO");
  const [drafts, setDrafts] = useState<DraftState>(EMPTY_DRAFTS);
  const [pinAction, setPinAction] = useState<"publish" | "unpublish" | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);

  useEffect(() => {
    if (!data) return;
    setDrafts((prev) => {
      const next = { ...prev };
      for (const translation of data.translations) {
        next[translation.locale] = translation.draftIntro ?? translation.intro ?? "";
      }
      return next;
    });
  }, [data]);

  function invalidate() {
    void queryClient.invalidateQueries({ queryKey });
    void queryClient.invalidateQueries({ queryKey: ["about"] });
  }

  const saveDraftMutation = useMutation({
    mutationFn: () => saveAboutDraftTranslation(session!.accessToken, activeLocale, drafts[activeLocale]),
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
    mutationFn: () => publishAbout(session!.accessToken),
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
    mutationFn: () => unpublishAbout(session!.accessToken),
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
    return <aside className={styles.panel}>{t("editing.panel.loading")}</aside>;
  }

  const koTranslation = data.translations.find((tr) => tr.locale === "KO");
  const hasKoIntro = Boolean(koTranslation?.intro ?? koTranslation?.draftIntro);

  return (
    <aside className={styles.panel} aria-label={t("editing.panel.heading")}>
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
        <span>{t("editing.panel.aboutIntroLabel")}</span>
        <textarea
          rows={12}
          value={drafts[activeLocale]}
          onChange={(e) => setDrafts((prev) => ({ ...prev, [activeLocale]: e.target.value }))}
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
        disabled={saveDraftMutation.isPending}
        onClick={() => {
          setSaveNotice(null);
          setActionError(null);
          saveDraftMutation.mutate();
        }}
      >
        {t("editing.panel.saveDraft")}
      </button>

      <hr className={styles.divider} />

      <div className={styles.publishRow}>
        <span className={styles.statusBadge}>{data.status}</span>
        <div className={styles.publishActions}>
          {/* 발행은 이미 공개된 페이지에서도 항상 눌러야 한다 — 새로 임시저장한 draft를
              공개본으로 밀어 올리는 동작이라 발행취소와 배타적이지 않다(§9 발행 버튼 버그). */}
          <button
            type="button"
            className={styles.publishButton}
            disabled={publishMutation.isPending || !hasKoIntro}
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
    </aside>
  );
}
