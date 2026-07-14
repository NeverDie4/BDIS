export interface HerbTableRecord {
  id: number;
  herbCode: string;
  herbName: string;
  latinName?: string;
  aliasName?: string;
  category?: string;
  categoryName?: string;
  medicinalPart?: string;
  efficacy?: string;
  description?: string;
  status?: number;
  statusText?: string;
  distributionRegions?: string[];
  distributionRegionText?: string;
  createTime?: string;
  updateTime?: string;
}