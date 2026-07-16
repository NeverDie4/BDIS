import { Suspense } from "react";
import { PerformancePageClient } from "@/components/performance/PerformancePageClient";
import { SiteLayout } from "@/components/layout/SiteLayout";

export default function PerformancePage() {
  return (
    <SiteLayout contentMode="fluid">
      <Suspense fallback={null}>
        <PerformancePageClient />
      </Suspense>
    </SiteLayout>
  );
}
