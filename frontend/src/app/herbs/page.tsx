import { HerbResourceClient } from "@/components/herbs/HerbResourceClient";
import { SiteLayout } from "@/components/layout/SiteLayout";

export default function HerbsPage() {
  return (
    <SiteLayout>
      <HerbResourceClient />
    </SiteLayout>
  );
}
