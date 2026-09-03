import { useRef, useState, type ChangeEvent, type DragEvent } from "react";
import { useTranslation } from "react-i18next";
import {
  completeMediaUpload,
  deleteMedia,
  presignUpload,
  updateMediaAltText,
  updateMediaCaption,
  uploadToPresignedUrl,
  type MediaAsset,
  type MediaOwnerType,
} from "../../api/media";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import styles from "./ImageDropzone.module.css";

type PendingUpload = {
  localId: string;
  fileName: string;
  stage: "UPLOADING" | "NEEDS_ALT" | "SAVING" | "FAILED";
  mediaAssetId?: number;
  altText: string;
  caption: string;
  errorMessage?: string;
};

type ImageDropzoneProps = {
  ownerType: MediaOwnerType;
  ownerId: number;
  images: MediaAsset[];
  onChanged: () => void;
  /** 프로필 사진처럼 "딱 1장"이어야 하는 경우 지정한다 — 지정하지 않으면 갤러리처럼 무제한이다. */
  maxImages?: number;
};

/**
 * 드래그&드롭 → presign → S3 직접 업로드 → 완료(alt 필수) (CLAUDE.md §3.9·§7.5·§8.8).
 * 대체 텍스트(altText, 접근성용 짧은 문구)와 설명 문단(caption, 방문자에게 보이는 본문)은
 * 별개 필드다 — 상세 페이지가 블로그형 레이아웃으로 바뀌면서(2026-09-04) 분리했다.
 */
export function ImageDropzone({ ownerType, ownerId, images, onChanged, maxImages }: ImageDropzoneProps) {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const inputRef = useRef<HTMLInputElement>(null);
  const [pending, setPending] = useState<PendingUpload[]>([]);
  const [dragOver, setDragOver] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editAltText, setEditAltText] = useState("");
  const [editCaption, setEditCaption] = useState("");
  const [savingEditId, setSavingEditId] = useState<number | null>(null);
  const [editError, setEditError] = useState<string | null>(null);

  const atLimit = maxImages !== undefined && images.length + pending.length >= maxImages;

  function updatePending(localId: string, patch: Partial<PendingUpload>) {
    setPending((prev) => prev.map((item) => (item.localId === localId ? { ...item, ...patch } : item)));
  }

  async function startUpload(file: File) {
    if (!session) return;
    const localId = `${file.name}-${Date.now()}-${Math.random()}`;
    setPending((prev) => [
      ...prev,
      { localId, fileName: file.name, stage: "UPLOADING", altText: "", caption: "" },
    ]);

    try {
      const { mediaAssetId, uploadUrl } = await presignUpload(session.accessToken, ownerType, ownerId, file);
      await uploadToPresignedUrl(uploadUrl, file);
      updatePending(localId, { stage: "NEEDS_ALT", mediaAssetId });
    } catch {
      updatePending(localId, { stage: "FAILED", errorMessage: t("editing.image.uploadFailed") });
    }
  }

  async function completeItem(localId: string) {
    const item = pending.find((p) => p.localId === localId);
    if (!item || !session || item.mediaAssetId === undefined) return;
    updatePending(localId, { stage: "SAVING" });
    try {
      await completeMediaUpload(session.accessToken, item.mediaAssetId, item.altText);
      if (item.caption.trim().length > 0) {
        await updateMediaCaption(session.accessToken, item.mediaAssetId, item.caption.trim());
      }
      setPending((prev) => prev.filter((p) => p.localId !== localId));
      onChanged();
    } catch {
      updatePending(localId, { stage: "NEEDS_ALT", errorMessage: t("editing.image.saveFailed") });
    }
  }

  function handleFiles(files: FileList | null) {
    if (!files || atLimit) return;
    const remaining = maxImages !== undefined ? maxImages - images.length - pending.length : files.length;
    Array.from(files)
      .slice(0, Math.max(remaining, 0))
      .forEach((file) => void startUpload(file));
  }

  function handleInputChange(e: ChangeEvent<HTMLInputElement>) {
    handleFiles(e.target.files);
    e.target.value = "";
  }

  function handleDrop(e: DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setDragOver(false);
    handleFiles(e.dataTransfer.files);
  }

  async function handleDelete(mediaAssetId: number) {
    if (!session) return;
    await deleteMedia(session.accessToken, mediaAssetId);
    onChanged();
  }

  function startEdit(image: MediaAsset) {
    setEditingId(image.id);
    setEditAltText(image.altText ?? "");
    setEditCaption(image.caption ?? "");
    setEditError(null);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditError(null);
  }

  async function saveEdit(mediaAssetId: number) {
    if (!session || editAltText.trim().length === 0) return;
    setSavingEditId(mediaAssetId);
    try {
      await Promise.all([
        updateMediaAltText(session.accessToken, mediaAssetId, editAltText.trim()),
        updateMediaCaption(session.accessToken, mediaAssetId, editCaption.trim()),
      ]);
      setEditingId(null);
      onChanged();
    } catch {
      setEditError(t("editing.image.saveFailed"));
    } finally {
      setSavingEditId(null);
    }
  }

  return (
    <div className={styles.wrapper}>
      {atLimit ? (
        <p className={styles.limitNotice}>{t("editing.image.limitReached")}</p>
      ) : (
        <div
          className={dragOver ? `${styles.dropzone} ${styles.dropzoneActive}` : styles.dropzone}
          onDragOver={(e) => {
            e.preventDefault();
            setDragOver(true);
          }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          onClick={() => inputRef.current?.click()}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") inputRef.current?.click();
          }}
        >
          <p>{t("editing.image.dropHint")}</p>
          <input
            ref={inputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple={maxImages !== 1}
            className={styles.hiddenInput}
            onChange={handleInputChange}
          />
        </div>
      )}

      {pending.map((item) => (
        <div key={item.localId} className={styles.pendingItem}>
          <span className={styles.fileName}>{item.fileName}</span>
          {item.stage === "UPLOADING" && <span>{t("editing.image.uploading")}</span>}
          {item.stage === "SAVING" && <span>{t("editing.image.saving")}</span>}
          {(item.stage === "NEEDS_ALT" || item.stage === "SAVING") && (
            <div className={styles.editFields}>
              <label>
                <span>{t("editing.image.altLabel")}</span>
                <input
                  type="text"
                  value={item.altText}
                  disabled={item.stage === "SAVING"}
                  onChange={(e) => updatePending(item.localId, { altText: e.target.value })}
                />
              </label>
              <label>
                <span>{t("editing.image.captionLabel")}</span>
                <textarea
                  rows={3}
                  value={item.caption}
                  disabled={item.stage === "SAVING"}
                  onChange={(e) => updatePending(item.localId, { caption: e.target.value })}
                />
              </label>
              <div className={styles.editActions}>
                <button
                  type="button"
                  disabled={item.stage === "SAVING" || item.altText.trim().length === 0}
                  onClick={() => void completeItem(item.localId)}
                >
                  {t("editing.image.save")}
                </button>
              </div>
            </div>
          )}
          {item.errorMessage && <p className={styles.error}>{item.errorMessage}</p>}
        </div>
      ))}

      <ul className={styles.imageList}>
        {images.map((image) => (
          <li key={image.id} className={styles.imageItem}>
            {image.url640 ? (
              <img src={image.url640} alt={image.altText ?? ""} width={64} height={64} className={styles.thumb} />
            ) : (
              <span className={styles.thumbPlaceholder}>{image.status}</span>
            )}

            {editingId === image.id ? (
              <div className={styles.editFields}>
                <label>
                  <span>{t("editing.image.altLabel")}</span>
                  <input
                    type="text"
                    value={editAltText}
                    disabled={savingEditId === image.id}
                    onChange={(e) => setEditAltText(e.target.value)}
                  />
                </label>
                <label>
                  <span>{t("editing.image.captionLabel")}</span>
                  <textarea
                    rows={4}
                    value={editCaption}
                    disabled={savingEditId === image.id}
                    onChange={(e) => setEditCaption(e.target.value)}
                  />
                </label>
                {editError && <p className={styles.error}>{editError}</p>}
                <div className={styles.editActions}>
                  <button
                    type="button"
                    disabled={savingEditId === image.id || editAltText.trim().length === 0}
                    onClick={() => void saveEdit(image.id)}
                  >
                    {t("editing.image.save")}
                  </button>
                  <button type="button" disabled={savingEditId === image.id} onClick={cancelEdit}>
                    {t("editing.image.cancel")}
                  </button>
                </div>
              </div>
            ) : (
              <>
                <span className={styles.imageSummary}>
                  <span className={styles.imageAlt}>{image.altText ?? t("editing.image.noAlt")}</span>
                  {image.caption && <span className={styles.imageCaptionPreview}>{image.caption}</span>}
                </span>
                <button type="button" onClick={() => startEdit(image)}>
                  {t("editing.image.edit")}
                </button>
              </>
            )}

            <button type="button" onClick={() => void handleDelete(image.id)}>
              {t("editing.image.delete")}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
