import { PerformancePageClient } from "@/components/performance/PerformancePageClient";
import { SiteLayout } from "@/components/layout/SiteLayout";

export default function PerformancePage() {
  return (
    <SiteLayout contentMode="fluid">
      <PerformancePageClient />
    </SiteLayout>
  );
}
