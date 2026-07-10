"use client";

import { Button, Card, Statistic } from "antd";
import Link from "next/link";
import { mapOverview } from "@/mocks/home";
import { AbstractChongqingMap } from "./AbstractChongqingMap";
import styles from "./HomeOverviewGrid.module.css";

export function MapOverviewPanel() {
  return (
    <Card
      className={`${styles.overviewCard} ${styles.paperPanel}`}
      extra={
        <Button className={styles.moreButton} type="link">
          <Link href="/map">进入地图 →</Link>
        </Button>
      }
      title="药材分布概览"
      variant="borderless"
    >
      <div className={styles.mapSummary}>
        <Statistic suffix="种" title="药材资源" value={mapOverview.totalSpecies} />
        <Statistic suffix="处" title="分布点位" value={mapOverview.totalPoints} />
        <Statistic suffix="个" title="教学基地" value={mapOverview.totalBases} />
      </div>

      <AbstractChongqingMap />
    </Card>
  );
}
