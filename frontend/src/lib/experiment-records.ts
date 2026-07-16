import { apiDelete, apiGet, apiPost, apiPut } from "@/lib/request";
import type { PageResult } from "@/types/api";
import { toBrowserFileUrl, uploadFile, type FileResource } from "@/lib/files";

export type ExperimentArchiveStatus = "draft" | "submitted" | "archived";

export type ExperimentRecordListApi = {
  id: number;
  recordNo: string;
  sourceType?: string;
  courseId?: number;
  courseNo?: string;
  courseName?: string;
  projectId?: number;
  projectNo?: string;
  projectName?: string;
  experimentTitle: string;
  recorderId?: number;
  recorderName?: string;
  recordedAt?: string;
  archiveStatus: ExperimentArchiveStatus;
  score?: number;
  gradedBy?: number;
  gradedAt?: string;
  gradeComment?: string;
  status?: number;
  createdAt?: string;
  updatedAt?: string;
};

export type ExperimentRecordDetailApi = ExperimentRecordListApi & {
  experimentProcess?: string;
  experimentResult?: string;
  recorderUsername?: string;
  submittedAt?: string;
  submittedBy?: number;
  submittedByName?: string;
  archivedAt?: string;
  archivedBy?: number;
  archivedByName?: string;
  archiveComment?: string;
  gradedByName?: string;
  remark?: string;
  version: number;
};

export type ExperimentRecordPayload = {
  recordNo: string;
  courseId: number;
  experimentTitle: string;
  experimentProcess?: string;
  experimentResult?: string;
  recordedAt?: string;
  remark?: string;
};

export type ExperimentRecordUpdatePayload = Omit<ExperimentRecordPayload, "recordNo" | "courseId"> & {
  version: number;
};

export type ExperimentAttachmentApi = FileResource & {
  fileUsage?: "attachment" | "image";
};

export function listExperimentRecords(courseId: number) {
  return apiGet<PageResult<ExperimentRecordListApi>>("/experiment-records", {
    courseId,
    pageNo: 1,
    pageSize: 50,
  });
}

export function getExperimentRecord(id: number) {
  return apiGet<ExperimentRecordDetailApi>(`/experiment-records/${id}`);
}

export function createExperimentRecord(payload: ExperimentRecordPayload) {
  return apiPost<ExperimentRecordDetailApi>("/experiment-records", payload);
}

export function updateExperimentRecord(id: number, payload: ExperimentRecordUpdatePayload) {
  return apiPut<ExperimentRecordDetailApi>(`/experiment-records/${id}`, payload);
}

export function deleteExperimentRecord(id: number) {
  return apiDelete<void>(`/experiment-records/${id}`);
}

export function submitExperimentRecord(id: number, version: number) {
  return apiPost<ExperimentRecordDetailApi>(`/experiment-records/${id}/submit`, { version });
}

export function archiveExperimentRecord(id: number, version: number, archiveComment?: string) {
  return apiPost<ExperimentRecordDetailApi>(`/experiment-records/${id}/archive`, {
    version,
    archiveComment,
  });
}

export function gradeExperimentRecord(
  id: number,
  payload: { version: number; score: number; gradeComment?: string },
) {
  return apiPost<ExperimentRecordDetailApi>(`/experiment-records/${id}/grade`, payload);
}

export function listExperimentAttachments(id: number) {
  return apiGet<ExperimentAttachmentApi[]>(`/experiment-records/${id}/attachments`).then((files) =>
    files.map((file) => ({ ...file, fileUrl: toBrowserFileUrl(file.fileUrl) })),
  );
}

export function bindExperimentAttachment(id: number, fileId: number, fileUsage: "attachment" | "image" = "attachment") {
  return apiPost<void>(`/experiment-records/${id}/attachments`, { fileId, fileUsage });
}

export function unbindExperimentAttachment(id: number, fileId: number) {
  return apiDelete<void>(`/experiment-records/${id}/attachments/${fileId}`);
}

export function uploadExperimentAttachment(file: File, id?: number) {
  return uploadFile(file, id ? {
    bizType: "edu_experiment_record",
    bizId: id,
    fileUsage: "attachment",
  } : undefined);
}
