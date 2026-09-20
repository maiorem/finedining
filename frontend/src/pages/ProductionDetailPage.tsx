import { lazy, Suspense, useState } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useProduction } from "../api/productions";
import { ApiError } from "../api/http";
import { useAdminAuth } from "../contexts/AdminAuthContext";
import { useCan } from "../hooks/useCan";
import { useMediaQuery } from "../hooks/useMediaQuery";
import { FathersTableDetail } from "../components/section/FathersTableDetail";
import { FATHERS_TABLE_SLUG } from "../constants/fathersTable";
import { MarkdownContent } from "../components/section/MarkdownContent";
import { referencedImageIds } from "../utils/markdown";
import { EditableSection } from "../features/editing/EditableSection";
import styles from "./ProductionDetailPage.module.css";

// features/editing/*는 React.lazy로만 import한다 — 익명 방문자 번들에 섞이지 않는다(CLAUDE.md §3.5·§9).
const ProductionEditPanel = lazy(() => import("../features/editing/ProductionEditPanel"));

export default function ProductionDetailPage() {
  const { slug = "" } = useParams();
  const { t, i18n } = useTranslation();
  const canEdit = useCan("production:edit");
  const { session } = useAdminAuth();
  // 관리자는 아직 발행되지 않은(DRAFT) 작품도 같은 URL에서 볼 수 있어야 편집 패널에 붙을 수
  // 있다 — 방금 "새 작품 추가"로 만든 작품이 대표적이다(§3.9 ?preview=true).
  const { data: production, isLoading, error } = useProduction(
    slug,
    i18n.language,
    canEdit ? session?.accessToken : undefined,
  );
  const isDesktop = useMediaQuery("(min-width: 1024px)");
  const [editMode, setEditMode] = useState(false);

  if (isLoading) {
    return (
      <main className={styles.page}>
        <p className={styles.status}>{t("productions.loading")}</p>
      </main>
    );
  }

  if (error || !production) {
    const notFound = error instanceof ApiError && error.code === "ENTITY_NOT_FOUND";
    return (
      <main className={styles.page}>
        <p className={styles.status}>{notFound ? t("productions.notFound") : t("productions.loadError")}</p>
      </main>
    );
  }

  const showPanel = canEdit && editMode;
  const editControls = (
    <>
      {canEdit && (
        <button
          type="button"
          className={styles.editToggle}
          aria-pressed={editMode}
          onClick={() => setEditMode((prev) => !prev)}
        >
          {editMode ? t("editing.exitEditMode") : t("editing.enterEditMode")}
        </button>
      )}
      {showPanel && !isDesktop && <p className={styles.desktopOnlyNotice}>{t("editing.desktopOnly")}</p>}
    </>
  );

  if (production.slug === FATHERS_TABLE_SLUG) {
    return (
      <div className={showPanel ? styles.layoutWithPanel : styles.layout}>
        <main className={styles.pageFlush}>
          <FathersTableDetail production={production} editSlot={<div className={styles.editBar}>{editControls}</div>} />
        </main>
        {showPanel && isDesktop && (
          <Suspense fallback={<aside className={styles.panelLoading}>{t("editing.panel.loading")}</aside>}>
            <ProductionEditPanel productionId={production.id} />
          </Suspense>
        )}
      </div>
    );
  }

  // 편집 모드(데스크톱)에서는 공개 화면 대신 가운데에 글쓰기 폼을 보여준다. "발행하기"가 끝나면
  // 편집 모드를 끄고 발행된 상세를 바로 보여준다. 고정 상세(아버지의 식탁)는 위에서 따로 처리한다.
  if (showPanel && isDesktop) {
    return (
      <main className={styles.editPage}>
        <button type="button" className={styles.editToggle} aria-pressed={editMode} onClick={() => setEditMode(false)}>
          {t("editing.exitEditMode")}
        </button>
        <Suspense fallback={<p className={styles.status}>{t("editing.panel.loading")}</p>}>
          <ProductionEditPanel productionId={production.id} variant="center" onPublished={() => setEditMode(false)} />
        </Suspense>
      </main>
    );
  }

  // 본문에 이미지를 직접 넣었으면(`image:번호`) 그 사진은 히어로·아래 갤러리에서 뺀다 — 두 번 나오지 않게.
  const inlineImageIds = referencedImageIds(production.description);
  const [heroImage, ...editorialImages] = production.images.filter((image) => !inlineImageIds.has(image.id));

  return (
    <div className={styles.layout}>
      <main className={styles.page}>
        {heroImage ? (
          <EditableSection active={showPanel}>
            <section className={styles.hero}>
              <img
                className={styles.heroImage}
                src={heroImage.url1600 ?? heroImage.url960 ?? heroImage.url640 ?? undefined}
                alt={heroImage.altText ?? ""}
                loading="eager"
                fetchPriority="high"
              />
              <div className={styles.heroScrim} />
              <div className={styles.heroOverlay}>
                {production.subtitle && <p className={styles.heroEyebrow}>{production.subtitle}</p>}
                {production.title && <h1 className={styles.heroTitle}>{production.title}</h1>}
              </div>
            </section>
          </EditableSection>
        ) : (
          <EditableSection active={showPanel}>
            {production.subtitle && <p className={styles.eyebrow}>{production.subtitle}</p>}
            {production.title && <h1 className={styles.title}>{production.title}</h1>}
          </EditableSection>
        )}

        {canEdit && (
          <button
            type="button"
            className={styles.editToggle}
            aria-pressed={editMode}
            onClick={() => setEditMode((prev) => !prev)}
          >
            {editMode ? t("editing.exitEditMode") : t("editing.enterEditMode")}
          </button>
        )}

        {showPanel && !isDesktop && <p className={styles.desktopOnlyNotice}>{t("editing.desktopOnly")}</p>}

        {production.description && (
          <EditableSection active={showPanel}>
            <div className={styles.description}>
              <MarkdownContent source={production.description} images={production.images} />
            </div>
          </EditableSection>
        )}

        {(production.bookingUrl || production.locationUrl) && (
          <EditableSection active={showPanel}>
            <div className={styles.actions}>
              {production.bookingUrl && (
                <a
                  className={styles.actionLink}
                  href={production.bookingUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={`${t("booking.reserve")} (${t("booking.opensNewWindow")})`}
                >
                  {t("booking.reserve")}
                </a>
              )}
              {production.locationUrl && (
                <a
                  className={styles.actionLink}
                  href={production.locationUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={`${t("booking.location")} (${t("booking.opensNewWindow")})`}
                >
                  {t("booking.location")}
                </a>
              )}
            </div>
          </EditableSection>
        )}

        {editorialImages.length > 0 && (
          <EditableSection active={showPanel}>
            <div className={styles.editorial}>
              {editorialImages.map((image) => {
                const src = image.url1600 ?? image.url960 ?? image.url640;
                return (
                  <figure key={image.id} className={styles.editorialBlock}>
                    {src && (
                      <img
                        className={styles.editorialImage}
                        src={src}
                        alt={image.altText ?? ""}
                        width={image.width ?? undefined}
                        height={image.height ?? undefined}
                        loading="lazy"
                        decoding="async"
                      />
                    )}
                    {image.caption && <figcaption className={styles.editorialCaption}>{image.caption}</figcaption>}
                  </figure>
                );
              })}
            </div>
          </EditableSection>
        )}
      </main>
    </div>
  );
}
