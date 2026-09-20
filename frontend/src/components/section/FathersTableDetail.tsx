import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import type { ProductionDetail } from "../../api/productions";
import { INSTAGRAM_URL, YOUTUBE_URL } from "../../constants/social";
import { HoverGallery } from "./HoverGallery";
import { TrailerVideo } from "./TrailerVideo";
import { TRAILER_VIDEO_ID } from "./trailer";
import styles from "./FathersTableDetail.module.css";
import banjul from "../../assets/fathers-table/banjul.jpg";
import dj from "../../assets/fathers-table/dj.jpg";
import father1 from "../../assets/fathers-table/father-1.jpg";
import father2 from "../../assets/fathers-table/father-2.jpg";
import father3 from "../../assets/fathers-table/father-3.jpg";
import hero from "../../assets/fathers-table/hero-4.jpg";
import labor1 from "../../assets/fathers-table/labor-1.jpg";
import labor2 from "../../assets/fathers-table/labor-2.jpg";
import labor3 from "../../assets/fathers-table/labor-3.jpg";
import poster from "../../assets/fathers-table/poster.jpg";
import question from "../../assets/fathers-table/question.jpg";
import stage1 from "../../assets/fathers-table/stage-1.jpg";
import stage2 from "../../assets/fathers-table/stage-2.jpg";
import stage3 from "../../assets/fathers-table/stage-3.jpg";
import archive2023 from "../../assets/fathers-table/archive-2023.jpg";
import archive2024 from "../../assets/fathers-table/archive-2024.jpg";
import archive2025 from "../../assets/fathers-table/archive-2025.jpg";

// 요청서(2026-09-14)의 "아버지의 식탁" 전용 상세 구성. 예약·위치 링크만 DB(편집 패널)에서 오고
// 나머지 스토리텔링 카피·사진은 이 작품 전용으로 고정한다(적용 대상은 constants/fathersTable.ts).

type Props = { production: ProductionDetail; editSlot?: ReactNode };

function ExternalLink({ href, label, className }: { href: string; label: string; className: string }) {
  const { t } = useTranslation();
  return (
    <a
      className={className}
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={`${label} (${t("booking.opensNewWindow")})`}
    >
      {label}
    </a>
  );
}

export function FathersTableDetail({ production, editSlot }: Props) {
  const { t } = useTranslation();
  const { bookingUrl, locationUrl } = production;

  const reserve = (label: string, className: string) =>
    bookingUrl ? (
      <ExternalLink href={bookingUrl} label={label} className={className} />
    ) : (
      <span className={`${className} ${styles.disabled}`} aria-disabled="true">
        {t("ft.reserveUnavailable")}
      </span>
    );

  return (
    <>
      <section className={styles.hero}>
        <img className={styles.heroImage} src={hero} alt="" loading="eager" fetchPriority="high" />
        <div className={styles.heroScrim} />
        <div className={styles.heroContent}>
          <p className={styles.eyebrow}>{t("ft.eyebrow")}</p>
          <h1 className={styles.headline}>{t("ft.headline")}</h1>
          <p className={styles.intro}>{t("ft.intro")}</p>
          {reserve(t("booking.reserve"), styles.cta)}
        </div>
      </section>

      {editSlot}

      <section className={`${styles.section} ${styles.infoSection}`}>
        <div className={styles.info}>
          <h2 className={styles.infoHeading}>{t("ft.info.heading")}</h2>
          <dl className={styles.infoList}>
            <dt>{t("ft.info.dateLabel")}</dt>
            <dd>{t("ft.info.dateValue")}</dd>
            <dt>{t("ft.info.durationLabel")}</dt>
            <dd>{t("ft.info.durationValue")}</dd>
            <dt>{t("ft.info.placeLabel")}</dt>
            <dd>{t("ft.info.placeValue")}</dd>
            <dt>{t("ft.info.priceLabel")}</dt>
            <dd>{t("ft.info.priceValue")}</dd>
          </dl>
          <p className={styles.infoTagline}>{t("ft.info.tagline")}</p>
          <div className={styles.actions}>
            {reserve(t("booking.reserve"), styles.cta)}
            {locationUrl && <ExternalLink href={locationUrl} label={t("booking.location")} className={styles.cta} />}
          </div>
        </div>
        <div className={styles.posterColumn}>
          <img className={styles.poster} src={poster} alt={t("ft.posterAlt")} loading="lazy" decoding="async" />
          <p className={styles.social}>
            <ExternalLink href={INSTAGRAM_URL} label={t("ft.instagram")} className={styles.socialLink} />
            <ExternalLink href={YOUTUBE_URL} label={t("ft.youtube")} className={styles.socialLink} />
          </p>
        </div>
      </section>

      {TRAILER_VIDEO_ID && (
        <section className={styles.section}>
          <TrailerVideo videoId={TRAILER_VIDEO_ID} title={t("ft.trailerHeading")} />
        </section>
      )}

      <section className={`${styles.section} ${styles.split} ${styles.reverse}`}>
        <div className={styles.text}>
          <h2 className={styles.heading}>{t("ft.father.heading")}</h2>
          <p className={styles.body}>{t("ft.father.body")}</p>
        </div>
        <HoverGallery
          items={[
            { src: father1, alt: t("ft.father.alt1") },
            { src: father2, alt: t("ft.father.alt2") },
            { src: father3, alt: t("ft.father.alt3") },
          ]}
        />
      </section>

      <section className={`${styles.section} ${styles.split}`}>
        <div className={styles.text}>
          <h2 className={styles.heading}>{t("ft.labor.heading")}</h2>
          <p className={styles.body}>{t("ft.labor.body")}</p>
        </div>
        <img className={styles.photo} src={labor1} alt={t("ft.labor.alt1")} loading="lazy" decoding="async" />
      </section>

      <section className={`${styles.section} ${styles.split}`}>
        <HoverGallery
          items={[
            { src: labor2, alt: t("ft.cutlet.alt1") },
            { src: labor3, alt: t("ft.cutlet.alt2") },
          ]}
        />
        <div className={styles.text}>
          <h2 className={styles.heading}>{t("ft.cutlet.heading")}</h2>
          <p className={styles.body}>{t("ft.cutlet.body")}</p>
        </div>
      </section>

      <section className={`${styles.section} ${styles.split}`}>
        <div className={styles.text}>
          <h2 className={styles.heading}>{t("ft.stage.heading")}</h2>
          <p className={styles.body}>{t("ft.stage.body")}</p>
        </div>
        <HoverGallery
          items={[
            { src: stage1, alt: t("ft.stage.alt1") },
            { src: stage2, alt: t("ft.stage.alt2") },
            { src: stage3, alt: t("ft.stage.alt3") },
          ]}
        />
      </section>

      <section className={`${styles.section} ${styles.split} ${styles.reverse}`}>
        <div className={styles.text}>
          <h2 className={styles.heading}>{t("ft.dj.heading")}</h2>
          <p className={styles.body}>{t("ft.dj.body")}</p>
        </div>
        <img className={styles.photo} src={dj} alt={t("ft.dj.alt")} loading="lazy" decoding="async" />
      </section>

      <section className={`${styles.section} ${styles.split}`}>
        <div className={styles.text}>
          <h2 className={styles.heading}>{t("ft.question.heading")}</h2>
          <p className={styles.body}>{t("ft.question.body")}</p>
        </div>
        <img className={`${styles.photo} ${styles.tall}`} src={question} alt={t("ft.question.alt")} loading="lazy" decoding="async" />
      </section>

      <section className={`${styles.section} ${styles.split} ${styles.reverse}`}>
        <div className={styles.text}>
          <h2 className={styles.heading}>{t("ft.banjul.heading")}</h2>
          <p className={styles.body}>{t("ft.banjul.body")}</p>
        </div>
        <img className={styles.smallPhoto} src={banjul} alt={t("ft.banjul.alt")} width={309} height={288} loading="lazy" decoding="async" />
      </section>

      <section className={`${styles.section} ${styles.finalCta}`}>{reserve(t("ft.finalCta"), styles.cta)}</section>

      <section className={styles.section}>
        <h2 className={styles.archiveHeading}>{t("ft.archive.heading")}</h2>
        <ul className={styles.archive}>
          {[
            [archive2023, "2023"],
            [archive2024, "2024"],
            [archive2025, "2025"],
          ].map(([src, year]) => (
            <li key={year}>
              <img src={src} alt={t(`ft.archive.alt${year}`)} loading="lazy" decoding="async" />
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
