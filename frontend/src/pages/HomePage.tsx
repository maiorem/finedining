import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { HeroSlideshow } from "../components/section/HeroSlideshow";
import styles from "./HomePage.module.css";

export default function HomePage() {
  const { t } = useTranslation();

  return (
    <main>
      <HeroSlideshow>
        <h1 className={styles.title}>{t("app.name")}</h1>
        <Link to="/productions" className={styles.bookingCta}>
          {t("booking.reserve")}
        </Link>
      </HeroSlideshow>
    </main>
  );
}
