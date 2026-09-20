import { lazy, Suspense, useState } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useProgram } from "../api/programs";
import { ApiError } from "../api/http";
import { useAdminAuth } from "../contexts/AdminAuthContext";
import { useCan } from "../hooks/useCan";
import { useMediaQuery } from "../hooks/useMediaQuery";
import { MarkdownContent } from "../components/section/MarkdownContent";
import { referencedImageIds } from "../utils/markdown";
import { EditableSection } from "../features/editing/EditableSection";
import styles from "./ProgramDetailPage.module.css";

// features/editing/*는 React.lazy로만 import한다 — 익명 방문자 번들에 섞이지 않는다(CLAUDE.md §3.5·§9).
const ProgramEditPanel = lazy(() => import("../features/editing/ProgramEditPanel"));

export default function ProgramDetailPage() {
  const { slug = "" } = useParams();
  const { t, i18n } = useTranslation();
  const canEdit = useCan("program:edit");
  const { session } = useAdminAuth();
  // 관리자는 아직 발행되지 않은(DRAFT) 프로그램도 같은 URL에서 볼 수 있어야 편집 패널에 붙을 수
  // 있다 — 방금 "새 프로그램 추가"로 만든 프로그램이 대표적이다(§3.9 ?preview=true).
  const { data: program, isLoading, error } = useProgram(
    slug,
    i18n.language,
    canEdit ? session?.accessToken : undefined,
  );
  const isDesktop = useMediaQuery("(min-width: 1024px)");
  const [editMode, setEditMode] = useState(false);

  if (isLoading) {
    return (
      <main className={styles.page}>
        <p className={styles.status}>{t("programs.loading")}</p>
      </main>
    );
  }

  if (error || !program) {
    const notFound = error instanceof ApiError && error.code === "ENTITY_NOT_FOUND";
    return (
      <main className={styles.page}>
        <p className={styles.status}>{notFound ? t("programs.notFound") : t("programs.loadError")}</p>
      </main>
    );
  }

  const showPanel = canEdit && editMode;
  // 본문에 이미지를 직접 넣었으면(`image:번호`) 그 사진은 히어로·아래 갤러리에서 뺀다 — 두 번 나오지 않게.
  const inlineImageIds = referencedImageIds(program.description);
  const [heroImage, ...editorialImages] = program.images.filter((image) => !inlineImageIds.has(image.id));

  // 편집 모드(데스크톱)에서는 공개 화면 대신 가운데에 편집 폼을 보여준다. "발행하기"가 끝나면
  // 편집 모드를 끄고 발행된 상세를 바로 보여준다.
  if (showPanel && isDesktop) {
    return (
      <main className={styles.editPage}>
        <button
          type="button"
          className={styles.editToggle}
          aria-pressed={editMode}
          onClick={() => setEditMode(false)}
        >
          {t("editing.exitEditMode")}
        </button>
        <Suspense fallback={<p className={styles.status}>{t("editing.panel.loading")}</p>}>
          <ProgramEditPanel programId={program.id} onPublished={() => setEditMode(false)} />
        </Suspense>
      </main>
    );
  }

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
                {program.title && <h1 className={styles.heroTitle}>{program.title}</h1>}
                {program.subtitle && <p className={styles.heroSubtitle}>{program.subtitle}</p>}
              </div>
            </section>
          </EditableSection>
        ) : (
          <EditableSection active={showPanel}>
            {program.title && <h1 className={styles.title}>{program.title}</h1>}
            {program.subtitle && <p className={styles.subtitle}>{program.subtitle}</p>}
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

        {program.description && (
          <EditableSection active={showPanel}>
            <div className={styles.description}>
              <MarkdownContent source={program.description} images={program.images} />
            </div>
          </EditableSection>
        )}

        <EditableSection active={showPanel}>
          <div className={styles.actions}>
            {program.applyUrl ? (
              <a
                className={styles.actionLink}
                href={program.applyUrl}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={`${t("programs.apply")} (${t("booking.opensNewWindow")})`}
              >
                {t("programs.apply")}
              </a>
            ) : (
              // 링크가 없으면 숨기지 않고 "준비중"으로 보여준다(2026-09-19 요청).
              <button type="button" className={styles.actionLink} disabled>
                {t("programs.comingSoon")}
              </button>
            )}
            {program.locationUrl && (
              <a
                className={styles.actionLink}
                href={program.locationUrl}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={`${t("booking.location")} (${t("booking.opensNewWindow")})`}
              >
                {t("booking.location")}
              </a>
            )}
          </div>
        </EditableSection>

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
