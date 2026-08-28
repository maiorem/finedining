import { lazy, Suspense, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useProductions } from "../api/productions";
import { useShowings, type SpokenLanguage } from "../api/showings";
import { ShowingCard } from "../components/section/ShowingCard";
import { useCan } from "../hooks/useCan";
import styles from "./BookingPage.module.css";

// 관리자 전용 API 경로가 익명 방문자 번들에 섞이면 안 되므로 React.lazy로만 import한다
// (CLAUDE.md §3.5·§9).
const ShowingManagementPanel = lazy(() => import("../features/editing/ShowingManagementPanel"));

function pad(value: number) {
  return String(value).padStart(2, "0");
}

// Date/toISOString은 로컬 타임존에 따라 하루가 밀릴 수 있어 연/월 숫자로 직접 계산한다.
function monthRange(year: number, month: number) {
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  return {
    from: `${year}-${pad(month + 1)}-01`,
    to: `${year}-${pad(month + 1)}-${pad(daysInMonth)}`,
  };
}

type LanguageFilter = "ALL" | SpokenLanguage;

export default function BookingPage() {
  const { t, i18n } = useTranslation();
  const canEdit = useCan("showing:edit");
  const [managing, setManaging] = useState(false);
  const now = useMemo(() => new Date(), []);
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth());
  const [languageFilter, setLanguageFilter] = useState<LanguageFilter>("ALL");
  const [productionSlug, setProductionSlug] = useState<string | undefined>(undefined);

  const { from, to } = useMemo(() => monthRange(year, month), [year, month]);
  const { data: productions } = useProductions(i18n.language);
  const { data: showings, isLoading } = useShowings({
    productionSlug,
    from,
    to,
    lang: i18n.language,
  });

  const filtered = (showings ?? []).filter(
    (showing) => languageFilter === "ALL" || showing.spokenLanguage === languageFilter,
  );

  const monthLabel = new Intl.DateTimeFormat(i18n.language.startsWith("en") ? "en-US" : "ko-KR", {
    year: "numeric",
    month: "long",
  }).format(new Date(year, month, 1));

  function shiftMonth(delta: number) {
    const next = new Date(year, month + delta, 1);
    setYear(next.getFullYear());
    setMonth(next.getMonth());
  }

  return (
    <main className={styles.page}>
      <h1 className={styles.heading}>{t("nav.booking")}</h1>

      {canEdit && (
        <button
          type="button"
          className={styles.manageToggle}
          aria-pressed={managing}
          onClick={() => setManaging((prev) => !prev)}
        >
          {managing ? t("editing.exitEditMode") : t("showings.manageToggle")}
        </button>
      )}

      {managing && (
        <Suspense fallback={<p className={styles.status}>{t("showings.loading")}</p>}>
          <ShowingManagementPanel />
        </Suspense>
      )}

      <div className={styles.toolbar}>
        <div className={styles.monthNav}>
          <button type="button" onClick={() => shiftMonth(-1)} aria-label={t("booking.prevMonth")}>
            ‹
          </button>
          <span className={styles.monthLabel}>{monthLabel}</span>
          <button type="button" onClick={() => shiftMonth(1)} aria-label={t("booking.nextMonth")}>
            ›
          </button>
        </div>

        <div className={styles.filters}>
          {productions && productions.length > 1 && (
            <select
              value={productionSlug ?? ""}
              onChange={(e) => setProductionSlug(e.target.value || undefined)}
              aria-label={t("booking.filterProduction")}
            >
              <option value="">{t("booking.allProductions")}</option>
              {productions.map((production) => (
                <option key={production.slug} value={production.slug}>
                  {production.title}
                </option>
              ))}
            </select>
          )}

          <div className={styles.langFilterGroup}>
            <span className={styles.langFilterLabel}>{t("booking.filterLanguage")}</span>
            <div className={styles.langFilter} role="group" aria-label={t("booking.filterLanguage")}>
              {(["ALL", "KO", "EN"] as const).map((value) => (
                <button
                  key={value}
                  type="button"
                  aria-pressed={languageFilter === value}
                  onClick={() => setLanguageFilter(value)}
                >
                  {t(`booking.languageFilter.${value}`)}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {isLoading && <p className={styles.status}>{t("booking.loading")}</p>}

      {!isLoading && filtered.length === 0 && <p className={styles.status}>{t("booking.empty")}</p>}

      <ul className={styles.list}>
        {filtered.map((showing) => (
          <li key={showing.id}>
            <ShowingCard showing={showing} />
          </li>
        ))}
      </ul>
    </main>
  );
}
