import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import {
  changeArtistLinkUrl,
  changeArtistPeopleInfo,
  getArtistForAdmin,
  publishArtist,
  saveArtistDraftTranslation,
  unpublishArtist,
} from "../../api/artistAdmin";
import { ApiError } from "../../api/http";
import { queryKeys } from "../../api/queryKeys";
import { ImageDropzone } from "./ImageDropzone";
import MarkdownEditor from "./MarkdownEditor";
import { PinModal } from "./PinModal";
import styles from "./ArtistEditPanel.module.css";

type Locale = "KO" | "EN";
const LOCALES: Locale[] = ["KO", "EN"];

type DraftState = Record<Locale, { name: string; role: string; bio: string; credits: string; quote: string }>;

type ArtistEditPanelProps = {
  artistId: number;
  /** 발행이 끝나면 부른다 — 페이지가 편집 모드를 끄고 발행된 상세를 보여주는 데 쓴다. */
  onPublished: () => void;
};

const EMPTY_DRAFTS: DraftState = {
  KO: { name: "", role: "", bio: "", credits: "", quote: "" },
  EN: { name: "", role: "", bio: "", credits: "", quote: "" },
};

/**
 * §3.9의 "같은 페이지, 편집 화면" — Production 패턴을 따른다. `features/editing/`에 있으므로
 * React.lazy로만 import된다(§3.5·§9). 프로필 사진은 첫 번째 사진이고, 본문(마크다운)에 넣는 사진은
 * 그 뒤에 올라간다. 참여 작품은 이 사이트의 Production 선택형이 아니라 자유 텍스트다(2026-08-29 결정).
 * "발행하기" 한 번이 두 로케일의 글·이메일·순서·링크를 전부 저장한 뒤 발행까지 마친다(ProgramEditPanel과 같은 이유).
 */
export default function ArtistEditPanel({ artistId, onPublished }: ArtistEditPanelProps) {
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
  const [emailDraft, setEmailDraft] = useState("");
  const [orderDraft, setOrderDraft] = useState("0");
  const [interviewDraft, setInterviewDraft] = useState("");
  const [pinAction, setPinAction] = useState<"publish" | "unpublish" | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // 서버 값으로 입력칸을 채우는 건 처음 한 번뿐이다 — 이미지를 올릴 때마다 데이터를 다시 불러오는데
  // 그때 채우면 저장하지 않은 글(본문에 넣은 이미지 표시 포함)이 지워진다.
  const seededRef = useRef(false);

  useEffect(() => {
    if (!data || seededRef.current) return;
    seededRef.current = true;
    setDrafts((prev) => {
      const next = { ...prev };
      for (const translation of data.translations) {
        next[translation.locale] = {
          name: translation.draftName ?? translation.name ?? "",
          role: translation.draftRole ?? translation.role ?? "",
          bio: translation.draftBio ?? translation.bio ?? "",
          credits: translation.draftCredits ?? translation.credits ?? "",
          quote: translation.draftQuote ?? translation.quote ?? "",
        };
      }
      return next;
    });
    setLinkUrlDraft(data.linkUrl ?? "");
    setEmailDraft(data.email ?? "");
    setOrderDraft(String(data.displayOrder));
    setInterviewDraft(data.interviewUrl ?? "");
  }, [data]);

  function invalidate() {
    void queryClient.invalidateQueries({ queryKey });
    void queryClient.invalidateQueries({ queryKey: queryKeys.artists.all });
  }

  /** 두 로케일의 글(이름이 있는 쪽만)과 이메일·순서·링크를 전부 저장한다 — 임시저장과 발행이 함께 쓴다. */
  async function saveAll() {
    if (!session) return;
    for (const locale of LOCALES) {
      // 이름이 비어 있는 로케일(대개 EN)은 보내지 않는다 — name은 서버에서 NotBlank 검증한다.
      if (drafts[locale].name.trim() === "") continue;
      await saveArtistDraftTranslation(
        session.accessToken,
        artistId,
        locale,
        drafts[locale].name,
        drafts[locale].role || null,
        drafts[locale].bio || null,
        drafts[locale].credits || null,
        drafts[locale].quote || null,
      );
    }
    if (linkUrlDraft !== (data?.linkUrl ?? "")) {
      await changeArtistLinkUrl(session.accessToken, artistId, linkUrlDraft || null);
    }
    await changeArtistPeopleInfo(session.accessToken, artistId, {
      email: emailDraft || null,
      displayOrder: Number.parseInt(orderDraft, 10) || 0,
      interviewUrl: interviewDraft || null,
    });
  }

  async function handleSaveDraft() {
    setSaveNotice(null);
    setActionError(null);
    setBusy(true);
    try {
      await saveAll();
      setSaveNotice(t("editing.panel.saved"));
      invalidate();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : t("editing.panel.saveFailed"));
    } finally {
      setBusy(false);
    }
  }

  async function handlePublish() {
    if (!session) return;
    setSaveNotice(null);
    setActionError(null);
    setBusy(true);
    try {
      await saveAll();
      await publishArtist(session.accessToken, artistId);
      invalidate();
      onPublished();
    } catch (err) {
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPinAction("publish");
        return;
      }
      setActionError(err instanceof ApiError ? err.message : t("editing.panel.publishFailed"));
    } finally {
      setBusy(false);
    }
  }

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
    return <section className={styles.panel}>{t("editing.panel.loading")}</section>;
  }

  // 발행하기가 저장까지 함께 하므로 서버 상태가 아니라 지금 입력 중인 값으로 발행 가능 여부를 본다.
  const hasKoName = drafts.KO.name.trim() !== "";

  return (
    <section className={styles.panel} aria-label={t("editing.panel.heading")}>
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
        <span>{t("editing.panel.artistQuoteLabel")}</span>
        <input
          type="text"
          maxLength={500}
          value={drafts[activeLocale].quote}
          onChange={(e) =>
            setDrafts((prev) => ({ ...prev, [activeLocale]: { ...prev[activeLocale], quote: e.target.value } }))
          }
        />
      </label>

      <MarkdownEditor
        id={`artist-${artistId}-bio`}
        label={t("editing.panel.artistBioLabel")}
        value={drafts[activeLocale].bio}
        onChange={(bio) => setDrafts((prev) => ({ ...prev, [activeLocale]: { ...prev[activeLocale], bio } }))}
        ownerType="ARTIST"
        ownerId={artistId}
        onImagesChanged={invalidate}
        maxLength={4000}
      />

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
        disabled={busy}
        onClick={() => void handleSaveDraft()}
      >
        {t("editing.panel.saveDraft")}
      </button>

      <hr className={styles.divider} />

      <h3 className={styles.imagesHeading}>{t("editing.panel.artistPhotoHeading")}</h3>
      <ImageDropzone ownerType="ARTIST" ownerId={artistId} images={data.images} onChanged={invalidate} />
      <small className={styles.hint}>{t("editing.panel.artistPhotoHint")}</small>

      <hr className={styles.divider} />

      {/* SNS 링크·이메일·순서·인터뷰 링크는 임시저장·발행하기를 누를 때 함께 저장된다. */}
      <label className={styles.field}>
        <span>{t("editing.panel.artistLinkLabel")}</span>
        <input
          type="url"
          value={linkUrlDraft}
          onChange={(e) => setLinkUrlDraft(e.target.value)}
          placeholder="https://instagram.com/..."
        />
      </label>

      <hr className={styles.divider} />

      <label className={styles.field}>
        <span>{t("editing.panel.artistEmailLabel")}</span>
        <input type="email" value={emailDraft} onChange={(e) => setEmailDraft(e.target.value)} />
      </label>
      <label className={styles.field}>
        <span>{t("editing.panel.artistOrderLabel")}</span>
        <input type="number" min={0} max={9999} value={orderDraft} onChange={(e) => setOrderDraft(e.target.value)} />
      </label>
      <label className={styles.field}>
        <span>{t("editing.panel.artistInterviewLabel")}</span>
        <input
          type="url"
          value={interviewDraft}
          onChange={(e) => setInterviewDraft(e.target.value)}
          placeholder="https://www.youtube.com/watch?v=..."
        />
      </label>

      <hr className={styles.divider} />

      <div className={styles.publishRow}>
        <span className={styles.statusBadge}>{data.status}</span>
        <div className={styles.publishActions}>
          {/* 발행은 이미 공개된 페이지에서도 항상 눌러야 한다 — 새로 임시저장한 draft를
              공개본으로 밀어 올리는 동작이라 발행취소와 배타적이지 않다(§9 발행 버튼 버그). */}
          <button
            type="button"
            className={styles.publishButton}
            disabled={busy || !hasKoName}
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
    </section>
  );
}
