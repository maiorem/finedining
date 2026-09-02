import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import {
  changeProductionBookingUrl,
  changeProductionLocationUrl,
  getProductionForAdmin,
  publishProduction,
  saveDraftTranslation,
  unpublishProduction,
} from "../../api/productionAdmin";
import { ApiError } from "../../api/http";
import { queryKeys } from "../../api/queryKeys";
import { ImageDropzone } from "./ImageDropzone";
import { PinModal } from "./PinModal";
import styles from "./ProductionEditPanel.module.css";

type Locale = "KO" | "EN";
const LOCALES: Locale[] = ["KO", "EN"];

type DraftState = Record<Locale, { title: string; subtitle: string; description: string }>;

type ProductionEditPanelProps = {
  productionId: number;
};

const EMPTY_DRAFTS: DraftState = {
  KO: { title: "", subtitle: "", description: "" },
  EN: { title: "", subtitle: "", description: "" },
};

/**
 * §3.9의 "같은 페이지, 편집 패널" 그 자체. 이 모듈은 `features/editing/`에 있으므로
 * React.lazy로만 import된다 — 익명 방문자 번들에 섞이지 않는다(§3.5·§9).
 */
export default function ProductionEditPanel({ productionId }: ProductionEditPanelProps) {
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
  const [locationUrlDraft, setLocationUrlDraft] = useState("");
  const [pinAction, setPinAction] = useState<"publish" | "unpublish" | "booking-url" | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);

  useEffect(() => {
    if (!data) return;
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
        setPinAction("booking-url");
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

  const publishMutation = useMutation({
    mutationFn: () => publishProduction(session!.accessToken, productionId),
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

  if (!data) {
    return <aside className={styles.panel}>{t("editing.panel.loading")}</aside>;
  }

  const koTranslation = data.translations.find((tr) => tr.locale === "KO");
  const enTranslation = data.translations.find((tr) => tr.locale === "EN");
  const hasKoTitle = Boolean(koTranslation?.title ?? koTranslation?.draftTitle);
  const hasEnTitle = Boolean(enTranslation?.title ?? enTranslation?.draftTitle);

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

      {/* 캘린더는 만들지 않는다 — 네이버 예약이 이미 제공한다(CLAUDE.md §4). 예매/위치 링크만 붙인다.
          제목·설명과 별개 저장 버튼으로 나눠뒀더니 운영자가 URL 저장 버튼을 놓치고 값이 비는
          사고가 실제로 있었다 — 아래 저장 버튼 하나로 전부 같이 저장한다. */}
      <label className={styles.field}>
        <span>{t("editing.panel.productionBookingUrlLabel")}</span>
        <input
          type="url"
          value={bookingUrlDraft}
          onChange={(e) => setBookingUrlDraft(e.target.value)}
          placeholder="https://booking.naver.com/..."
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
        disabled={saveDraftMutation.isPending || bookingUrlMutation.isPending || locationUrlMutation.isPending}
        onClick={() => {
          setSaveNotice(null);
          setActionError(null);
          saveDraftMutation.mutate();
          // 값이 바뀌지 않았으면 보내지 않는다 — bookingUrl은 매번 sudo(PIN)를 요구하므로(§3.4),
          // 건드리지 않은 예매 링크 때문에 제목만 고치려는 저장에도 PIN이 뜨면 안 된다.
          if (locationUrlDraft !== (data.locationUrl ?? "")) locationUrlMutation.mutate();
          if (bookingUrlDraft !== (data.bookingUrl ?? "")) bookingUrlMutation.mutate();
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
            if (action === "booking-url") bookingUrlMutation.mutate();
          }}
        />
      )}
    </aside>
  );
}
