import { apiGet, apiPost, request } from "@/lib/request";
import type { PageResult } from "@/types/api";

export type GrowthRecordApi = {
  id: number;
  speciesId: number;
  speciesName?: string;
  distributionId?: number;
  collectorName?: string;
  regionId?: number;
  longitude?: number;
  latitude?: number;
  growthStage?: string;
  soilType?: string;
  soilPh?: number;
  temperature?: number;
  humidity?: number;
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

export type GrowthTraceEventApi = {
  eventType: string;
  action: string;
  beforeStatus?: string;
  afterStatus?: string;
  operatorId?: number;
  operatorName?: string;
  comment?: string;
  eventTime?: string;
};

export type GrowthRecordPayload = {
  speciesId: number;
  distributionId?: number;
  growthStage?: string;
  soilType?: string;
  soilPh?: number;
  temperature?: number;
  humidity?: number;
  weather?: string;
  sampleWeight?: number;
  collectedAt?: string;
  remark?: string;
};

export function fetchGrowthRecordPage(params: {
  page: number;
  size: number;
  reviewStatus?: string;
}) {
  return apiGet<PageResult<GrowthRecordApi>>("/growth-records", params);
}

export function fetchGrowthTrace(recordId: number) {
  return apiGet<GrowthTraceEventApi[]>(`/growth-records/${recordId}/trace-events`);
}

export function fetchGrowthRecordDetail(recordId: number) {
  return apiGet<GrowthRecordApi>(`/growth-records/${recordId}`);
}

export async function uploadGrowthImage(record: GrowthRecordApi, file: File) {
  const form = new FormData();
  form.append("file", file);
  form.append("speciesId", String(record.speciesId));
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
  return apiPost<GrowthRecordApi>(`/growth-records/${recordId}/submissions`);
}
