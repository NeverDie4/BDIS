import { TeachingPageClient } from "@/components/teaching/TeachingPageClient";
import { SiteLayout } from "@/components/layout/SiteLayout";

export default function TeachingPage() {
  return (
    <SiteLayout contentMode="fluid">
      <TeachingPageClient />
    </SiteLayout>
  );
}
