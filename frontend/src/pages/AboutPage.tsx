import { lazy, Suspense, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAbout } from "../api/about";
import { ApiError } from "../api/http";
import { useAdminAuth } from "../contexts/AdminAuthContext";
import { useCan } from "../hooks/useCan";
import { useMediaQuery } from "../hooks/useMediaQuery";
import { EditableSection } from "../features/editing/EditableSection";
import styles from "./AboutPage.module.css";

// features/editing/*는 React.lazy로만 import한다 — 익명 방문자 번들에 섞이지 않는다(CLAUDE.md §3.5·§9).
const AboutEditPanel = lazy(() => import("../features/editing/AboutEditPanel"));

export default function AboutPage() {
  const { t, i18n } = useTranslation();
  const canEdit = useCan("about:edit");
  const { session } = useAdminAuth();
  // 관리자는 한 번도 발행 안 된 소개문도 같은 페이지에서 편집 패널에 붙을 수 있어야 한다
  // (§3.9 ?preview=true).
  const { data: about, isLoading, error } = useAbout(i18n.language, canEdit ? session?.accessToken : undefined);
  const isDesktop = useMediaQuery("(min-width: 1024px)");
  const [editMode, setEditMode] = useState(false);

  if (isLoading) {
    return (
      <main className={styles.page}>
        <p className={styles.status}>{t("about.loading")}</p>
      </main>
    );
  }

  const showPanel = canEdit && editMode;

  // 아직 한 번도 발행 안 됐으면 404다 — 관리자가 편집 모드에 붙을 수 있어야 하므로 에러로
  // 막지 않고 빈 상태로 넘어간다.
  if ((error || !about) && !showPanel) {
    const notFound = error instanceof ApiError && error.code === "ENTITY_NOT_FOUND";
    return (
      <main className={styles.page}>
        {canEdit && (
          <button
            type="button"
            className={styles.editToggle}
            aria-pressed={editMode}
            onClick={() => setEditMode((prev) => !prev)}
          >
            {t("editing.enterEditMode")}
          </button>
        )}
        <h1 className={styles.srOnly}>{t("nav.about")}</h1>
        <p className={styles.status}>{notFound ? t("about.notFound") : t("about.loadError")}</p>
      </main>
    );
  }

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
          <h1 className={styles.srOnly}>{t("nav.about")}</h1>
          {about?.intro ? (
            <p className={styles.intro}>{about.intro}</p>
          ) : (
            showPanel && <p className={styles.status}>{t("about.notFound")}</p>
          )}
        </EditableSection>
      </main>

      {showPanel && isDesktop && (
        <Suspense fallback={<aside className={styles.panelLoading}>{t("editing.panel.loading")}</aside>}>
          <AboutEditPanel />
        </Suspense>
      )}
    </div>
  );
}
