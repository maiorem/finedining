import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { HeroSlideshow } from "../components/section/HeroSlideshow";
import { useProductions } from "../api/productions";
import { usePrograms } from "../api/programs";
import { ProductionCard } from "../components/section/ProductionCard";
import { ProgramCard } from "../components/section/ProgramCard";
import styles from "./HomePage.module.css";

const FEATURED_COUNT = 2;

export default function HomePage() {
  const { t, i18n } = useTranslation();
  const { data: productions } = useProductions(i18n.language);
  const { data: programs } = usePrograms(i18n.language);

  const featuredProductions = productions?.slice(0, FEATURED_COUNT) ?? [];
  const featuredPrograms = programs?.slice(0, FEATURED_COUNT) ?? [];

  return (
    <main>
      <HeroSlideshow>
        <h1 className={styles.title}>{t("app.name")}</h1>
        <Link to="/productions" className={styles.bookingCta}>
          {t("booking.reserve")}
        </Link>
      </HeroSlideshow>

      <div className={styles.collaborateRow}>
        <Link to="/proposal" className={styles.collaborateCta}>
          {t("home.collaborateCta")}
        </Link>
      </div>

      {featuredProductions.length > 0 && (
        <section className={styles.section}>
          <h2 className={styles.sectionHeading}>{t("home.productionsHeading")}</h2>
          <ul className={styles.grid}>
            {featuredProductions.map((production) => (
              <li key={production.id}>
                <ProductionCard production={production} />
              </li>
            ))}
          </ul>
        </section>
      )}

      {featuredPrograms.length > 0 && (
        <section className={styles.section}>
          <h2 className={styles.sectionHeading}>{t("home.programsHeading")}</h2>
          <ul className={styles.grid}>
            {featuredPrograms.map((program) => (
              <li key={program.id}>
                <ProgramCard program={program} />
              </li>
            ))}
          </ul>
        </section>
      )}
    </main>
  );
}
