import { apiDelete, apiGet, apiPost, apiPut } from "@/lib/request";
import { toBrowserFileUrl } from "@/lib/files";
import type { PageResult } from "@/types/api";

export type HerbSpeciesApi = {
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
  coverImageUrl?: string;
  distributionRegions?: string[];
  distributionRegionText?: string;
  createTime?: string;
  updateTime?: string;
};

export type HerbBaseApi = {
  id: number;
  baseNo: string;
  baseName: string;
  baseType?: string;
  regionId?: number;
  regionName?: string;
  address?: string;
  longitude?: number;
  latitude?: number;
  contactName?: string;
  contactPhone?: string;
  description?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type DictItemApi = {
  id: number;
  typeId?: number;
  itemCode: string;
  itemName: string;
  itemValue?: string;
  parentId?: number;
  sortOrder?: number;
  remark?: string;
  children?: DictItemApi[];
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
  coverImageUrl?: string;
};

export type HerbSpeciesUpdatePayload = Omit<HerbSpeciesPayload, "herbCode">;

export type DictItemPayload = {
  itemCode: string;
  itemName: string;
  itemValue?: string;
  parentId?: number;
  sortOrder?: number;
  remark?: string;
};

export type HerbBasePayload = {
  baseNo: string;
  baseName: string;
  baseType?: string;
  regionId?: number;
  address?: string;
  longitude?: number;
  latitude?: number;
  contactName?: string;
  contactPhone?: string;
  description?: string;
  remark?: string;
};

export function fetchHerbSpecies(params: {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  category?: string;
  medicinalPart?: string;
}) {
  return apiGet<PageResult<HerbSpeciesApi>>("/herb/species/page", params).then((page) => ({
    ...page,
    records: page.records.map(withBrowserCoverUrl),
  }));
}

export function fetchEnabledHerbs() {
  return apiGet<HerbSpeciesApi[]>("/herb/species/list").then((items) =>
    items.map(withBrowserCoverUrl),
  );
}

export function createHerbSpecies(payload: HerbSpeciesPayload) {
  return apiPost<HerbSpeciesApi>("/herb/species", payload).then(withBrowserCoverUrl);
}

export function updateHerbSpecies(id: number, payload: HerbSpeciesUpdatePayload) {
  return apiPut<HerbSpeciesApi>(`/herb/species/${id}`, payload).then(withBrowserCoverUrl);
}

export function deleteHerbSpecies(id: number) {
  return apiDelete<void>(`/herb/species/${id}`);
}

export function fetchHerbCategories() {
  return apiGet<DictItemApi[]>("/dictionaries/herb_category/items");
}

export function createHerbCategory(payload: DictItemPayload) {
  return apiPost<number>("/dictionaries/herb_category/items", payload);
}

export function updateHerbCategory(id: number, payload: DictItemPayload) {
  return apiPut<void>(`/dictionaries/herb_category/items/${id}`, payload);
}

export function deleteHerbCategory(id: number) {
  return apiDelete<void>(`/dictionaries/herb_category/items/${id}`);
}

export function fetchHerbBases(params?: {
  page?: number;
  size?: number;
  keyword?: string;
  baseType?: string;
  regionId?: number;
}) {
  return apiGet<PageResult<HerbBaseApi>>("/herb-bases", params ?? { page: 1, size: 100 });
}

export function createHerbBase(payload: HerbBasePayload) {
  return apiPost<number>("/herb-bases", payload);
}

export function updateHerbBase(id: number, payload: HerbBasePayload) {
  return apiPut<void>(`/herb-bases/${id}`, payload);
}

export function deleteHerbBase(id: number) {
  return apiDelete<void>(`/herb-bases/${id}`);
}

function withBrowserCoverUrl(herb: HerbSpeciesApi): HerbSpeciesApi {
  return {
    ...herb,
    coverImageUrl: herb.coverImageUrl
      ? toBrowserFileUrl(herb.coverImageUrl)
      : herb.coverImageUrl,
  };
}
