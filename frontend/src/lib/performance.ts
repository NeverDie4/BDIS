import { apiDelete, apiGet, apiPost, apiPut } from "@/lib/request";

export type PerformanceStatus = "draft" | "submitted" | "approved" | "rejected";
export type StandardLifecycle = "draft" | "published" | "disabled";

export type ApiPage<T> = {
  records: T[];
  total: number;
  current?: number;
  size?: number;
};

function normalizeApiPage<T>(data: unknown): ApiPage<T> {
  if (Array.isArray(data)) {
    return { records: data as T[], total: data.length };
  }
  if (!data || typeof data !== "object") {
    return { records: [], total: 0 };
  }
  const page = data as Partial<ApiPage<T>>;
  const records = Array.isArray(page.records) ? page.records : [];
  return {
    records,
    total: typeof page.total === "number" && page.total >= 0 ? page.total : records.length,
    current: page.current,
    size: page.size,
  };
}

export type PerformanceRecord = {
  id: number;
  performanceNo: string;
  userId: number;
  performanceTitle: string;
  performanceType?: string;
  performanceLevel?: string;
  occurredAt?: string;
  standardId?: number;
  sourceType?: string;
  sourceId?: number;
  sourceNameSnapshot?: string;
  identifyStatus: PerformanceStatus;
  submittedAt?: string;
  standardNoSnapshot?: string;
  standardVersionSnapshot?: number;
  standardNameSnapshot?: string;
  standardRuleSnapshot?: string;
  remark?: string;
  updatedAt?: string;
};

export type PerformanceMaterial = {
  id: number;
  fileId: number;
  fileUsage?: string;
  sortOrder?: number;
  remark?: string;
};

export type PerformanceParticipantUser = {
  id: number;
  username: string;
  realName?: string;
};

export type PerformanceParticipant = {
  id: number;
  performanceId: number;
  userId: number;
  username?: string;
  realName?: string;
  participantRole: string;
  sortOrder: number;
  isPrimary: number;
  remark?: string;
};

export type PerformanceAuditRecord = {
  id: number;
  identifierId: number;
  identifyAction: string;
  identifyResult: string;
  identifyComment?: string;
  identifiedAt?: string;
};

export type PerformanceStandard = {
  id: number;
  standardNo: string;
  standardVersion: number;
  standardName: string;
  performanceType: string;
  standardDesc?: string;
  scoreRule?: string;
  levelRule?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  materialRequired: number;
  minMaterialCount: number;
  lifecycleStatus: StandardLifecycle;
  publishedAt?: string;
  sortOrder?: number;
  remark?: string;
};

export type PerformanceDetail = {
  performance: PerformanceRecord;
  standard?: PerformanceStandard;
  materials: PerformanceMaterial[];
  participants: PerformanceParticipant[];
  auditRecords: PerformanceAuditRecord[];
};

export type PerformancePayload = {
  performanceNo?: string;
  performanceTitle: string;
  performanceType?: string;
  performanceLevel?: string;
  occurredAt?: string;
  standardId?: number;
  sourceType?: string;
  sourceId?: number;
  remark?: string;
};

export type StandardPayload = {
  standardNo?: string;
  standardName: string;
  performanceType: string;
  standardDesc?: string;
  scoreRule?: string;
  levelRule?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  materialRequired?: boolean;
  minMaterialCount?: number;
  sortOrder?: number;
  remark?: string;
};

export type PerformanceStatistics = {
  totalCount: number;
  draftCount: number;
  submittedCount: number;
  approvedCount: number;
  rejectedCount: number;
  typeCounts: Record<string, number>;
};

export function fetchPerformances(params: Record<string, unknown>) {
  return apiGet<ApiPage<PerformanceRecord>>("/performances", params);
}

export function fetchPerformance(id: number) {
  return apiGet<PerformanceDetail>(`/performances/${id}`);
}

export function createPerformance(payload: PerformancePayload) {
  return apiPost<PerformanceRecord>("/performances", payload);
}

export function updatePerformance(id: number, payload: PerformancePayload) {
  return apiPut<PerformanceRecord>(`/performances/${id}`, payload);
}

export function updateStandard(id: number, payload: StandardPayload) {
  return apiPut<PerformanceStandard>(`/performance-standards/${id}`, payload);
}

export async function fetchPerformanceParticipantUsers(
  id: number,
  params: Record<string, unknown>,
) {
  const data = await apiGet<unknown>(`/performances/${id}/participants/participant-users`, params);
  return normalizeApiPage<PerformanceParticipantUser>(data);
}

export function submitPerformance(id: number) {
  return apiPost<PerformanceRecord>(`/performances/${id}/submissions`);
}

export function auditPerformance(id: number, decision: "approved" | "rejected", comment?: string) {
  return apiPost<PerformanceAuditRecord>(`/performances/${id}/audit-records`, {
    decision,
    comment,
  });
}

export function addPerformanceMaterial(id: number, fileId: number, sortOrder = 0) {
  return apiPost<PerformanceMaterial>(`/performances/${id}/materials`, { fileId, sortOrder });
}

export function removePerformanceMaterial(id: number, relationId: number) {
  return apiDelete<void>(`/performances/${id}/materials/${relationId}`);
}

export function addParticipant(
  id: number,
  payload: { userId: number; participantRole: string; sortOrder?: number },
) {
  return apiPost<PerformanceParticipant>(`/performances/${id}/participants`, payload);
}

export function updateParticipant(
  id: number,
  participantId: number,
  payload: { userId: number; participantRole: string; sortOrder?: number },
) {
  return apiPut<PerformanceParticipant>(
    `/performances/${id}/participants/${participantId}`,
    payload,
  );
}

export function removeParticipant(id: number, participantId: number) {
  return apiDelete<void>(`/performances/${id}/participants/${participantId}`);
}

export function fetchStandards(params: Record<string, unknown>) {
  return apiGet<ApiPage<PerformanceStandard>>("/performance-standards", params);
}

export function createStandard(payload: StandardPayload) {
  return apiPost<PerformanceStandard>("/performance-standards", payload);
}

export function createStandardVersion(id: number, payload: StandardPayload) {
  return apiPost<PerformanceStandard>(`/performance-standards/${id}/versions`, payload);
}

export function publishStandard(id: number) {
  return apiPost<PerformanceStandard>(`/performance-standards/${id}/publish`);
}

export function disableStandard(id: number) {
  return apiPost<PerformanceStandard>(`/performance-standards/${id}/disable`);
}

export function fetchPerformanceStatistics(params: Record<string, unknown>) {
  return apiGet<PerformanceStatistics>("/performance-statistics", params);
}
