import { useTranslation } from "react-i18next";
import { HeroSlideshow } from "../components/section/HeroSlideshow";

export default function HomePage() {
  const { t } = useTranslation();

  return (
    <main>
      <HeroSlideshow>
        <h1 style={{ fontFamily: "var(--f-display)", fontSize: "var(--t-hero)", margin: 0 }}>
          {t("app.name")}
        </h1>
      </HeroSlideshow>
    </main>
  );
}
