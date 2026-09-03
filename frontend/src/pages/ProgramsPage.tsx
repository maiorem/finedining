import { lazy, Suspense, useState } from "react";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { usePrograms } from "../api/programs";
import { ProgramCard } from "../components/section/ProgramCard";
import { queryKeys } from "../api/queryKeys";
import { useCan } from "../hooks/useCan";
import styles from "./ProgramsPage.module.css";

// 프로그램 생성은 관리자 전용 쓰기 동작이라 React.lazy로만 import한다 — 익명 방문자 번들에
// 섞이면 안 된다(CLAUDE.md §3.5·§9).
const CreateProgramForm = lazy(() => import("../features/editing/CreateProgramForm"));

export default function ProgramsPage() {
  const { t, i18n } = useTranslation();
  const { data: programs, isLoading } = usePrograms(i18n.language);
  const canEdit = useCan("program:edit");
  const [addingNew, setAddingNew] = useState(false);
  const queryClient = useQueryClient();

  return (
    <main className={styles.page}>
      <h1 className={styles.srOnly}>{t("nav.programs")}</h1>

      {canEdit && (
        <button
          type="button"
          className={styles.addToggle}
          aria-pressed={addingNew}
          onClick={() => setAddingNew((prev) => !prev)}
        >
          {addingNew ? t("editing.exitEditMode") : t("programs.addNew")}
        </button>
      )}

      {addingNew && (
        <Suspense fallback={<p className={styles.status}>{t("editing.panel.loading")}</p>}>
          <CreateProgramForm onCreated={() => void queryClient.invalidateQueries({ queryKey: queryKeys.programs.all })} />
        </Suspense>
      )}

      {isLoading && <p className={styles.status}>{t("programs.loading")}</p>}
      {!isLoading && programs?.length === 0 && <p className={styles.status}>{t("programs.empty")}</p>}

      <ul className={styles.list}>
        {programs?.map((program) => (
          <li key={program.id}>
            <ProgramCard program={program} />
          </li>
        ))}
      </ul>
    </main>
  );
}
