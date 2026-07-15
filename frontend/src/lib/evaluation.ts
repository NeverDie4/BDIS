import { apiGet, apiPost, apiPut } from "@/lib/request";

export type ApiPage<T> = {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
};

export type EvaluationIndicator = {
  id: number;
  indicatorNo: string;
  indicatorName: string;
  indicatorType?: string;
  parentId?: number;
  weight: number;
  maxScore: number;
  scoreDesc?: string;
  sortOrder?: number;
  status?: number;
  remark?: string;
  updatedAt?: string;
};

export type EvaluationIndicatorPayload = {
  indicatorName: string;
  indicatorNo?: string;
  indicatorType?: string;
  parentId?: number;
  weight?: number;
  maxScore?: number;
  scoreDesc?: string;
  sortOrder?: number;
  status?: number;
  remark?: string;
};

export type EvaluationTask = {
  id: number;
  taskNo: string;
  taskName: string;
  taskType?: string;
  targetType: string;
  targetId: number;
  ownerId: number;
  startedAt?: string;
  endedAt?: string;
  taskStatus: "draft" | "scoring" | "confirmed" | string;
  status?: number;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type EvaluationTaskPayload = {
  taskName: string;
  taskNo?: string;
  taskType?: string;
  targetType: string;
  targetId: number;
  startedAt?: string;
  endedAt?: string;
  status?: number;
  remark?: string;
};

export type EvaluationScore = {
  id: number;
  taskId: number;
  indicatorId: number;
  evaluatorId: number;
  score: number;
  scoreComment?: string;
  scoredAt?: string;
  remark?: string;
};

export type EvaluationResult = {
  id: number;
  taskId: number;
  totalScore: number;
  resultLevel: string;
  resultDesc?: string;
  confirmedBy: number;
  confirmedAt?: string;
  remark?: string;
};

export type EvaluationTaskDetail = {
  task: EvaluationTask;
  scores: EvaluationScore[];
  result?: EvaluationResult;
};

export type Declaration = {
  id: number;
  applicationNo: string;
  applicationTitle: string;
  applicationType?: string;
  applicantId: number;
  reviewStatus: "draft" | "submitted" | "approved" | "rejected" | "archived" | string;
  submittedAt?: string;
  reviewerId?: number;
  reviewedAt?: string;
  reviewComment?: string;
  status?: number;
  createdBy?: number;
  updatedBy?: number;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type DeclarationMaterial = {
  id: number;
  applicationId: number;
  fileId?: number;
  fileName: string;
  fileType?: string;
  fileUrl?: string;
  fileSize?: number;
  uploaderId?: number;
  uploadedAt?: string;
  remark?: string;
};

export type DeclarationReview = {
  id: number;
  applicationId: number;
  reviewerId: number;
  reviewAction: string;
  beforeStatus?: string;
  reviewStatus: string;
  reviewComment?: string;
  reviewedAt?: string;
};

export type DeclarationArchive = {
  id: number;
  archiveNo: string;
  applicationId: number;
  archiveTitle: string;
  ownerId: number;
  archiveStatus: string;
  generatedAt?: string;
};

export type DeclarationArchiveItem = {
  id: number;
  archiveId: number;
  sourceType: string;
  sourceId: number;
  itemName?: string;
  itemDesc?: string;
};

export type DeclarationDetail = {
  declaration: Declaration;
  materials: DeclarationMaterial[];
  reviewRecords: DeclarationReview[];
  archive?: DeclarationArchive;
  archiveItems: DeclarationArchiveItem[];
};

export type DeclarationSummary = {
  declarationId: number;
  applicationTitle: string;
  reviewStatus: string;
  materialCount: number;
  reviewRecordCount: number;
  archiveId?: number;
  archiveNo?: string;
  archiveItemCount: number;
};

export function fetchIndicators(params: Record<string, unknown> = {}) {
  return apiGet<ApiPage<EvaluationIndicator>>("/evaluation-standards", params);
}

export function createIndicator(payload: EvaluationIndicatorPayload) {
  return apiPost<EvaluationIndicator>("/evaluation-standards", payload);
}

export function updateIndicator(id: number, payload: EvaluationIndicatorPayload) {
  return apiPut<EvaluationIndicator>(`/evaluation-standards/${id}`, payload);
}

export function fetchEvaluationTasks(params: Record<string, unknown> = {}) {
  return apiGet<ApiPage<EvaluationTask>>("/evaluation-tasks", params);
}

export function createEvaluationTask(payload: EvaluationTaskPayload) {
  return apiPost<EvaluationTask>("/evaluation-tasks", payload);
}

export function fetchEvaluationTaskDetail(id: number) {
  return apiGet<EvaluationTaskDetail>(`/evaluation-tasks/${id}`);
}

export function saveEvaluationScore(payload: {
  taskId: number;
  indicatorId: number;
  score: number;
  scoreComment?: string;
  remark?: string;
}) {
  return apiPost<EvaluationScore>("/evaluation-records", payload);
}

export function confirmEvaluationResult(
  recordId: number,
  payload: { resultDesc?: string; remark?: string },
) {
  return apiPost<EvaluationResult>(`/evaluation-records/${recordId}/confirmations`, payload);
}

export function fetchDeclarations(params: Record<string, unknown> = {}) {
  return apiGet<ApiPage<Declaration>>("/declarations", params);
}

export function createDeclaration(payload: {
  applicationTitle: string;
  applicationType?: string;
  remark?: string;
}) {
  return apiPost<Declaration>("/declarations", payload);
}

export function fetchDeclarationDetail(id: number) {
  return apiGet<DeclarationDetail>(`/declarations/${id}`);
}

export function fetchDeclarationSummary(id: number) {
  return apiGet<DeclarationSummary>(`/declarations/${id}/summary`);
}

export function addDeclarationMaterial(id: number, fileId: number, remark?: string) {
  return apiPost<DeclarationMaterial>(`/declarations/${id}/materials`, { fileId, remark });
}

export function submitDeclaration(id: number) {
  return apiPost<Declaration>(`/declarations/${id}/submissions`);
}

export function reviewDeclaration(
  id: number,
  payload: {
    reviewAction: "approve" | "reject";
    reviewStatus: "approved" | "rejected";
    reviewComment?: string;
    remark?: string;
  },
) {
  return apiPost<DeclarationReview>(`/declarations/${id}/reviews`, payload);
}

export function generateDeclarationArchive(id: number) {
  return apiPost<DeclarationArchive>(`/declarations/${id}/archives`);
}
