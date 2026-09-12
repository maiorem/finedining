import { useTranslation } from "react-i18next";
import styles from "./LegalDocumentPage.module.css";

type LegalDocumentPageProps = {
  headingKey: string;
  koContent: string;
  enContent: string;
};

/**
 * 개인정보처리방침·이용약관처럼 거의 안 바뀌는 정적 법률 문서가 공유하는 골격이다 —
 * About 인트로 문단과 같은 이유로 편집 모드/DB 없이 텍스트 파일을 그대로 렌더한다(§17).
 * 원문은 src/content/legal/*.txt에 있고, 그 파일만 바꾸면 코드 수정 없이 반영된다.
 */
export function LegalDocumentPage({ headingKey, koContent, enContent }: LegalDocumentPageProps) {
  const { t, i18n } = useTranslation();
  const content = i18n.language.startsWith("en") ? enContent : koContent;

  return (
    <main className={styles.page}>
      <h1 className={styles.heading}>{t(headingKey)}</h1>
      <p className={styles.body}>{content}</p>
    </main>
  );
}
