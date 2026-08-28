import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ApiError } from "../../api/http";
import { createProduction } from "../../api/productionAdmin";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import styles from "./CreateProductionForm.module.css";

const KNOWN_ERROR_CODES = ["DUPLICATE_SLUG", "VALIDATION_ERROR"] as const;

function errorMessageKey(code: string): string {
  return (KNOWN_ERROR_CODES as readonly string[]).includes(code)
    ? `productions.create.error.${code}`
    : "productions.create.error.generic";
}

type CreateProductionFormProps = {
  onCreated: () => void;
};

/**
 * 목록의 "새 작품 추가". 슬러그만 받아 DRAFT로 만들고 곧장 그 작품 상세(편집 패널)로 이동한다 —
 * 관리자 전용 API 경로라 React.lazy로만 import된다(CLAUDE.md §3.5·§9).
 */
export default function CreateProductionForm({ onCreated }: CreateProductionFormProps) {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const navigate = useNavigate();
  const [slug, setSlug] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!session) return;
    setError(null);
    setSubmitting(true);
    try {
      const created = await createProduction(session.accessToken, slug);
      onCreated();
      navigate(`/productions/${created.slug}`);
    } catch (err) {
      setError(err instanceof ApiError ? t(errorMessageKey(err.code)) : t("productions.create.error.generic"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <label className={styles.field}>
        <span>{t("productions.create.slugLabel")}</span>
        <input
          type="text"
          value={slug}
          onChange={(e) => setSlug(e.target.value)}
          placeholder="new-show"
          pattern="[a-z0-9-]+"
          title={t("productions.create.slugHint")}
          required
        />
      </label>
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
      <button type="submit" className={styles.submit} disabled={submitting}>
        {t("productions.create.submit")}
      </button>
    </form>
  );
}
