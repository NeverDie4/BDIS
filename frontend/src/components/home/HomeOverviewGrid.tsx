"use client";

import { FeaturedHerbsPanel } from "./FeaturedHerbsPanel";
import { GrowthObservationPanel } from "./GrowthObservationPanel";
import { MapOverviewPanel } from "./MapOverviewPanel";
import { ResearchTeachingPanel } from "./ResearchTeachingPanel";
import styles from "./HomeOverviewGrid.module.css";

export function HomeOverviewGrid() {
  return (
    <section aria-label="首页功能摘要" className={styles.overview}>
      <FeaturedHerbsPanel />
      <MapOverviewPanel />
      <ResearchTeachingPanel />
      <GrowthObservationPanel />
    </section>
  );
}
