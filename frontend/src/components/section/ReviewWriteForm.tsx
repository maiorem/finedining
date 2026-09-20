import { lazy, Suspense, useState, type ChangeEvent, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { ApiError } from "../../api/http";
import { createReview, updateOwnReview, uploadReviewImage } from "../../api/reviewMember";
import { useMemberAuth } from "../../contexts/MemberAuthContext";
import { removeNewImage, resolveNewImageTokens } from "../../utils/reviewImages";
import styles from "./ReviewWriteForm.module.css";

// 서식 편집기는 글을 쓸 때만 필요하다 — 초기 번들에 넣지 않는다.
const ReviewMarkdownEditor = lazy(() => import("./ReviewMarkdownEditor"));

const MAX_IMAGES = 3;
const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const KNOWN_ERROR_CODES = ["VALIDATION_ERROR", "RATE_LIMITED"] as const;

function errorMessageKey(code: string): string {
  return (KNOWN_ERROR_CODES as readonly string[]).includes(code) ? `reviews.error.${code}` : "reviews.error.generic";
}

type Props = {
  /** 글이 등록된 뒤 호출된다. 사진 일부가 실패했으면 그 안내 문구를 함께 넘긴다. */
  onCreated: (notice: string | null) => void;
};

export function ReviewWriteForm({ onCreated }: Props) {
  const { t } = useTranslation();
  const { session } = useMemberAuth();
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [authorName, setAuthorName] = useState("");
  const [contact, setContact] = useState("");
  const [consent, setConsent] = useState(false);
  const [files, setFiles] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function handleFiles(e: ChangeEvent<HTMLInputElement>) {
    const picked = Array.from(e.target.files ?? []);
    e.target.value = ""; // 같은 파일을 다시 고를 수 있게 비운다.
    if (picked.some((file) => !IMAGE_TYPES.includes(file.type))) {
      setError(t("reviews.error.IMAGE_TYPE"));
      return;
    }
    if (picked.some((file) => file.size > MAX_IMAGE_BYTES)) {
      setError(t("reviews.error.IMAGE_TOO_LARGE"));
      return;
    }
    if (files.length + picked.length > MAX_IMAGES) {
      setError(t("reviews.error.IMAGE_COUNT"));
      return;
    }
    setError(null);
    setFiles((prev) => [...prev, ...picked]);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!session) return;
    setError(null);
    setSubmitting(true);
    try {
      const review = await createReview(session.accessToken, {
        title,
        body,
        authorName,
        contact,
        privacyConsent: consent,
      });
      // 글은 이미 만들어졌다 — 사진이 실패해도 글을 되돌리지 않고 몇 장이 실패했는지만 알린다.
      let failed = 0;
      const mediaIds: (number | null)[] = [];
      for (const file of files) {
        try {
          mediaIds.push((await uploadReviewImage(session.accessToken, review.id, file)).id);
        } catch {
          mediaIds.push(null);
          failed += 1;
        }
      }
      // 본문에 넣어 둔 임시 표시(image:new1…)를 올라간 사진의 실제 번호로 바꿔 다시 저장한다.
      if (body.includes("image:new")) {
        try {
          await updateOwnReview(session.accessToken, review.id, title, resolveNewImageTokens(body, mediaIds));
        } catch {
          failed += 1;
        }
      }
      onCreated(failed > 0 ? t("reviews.notice.imageFailed", { count: failed }) : null);
    } catch (err) {
      const code = err instanceof ApiError ? err.code : "UNKNOWN";
      setError(t(errorMessageKey(code)));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.field}>
        <label htmlFor="review-title">{t("editing.panel.titleLabel")}</label>
        <input id="review-title" type="text" maxLength={200} value={title} onChange={(e) => setTitle(e.target.value)} required />
      </div>
      <Suspense fallback={<p className={styles.hint}>{t("reviews.loading")}</p>}>
        <ReviewMarkdownEditor
          id="review-body"
          label={t("reviews.bodyLabel")}
          value={body}
          onChange={setBody}
          required
          images={{
            kind: "deferred",
            fileCount: files.length,
            onAttach: (file) => setFiles((prev) => [...prev, file]),
          }}
        />
      </Suspense>

      <div className={styles.field}>
        <label htmlFor="review-name">{t("reviews.nameLabel")}</label>
        <input id="review-name" type="text" maxLength={50} value={authorName} onChange={(e) => setAuthorName(e.target.value)} required />
        <p className={styles.hint}>{t("reviews.nameHint")}</p>
      </div>
      <div className={styles.field}>
        <label htmlFor="review-contact">{t("reviews.contactLabel")}</label>
        <input id="review-contact" type="text" maxLength={100} value={contact} onChange={(e) => setContact(e.target.value)} />
        <p className={styles.hint}>{t("reviews.contactHint")}</p>
      </div>

      <div className={styles.field}>
        <label htmlFor="review-images">{t("reviews.imagesLabel")}</label>
        <input
          id="review-images"
          type="file"
          accept={IMAGE_TYPES.join(",")}
          multiple
          disabled={files.length >= MAX_IMAGES}
          onChange={handleFiles}
        />
        {files.length > 0 && (
          <ul className={styles.fileList}>
            {files.map((file, index) => (
              <li key={`${file.name}-${index}`}>
                <span>{file.name}</span>
                <button
                  type="button"
                  onClick={() => {
                    setFiles((prev) => prev.filter((_, i) => i !== index));
                    setBody((prev) => removeNewImage(prev, index + 1)); // 본문에 넣어 둔 그 사진의 자리도 지운다
                  }}
                >
                  {t("reviews.imagesRemove")}
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className={styles.consent}>
        <label>
          <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} required />
          <span>{t("reviews.consentPrefix")}</span>
        </label>
        <p className={styles.hint}>
          {t("reviews.consentDetail")} · <Link to="/privacy">{t("reviews.consentPolicyLink")}</Link>
        </p>
      </div>

      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      <button type="submit" className={styles.submit} disabled={submitting || !consent}>
        {t("reviews.submit")}
      </button>
    </form>
  );
}
