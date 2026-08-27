import { useEffect, useRef, type KeyboardEvent, type ReactNode } from "react";
import styles from "./Modal.module.css";

type ModalProps = {
  titleId: string;
  title: string;
  closeLabel: string;
  onClose: () => void;
  children: ReactNode;
};

const FOCUSABLE_SELECTOR =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

// 모바일 메뉴·모달 공통: 포커스 트랩 + Esc 닫기 + 배경 스크롤 락 (CLAUDE.md §8.8).
export function Modal({ titleId, title, closeLabel, onClose, children }: ModalProps) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const bodyRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    // 본문(children) 안의 첫 요소에 초점을 준다 — 헤더의 닫기 버튼으로 가면 PIN 입력 같은
    // 본문 필드에 매번 다시 탭해야 해서 불편하다. 탭 순서 자체는 여전히 닫기 버튼을 포함한다.
    const firstFocusable = bodyRef.current?.querySelector<HTMLElement>(FOCUSABLE_SELECTOR);
    firstFocusable?.focus();

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  function handleKeyDown(e: KeyboardEvent<HTMLDivElement>) {
    if (e.key === "Escape") {
      onClose();
      return;
    }
    if (e.key !== "Tab" || !dialogRef.current) {
      return;
    }
    const focusable = Array.from(dialogRef.current.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR));
    if (focusable.length === 0) {
      return;
    }
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault();
      last.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault();
      first.focus();
    }
  }

  return (
    <div className={styles.overlay} onMouseDown={onClose}>
      <div
        ref={dialogRef}
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onMouseDown={(e) => e.stopPropagation()}
        onKeyDown={handleKeyDown}
      >
        <div className={styles.header}>
          <h2 id={titleId} className={styles.title}>
            {title}
          </h2>
          <button type="button" className={styles.closeButton} onClick={onClose} aria-label={closeLabel}>
            ×
          </button>
        </div>
        <div ref={bodyRef}>{children}</div>
      </div>
    </div>
  );
}
