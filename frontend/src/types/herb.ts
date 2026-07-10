import type { ID, StatusType } from "./common";

export type HerbCategory = {
  id: ID;
  categoryCode: string;
  categoryName: string;
  parentId?: ID;
  description?: string;
  sortOrder: number;
  status: StatusType;
};

export type HerbBase = {
  id: ID;
  baseNo: string;
  baseName: string;
  district: string;
  address: string;
  longitude: number;
  latitude: number;
  manager: string;
  contactPhone?: string;
  description?: string;
  status: StatusType;
};

export type HerbSpecies = {
  id: ID;
  herbNo: string;
  herbName: string;
  aliasNames: string[];
  latinName?: string;
  categoryId: ID;
  categoryName: string;
  medicinalPart: string;
  efficacy: string;
  growthHabit?: string;
  suitableEnvironment?: string;
  imageUrl?: string;
  tags: string[];
  status: StatusType;
  baseIds?: ID[];
  updatedAt: string;
};

export type HerbDistributionPoint = {
  id: ID;
  pointNo: string;
  herbId: ID;
  herbName: string;
  baseId?: ID;
  baseName?: string;
  district: string;
  locationName: string;
  longitude: number;
  latitude: number;
  altitude?: number;
  distributionType: "人工栽培" | "野生分布" | "示范种植" | "科研样地";
  resourceScale: string;
  status: StatusType;
  description?: string;
};
