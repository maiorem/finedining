import { useTranslation } from "react-i18next";
import { useProductions } from "../api/productions";
import { ProductionCard } from "../components/section/ProductionCard";
import styles from "./ProductionsPage.module.css";

export default function ProductionsPage() {
  const { t, i18n } = useTranslation();
  const { data: productions, isLoading } = useProductions(i18n.language);

  return (
    <main className={styles.page}>
      <h1 className={styles.heading}>{t("nav.productions")}</h1>

      {isLoading && <p className={styles.status}>{t("productions.loading")}</p>}
      {!isLoading && productions?.length === 0 && <p className={styles.status}>{t("productions.empty")}</p>}

      <ul className={styles.grid}>
        {productions?.map((production) => (
          <li key={production.id}>
            <ProductionCard production={production} />
          </li>
        ))}
      </ul>
    </main>
  );
}
