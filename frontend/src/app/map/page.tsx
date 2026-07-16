"use client";

import dynamic from "next/dynamic";
import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import styles from "@/styles/mockPages.module.css";
import pageStyles from "./page.module.css";

const HerbDistributionMap = dynamic(
  () => import("@/components/map/HerbDistributionMap").then((module) => module.HerbDistributionMap),
  { ssr: false },
);

export default function MapPage() {
  return (
    <SiteLayout>
      <div className={`${styles.pageStack} ${pageStyles.mapPage}`}>
        <div className={pageStyles.bannerWrap}>
          <ModuleHeroBanner
            description="查询和维护药材分布点位、地理位置与历次采集记录。"
            eyebrow="CHONGQING DISTRIBUTION"
            sealText="分布"
            title="重庆中药材分布地图"
          />
        </div>
        <HerbDistributionMap />
      </div>
    </SiteLayout>
  );
}
