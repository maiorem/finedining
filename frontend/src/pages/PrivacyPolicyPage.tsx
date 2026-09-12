import koContent from "../content/legal/privacy-policy.ko.txt?raw";
import enContent from "../content/legal/privacy-policy.en.txt?raw";
import { LegalDocumentPage } from "./LegalDocumentPage";

export default function PrivacyPolicyPage() {
  return <LegalDocumentPage headingKey="privacy.heading" koContent={koContent} enContent={enContent} />;
}
