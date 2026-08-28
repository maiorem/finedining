import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import {
  changeArtistLinkUrl,
  getArtistForAdmin,
  publishArtist,
  saveArtistDraftTranslation,
  unpublishArtist,
} from "../../api/artistAdmin";
import { ApiError } from "../../api/http";
import { queryKeys } from "../../api/queryKeys";
import { ImageDropzone } from "./ImageDropzone";
import { PinModal } from "./PinModal";
import styles from "./ArtistEditPanel.module.css";

type Locale = "KO" | "EN";
const LOCALES: Locale[] = ["KO", "EN"];

type DraftState = Record<Locale, { name: string; role: string; bio: string; credits: string }>;

type ArtistEditPanelProps = {
  artistId: number;
};

const EMPTY_DRAFTS: DraftState = {
  KO: { name: "", role: "", bio: "", credits: "" },
  EN: { name: "", role: "", bio: "", credits: "" },
};

/**
 * §3.9의 "같은 페이지, 편집 패널" — Production 패턴을 그대로 따른다. `features/editing/`에
 * 있으므로 React.lazy로만 import된다(§3.5·§9). 프로필 사진은 아티스트당 1장으로 제한한다
 * (ImageDropzone의 maxImages). 참여 작품은 이 사이트의 Production 선택형이 아니라 자유
 * 텍스트다(2026-08-29 결정) — 외부 프로젝트 이력도 적을 수 있어야 한다.
 */
export default function ArtistEditPanel({ artistId }: ArtistEditPanelProps) {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const queryClient = useQueryClient();
  const queryKey = queryKeys.artists.adminDetail(artistId);

  const { data } = useQuery({
    queryKey,
    queryFn: () => getArtistForAdmin(session!.accessToken, artistId),
    enabled: Boolean(session),
    staleTime: 0, // 편집 모드는 방금 저장한 값이 바로 보여야 한다 (CLAUDE.md §9).
  });

  const [activeLocale, setActiveLocale] = useState<Locale>("KO");
  const [drafts, setDrafts] = useState<DraftState>(EMPTY_DRAFTS);
  const [linkUrlDraft, setLinkUrlDraft] = useState("");
  const [pinAction, setPinAction] = useState<"publish" | "unpublish" | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);

  useEffect(() => {
    if (!data) return;
    setDrafts((prev) => {
      const next = { ...prev };
      for (const translation of data.translations) {
        next[translation.locale] = {
          name: translation.draftName ?? translation.name ?? "",
          role: translation.draftRole ?? translation.role ?? "",
          bio: translation.draftBio ?? translation.bio ?? "",
          credits: translation.draftCredits ?? translation.credits ?? "",
        };
      }
      return next;
    });
    setLinkUrlDraft(data.linkUrl ?? "");
  }, [data]);

  function invalidate() {
    void queryClient.invalidateQueries({ queryKey });
    void queryClient.invalidateQueries({ queryKey: queryKeys.artists.all });
  }

  const saveDraftMutation = useMutation({
    mutationFn: () =>
      saveArtistDraftTranslation(
        session!.accessToken,
        artistId,
        activeLocale,
        drafts[activeLocale].name,
        drafts[activeLocale].role || null,
        drafts[activeLocale].bio || null,
        drafts[activeLocale].credits || null,
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

  const linkMutation = useMutation({
    mutationFn: () => changeArtistLinkUrl(session!.accessToken, artistId, linkUrlDraft || null),
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
    mutationFn: () => publishArtist(session!.accessToken, artistId),
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
    mutationFn: () => unpublishArtist(session!.accessToken, artistId),
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
  const hasKoName = Boolean(koTranslation?.name ?? koTranslation?.draftName);

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
        <span>{t("editing.panel.artistNameLabel")}</span>
        <input
          type="text"
          value={drafts[activeLocale].name}
          onChange={(e) =>
            setDrafts((prev) => ({ ...prev, [activeLocale]: { ...prev[activeLocale], name: e.target.value } }))
          }
        />
      </label>

      <label className={styles.field}>
        <span>{t("editing.panel.artistRoleLabel")}</span>
        <input
          type="text"
          value={drafts[activeLocale].role}
          onChange={(e) =>
            setDrafts((prev) => ({ ...prev, [activeLocale]: { ...prev[activeLocale], role: e.target.value } }))
          }
        />
      </label>

      <label className={styles.field}>
        <span>{t("editing.panel.artistBioLabel")}</span>
        <textarea
          rows={6}
          value={drafts[activeLocale].bio}
          onChange={(e) =>
            setDrafts((prev) => ({ ...prev, [activeLocale]: { ...prev[activeLocale], bio: e.target.value } }))
          }
        />
      </label>

      <label className={styles.field}>
        <span>{t("editing.panel.artistCreditsLabel")}</span>
        <textarea
          rows={5}
          value={drafts[activeLocale].credits}
          onChange={(e) =>
            setDrafts((prev) => ({ ...prev, [activeLocale]: { ...prev[activeLocale], credits: e.target.value } }))
          }
          placeholder={t("editing.panel.artistCreditsPlaceholder")}
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

      <h3 className={styles.imagesHeading}>{t("editing.panel.artistPhotoHeading")}</h3>
      <ImageDropzone ownerType="ARTIST" ownerId={artistId} images={data.images} onChanged={invalidate} maxImages={1} />

      <hr className={styles.divider} />

      {/* SNS 링크는 draft가 아니라 즉시 공개본에 반영된다 — 발행 버튼을 거치지 않는다. */}
      <label className={styles.field}>
        <span>{t("editing.panel.artistLinkLabel")}</span>
        <input
          type="url"
          value={linkUrlDraft}
          onChange={(e) => setLinkUrlDraft(e.target.value)}
          placeholder="https://instagram.com/..."
        />
      </label>
      <button
        type="button"
        className={styles.saveButton}
        disabled={linkMutation.isPending}
        onClick={() => {
          setSaveNotice(null);
          setActionError(null);
          linkMutation.mutate();
        }}
      >
        {t("editing.image.save")}
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
            disabled={publishMutation.isPending || !hasKoName}
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
