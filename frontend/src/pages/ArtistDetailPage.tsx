import { lazy, Suspense, useState } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useArtist } from "../api/artists";
import { ApiError } from "../api/http";
import { useAdminAuth } from "../contexts/AdminAuthContext";
import { useCan } from "../hooks/useCan";
import { useMediaQuery } from "../hooks/useMediaQuery";
import { EditableSection } from "../features/editing/EditableSection";
import styles from "./ArtistDetailPage.module.css";

// features/editing/*는 React.lazy로만 import한다 — 익명 방문자 번들에 섞이지 않는다(CLAUDE.md §3.5·§9).
const ArtistEditPanel = lazy(() => import("../features/editing/ArtistEditPanel"));

export default function ArtistDetailPage() {
  const { slug = "" } = useParams();
  const { t, i18n } = useTranslation();
  const canEdit = useCan("artist:edit");
  const { session } = useAdminAuth();
  // 관리자는 아직 발행되지 않은(DRAFT) 아티스트도 같은 URL에서 볼 수 있어야 편집 패널에 붙을 수
  // 있다 — 방금 "새 아티스트 추가"로 만든 아티스트가 대표적이다(§3.9 ?preview=true).
  const { data: artist, isLoading, error } = useArtist(slug, i18n.language, canEdit ? session?.accessToken : undefined);
  const isDesktop = useMediaQuery("(min-width: 1024px)");
  const [editMode, setEditMode] = useState(false);

  if (isLoading) {
    return (
      <main className={styles.page}>
        <p className={styles.status}>{t("artists.loading")}</p>
      </main>
    );
  }

  if (error || !artist) {
    const notFound = error instanceof ApiError && error.code === "ENTITY_NOT_FOUND";
    return (
      <main className={styles.page}>
        <p className={styles.status}>{notFound ? t("artists.notFound") : t("artists.loadError")}</p>
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
          {artist.photo?.url640 && (
            <img
              className={styles.photo}
              src={artist.photo.url640}
              alt={artist.photo.altText ?? ""}
              width={artist.photo.width ?? undefined}
              height={artist.photo.height ?? undefined}
              loading="eager"
              decoding="async"
            />
          )}
          {artist.role && <p className={styles.eyebrow}>{artist.role}</p>}
          <h1 className={styles.name}>{artist.name}</h1>

          {artist.bio && <p className={styles.bio}>{artist.bio}</p>}

          {artist.linkUrl && (
            <a
              className={styles.link}
              href={artist.linkUrl}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`${artist.name} (${t("booking.opensNewWindow")})`}
            >
              {artist.linkUrl}
            </a>
          )}
        </EditableSection>

        {artist.productions.length > 0 && (
          <section>
            <h2 className={styles.productionsHeading}>{t("artists.productionsHeading")}</h2>
            <ul className={styles.productionList}>
              {artist.productions.map((production) => (
                <li key={production.id}>{production.title}</li>
              ))}
            </ul>
          </section>
        )}
      </main>

      {showPanel && isDesktop && (
        <Suspense fallback={<aside className={styles.panelLoading}>{t("editing.panel.loading")}</aside>}>
          <ArtistEditPanel artistId={artist.id} />
        </Suspense>
      )}
    </div>
  );
}
