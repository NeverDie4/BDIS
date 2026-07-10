<<<<<<< Updated upstream
import { redirect } from "next/navigation";

export default function HomePage() {
  redirect("/login");
=======
"use client";

import { HerbEssenceStrip } from "@/components/home/HerbEssenceStrip";
import { HeroSearchSection } from "@/components/home/HeroSearchSection";
import { HomeOverviewGrid } from "@/components/home/HomeOverviewGrid";
import { SiteLayout } from "@/components/layout/SiteLayout";

export default function HomePage() {
  return (
    <SiteLayout>
      <HeroSearchSection />
      <HerbEssenceStrip />
      <HomeOverviewGrid />
    </SiteLayout>
  );
>>>>>>> Stashed changes
}
