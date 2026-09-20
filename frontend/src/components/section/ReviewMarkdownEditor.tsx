import { useRef, useState, type ChangeEvent } from "react";
import { useTranslation } from "react-i18next";
import { IMAGE_MAX_WIDTH, IMAGE_MIN_WIDTH } from "../../utils/markdown";
import { insertBlock, prefixLines, wrapSelection, type EditResult } from "../../utils/markdownEditing";
import { newImageToken } from "../../utils/reviewImages";
import styles from "./ReviewMarkdownEditor.module.css";

const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const MAX_IMAGE_BYTES = 10 * 1024 * 1024; // 회원 업로드 상한(서버와 같다)
export const MAX_REVIEW_IMAGES = 3;

/**
 * 이미지를 다루는 방식:
 * - none: 이미지 버튼이 없다(관리자가 원문을 고칠 때 — 서식만).
 * - deferred: 새 글. 글 번호가 아직 없어서 사진은 첨부 목록에만 담고 본문에는 임시 표시(image:newN)를 넣는다.
 * - immediate: 이미 있는 글을 고칠 때. 바로 올리고 실제 번호를 본문에 넣는다.
 */
export type ReviewImageMode =
  | { kind: "none" }
  | { kind: "deferred"; fileCount: number; onAttach: (file: File) => void }
  | { kind: "immediate"; imageCount: number; onUpload: (file: File) => Promise<number> };

type Props = {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  images: ReviewImageMode;
  maxLength?: number;
  rows?: number;
  required?: boolean;
};

/**
 * 회원이 이야기를 쓰는 서식 편집기 — 굵게·기울임·소제목·목록·인용과 자기 글의 사진(최대 3장)만 다룬다.
 * 링크·유튜브·구분선은 일부러 뺐다: 스팸이 링크로 들어오기 때문이다(CLAUDE.md §3.6). 화면도 링크·유튜브를 그리지 않는다.
 */
export default function ReviewMarkdownEditor({
  id,
  label,
  value,
  onChange,
  images,
  maxLength = 4000,
  rows = 10,
  required = false,
}: Props) {
  const { t } = useTranslation();
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [alt, setAlt] = useState("");
  const [widthInput, setWidthInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const usedImages = images.kind === "deferred" ? images.fileCount : images.kind === "immediate" ? images.imageCount : 0;

  function apply(edit: (start: number, end: number) => EditResult) {
    const textarea = textareaRef.current;
    const result = edit(textarea?.selectionStart ?? value.length, textarea?.selectionEnd ?? value.length);
    onChange(result.value);
    requestAnimationFrame(() => {
      textarea?.focus();
      textarea?.setSelectionRange(result.selectionStart, result.selectionEnd);
    });
  }

  function handleFile(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0] ?? null;
    e.target.value = "";
    if (!file) return;
    if (!IMAGE_TYPES.includes(file.type)) return setNotice(t("reviews.error.IMAGE_TYPE"));
    if (file.size > MAX_IMAGE_BYTES) return setNotice(t("reviews.error.IMAGE_TOO_LARGE"));
    if (usedImages >= MAX_REVIEW_IMAGES) return setNotice(t("reviews.error.IMAGE_COUNT"));
    setNotice(null);
    setAlt("");
    setWidthInput("");
    setPendingFile(file);
  }

  async function handleInsert() {
    if (!pendingFile || images.kind === "none") return;
    const width = widthInput.trim() === "" ? null : Number(widthInput);
    if (width !== null && (!Number.isInteger(width) || width < IMAGE_MIN_WIDTH || width > IMAGE_MAX_WIDTH)) {
      setNotice(t("editing.md.widthInvalid", { min: IMAGE_MIN_WIDTH, max: IMAGE_MAX_WIDTH }));
      return;
    }
    setNotice(null);
    let token: string;
    if (images.kind === "deferred") {
      images.onAttach(pendingFile);
      token = newImageToken(images.fileCount + 1, alt.trim(), width);
    } else {
      setBusy(true);
      try {
        const mediaId = await images.onUpload(pendingFile);
        token = `![${alt.trim().replace(/[[\]]/g, "")}](image:${mediaId}${width ? `|${width}` : ""})`;
      } catch {
        setNotice(t("reviews.notice.imageFailed", { count: 1 }));
        return;
      } finally {
        setBusy(false);
      }
    }
    apply((s, e) => insertBlock(value, s, e, token));
    setPendingFile(null);
  }

  return (
    <div className={styles.editor}>
      <label htmlFor={id} className={styles.label}>
        {label}
      </label>

      <div className={styles.toolbar} role="toolbar" aria-label={t("editing.md.toolbar")}>
        <button type="button" onClick={() => apply((s, e) => wrapSelection(value, s, e, "**", "**", t("editing.md.boldText")))}>
          {t("editing.md.bold")}
        </button>
        <button type="button" onClick={() => apply((s, e) => wrapSelection(value, s, e, "*", "*", t("editing.md.italicText")))}>
          {t("editing.md.italic")}
        </button>
        <button type="button" onClick={() => apply((s, e) => prefixLines(value, s, e, "# "))}>
          {t("editing.md.heading")}
        </button>
        <button type="button" onClick={() => apply((s, e) => prefixLines(value, s, e, "- "))}>
          {t("editing.md.list")}
        </button>
        <button type="button" onClick={() => apply((s, e) => prefixLines(value, s, e, "> "))}>
          {t("editing.md.quote")}
        </button>
        {images.kind !== "none" && (
          <>
            <button type="button" onClick={() => fileInputRef.current?.click()}>
              {t("editing.md.image")}
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept={IMAGE_TYPES.join(",")}
              className={styles.fileInput}
              aria-label={t("editing.md.image")}
              onChange={handleFile}
              tabIndex={-1}
            />
          </>
        )}
      </div>

      {pendingFile && (
        <div className={styles.imageRow}>
          <label htmlFor={`${id}-alt`}>{t("reviews.imageAltLabel", { name: pendingFile.name })}</label>
          <input id={`${id}-alt`} type="text" value={alt} onChange={(e) => setAlt(e.target.value)} maxLength={200} />
          <label htmlFor={`${id}-width`}>{t("editing.md.widthLabel")}</label>
          <input
            id={`${id}-width`}
            type="number"
            inputMode="numeric"
            min={IMAGE_MIN_WIDTH}
            max={IMAGE_MAX_WIDTH}
            value={widthInput}
            onChange={(e) => setWidthInput(e.target.value)}
            placeholder={t("editing.md.widthPlaceholder")}
          />
          <div className={styles.imageActions}>
            <button type="button" disabled={busy} onClick={() => void handleInsert()}>
              {images.kind === "deferred" ? t("reviews.imageInsertDeferred") : t("editing.md.uploadInsert")}
            </button>
            <button type="button" disabled={busy} onClick={() => setPendingFile(null)}>
              {t("editing.md.cancel")}
            </button>
          </div>
        </div>
      )}

      {notice && (
        <p className={styles.notice} role="alert">
          {notice}
        </p>
      )}

      <textarea
        id={id}
        ref={textareaRef}
        className={styles.textarea}
        rows={rows}
        maxLength={maxLength}
        required={required}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
      <small className={styles.counter}>
        {value.length} / {maxLength}
      </small>
    </div>
  );
}
