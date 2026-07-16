import { apiGet } from "@/lib/request";

export type DashboardSummary = {
  herbCount: number;
  baseCount: number;
  mapPointCount: number;
  growthRecordCount: number;
  pendingGrowthReviewCount: number;
  courseCount: number;
  fileCount: number;
  soapFailedCount: number;
};

export type DashboardRecentGrowth = {
  recordId: number;
  herbName?: string;
  baseName?: string;
  collectorName?: string;
  reviewStatus?: string;
  collectedAt?: string;
};

export type DashboardMap = {
  pointCount: number;
  districtStatistics: Array<Record<string, unknown>>;
  points: Array<Record<string, unknown>>;
};

export function fetchDashboardSummary() {
  return apiGet<DashboardSummary>("/dashboard/summary");
}

export function fetchDashboardRecentGrowth() {
  return apiGet<DashboardRecentGrowth[]>("/dashboard/recent-growth-records", { limit: 5 });
}

export function fetchDashboardMap() {
  return apiGet<DashboardMap>("/dashboard/map-overview");
}
