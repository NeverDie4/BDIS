import { apiGet, apiPost } from "@/lib/request";
import type { PageResult } from "@/types/api";

export type HerbSpeciesApi = {
  id: number;
  herbCode: string;
  herbName: string;
  latinName?: string;
  aliasName?: string;
  category?: string;
  medicinalPart?: string;
  efficacy?: string;
  description?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
};

export type HerbBaseApi = {
  id: number;
  baseNo: string;
  baseName: string;
  baseType?: string;
  regionName?: string;
  address?: string;
  contactName?: string;
  contactPhone?: string;
  status?: number;
};

export type HerbSpeciesPayload = {
  herbCode: string;
  herbName: string;
  latinName?: string;
  aliasName?: string;
  category?: string;
  medicinalPart?: string;
  efficacy?: string;
  description?: string;
  status?: number;
};

export function fetchHerbSpecies(params: {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  category?: string;
}) {
  return apiGet<PageResult<HerbSpeciesApi>>("/herb/species/page", params);
}

export function fetchEnabledHerbs() {
  return apiGet<HerbSpeciesApi[]>("/herb/species/list");
}

export function createHerbSpecies(payload: HerbSpeciesPayload) {
  return apiPost<HerbSpeciesApi>("/herb/species", payload);
}

export function fetchHerbBases() {
  return apiGet<PageResult<HerbBaseApi>>("/herb-bases", { page: 1, size: 100, status: 1 });
}

export function fetchEnabledHerbBases() {
  return apiGet<HerbBaseApi[]>("/herb-bases/enabled");
}
