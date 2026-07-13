import axios from "axios";
import { apiGet, apiPost, apiPut, request } from "@/lib/request";
import type { ApiResult, PageResult } from "@/types/api";

export type GrowthRecordApi = {
  id: number;
  batchId?: number;
  batchName?: string;
  taskId?: number;
  taskName?: string;
  speciesId?: number;
  speciesName?: string;
  baseId?: number;
  baseName?: string;
  collectPlace?: string;
  distributionId?: number;
  collectorName?: string;
  regionId?: number;
  longitude?: number;
  latitude?: number;
  growthStage?: string;
  plantHeight?: number;
  soilType?: string;
  soilPh?: number;
  temperature?: number;
  humidity?: number;
  soilMoisture?: number;
  light?: number;
  stemDiameter?: number;
  leafColor?: string;
  floweringStatus?: string;
  growthEvaluation?: string;
  weather?: string;
  sampleWeight?: number;
  deviceType?: string;
  dataSource?: string;
  externalSource?: string;
  externalNo?: string;
  reviewStatus?: string;
  collectedAt?: string;
  remark?: string;
  createdAt?: string;
  images?: GrowthImageApi[];
};

export type GrowthImageApi = {
  id: number;
  imageCode: string;
  imageUrl: string;
  imageName?: string;
  growthRecordId?: number;
  imageType?: string;
  processStatus?: string;
};

export type GrowthBatchImageApi = {
  id: number;
  batchId: number;
  imageId?: number;
  imageCode?: string;
  imageUrl: string;
  imageName?: string;
  imageType?: string;
  collectTime?: string;
};

export type GrowthTraceEventApi = {
  eventType: string;
  eventTitle?: string;
  eventContent?: string;
  action: string;
  beforeStatus?: string;
  afterStatus?: string;
  operatorId?: number;
  operatorName?: string;
  operatorRole?: string;
  comment?: string;
  eventTime?: string;
  remark?: string;
  metadataJson?: string;
};

export type GrowthMetricKey =
  "plantHeight" | "temperature" | "humidity" | "soilMoisture" | "soilPh" | "light" | "sampleWeight";

export type GrowthTaskApi = {
  id: number;
  taskCode: string;
  taskName: string;
  speciesId?: number;
  speciesName?: string;
  baseId?: number;
  baseName?: string;
  collectPlace?: string;
  collectorId?: number;
  collectorName?: string;
  taskStatus?: string;
};

export type GrowthChartPointApi = {
  recordId: number;
  taskId: number;
  batchId: number;
  batchName?: string;
  collectTime?: string;
  metric?: GrowthMetricKey;
  value?: number | null;
  plantHeight?: number | null;
  temperature?: number | null;
  humidity?: number | null;
  soilMoisture?: number | null;
  soilPh?: number | null;
  light?: number | null;
  sampleWeight?: number | null;
  growthStage?: string;
  collectorName?: string;
  auditStatus?: string;
  summary?: string;
};

export type GrowthChartDatum = GrowthChartPointApi & {
  value: number;
};

export type GrowthChartEmptyReason =
  "no-records" | "no-status-records" | "no-metric-values" | "metric-missing";

export type GrowthChartBuildResult = {
  validPoints: GrowthChartDatum[];
  matchedRecordCount: number;
  reason?: GrowthChartEmptyReason;
};

export type GrowthAuditHistoryApi = {
  actionType: string;
  beforeStatus?: string;
  afterStatus?: string;
  operatorId?: number;
  operatorName?: string;
  operatorRole?: string;
  comment?: string;
  operateTime?: string;
};

export type GrowthPublicAuditApi = {
  actionType: string;
  beforeStatus?: string;
  afterStatus?: string;
  operatorName?: string;
  operateTime?: string;
};

export type GrowthPublicTraceEventApi = {
  eventType: string;
  eventTitle?: string;
  beforeStatus?: string;
  afterStatus?: string;
  operatorName?: string;
  eventTime?: string;
};
export type GrowthTraceQrCodeApi = {
  recordId: number;
  traceCode?: string;
  traceUrl?: string;
  qrCodeUrl?: string;
  publicVisible?: number;
  traceGeneratedTime?: string;
};

export type GrowthPublicTraceImageApi = {
  imageUrl: string;
  imageType?: string;
  imageRole?: string;
  uploadTime?: string;
  uploaderName?: string;
};

export type GrowthPublicTraceArchiveApi = {
  recordId: number;
  traceCode: string;
  traceUrl?: string;
  qrCodeUrl?: string;
  publicVisible?: number;
  speciesName?: string;
  herbName?: string;
  taskId?: number;
  taskName?: string;
  batchId?: number;
  batchName?: string;
  baseName?: string;
  collectPlace?: string;
  collectTime?: string;
  collectorName?: string;
  growthStage?: string;
  auditStatus?: string;
  temperature?: number;
  humidity?: number;
  light?: number;
  soilMoisture?: number;
  soilPh?: number;
  soilType?: string;
  plantHeight?: number;
  stemDiameter?: number;
  leafColor?: string;
  floweringStatus?: string;
  growthEvaluation?: string;
  sampleWeight?: number;
  latestAuditResult?: string;
  latestAuditTime?: string;
  reviewerName?: string;
  images?: GrowthPublicTraceImageApi[];
  auditHistory?: GrowthPublicAuditApi[];
  traceTimeline?: GrowthPublicTraceEventApi[];
};

export type GrowthRecordPayload = {
  batchId: number;
  speciesId?: number;
  distributionId?: number;
  growthStage?: string;
  plantHeight?: number;
  soilType?: string;
  soilPh?: number;
  temperature?: number;
  humidity?: number;
  soilMoisture?: number;
  light?: number;
  stemDiameter?: number;
  leafColor?: string;
  floweringStatus?: string;
  growthEvaluation?: string;
  weather?: string;
  sampleWeight?: number;
  collectedAt?: string;
  remark?: string;
};

export function fetchGrowthRecordPage(params: {
  page: number;
  size: number;
  reviewStatus?: string;
  keyword?: string;
  herbId?: number;
  collectorId?: number;
  taskId?: number;
  batchId?: number;
  baseId?: number;
  auditStatus?: string;
  startTime?: string;
  endTime?: string;
}) {
  return apiGet<PageResult<GrowthRecordApi>>("/growth-records", params);
}

export function fetchGrowthRecordReviewPage(params: {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  status?: string;
  herbId?: number;
  collectorId?: number;
  taskId?: number;
  batchId?: number;
  baseId?: number;
  auditStatus?: string;
  startTime?: string;
  endTime?: string;
}) {
  return apiGet<PageResult<GrowthRecordApi>>("/growth-records/review/page", params);
}

export function fetchGrowthTasks() {
  return apiGet<PageResult<GrowthTaskApi>>("/herb/collection-task/page", {
    pageNum: 1,
    pageSize: 200,
  });
}

export function fetchMyGrowthTasks() {
  return apiGet<PageResult<GrowthTaskApi>>("/herb/collection-task/my", {
    pageNum: 1,
    pageSize: 200,
  });
}

export function fetchGrowthChart(taskId: number, metric: GrowthMetricKey) {
  return apiGet<GrowthChartPointApi[]>(`/herb/collection-task/${taskId}/growth-records/chart`, {
    metric,
  });
}

export function buildGrowthChartData(
  records: GrowthChartPointApi[],
  metricKey: GrowthMetricKey,
  auditStatus = "all",
): GrowthChartBuildResult {
  if (!records.length) {
    return { validPoints: [], matchedRecordCount: 0, reason: "no-records" };
  }

  const matchedRecords =
    auditStatus === "all"
      ? records
      : records.filter((record) => record.auditStatus === auditStatus);
  if (!matchedRecords.length) {
    return { validPoints: [], matchedRecordCount: 0, reason: "no-status-records" };
  }

  const metricExists = matchedRecords.some((record) =>
    Object.prototype.hasOwnProperty.call(record, metricKey),
  );
  if (!metricExists) {
    return {
      validPoints: [],
      matchedRecordCount: matchedRecords.length,
      reason: "metric-missing",
    };
  }

  const validPoints = [...matchedRecords]
    .sort((left, right) => {
      const leftTime = left.collectTime ? new Date(left.collectTime).getTime() : 0;
      const rightTime = right.collectTime ? new Date(right.collectTime).getTime() : 0;
      return leftTime - rightTime || left.recordId - right.recordId;
    })
    .flatMap((record) => {
      const value = record[metricKey];
      return typeof value === "number" && Number.isFinite(value)
        ? [{ ...record, metric: metricKey, value }]
        : [];
    });

  return {
    validPoints,
    matchedRecordCount: matchedRecords.length,
    reason: validPoints.length ? undefined : "no-metric-values",
  };
}

export function fetchGrowthTrace(recordId: number) {
  return apiGet<GrowthTraceEventApi[]>(`/growth-records/${recordId}/trace-events`);
}

export function generateGrowthTraceCode(recordId: number) {
  return apiPost<GrowthTraceQrCodeApi>(`/growth-records/${recordId}/trace-code/generate`);
}

export function generateGrowthTraceQrCode(recordId: number) {
  return apiPost<GrowthTraceQrCodeApi>(`/growth-records/${recordId}/trace-qrcode/generate`);
}

export function getGrowthTraceQrCode(recordId: number) {
  return apiGet<GrowthTraceQrCodeApi>(`/growth-records/${recordId}/trace-qrcode`);
}

export async function downloadGrowthTraceQrCode(recordId: number) {
  const response = await request.get<Blob>(`/growth-records/${recordId}/trace-qrcode/content`, {
    responseType: "blob",
  });
  return response.data;
}

export function enableGrowthPublicTrace(recordId: number) {
  return apiPut<GrowthTraceQrCodeApi>(`/growth-records/${recordId}/trace/public-enable`);
}

export function disableGrowthPublicTrace(recordId: number) {
  return apiPut<GrowthTraceQrCodeApi>(`/growth-records/${recordId}/trace/public-disable`);
}

export async function getPublicGrowthTrace(traceCode: string) {
  const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
  const response = await axios.get<ApiResult<GrowthPublicTraceArchiveApi>>(
    `${baseUrl}/trace/growth/${encodeURIComponent(traceCode)}`,
    { headers: { Accept: "application/json" } },
  );
  return response.data.data;
}

export function resolveGrowthResourceUrl(value?: string) {
  if (!value || /^https?:\/\//i.test(value)) return value || "";
  const baseUrl = request.defaults.baseURL || "http://localhost:8080/api";
  const origin = new URL(baseUrl, "http://localhost").origin;
  return `${origin}${value.startsWith("/") ? value : `/${value}`}`;
}

export function fetchGrowthAuditHistory(recordId: number) {
  return apiGet<GrowthAuditHistoryApi[]>(`/growth-records/${recordId}/audit-history`);
}

export function fetchGrowthRecordDetail(recordId: number) {
  return apiGet<GrowthRecordApi>(`/growth-records/${recordId}`);
}

export function fetchGrowthBatchImages(batchId: number) {
  return apiGet<GrowthBatchImageApi[]>(`/herb/batch/${batchId}/images`);
}

export async function uploadGrowthImage(record: GrowthRecordApi, file: File) {
  const form = new FormData();
  form.append("file", file);
  if (record.speciesId) form.append("speciesId", String(record.speciesId));
  form.append("growthRecordId", String(record.id));
  if (record.distributionId) form.append("distributionId", String(record.distributionId));
  form.append("uploadSource", "web");
  form.append("imageType", "field");
  const response = await request.post("/herb/image/upload", form);
  return response.data.data as GrowthImageApi;
}

export function createGrowthRecord(payload: GrowthRecordPayload) {
  return apiPost<GrowthRecordApi>("/growth-records", payload);
}

export function submitGrowthRecord(recordId: number) {
  return apiPut<GrowthRecordApi>(`/growth-records/${recordId}/submit`);
}

export function approveGrowthRecord(recordId: number, data: { comment?: string }) {
  return apiPut<GrowthRecordApi>(`/growth-records/${recordId}/approve`, data);
}

export function rejectGrowthRecord(recordId: number, data: { comment: string }) {
  return apiPut<GrowthRecordApi>(`/growth-records/${recordId}/reject`, data);
}

export function archiveGrowthRecord(recordId: number, data: { comment?: string }) {
  return apiPut<GrowthRecordApi>(`/growth-records/${recordId}/archive`, data);
}
