"use client";

import { Button, Card, Statistic } from "antd";
import Link from "next/link";
import type { DashboardMap, DashboardSummary } from "@/lib/dashboard";
import { AbstractChongqingMap } from "./AbstractChongqingMap";
import styles from "./HomeOverviewGrid.module.css";

export function MapOverviewPanel({ map, summary }: { map: DashboardMap | null; summary: DashboardSummary | null }) {
  return (
    <Card className={`${styles.overviewCard} ${styles.paperPanel}`} extra={<Button className={styles.moreButton} type="link"><Link href="/map">进入地图 →</Link></Button>} title="药材分布概览" variant="borderless">
      <div className={styles.mapSummary}>
        <Statistic suffix="种" title="药材资源" value={summary?.herbCount ?? 0} />
        <Statistic suffix="处" title="分布点位" value={map?.pointCount ?? summary?.mapPointCount ?? 0} />
        <Statistic suffix="个" title="教学基地" value={summary?.baseCount ?? 0} />
      </div>
      <AbstractChongqingMap />
    </Card>
  );
}
