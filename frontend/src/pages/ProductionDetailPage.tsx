import { lazy, Suspense, useState } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useProduction } from "../api/productions";
import { ApiError } from "../api/http";
import { useAdminAuth } from "../contexts/AdminAuthContext";
import { useCan } from "../hooks/useCan";
import { useMediaQuery } from "../hooks/useMediaQuery";
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

  return (
    <div className={showPanel ? styles.layoutWithPanel : styles.layout}>
      <main className={styles.page}>
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

        <EditableSection active={showPanel}>
          {production.title && <h1 className={styles.title}>{production.title}</h1>}
          {production.subtitle && <p className={styles.subtitle}>{production.subtitle}</p>}
        </EditableSection>

        {production.images.length > 0 && (
          <EditableSection active={showPanel}>
            <ul className={styles.imageGrid}>
              {production.images.map((image) => (
                <li key={image.id}>
                  {image.url640 && (
                    <img
                      src={image.url640}
                      alt={image.altText ?? ""}
                      width={image.width ?? undefined}
                      height={image.height ?? undefined}
                      loading="lazy"
                      decoding="async"
                    />
                  )}
                </li>
              ))}
            </ul>
          </EditableSection>
        )}
      </main>

      {showPanel && isDesktop && (
        <Suspense fallback={<aside className={styles.panelLoading}>{t("editing.panel.loading")}</aside>}>
          <ProductionEditPanel productionId={production.id} />
        </Suspense>
      )}
    </div>
  );
}
