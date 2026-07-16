import { SiteLayout } from "@/components/layout/SiteLayout";
import { Suspense } from "react";
import { ResearchAgentContent } from "./ResearchAgentContent";

export default function ResearchAgentPage() {
  return (
    <SiteLayout contentMode="fluid">
      <Suspense fallback={null}>
        <ResearchAgentContent />
      </Suspense>
    </SiteLayout>
  );
}
