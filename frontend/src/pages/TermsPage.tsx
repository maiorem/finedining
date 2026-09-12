import koContent from "../content/legal/terms.ko.txt?raw";
import enContent from "../content/legal/terms.en.txt?raw";
import { LegalDocumentPage } from "./LegalDocumentPage";

export default function TermsPage() {
  return <LegalDocumentPage headingKey="terms.heading" koContent={koContent} enContent={enContent} />;
}
