"use client";

import dynamic from "next/dynamic";
import { PageBanner } from "@/components/layout/PageBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import styles from "@/styles/mockPages.module.css";

const HerbDistributionMap = dynamic(
  () => import("@/components/map/HerbDistributionMap").then((module) => module.HerbDistributionMap),
  { ssr: false },
);

export default function MapPage() {
  return (
    <SiteLayout>
      <div className={styles.pageStack}>
        <PageBanner
          sealText="CHONGQING DISTRIBUTION"
          title="重庆中药材分布地图"
          subtitle="查询和维护药材分布点位、地理位置与历次采集记录。"
        />
        <HerbDistributionMap />
      </div>
    </SiteLayout>
  );
}
