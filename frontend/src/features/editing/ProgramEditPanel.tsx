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
import { ImageDropzone } from "./ImageDropzone";
import { PinModal } from "./PinModal";
import styles from "./ProgramEditPanel.module.css";

type Locale = "KO" | "EN";
const LOCALES: Locale[] = ["KO", "EN"];

type DraftState = Record<Locale, { title: string; description: string }>;

type ProgramEditPanelProps = {
  programId: number;
};

const EMPTY_DRAFTS: DraftState = {
  KO: { title: "", description: "" },
  EN: { title: "", description: "" },
};

/**
 * §3.9의 "같은 페이지, 편집 패널" — ProductionEditPanel과 같은 패턴이다. 이 모듈은
 * `features/editing/`에 있으므로 React.lazy로만 import된다(§3.5·§9).
 */
export default function ProgramEditPanel({ programId }: ProgramEditPanelProps) {
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
  const [publishing, setPublishing] = useState(false);

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

  /**
   * "발행하기" 한 번으로 제목·설명(양쪽 로케일)·참가/위치 링크를 전부 저장한 뒤 발행까지
   * 마친다 — ProductionEditPanel과 같은 이유로 합쳤다(따로 "임시저장"을 눌러야 했던 게
   * 귀찮다는 피드백). 발행만 PIN sudo를 요구하므로(§3.4) PIN 모달은 최대 한 번만 뜬다.
   */
  async function handlePublish() {
    if (!session || !data) return;
    setSaveNotice(null);
    setActionError(null);
    setPublishing(true);
    try {
      for (const locale of LOCALES) {
        // 제목이 비어있는 로케일(대개 EN)은 보내지 않는다 — title은 서버에서 NotBlank 검증한다.
        if (drafts[locale].title.trim() === "") continue;
        await saveProgramDraftTranslation(
          session.accessToken,
          programId,
          locale,
          drafts[locale].title,
          drafts[locale].description || null,
        );
      }
      if (applyUrlDraft !== (data.applyUrl ?? "")) {
        await changeProgramApplyUrl(session.accessToken, programId, applyUrlDraft || null);
      }
      if (locationUrlDraft !== (data.locationUrl ?? "")) {
        await changeProgramLocationUrl(session.accessToken, programId, locationUrlDraft || null);
      }
      await publishProgram(session.accessToken, programId);
      invalidate();
    } catch (err) {
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPinAction("publish");
        return;
      }
      setActionError(err instanceof ApiError ? err.message : t("editing.panel.publishFailed"));
    } finally {
      setPublishing(false);
    }
  }

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
    return <aside className={styles.panel}>{t("editing.panel.loading")}</aside>;
  }

  // "발행하기"가 저장까지 함께 하므로(handlePublish) 서버 상태가 아니라 지금 입력 중인
  // draft를 기준으로 발행 가능 여부를 본다.
  const hasKoTitle = drafts.KO.title.trim() !== "";
  const hasEnTitle = drafts.EN.title.trim() !== "";

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
          rows={6}
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
          사고가 실제로 있었다 — 아래 발행하기 버튼 하나로 전부 같이 저장한다. */}
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

      {/* 대표 이미지 + 본문에 들어갈 이미지들을 블로그처럼 쓸 수 있게 — 이미지마다 alt/캡션을
          붙인다(ProductionEditPanel과 같은 ImageDropzone 재사용, §7.5). 목록에서는 첫 번째
          이미지가 대표 이미지로 쓰인다. */}
      <h3 className={styles.imagesHeading}>{t("editing.panel.imagesHeading")}</h3>
      <ImageDropzone ownerType="PROGRAM" ownerId={programId} images={data.images} onChanged={invalidate} />

      <hr className={styles.divider} />

      {!hasEnTitle && <p className={styles.warning}>{t("editing.panel.enMissing")}</p>}

      <div className={styles.publishRow}>
        <span className={styles.statusBadge}>{data.status}</span>
        <div className={styles.publishActions}>
          <button
            type="button"
            className={styles.publishButton}
            disabled={publishing || !hasKoTitle}
            onClick={() => void handlePublish()}
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
            if (action === "publish") void handlePublish();
            if (action === "unpublish") unpublishMutation.mutate();
          }}
        />
      )}
    </aside>
  );
}
