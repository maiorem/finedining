import { lazy, Suspense, useState } from "react";
import { useTranslation } from "react-i18next";
import { usePrograms } from "../api/programs";
import { ProgramCard } from "../components/section/ProgramCard";
import { useCan } from "../hooks/useCan";
import styles from "./ProgramsPage.module.css";

// 관리자 전용 API 경로가 익명 방문자 번들에 섞이면 안 되므로 React.lazy로만 import한다(CLAUDE.md §3.5·§9).
const ProgramManagementPanel = lazy(() => import("../features/editing/ProgramManagementPanel"));

export default function ProgramsPage() {
  const { t, i18n } = useTranslation();
  const { data: programs, isLoading } = usePrograms(i18n.language);
  const canEdit = useCan("program:edit");
  const [managing, setManaging] = useState(false);

  return (
    <main className={styles.page}>
      <h1 className={styles.heading}>{t("nav.programs")}</h1>

      {canEdit && (
        <button
          type="button"
          className={styles.manageToggle}
          aria-pressed={managing}
          onClick={() => setManaging((prev) => !prev)}
        >
          {managing ? t("editing.exitEditMode") : t("programs.manageToggle")}
        </button>
      )}

      {managing && (
        <Suspense fallback={<p className={styles.status}>{t("programs.loading")}</p>}>
          <ProgramManagementPanel />
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
