import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import {
  changeProductionBookingUrl,
  changeProductionLocationUrl,
  changeProductionNolBookingUrl,
  getProductionForAdmin,
  publishProduction,
  saveDraftTranslation,
  unpublishProduction,
} from "../../api/productionAdmin";
import { ApiError } from "../../api/http";
import { queryKeys } from "../../api/queryKeys";
import { ImageDropzone } from "./ImageDropzone";
import MarkdownEditor from "./MarkdownEditor";
import { PinModal } from "./PinModal";
import styles from "./ProductionEditPanel.module.css";

type Locale = "KO" | "EN";
const LOCALES: Locale[] = ["KO", "EN"];

type DraftState = Record<Locale, { title: string; subtitle: string; description: string }>;

type ProductionEditPanelProps = {
  productionId: number;
  /**
   * center: 페이지 가운데에 놓는 글쓰기 화면 — 설명을 마크다운 편집기로 쓰고, 발행이 끝나면 onPublished를 부른다.
   * side: 고정 상세(아버지의 식탁) 옆 패널 — 예약·위치 링크만 쓰므로 예전 입력칸 그대로다.
   */
  variant?: "center" | "side";
  onPublished?: () => void;
};

const EMPTY_DRAFTS: DraftState = {
  KO: { title: "", subtitle: "", description: "" },
  EN: { title: "", subtitle: "", description: "" },
};

/**
 * §3.9의 "같은 페이지, 편집 패널" 그 자체. 이 모듈은 `features/editing/`에 있으므로
 * React.lazy로만 import된다 — 익명 방문자 번들에 섞이지 않는다(§3.5·§9).
 */
export default function ProductionEditPanel({
  productionId,
  variant = "side",
  onPublished,
}: ProductionEditPanelProps) {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const queryClient = useQueryClient();
  const queryKey = queryKeys.productions.adminDetail(productionId);

  const { data } = useQuery({
    queryKey,
    queryFn: () => getProductionForAdmin(session!.accessToken, productionId),
    enabled: Boolean(session),
    staleTime: 0, // 편집 모드는 방금 저장한 값이 바로 보여야 한다 (CLAUDE.md §9).
  });

  const [activeLocale, setActiveLocale] = useState<Locale>("KO");
  const [drafts, setDrafts] = useState<DraftState>(EMPTY_DRAFTS);
  const [bookingUrlDraft, setBookingUrlDraft] = useState("");
  const [nolBookingUrlDraft, setNolBookingUrlDraft] = useState("");
  const [locationUrlDraft, setLocationUrlDraft] = useState("");
  const [pinAction, setPinAction] = useState<"publish" | "unpublish" | "save-links" | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);
  const [publishing, setPublishing] = useState(false);

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
          title: translation.draftTitle ?? translation.title ?? "",
          subtitle: translation.draftSubtitle ?? translation.subtitle ?? "",
          description: translation.draftDescription ?? translation.description ?? "",
        };
      }
      return next;
    });
    setBookingUrlDraft(data.bookingUrl ?? "");
    setNolBookingUrlDraft(data.nolBookingUrl ?? "");
    setLocationUrlDraft(data.locationUrl ?? "");
  }, [data]);

  function invalidate() {
    void queryClient.invalidateQueries({ queryKey });
    void queryClient.invalidateQueries({ queryKey: queryKeys.productions.all });
  }

  const saveDraftMutation = useMutation({
    mutationFn: () =>
      saveDraftTranslation(
        session!.accessToken,
        productionId,
        activeLocale,
        drafts[activeLocale].title,
        drafts[activeLocale].subtitle || null,
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

  const bookingUrlMutation = useMutation({
    mutationFn: () => changeProductionBookingUrl(session!.accessToken, productionId, bookingUrlDraft || null),
    onSuccess: () => {
      setActionError(null);
      setSaveNotice(t("editing.panel.saved"));
      invalidate();
    },
    onError: (err: unknown) => {
      setSaveNotice(null);
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPinAction("save-links");
        return;
      }
      setActionError(err instanceof ApiError ? err.message : t("editing.panel.saveFailed"));
    },
  });

  const nolBookingUrlMutation = useMutation({
    mutationFn: () => changeProductionNolBookingUrl(session!.accessToken, productionId, nolBookingUrlDraft || null),
    onSuccess: () => {
      setActionError(null);
      setSaveNotice(t("editing.panel.saved"));
      invalidate();
    },
    onError: (err: unknown) => {
      setSaveNotice(null);
      if (err instanceof ApiError && err.code === "PIN_REQUIRED") {
        setPinAction("save-links");
        return;
      }
      setActionError(err instanceof ApiError ? err.message : t("editing.panel.saveFailed"));
    },
  });

  const locationUrlMutation = useMutation({
    mutationFn: () => changeProductionLocationUrl(session!.accessToken, productionId, locationUrlDraft || null),
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
   * "발행하기" 한 번으로 제목·설명(양쪽 로케일)·예약/위치 링크를 전부 저장한 뒤 발행까지
   * 마친다 — 따로 "임시저장"을 눌러야 했던 게 귀찮다는 피드백으로 합쳤다. bookingUrl 변경과
   * 발행 둘 다 PIN sudo 모드를 요구하지만(§3.4), sudo는 서버에서 15분간 유지되므로 순서대로
   * await하면 PIN 모달은 최대 한 번만 뜬다 — 뜬 뒤 같은 함수를 처음부터 다시 실행하면(이미
   * 끝난 단계는 값이 그대로라 다시 보내도 무해하다) 나머지가 그대로 이어진다.
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
        await saveDraftTranslation(
          session.accessToken,
          productionId,
          locale,
          drafts[locale].title,
          drafts[locale].subtitle || null,
          drafts[locale].description || null,
        );
      }
      if (locationUrlDraft !== (data.locationUrl ?? "")) {
        await changeProductionLocationUrl(session.accessToken, productionId, locationUrlDraft || null);
      }
      if (bookingUrlDraft !== (data.bookingUrl ?? "")) {
        await changeProductionBookingUrl(session.accessToken, productionId, bookingUrlDraft || null);
      }
      if (nolBookingUrlDraft !== (data.nolBookingUrl ?? "")) {
        await changeProductionNolBookingUrl(session.accessToken, productionId, nolBookingUrlDraft || null);
      }
      await publishProduction(session.accessToken, productionId);
      invalidate();
      onPublished?.();
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
    mutationFn: () => unpublishProduction(session!.accessToken, productionId),
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

  const panelClass = variant === "center" ? `${styles.panel} ${styles.panelCenter}` : styles.panel;

  if (!data) {
    return <aside className={panelClass}>{t("editing.panel.loading")}</aside>;
  }

  // 서버에 아직 저장되지 않은 방금 입력한 제목으로도 발행 버튼이 바로 켜져야 한다 — "발행하기"가
  // 저장까지 함께 하므로(handlePublish) 서버 상태가 아니라 지금 입력 중인 draft를 기준으로 본다.
  const hasKoTitle = drafts.KO.title.trim() !== "";
  const hasEnTitle = drafts.EN.title.trim() !== "";

  return (
    <aside className={panelClass} aria-label={t("editing.panel.heading")}>
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
        <span>{t("editing.panel.titleLabel")}</span>
        <input
          type="text"
          value={drafts[activeLocale].title}
          onChange={(e) =>
            setDrafts((prev) => ({ ...prev, [activeLocale]: { ...prev[activeLocale], title: e.target.value } }))
          }
        />
      </label>

      <label className={styles.field}>
        <span>{t("editing.panel.subtitleLabel")}</span>
        <input
          type="text"
          value={drafts[activeLocale].subtitle}
          onChange={(e) =>
            setDrafts((prev) => ({ ...prev, [activeLocale]: { ...prev[activeLocale], subtitle: e.target.value } }))
          }
        />
      </label>

      {variant === "center" ? (
        <MarkdownEditor
          id={`production-${productionId}-description`}
          label={t("editing.panel.descriptionLabel")}
          value={drafts[activeLocale].description}
          onChange={(description) =>
            setDrafts((prev) => ({ ...prev, [activeLocale]: { ...prev[activeLocale], description } }))
          }
          ownerType="PRODUCTION"
          ownerId={productionId}
          onImagesChanged={invalidate}
          maxLength={4000}
        />
      ) : (
        <label className={styles.field}>
          <span>{t("editing.panel.descriptionLabel")}</span>
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
      )}

      {/* 캘린더는 만들지 않는다 — 네이버 예약이 이미 제공한다(CLAUDE.md §4). 예약/위치 링크만 붙인다.
          제목·설명과 별개 저장 버튼으로 나눠뒀더니 운영자가 URL 저장 버튼을 놓치고 값이 비는
          사고가 실제로 있었다 — 아래 저장 버튼 하나로 전부 같이 저장한다. */}
      <label className={styles.field}>
        <span>{t("editing.panel.productionBookingUrlLabel")}</span>
        <input
          type="url"
          value={bookingUrlDraft}
          onChange={(e) => setBookingUrlDraft(e.target.value)}
          placeholder="https://..."
        />
      </label>

      <label className={styles.field}>
        <span>{t("editing.panel.productionNolBookingUrlLabel")}</span>
        <input
          type="url"
          value={nolBookingUrlDraft}
          onChange={(e) => setNolBookingUrlDraft(e.target.value)}
          placeholder="https://nol.yanolja.com/..."
        />
      </label>

      <label className={styles.field}>
        <span>{t("editing.panel.productionLocationUrlLabel")}</span>
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
        disabled={
          saveDraftMutation.isPending ||
          bookingUrlMutation.isPending ||
          nolBookingUrlMutation.isPending ||
          locationUrlMutation.isPending
        }
        onClick={() => {
          setSaveNotice(null);
          setActionError(null);
          saveDraftMutation.mutate();
          // 값이 바뀌지 않았으면 보내지 않는다 — 예약 URL은 매번 sudo(PIN)를 요구하므로(§3.4),
          // 건드리지 않은 예약 링크 때문에 제목만 고치려는 저장에도 PIN이 뜨면 안 된다.
          if (locationUrlDraft !== (data.locationUrl ?? "")) locationUrlMutation.mutate();
          if (bookingUrlDraft !== (data.bookingUrl ?? "")) bookingUrlMutation.mutate();
          if (nolBookingUrlDraft !== (data.nolBookingUrl ?? "")) nolBookingUrlMutation.mutate();
        }}
      >
        {t("editing.panel.saveDraft")}
      </button>

      <hr className={styles.divider} />

      <h3 className={styles.imagesHeading}>{t("editing.panel.imagesHeading")}</h3>
      <ImageDropzone ownerType="PRODUCTION" ownerId={productionId} images={data.images} onChanged={invalidate} />

      <hr className={styles.divider} />

      {!hasEnTitle && <p className={styles.warning}>{t("editing.panel.enMissing")}</p>}

      <div className={styles.publishRow}>
        <span className={styles.statusBadge}>{data.status}</span>
        <div className={styles.publishActions}>
          {/* 발행은 이미 공개된 페이지에서도 항상 눌러야 한다 — 새로 임시저장한 draft를
              공개본으로 밀어 올리는 동작이라 발행취소와 배타적이지 않다. */}
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
            if (action === "save-links") {
              // 둘 중 어느 쪽이 PIN_REQUIRED를 냈는지 알 수 없으니, 바뀐 값이 있는 쪽을 다시 보낸다.
              if (bookingUrlDraft !== (data.bookingUrl ?? "")) bookingUrlMutation.mutate();
              if (nolBookingUrlDraft !== (data.nolBookingUrl ?? "")) nolBookingUrlMutation.mutate();
            }
          }}
        />
      )}
    </aside>
  );
}
