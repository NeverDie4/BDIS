"use client";

import { App } from "antd";
import { useEffect, useState } from "react";
import {
  fetchDashboardMap,
  fetchDashboardRecentGrowth,
  fetchDashboardSummary,
  type DashboardMap,
  type DashboardRecentGrowth,
  type DashboardSummary,
} from "@/lib/dashboard";
import { fetchEnabledHerbs, type HerbSpeciesApi } from "@/lib/herbs";
import { getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import { FeaturedHerbsPanel } from "./FeaturedHerbsPanel";
import { GrowthObservationPanel } from "./GrowthObservationPanel";
import { MapOverviewPanel } from "./MapOverviewPanel";
import { ResearchTeachingPanel } from "./ResearchTeachingPanel";
import styles from "./HomeOverviewGrid.module.css";

export function HomeOverviewGrid() {
  const { message } = App.useApp();
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [map, setMap] = useState<DashboardMap | null>(null);
  const [recentGrowth, setRecentGrowth] = useState<DashboardRecentGrowth[]>([]);
  const [herbs, setHerbs] = useState<HerbSpeciesApi[]>([]);

  useEffect(() => {
    Promise.all([
      fetchDashboardSummary(),
      fetchDashboardMap(),
      fetchDashboardRecentGrowth(),
      fetchEnabledHerbs(),
    ])
      .then(([nextSummary, nextMap, growth, species]) => {
        setSummary(nextSummary);
        setMap(nextMap);
        setRecentGrowth(growth);
        setHerbs(species.slice(0, 4));
      })
      .catch((error) => {
        if (!isAuthRedirectError(error)) message.error(getApiErrorMessage(error, "首页数据加载失败"));
      });
  }, [message]);

  return (
    <section aria-label="首页功能摘要" className={styles.overview}>
      <FeaturedHerbsPanel herbs={herbs} />
      <MapOverviewPanel map={map} summary={summary} />
      <ResearchTeachingPanel />
      <GrowthObservationPanel records={recentGrowth} summary={summary} />
    </section>
  );
}
