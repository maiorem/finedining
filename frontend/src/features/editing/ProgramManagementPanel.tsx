import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import { createProgram, listProgramsForAdmin } from "../../api/programAdmin";
import { queryKeys } from "../../api/queryKeys";
import ProgramForm from "./ProgramForm";
import styles from "./ProgramManagementPanel.module.css";

/**
 * 프로그램(이벤트 공지) 관리자 패널. ProgramsPage에서 React.lazy로만 import된다 — 관리자 전용
 * API 경로가 익명 방문자 번들에 섞이면 안 된다(CLAUDE.md §3.5·§9). Program은 슬러그도 별도
 * 상세 페이지도 없다(Casting과 같은 패턴) — 목록 안에서 바로 만들고 바로 편집한다.
 */
export default function ProgramManagementPanel() {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);

  const { data: programs, isLoading } = useQuery({
    queryKey: queryKeys.programs.adminList,
    queryFn: () => listProgramsForAdmin(session!.accessToken),
    enabled: Boolean(session),
  });

  const createMutation = useMutation({
    mutationFn: () => createProgram(session!.accessToken),
    onSuccess: (created) => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.programs.adminList });
      setEditingId(created.id);
    },
  });

  return (
    <div className={styles.panel}>
      <button
        type="button"
        className={styles.addToggle}
        disabled={createMutation.isPending}
        onClick={() => createMutation.mutate()}
      >
        {t("programs.addNew")}
      </button>

      {isLoading && <p className={styles.status}>{t("programs.loading")}</p>}

      <ul className={styles.list}>
        {programs?.map((program) => {
          const koTitle = program.translations.find((tr) => tr.locale === "KO");
          const title = koTitle?.title ?? koTitle?.draftTitle ?? t("programs.form.untitled");

          return (
            <li key={program.id} className={styles.item}>
              {editingId === program.id ? (
                <ProgramForm programId={program.id} onClose={() => setEditingId(null)} />
              ) : (
                <div className={styles.row}>
                  <div>
                    <p className={styles.title}>{title}</p>
                    <span className={styles.statusBadge}>{program.status}</span>
                  </div>
                  <button type="button" onClick={() => setEditingId(program.id)}>
                    {t("programs.form.edit")}
                  </button>
                </div>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
