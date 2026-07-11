"use client";

import { App } from "antd";
import { useEffect, useState } from "react";
import { fetchCoursePage, type CourseApi } from "@/lib/courses";
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
  const [courses, setCourses] = useState<CourseApi[]>([]);

  useEffect(() => {
    Promise.all([
      fetchDashboardSummary(),
      fetchDashboardMap(),
      fetchDashboardRecentGrowth(),
      fetchEnabledHerbs(),
      fetchCoursePage(),
    ])
      .then(([nextSummary, nextMap, growth, species, coursePage]) => {
        setSummary(nextSummary);
        setMap(nextMap);
        setRecentGrowth(growth);
        setHerbs(species.slice(0, 4));
        setCourses(coursePage.records.slice(0, 4));
      })
      .catch((error) => {
        if (!isAuthRedirectError(error)) message.error(getApiErrorMessage(error, "首页数据加载失败"));
      });
  }, [message]);

  return (
    <section aria-label="首页功能摘要" className={styles.overview}>
      <FeaturedHerbsPanel herbs={herbs} />
      <MapOverviewPanel map={map} summary={summary} />
      <ResearchTeachingPanel courses={courses} />
      <GrowthObservationPanel records={recentGrowth} summary={summary} />
    </section>
  );
}
