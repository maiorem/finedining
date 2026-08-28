import { useEffect, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import { ApiError } from "../../api/http";
import { listProductionsForAdmin } from "../../api/productionAdmin";
import { queryKeys } from "../../api/queryKeys";
import { createShowing, updateShowingDetails, type ShowingDetailsInput } from "../../api/showingAdmin";
import type { SpokenLanguage } from "../../api/showings";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import styles from "./ShowingForm.module.css";

// datetime-local은 타임존 개념이 없다 — 값을 항상 한국 시간(KST) 벽시계로 다룬다(CLAUDE.md §13.4).
function toDatetimeLocalValue(iso: string): string {
  const date = new Date(new Date(iso).getTime() + 9 * 60 * 60 * 1000);
  return date.toISOString().slice(0, 16);
}

function fromDatetimeLocalValue(value: string): string {
  return new Date(`${value}:00+09:00`).toISOString();
}

type ShowingFormProps = {
  mode: "create" | "edit";
  showingId?: number;
  initial?: ShowingDetailsInput;
  onSaved: () => void;
  onCancel: () => void;
};

const EMPTY_DETAILS: ShowingDetailsInput = {
  startsAt: new Date().toISOString(),
  durationMinutes: 120,
  venueName: "",
  venueAddress: "",
  spokenLanguage: "KO",
  interpretationAvailable: false,
};

/**
 * 회차 등록·수정 공용 폼. 관리자 전용 쓰기 경로라 features/editing/에 두고 React.lazy로만
 * import된다(CLAUDE.md §3.5·§9).
 */
export default function ShowingForm({ mode, showingId, initial, onSaved, onCancel }: ShowingFormProps) {
  const { t } = useTranslation();
  const { session } = useAdminAuth();
  const [productionId, setProductionId] = useState<number | null>(null);
  const [startsAtLocal, setStartsAtLocal] = useState(toDatetimeLocalValue((initial ?? EMPTY_DETAILS).startsAt));
  const [durationMinutes, setDurationMinutes] = useState((initial ?? EMPTY_DETAILS).durationMinutes);
  const [venueName, setVenueName] = useState((initial ?? EMPTY_DETAILS).venueName);
  const [venueAddress, setVenueAddress] = useState((initial ?? EMPTY_DETAILS).venueAddress ?? "");
  const [spokenLanguage, setSpokenLanguage] = useState<SpokenLanguage>((initial ?? EMPTY_DETAILS).spokenLanguage);
  const [interpretationAvailable, setInterpretationAvailable] = useState(
    (initial ?? EMPTY_DETAILS).interpretationAvailable,
  );
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const { data: productions } = useQuery({
    queryKey: queryKeys.productions.adminList,
    queryFn: () => listProductionsForAdmin(session!.accessToken),
    enabled: mode === "create" && Boolean(session),
  });

  useEffect(() => {
    if (mode === "create" && productions && productions.length > 0 && productionId === null) {
      setProductionId(productions[0].id);
    }
  }, [mode, productions, productionId]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!session) return;
    setError(null);
    setSubmitting(true);
    const details: ShowingDetailsInput = {
      startsAt: fromDatetimeLocalValue(startsAtLocal),
      durationMinutes,
      venueName,
      venueAddress: venueAddress || null,
      spokenLanguage,
      interpretationAvailable,
    };
    try {
      if (mode === "create") {
        if (productionId === null) {
          setError(t("showings.form.error.noProduction"));
          return;
        }
        await createShowing(session.accessToken, productionId, details);
      } else if (showingId !== undefined) {
        await updateShowingDetails(session.accessToken, showingId, details);
      }
      onSaved();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t("showings.form.error.generic"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      {mode === "create" && (
        <label className={styles.field}>
          <span>{t("showings.form.production")}</span>
          <select
            value={productionId ?? ""}
            onChange={(e) => setProductionId(Number(e.target.value))}
            required
          >
            {productions?.map((production) => (
              <option key={production.id} value={production.id}>
                {production.translations.find((tr) => tr.locale === "KO")?.title ?? production.slug}
              </option>
            ))}
          </select>
        </label>
      )}

      <label className={styles.field}>
        <span>{t("showings.form.startsAt")}</span>
        <input
          type="datetime-local"
          value={startsAtLocal}
          onChange={(e) => setStartsAtLocal(e.target.value)}
          required
        />
      </label>

      <label className={styles.field}>
        <span>{t("showings.form.durationMinutes")}</span>
        <input
          type="number"
          min={1}
          value={durationMinutes}
          onChange={(e) => setDurationMinutes(Number(e.target.value))}
          required
        />
      </label>

      <label className={styles.field}>
        <span>{t("showings.form.venueName")}</span>
        <input type="text" value={venueName} onChange={(e) => setVenueName(e.target.value)} required />
      </label>

      <label className={styles.field}>
        <span>{t("showings.form.venueAddress")}</span>
        <input type="text" value={venueAddress} onChange={(e) => setVenueAddress(e.target.value)} />
      </label>

      <label className={styles.field}>
        <span>{t("showings.form.spokenLanguage")}</span>
        <select value={spokenLanguage} onChange={(e) => setSpokenLanguage(e.target.value as SpokenLanguage)}>
          <option value="KO">{t("booking.language.KO")}</option>
          <option value="EN">{t("booking.language.EN")}</option>
        </select>
      </label>

      <label className={styles.checkboxField}>
        <input
          type="checkbox"
          checked={interpretationAvailable}
          onChange={(e) => setInterpretationAvailable(e.target.checked)}
        />
        <span>{t("booking.interpretationAvailable")}</span>
      </label>

      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}

      <div className={styles.actions}>
        <button type="submit" className={styles.submit} disabled={submitting}>
          {t("showings.form.save")}
        </button>
        <button type="button" className={styles.cancel} onClick={onCancel}>
          {t("showings.form.cancel")}
        </button>
      </div>
    </form>
  );
}
