import { request } from "@/lib/request";
import axios from "axios";

export interface ApiResult<T> {
  code: string;
  message: string;
  data: T;
  timestamp: string;
}

export interface MapPoint {
  id: number;
  speciesId: number;
  herbName: string;
  aliasName?: string;
  latinName?: string;
  medicinalPart?: string;
  efficacy?: string;
  growthEnvironment?: string;
  originArea?: string;
  growthCycle?: string;
  herbDescription?: string;
  baseId?: number;
  baseName?: string;
  regionId?: number;
  locationName?: string;
  longitude: number;
  latitude: number;
  province?: string;
  city?: string;
  district?: string;
  address?: string;
  altitude?: number;
  distributionType?: string;
  distributionLevel?: string;
  distributionDesc?: string;
  coverImageUrl?: string;
  lastCollectedAt?: string;
  sourceType?: string;
  dataSource?: string;
  status?: number;
  remark?: string;
}

export interface GrowthRecord {
  id: number;
  speciesId: number;
  distributionId: number;
  collectorName?: string;
  longitude?: number;
  latitude?: number;
  growthStage?: string;
  soilType?: string;
  soilPh?: number;
  temperature?: number;
  humidity?: number;
  weather?: string;
  sampleWeight?: number;
  dataSource?: string;
  reviewStatus?: string;
  collectedAt?: string;
  remark?: string;
}

export interface GrowthRecordPayload {
  collectorName: string;
  collectedAt?: string;
  growthStage?: string;
  weather?: string;
  temperature?: number;
  humidity?: number;
  soilType?: string;
  soilPh?: number;
  sampleWeight?: number;
  dataSource?: string;
  remark?: string;
}

export interface MapPointPayload {
  speciesId?: number;
  herbName: string;
  aliasName?: string;
  latinName?: string;
  medicinalPart?: string;
  efficacy?: string;
  growthEnvironment?: string;
  originArea?: string;
  growthCycle?: string;
  herbDescription?: string;
  baseId?: number;
  regionId?: number;
  locationName?: string;
  longitude: number;
  latitude: number;
  province?: string;
  city?: string;
  district?: string;
  address?: string;
  altitude?: number;
  distributionType?: string;
  distributionLevel?: string;
  distributionDesc?: string;
  coverImageUrl?: string;
  lastCollectedAt?: string;
  sourceType?: string;
  dataSource?: string;
  remark?: string;
}

export interface MapPointQuery {
  keyword?: string;
  district?: string;
  speciesId?: number;
  baseId?: number;
}

export function getMapPointRequestErrorMessage(error: unknown, action: string) {
  if (!axios.isAxiosError(error)) {
    return `${action}失败，请稍后重试`;
  }

  if (!error.response) {
    return `${action}失败，无法连接后端服务，请确认后端已启动`;
  }

  if (error.response.status === 403) {
    return `${action}失败，后端接口未放行或权限不足`;
  }

  return `${action}失败，后端返回 ${error.response.status}`;
}

export async function fetchMapPoints(params?: MapPointQuery) {
  const response = await request.get<ApiResult<MapPoint[]>>("/map-points", { params });
  return response.data.data;
}

export async function createMapPoint(payload: MapPointPayload) {
  const response = await request.post<ApiResult<MapPoint>>("/map-points", payload);
  return response.data.data;
}

export async function updateMapPoint(pointId: number, payload: MapPointPayload) {
  const response = await request.put<ApiResult<MapPoint>>(`/map-points/${pointId}`, payload);
  return response.data.data;
}

export async function updateMapPointStatus(pointId: number, status: 0 | 1) {
  const response = await request.patch<ApiResult<MapPoint>>(`/map-points/${pointId}/status`, { status });
  return response.data.data;
}

export async function deleteMapPoint(pointId: number) {
  await request.delete<ApiResult<void>>(`/map-points/${pointId}`);
}

export async function fetchGrowthRecords(pointId: number) {
  const response = await request.get<ApiResult<GrowthRecord[]>>(`/map-points/${pointId}/collections`);
  return response.data.data;
}

export async function createGrowthRecord(pointId: number, payload: GrowthRecordPayload) {
  const response = await request.post<ApiResult<GrowthRecord>>(`/map-points/${pointId}/collections`, payload);
  return response.data.data;
}
