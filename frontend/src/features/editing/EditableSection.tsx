import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import styles from "./EditableSection.module.css";

type EditableSectionProps = {
  active: boolean;
  children: ReactNode;
};

/**
 * 편집 모드일 때 공개 화면 섹션에 점선 테두리 + ✎ 표시를 얹는다 (CLAUDE.md §3.9 mockup).
 * 실제 값 수정은 옆 편집 패널(ProductionEditPanel)에서 하므로 이 래퍼는 순수 시각 표시다 —
 * 편집 가능 영역 표시는 새 색을 들이지 않고 --c-spot 점선 하나로만 한다(§8.3).
 */
export function EditableSection({ active, children }: EditableSectionProps) {
  const { t } = useTranslation();

  if (!active) {
    return <>{children}</>;
  }

  return (
    <div className={styles.wrapper}>
      <span className={styles.badge} aria-hidden="true">
        ✎
      </span>
      <span className={styles.srOnly}>{t("editing.editableRegion")}</span>
      {children}
    </div>
  );
}
