"use client";

import dynamic from "next/dynamic";

const HerbDistributionMap = dynamic(
  () => import("@/components/map/HerbDistributionMap").then((module) => module.HerbDistributionMap),
  { ssr: false },
);

export default function MapTestPage() {
  return (
    <main className="map-test-page">
      <div className="map-test-header">
        <div>
          <p className="eyebrow">M08 基地与地图模块</p>
          <h1>中药材分布地图测试界面</h1>
        </div>
      </div>
      <HerbDistributionMap />
    </main>
  );
}
