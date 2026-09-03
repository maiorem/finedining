import { lazy, Suspense, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { ApiError } from "../api/http";
import { submitProposal, type ProposalCategory } from "../api/proposals";
import { useCan } from "../hooks/useCan";
import styles from "./ProposalPage.module.css";

// 관리자 전용 API 경로가 익명 방문자 번들에 섞이면 안 되므로 React.lazy로만 import한다
// (CLAUDE.md §3.5·§9).
const ProposalReviewList = lazy(() => import("../features/editing/ProposalReviewList"));

const PROPOSAL_CATEGORIES: ProposalCategory[] = ["CORPORATE_EVENT", "LOCAL_CULTURE", "CUSTOM_CONSULTING"];

const KNOWN_ERROR_CODES = ["VALIDATION_ERROR", "RATE_LIMITED"] as const;

function errorMessageKey(code: string): string {
  return (KNOWN_ERROR_CODES as readonly string[]).includes(code)
    ? `proposal.error.${code}`
    : "proposal.error.generic";
}

export default function ProposalPage() {
  const { t } = useTranslation();
  const canReview = useCan("proposal:review");
  const [reviewing, setReviewing] = useState(false);
  const [name, setName] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [category, setCategory] = useState<ProposalCategory | "">("");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [privacyConsent, setPrivacyConsent] = useState(false);
  const [website, setWebsite] = useState(""); // 허니팟
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (category === "") return;
    setError(null);
    setSubmitting(true);
    try {
      await submitProposal({ name, contactEmail, category, title, body, privacyConsent, website });
      setSubmitted(true);
    } catch (err) {
      const code = err instanceof ApiError ? err.code : "UNKNOWN";
      setError(t(errorMessageKey(code)));
    } finally {
      setSubmitting(false);
    }
  }

  const reviewToggle = canReview && (
    <button
      type="button"
      className={styles.reviewToggle}
      aria-pressed={reviewing}
      onClick={() => setReviewing((prev) => !prev)}
    >
      {reviewing ? t("editing.exitEditMode") : t("proposal.admin.reviewToggle")}
    </button>
  );

  const reviewPanel = reviewing && (
    <Suspense fallback={<p className={styles.status}>{t("proposal.admin.loading")}</p>}>
      <ProposalReviewList />
    </Suspense>
  );

  if (submitted) {
    return (
      <main className={styles.page}>
        <h1 className={styles.srOnly}>{t("nav.proposal")}</h1>
        {reviewToggle}
        {reviewPanel}
        <div className={styles.success}>
          <p>{t("proposal.success")}</p>
        </div>
      </main>
    );
  }

  return (
    <main className={styles.page}>
      <h1 className={styles.srOnly}>{t("nav.proposal")}</h1>
      {reviewToggle}
      {reviewPanel}
      <p className={styles.tagline}>{t("proposal.tagline")}</p>
      <p className={styles.lead}>{t("proposal.lead")}</p>

      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.field}>
          <label htmlFor="proposal-name">{t("proposal.name")}</label>
          <input
            id="proposal-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className={styles.field}>
          <label htmlFor="proposal-email">{t("proposal.contactEmail")}</label>
          <input
            id="proposal-email"
            type="email"
            value={contactEmail}
            onChange={(e) => setContactEmail(e.target.value)}
            required
          />
        </div>

        <div className={styles.field}>
          <label htmlFor="proposal-category">{t("proposal.category.label")}</label>
          <select
            id="proposal-category"
            value={category}
            onChange={(e) => setCategory(e.target.value as ProposalCategory)}
            required
          >
            <option value="" disabled>
              {t("proposal.category.placeholder")}
            </option>
            {PROPOSAL_CATEGORIES.map((value) => (
              <option key={value} value={value}>
                {t(`proposal.category.${value}`)}
              </option>
            ))}
          </select>
        </div>

        <div className={styles.field}>
          <label htmlFor="proposal-title">{t("proposal.title")}</label>
          <input
            id="proposal-title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
        </div>

        <div className={styles.field}>
          <label htmlFor="proposal-body">{t("proposal.body")}</label>
          <textarea
            id="proposal-body"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            required
          />
        </div>

        {/* 허니팟: 사람에게는 안 보이고, 자동입력 봇만 채운다 */}
        <div className={styles.honeypot} aria-hidden="true">
          <label htmlFor="proposal-website">Website</label>
          <input
            id="proposal-website"
            type="text"
            tabIndex={-1}
            autoComplete="off"
            value={website}
            onChange={(e) => setWebsite(e.target.value)}
          />
        </div>

        <label className={styles.consent}>
          <input
            type="checkbox"
            checked={privacyConsent}
            onChange={(e) => setPrivacyConsent(e.target.checked)}
            required
          />
          <span>
            {t("proposal.consent")}
            <span className={styles.consentDetail}>{t("proposal.consentDetail")}</span>
          </span>
        </label>

        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}

        <button type="submit" className={styles.submit} disabled={submitting}>
          {t("proposal.submit")}
        </button>
      </form>
    </main>
  );
}
