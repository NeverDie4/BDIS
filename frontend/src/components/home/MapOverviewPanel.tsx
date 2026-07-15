"use client";

import { Button, Card, Statistic } from "antd";
import Link from "next/link";
import type { DashboardMap, DashboardSummary } from "@/lib/dashboard";
import { AbstractChongqingMap } from "./AbstractChongqingMap";
import { abstractChongqingRegions, type AbstractRegion } from "./abstractChongqingMapData";
import styles from "./HomeOverviewGrid.module.css";

function readString(value: unknown) {
  return typeof value === "string" ? value.trim() : "";
}

function readNumber(value: unknown) {
  return typeof value === "number" && Number.isFinite(value) ? value : 0;
}

function readFiniteNumber(value: unknown) {
  if (typeof value === "number") return Number.isFinite(value) ? value : null;
  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function getDistrictName(record: Record<string, unknown>) {
  return (
    readString(record.districtName) ||
    readString(record.district_name) ||
    readString(record.district)
  );
}

function getPointCount(record: Record<string, unknown>) {
  return readNumber(record.pointCount) || readNumber(record.point_count) || readNumber(record.count);
}

function getLongitude(record: Record<string, unknown>) {
  return readFiniteNumber(record.longitude);
}

function getLatitude(record: Record<string, unknown>) {
  return readFiniteNumber(record.latitude);
}

function normalizeDistrictName(value: string) {
  return value
    .replace(/^重庆市/, "")
    .replace(/^市辖区/, "")
    .replace(/[区县]$/, "")
    .trim();
}

function regionKeys(region: AbstractRegion) {
  return [
    normalizeDistrictName(region.name),
    normalizeDistrictName(region.shortName),
  ];
}

function isKnownRegionName(value: string) {
  return abstractChongqingRegions.some((region) => regionKeys(region).includes(value));
}

function inferCentralChongqingRegion(longitude: number | null, latitude: number | null) {
  if (longitude === null || latitude === null) return "";
  if (longitude < 105.2 || longitude > 110.2 || latitude < 28.0 || latitude > 32.3) return "";
  if (longitude >= 106.48 && longitude <= 106.62 && latitude >= 29.5 && latitude <= 29.62) {
    return "渝中";
  }
  if (longitude >= 106.44 && longitude <= 106.72 && latitude >= 29.48 && latitude <= 29.72) {
    return "江北";
  }
  if (longitude >= 106.38 && longitude <= 106.58 && latitude >= 29.42 && latitude <= 29.58) {
    return "九龙坡";
  }
  if (longitude >= 106.5 && longitude <= 106.76 && latitude >= 29.36 && latitude <= 29.56) {
    return "南岸";
  }
  if (longitude >= 106.28 && longitude <= 106.58 && latitude >= 29.52 && latitude <= 29.78) {
    return "沙坪坝";
  }
  return "";
}

function getRegionNameFromPoint(point: Record<string, unknown>) {
  const districtName = normalizeDistrictName(getDistrictName(point));
  if (districtName && isKnownRegionName(districtName)) return districtName;
  return inferCentralChongqingRegion(getLongitude(point), getLatitude(point));
}

function buildRealRegions(map: DashboardMap | null): AbstractRegion[] {
  const counts = new Map<string, number>();

  for (const stat of map?.districtStatistics ?? []) {
    const districtName = getDistrictName(stat);
    const count = getPointCount(stat);
    const normalizedName = normalizeDistrictName(districtName);
    if (normalizedName && count > 0 && isKnownRegionName(normalizedName)) {
      counts.set(normalizedName, (counts.get(normalizedName) ?? 0) + count);
    }
  }

  if (counts.size === 0) {
    for (const point of map?.points ?? []) {
      const regionName = getRegionNameFromPoint(point);
      if (regionName) counts.set(regionName, (counts.get(regionName) ?? 0) + 1);
    }
  }

  return abstractChongqingRegions
    .map((region) => ({
      ...region,
      count:
        counts.get(normalizeDistrictName(region.name)) ??
        counts.get(normalizeDistrictName(region.shortName)) ??
        0,
    }))
    .filter((region) => region.count > 0);
}

export function MapOverviewPanel({ map, summary }: { map: DashboardMap | null; summary: DashboardSummary | null }) {
  const realRegions = buildRealRegions(map);

  return (
    <Card className={`${styles.overviewCard} ${styles.paperPanel}`} extra={<Button className={styles.moreButton} type="link"><Link href="/map">进入地图 →</Link></Button>} title="药材分布概览" variant="borderless">
      <div className={styles.mapSummary}>
        <Statistic suffix="种" title="药材资源" value={summary?.herbCount ?? 0} />
        <Statistic suffix="处" title="分布点位" value={map?.pointCount ?? summary?.mapPointCount ?? 0} />
        <Statistic suffix="个" title="教学基地" value={summary?.baseCount ?? 0} />
      </div>
      <AbstractChongqingMap regions={realRegions} />
    </Card>
  );
}
