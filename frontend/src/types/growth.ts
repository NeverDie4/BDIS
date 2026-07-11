import type { ID, StatusType } from "./common";

export type EnvironmentMetric = {
  id: ID;
  recordId: ID;
  temperature: number;
  humidity: number;
  soilPh: number;
  lightIntensity?: number;
  weather: string;
  soilMoisture?: number;
  measuredAt: string;
};

export type MorphologyMetric = {
  id: ID;
  recordId: ID;
  plantHeight?: number;
  leafCount?: number;
  stemDiameter?: number;
  flowerStatus?: string;
  fruitStatus?: string;
  growthStage: string;
  sampleDescription?: string;
};

export type GrowthRecord = {
  id: ID;
  recordNo: string;
  herbId: ID;
  herbName: string;
  baseId?: ID;
  baseName?: string;
  district: string;
  locationName: string;
  collector: string;
  collectedAt: string;
  clientRecordNo?: string;
  syncBatchNo?: string;
  imageUrls: string[];
  status: StatusType;
  reviewStatus: StatusType;
  environmentMetric?: EnvironmentMetric;
  morphologyMetric?: MorphologyMetric;
  remark?: string;
};

export type TraceEvent = {
  id: ID;
  recordId: ID;
  eventTime: string;
  actor: string;
  action: string;
  status: StatusType;
  description?: string;
};
