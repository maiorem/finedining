import { lazy, Suspense, useState } from "react";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { useArtists } from "../api/artists";
import { useCastings } from "../api/castings";
import { ArtistCard } from "../components/section/ArtistCard";
import { queryKeys } from "../api/queryKeys";
import { useCan } from "../hooks/useCan";
import styles from "./ArtistsPage.module.css";

// 아티스트 생성은 관리자 전용 쓰기 동작이라 React.lazy로만 import한다 — 익명 방문자 번들에
// 섞이면 안 된다(CLAUDE.md §3.5·§9).
const CreateArtistForm = lazy(() => import("../features/editing/CreateArtistForm"));

export default function ArtistsPage() {
  const { t, i18n } = useTranslation();
  const { data: artists, isLoading: artistsLoading } = useArtists(i18n.language);
  const { data: castings } = useCastings(i18n.language);
  const canEdit = useCan("artist:edit");
  const [addingNew, setAddingNew] = useState(false);
  const queryClient = useQueryClient();

  return (
    <main className={styles.page}>
      <h1 className={styles.heading}>{t("nav.artists")}</h1>

      {canEdit && (
        <button
          type="button"
          className={styles.addToggle}
          aria-pressed={addingNew}
          onClick={() => setAddingNew((prev) => !prev)}
        >
          {addingNew ? t("editing.exitEditMode") : t("artists.create.addNew")}
        </button>
      )}

      {addingNew && (
        <Suspense fallback={<p className={styles.status}>{t("editing.panel.loading")}</p>}>
          <CreateArtistForm onCreated={() => void queryClient.invalidateQueries({ queryKey: queryKeys.artists.all })} />
        </Suspense>
      )}

      {artistsLoading && <p className={styles.status}>{t("artists.loading")}</p>}
      {!artistsLoading && artists?.length === 0 && (
        <p className={styles.status}>{t("artists.empty")}</p>
      )}

      <ul className={styles.list}>
        {artists?.map((artist) => (
          <li key={artist.id}>
            <ArtistCard artist={artist} />
          </li>
        ))}
      </ul>

      {castings && castings.length > 0 && (
        <section>
          <h2 className={styles.castingHeading}>{t("artists.castingHeading")}</h2>
          <ul className={styles.castingList}>
            {castings.map((casting) => (
              <li key={casting.id} className={styles.castingItem}>
                <h3 className={styles.castingTitle}>{casting.title}</h3>
                <p className={styles.castingBody}>{casting.body}</p>
              </li>
            ))}
          </ul>
        </section>
      )}
    </main>
  );
}
