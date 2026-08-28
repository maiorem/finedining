import { lazy, Suspense, useState } from "react";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { useProductions } from "../api/productions";
import { ProductionCard } from "../components/section/ProductionCard";
import { queryKeys } from "../api/queryKeys";
import { useCan } from "../hooks/useCan";
import styles from "./ProductionsPage.module.css";

// 작품 생성은 관리자 전용 쓰기 동작이라 React.lazy로만 import한다 — 익명 방문자 번들에
// 섞이면 안 된다(CLAUDE.md §3.5·§9).
const CreateProductionForm = lazy(() => import("../features/editing/CreateProductionForm"));

export default function ProductionsPage() {
  const { t, i18n } = useTranslation();
  const { data: productions, isLoading } = useProductions(i18n.language);
  const canEdit = useCan("production:edit");
  const [addingNew, setAddingNew] = useState(false);
  const queryClient = useQueryClient();

  return (
    <main className={styles.page}>
      <h1 className={styles.heading}>{t("nav.productions")}</h1>

      {canEdit && (
        <button
          type="button"
          className={styles.addToggle}
          aria-pressed={addingNew}
          onClick={() => setAddingNew((prev) => !prev)}
        >
          {addingNew ? t("editing.exitEditMode") : t("productions.create.addNew")}
        </button>
      )}

      {addingNew && (
        <Suspense fallback={<p className={styles.status}>{t("editing.panel.loading")}</p>}>
          <CreateProductionForm onCreated={() => void queryClient.invalidateQueries({ queryKey: queryKeys.productions.all })} />
        </Suspense>
      )}

      {isLoading && <p className={styles.status}>{t("productions.loading")}</p>}
      {!isLoading && productions?.length === 0 && <p className={styles.status}>{t("productions.empty")}</p>}

      <ul className={styles.grid}>
        {productions?.map((production) => (
          <li key={production.id}>
            <ProductionCard production={production} />
          </li>
        ))}
      </ul>
    </main>
  );
}
