import { apiGet, apiPut, request } from "@/lib/request";
import type { ApiResult } from "@/types/api";

export type IdentificationCandidateApi = {
  rank: number;
  atlasId: number;
  atlasCode?: string;
  atlasImageUrl?: string;
  speciesId?: number;
  speciesName?: string;
  similarity?: number;
  matchResult?: string;
  doubaoAgreed?: boolean;
};

export type DoubaoRecognitionApi = {
  id?: number;
  predictedSpeciesId?: number;
  predictedSpeciesName?: string;
  predictedName?: string;
  confidence?: number;
  recognitionStatus?: string;
};

export type IdentificationApi = {
  id?: number;
  imageId: number;
  imageCode?: string;
  imageUrl?: string;
  finalSpeciesId?: number;
  finalSpeciesName?: string;
  finalConfidence?: number;
  resultSource?: string;
  matchResult?: string;
  needReview?: boolean;
  reviewStatus?: string;
  suggestion?: string;
  reviewComment?: string;
  reviewerName?: string;
  reviewTime?: string;
  identifyTime?: string;
  localCandidates?: IdentificationCandidateApi[];
  doubaoRecognition?: DoubaoRecognitionApi;
};

export type IdentificationReviewPayload = {
  finalSpeciesId: number;
  reviewStatus: "confirmed" | "rejected";
  reviewComment?: string;
};

export function fetchLatestIdentification(imageId: number) {
  return apiGet<IdentificationApi>(`/herb/image/${imageId}/identification/latest`);
}

export async function identifyImage(imageId: number, forceRefresh = false) {
  const response = await request.post<ApiResult<IdentificationApi>>(
    `/herb/image/${imageId}/identify`,
    { forceRefresh, topK: 5 },
    { timeout: 60000 },
  );
  return response.data.data;
}

export function reviewIdentification(id: number, payload: IdentificationReviewPayload) {
  return apiPut<IdentificationApi>(`/herb/identification/${id}/review`, payload);
}
