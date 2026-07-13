import { EvaluationPageClient } from "@/components/evaluation/EvaluationPageClient";
import { SiteLayout } from "@/components/layout/SiteLayout";

export default function EvaluationPage() {
  return (
    <SiteLayout contentMode="fluid">
      <EvaluationPageClient />
    </SiteLayout>
  );
}
