import axios from "axios";
import type { ApiResult } from "@/types/api";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";

export type PublicDigitalLifeMetricsApi = {
  plantHeight?: number | null;
  stemDiameter?: number | null;
  leafColor?: string | null;
  floweringStatus?: string | null;
  temperature?: number | null;
  humidity?: number | null;
  soilMoisture?: number | null;
  soilPh?: number | null;
  light?: number | null;
  growthEvaluation?: string | null;
};

export type PublicDigitalLifeImageApi = {
  imageUrl: string;
  imageType?: string;
  imageTypeName?: string;
  uploadTime?: string;
  uploaderName?: string;
  primaryImage?: boolean;
};

export type PublicDigitalLifeRecognitionApi = {
  speciesName?: string;
  confidence?: number | null;
  similarity?: number | null;
  needReview?: boolean;
  recognitionSource?: string;
  conclusion?: string;
};

export type PublicDigitalLifeStageApi = {
  sequence: number;
  batchCode?: string;
  batchName?: string;
  growthStage?: string;
  collectedAt?: string;
  baseName?: string;
  locationName?: string;
  longitude?: number | null;
  latitude?: number | null;
  collectorName?: string;
  auditStatus?: string;
  reviewedAt?: string | null;
  dataStatus?: string;
  metrics?: PublicDigitalLifeMetricsApi | null;
  images?: PublicDigitalLifeImageApi[];
  recognition?: PublicDigitalLifeRecognitionApi | null;
  aiNarration?: string | null;
  narrationSource?: "ai" | "template" | string | null;
  narrationGeneratedTime?: string | null;
};

export type PublicDigitalLifeArchiveApi = {
  traceCode: string;
  taskCode?: string;
  taskName?: string;
  speciesName?: string;
  baseName?: string;
  description?: string;
  stageCount?: number;
  validStageCount?: number;
  imageCount?: number;
  startTime?: string;
  endTime?: string;
  archiveStatus?: string;
  qrCodeUrl?: string;
  stages?: PublicDigitalLifeStageApi[];
};

export type PublicDigitalLifeIntegrityApi = {
  verified: boolean;
  eventCount: number;
  rootHash?: string | null;
  hashVersion?: string | null;
  generatedTime?: string | null;
  failedSequence?: number | null;
  failedEventType?: string | null;
  message: string;
};

export async function getPublicDigitalLifeArchive(traceCode: string) {
  const response = await axios.get<ApiResult<PublicDigitalLifeArchiveApi>>(
    `${API_BASE_URL}/trace/digital-life/${encodeURIComponent(traceCode)}`,
    { headers: { Accept: "application/json" } },
  );
  return response.data.data;
}

export async function getPublicDigitalLifeIntegrity(traceCode: string) {
  const response = await axios.get<ApiResult<PublicDigitalLifeIntegrityApi>>(
    `${API_BASE_URL}/trace/digital-life/${encodeURIComponent(traceCode)}/integrity`,
    { headers: { Accept: "application/json" } },
  );
  return response.data.data;
}

export function resolveDigitalLifeResourceUrl(value?: string) {
  if (!value || /^https?:\/\//i.test(value)) return value || "";
  const normalizedApiBase = API_BASE_URL.replace(/\/+$/, "");
  const absoluteApiBase = /^https?:\/\//i.test(normalizedApiBase);
  if (value.startsWith("/")) {
    return absoluteApiBase ? `${new URL(normalizedApiBase).origin}${value}` : value;
  }
  const resourcePath = `/${value}`;
  return `${normalizedApiBase}${resourcePath}`;
}
