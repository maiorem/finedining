import { useRef, useState, type ChangeEvent } from "react";
import { useTranslation } from "react-i18next";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import {
  completeMediaUpload,
  presignUpload,
  uploadToPresignedUrl,
  type MediaOwnerType,
} from "../../api/media";
import { extractYoutubeId } from "../../utils/youtube";
import { IMAGE_MAX_WIDTH, IMAGE_MIN_WIDTH } from "../../utils/markdown";
import { insertBlock, prefixLines, wrapSelection, type EditResult } from "../../utils/markdownEditing";
import styles from "./MarkdownEditor.module.css";

const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const MAX_IMAGE_BYTES = 20 * 1024 * 1024; // CLAUDE.md §2: 관리자 업로드 ≤20MB

type Props = {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  /** 삽입한 이미지가 붙는 글. 이미지는 이 글의 이미지가 되어 `image:번호`로 본문에 참조된다. */
  ownerType: MediaOwnerType;
  ownerId: number;
  onImagesChanged: () => void;
  maxLength: number;
  rows?: number;
};

/**
 * 관리자 전용 마크다운 편집기. 서식 버튼 + 이미지·유튜브 삽입(미리보기는 두지 않는다 — 발행하면 바로 결과를 보여준다). features/editing에
 * 있으므로 React.lazy로만 들어온다(CLAUDE.md §3.5·§9). 저장 형식은 마크다운 텍스트 그대로다.
 */
export default function MarkdownEditor({
  id,
  label,
  value,
  onChange,
  ownerType,
  ownerId,
  onImagesChanged,
  maxLength,
  rows = 18,
}: Props) {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [alt, setAlt] = useState("");
  const [widthInput, setWidthInput] = useState("");
  const [uploading, setUploading] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  function apply(edit: (start: number, end: number) => EditResult) {
    const textarea = textareaRef.current;
    const start = textarea?.selectionStart ?? value.length;
    const end = textarea?.selectionEnd ?? value.length;
    const result = edit(start, end);
    onChange(result.value);
    requestAnimationFrame(() => {
      textarea?.focus();
      textarea?.setSelectionRange(result.selectionStart, result.selectionEnd);
    });
  }

  function handleLink() {
    const url = window.prompt(t("editing.md.linkPrompt"), "https://");
    if (!url) return;
    if (!/^https?:\/\//i.test(url) && !/^mailto:/i.test(url)) {
      setNotice(t("editing.md.linkInvalid"));
      return;
    }
    setNotice(null);
    apply((s, e) => wrapSelection(value, s, e, "[", `](${url})`, t("editing.md.linkText")));
  }

  function handleYoutube() {
    const url = window.prompt(t("editing.md.youtubePrompt"), "https://www.youtube.com/watch?v=");
    if (!url) return;
    if (!extractYoutubeId(url)) {
      setNotice(t("editing.md.youtubeInvalid"));
      return;
    }
    setNotice(null);
    apply((s, e) => insertBlock(value, s, e, `@[youtube](${url.trim()})`));
  }

  function handleFile(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0] ?? null;
    e.target.value = "";
    if (!file) return;
    if (!IMAGE_TYPES.includes(file.type)) {
      setNotice(t("editing.md.imageType"));
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setNotice(t("editing.md.imageTooLarge"));
      return;
    }
    setNotice(null);
    setAlt("");
    setWidthInput("");
    setPendingFile(file);
  }

  async function handleUpload() {
    if (!pendingFile || !session) return;
    if (alt.trim() === "") {
      setNotice(t("editing.md.altRequired")); // 대체 텍스트는 필수다(CLAUDE.md §8.8)
      return;
    }
    // 가로 크기는 비워 두면 원본 그대로, 적으면 그 폭(px)으로 보인다 — 세로는 원본 비율로 저절로 정해진다.
    const width = widthInput.trim() === "" ? null : Number(widthInput);
    if (width !== null && (!Number.isInteger(width) || width < IMAGE_MIN_WIDTH || width > IMAGE_MAX_WIDTH)) {
      setNotice(t("editing.md.widthInvalid", { min: IMAGE_MIN_WIDTH, max: IMAGE_MAX_WIDTH }));
      return;
    }
    setUploading(true);
    setNotice(null);
    try {
      const { mediaAssetId, uploadUrl } = await presignUpload(session.accessToken, ownerType, ownerId, pendingFile);
      await uploadToPresignedUrl(uploadUrl, pendingFile);
      const asset = await completeMediaUpload(session.accessToken, mediaAssetId, alt.trim());
      if (asset.status === "FAILED") {
        setNotice(asset.failureReason ?? t("editing.md.uploadFailed"));
        return;
      }
      apply((s, en) => insertBlock(value, s, en, `![${alt.trim().replace(/[[\]]/g, "")}](image:${asset.id}${width ? `|${width}` : ""})`));
      setPendingFile(null);
      onImagesChanged();
    } catch {
      setNotice(t("editing.md.uploadFailed"));
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className={styles.editor}>
      <span className={styles.label}>{label}</span>

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
        <button type="button" onClick={handleLink}>
          {t("editing.md.link")}
        </button>
        <button type="button" onClick={() => apply((s, e) => insertBlock(value, s, e, "---"))}>
          {t("editing.md.hr")}
        </button>
        <button type="button" onClick={() => fileInputRef.current?.click()}>
          {t("editing.md.image")}
        </button>
        <button type="button" onClick={handleYoutube}>
          {t("editing.md.youtube")}
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
      </div>

      {pendingFile && (
        <div className={styles.altRow}>
          <label htmlFor={`${id}-alt`}>{t("editing.md.altLabel", { name: pendingFile.name })}</label>
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
          <div className={styles.altActions}>
            <button type="button" disabled={uploading} onClick={() => void handleUpload()}>
              {t("editing.md.uploadInsert")}
            </button>
            <button type="button" disabled={uploading} onClick={() => setPendingFile(null)}>
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
        value={value}
        aria-label={label}
        onChange={(e) => onChange(e.target.value)}
      />

      <small className={styles.counter}>
        {value.length} / {maxLength}
      </small>
    </div>
  );
}
