import { apiDelete, apiGet, apiPost, apiPut } from "@/lib/request";
import type { PageResult } from "@/types/api";

export type TrainingPlanStatus = "draft" | "published" | "closed";

export type TrainingPlan = {
  id: number;
  planNo: string;
  planName: string;
  planType: string;
  ownerId: number;
  ownerName?: string;
  trainerId?: number;
  trainerName?: string;
  courseId?: number;
  courseName?: string;
  description?: string;
  location?: string;
  startedAt?: string;
  endedAt?: string;
  publishStatus: TrainingPlanStatus;
  version: number;
  participantCount?: number;
  materialCount?: number;
  updatedAt?: string;
};

export type TrainingMaterial = {
  id: number;
  materialNo: string;
  materialName: string;
  materialType: string;
  description?: string;
  fileId: number;
  originalFilename?: string;
  fileUrl?: string;
  sourceType: string;
  sourceResourceId?: number;
  reuseCount?: number;
  version: number;
};

export type TrainingPlanMaterial = TrainingMaterial & {
  isRequired: number;
  sortOrder: number;
};

export type TrainingRecord = {
  id: number;
  planId: number;
  userId: number;
  userName?: string;
  realName?: string;
  attendanceStatus?: string;
  checkedInAt?: string;
  trainingStatus?: string;
  progress?: number;
  score?: number;
  startedAt?: string;
  completedAt?: string;
  resultComment?: string;
};

export type TrainingFeedback = {
  id: number;
  trainingRecordId: number;
  userId: number;
  userName?: string;
  rating: number;
  feedbackContent?: string;
  submittedAt?: string;
};

export type TrainingSummary = {
  totalParticipantCount: number;
  completedCount: number;
  learningCount: number;
  notStartedCount: number;
  presentCount: number;
  absentCount: number;
  averageProgress: number;
  averageScore?: number;
  feedbackCount: number;
  averageRating?: number;
};

export type TrainingPlanPayload = {
  planNo: string;
  planName: string;
  planType: string;
  ownerId: number;
  trainerId?: number;
  courseId?: number;
  description?: string;
  location?: string;
  startedAt?: string;
  endedAt?: string;
  remark?: string;
};

export function listTrainingPlans(params?: Record<string, unknown>) {
  return apiGet<PageResult<TrainingPlan>>("/training-plans", { pageNo: 1, pageSize: 50, ...params });
}

export function getTrainingPlan(id: number) {
  return apiGet<TrainingPlan & { materials: TrainingPlanMaterial[] }>(`/training-plans/${id}`);
}

export function createTrainingPlan(payload: TrainingPlanPayload) {
  return apiPost<TrainingPlan>("/training-plans", payload);
}

export function updateTrainingPlan(id: number, payload: TrainingPlanPayload & { version: number }) {
  return apiPut<TrainingPlan>(`/training-plans/${id}`, payload);
}

export function deleteTrainingPlan(id: number) {
  return apiDelete<void>(`/training-plans/${id}`);
}

export function publishTrainingPlan(id: number, version: number) {
  return apiPost<TrainingPlan>(`/training-plans/${id}/publish`, { version });
}

export function closeTrainingPlan(id: number, version: number, reason: string) {
  return apiPost<TrainingPlan>(`/training-plans/${id}/close`, { version, reason });
}

export function listTrainingMaterials(params?: Record<string, unknown>) {
  return apiGet<PageResult<TrainingMaterial>>("/training-materials", { pageNo: 1, pageSize: 100, ...params });
}

export function createTrainingMaterial(payload: {
  materialNo: string;
  materialName: string;
  materialType: string;
  fileId: number;
  sourceType: string;
  sourceResourceId?: number;
  description?: string;
}) {
  return apiPost<TrainingMaterial>("/training-materials", payload);
}

export function listPlanMaterials(planId: number) {
  return apiGet<TrainingPlanMaterial[]>(`/training-plans/${planId}/materials`);
}

export function bindPlanMaterial(planId: number, payload: { materialId: number; isRequired?: number; sortOrder?: number }) {
  return apiPost<number>(`/training-plans/${planId}/materials`, payload);
}

export function unbindPlanMaterial(planId: number, materialId: number) {
  return apiDelete<void>(`/training-plans/${planId}/materials/${materialId}`);
}

export function listTrainingRecords(params?: Record<string, unknown>) {
  return apiGet<PageResult<TrainingRecord>>("/training-records", { pageNo: 1, pageSize: 100, ...params });
}

export function addTrainingParticipants(planId: number, userIds: number[]) {
  return apiPost<{ successCount: number; duplicateCount: number; failureCount: number }>(
    `/training-plans/${planId}/participants/batch`,
    { userIds },
  );
}

export function updateTrainingRecord(id: number, payload: Partial<TrainingRecord>) {
  return apiPut<TrainingRecord>(`/training-records/${id}`, payload);
}

export function listTrainingFeedback(params?: Record<string, unknown>) {
  return apiGet<PageResult<TrainingFeedback>>("/training-feedbacks", { pageNo: 1, pageSize: 100, ...params });
}

export function createTrainingFeedback(payload: {
  trainingRecordId: number;
  rating: number;
  feedbackContent?: string;
}) {
  return apiPost<TrainingFeedback>("/training-feedbacks", payload);
}

export function getTrainingSummary(planId: number) {
  return apiGet<TrainingSummary>(`/training-plans/${planId}/summary`);
}
