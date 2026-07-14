import { apiDelete, apiGet, apiPost, apiPut, getApiErrorMessage } from "@/lib/request";
import type { PageResult } from "@/types/api";
import type { ResearchRecord } from "@/components/teaching/types";
import { toBrowserFileUrl } from "@/lib/files";

export type ResearchProjectApi = {
  id: number;
  projectNo: string;
  projectName: string;
  projectType: string;
  leaderId: number;
  leaderName?: string;
  speciesId?: number;
  speciesName?: string;
  description?: string;
  projectStatus: "planning" | "ongoing" | "suspended" | "completed";
  startedAt?: string;
  endedAt?: string;
  status?: number;
  remark?: string;
  updatedAt?: string;
  version: number;
};

export type ResearchMemberApi = {
  id: number;
  projectId: number;
  userId: number;
  username?: string;
  realName?: string;
  memberRole: string;
  memberStatus: string;
  joinedAt?: string;
  leftAt?: string;
  remark?: string;
};

export type ResearchMaterialApi = {
  bindingId: number;
  projectId: number;
  fileId: number;
  fileName?: string;
  originalFilename?: string;
  fileType?: string;
  fileFormat?: string;
  fileSize?: number;
  fileUrl?: string;
  fileUsage: string;
  remark?: string;
  createdAt?: string;
};

export type ResearchAchievementApi = {
  id: number;
  achievementNo: string;
  projectId: number;
  projectName?: string;
  achievementName: string;
  achievementType: string;
  achievementStage?: string;
  achievementStatus: "draft" | "submitted" | "confirmed";
  description?: string;
  fileId?: number;
  fileName?: string;
  publishedAt?: string;
  version: number;
};

export type ResearchProjectDetailApi = ResearchProjectApi & {
  members: ResearchMemberApi[];
  materials: ResearchMaterialApi[];
  achievements: ResearchAchievementApi[];
};

export type ResearchUserCandidateApi = { id: number; username: string; realName?: string; userType?: string; status?: number };

export type ResearchProjectPayload = {
  projectNo: string;
  projectName: string;
  projectType: string;
  leaderId: number;
  speciesId?: number;
  description?: string;
  startedAt?: string;
  endedAt?: string;
  remark?: string;
};

export type ResearchProjectUpdatePayload = Omit<ResearchProjectPayload, "projectNo" | "leaderId"> & { version: number };

export function listResearchProjects(params?: Record<string, unknown>) {
  return apiGet<PageResult<ResearchProjectApi>>("/research-projects", { pageNo: 1, pageSize: 50, ...params });
}

export function getResearchProject(id: number) {
  return apiGet<ResearchProjectDetailApi>(`/research-projects/${id}`);
}

export function createResearchProject(payload: ResearchProjectPayload) {
  return apiPost<ResearchProjectDetailApi>("/research-projects", payload);
}

export function updateResearchProject(id: number, payload: ResearchProjectUpdatePayload) {
  return apiPut<ResearchProjectDetailApi>(`/research-projects/${id}`, payload);
}

export function changeResearchLeader(id: number, payload: { newLeaderId: number; reason: string; oldLeaderRole?: string; version: number }) {
  return apiPost<ResearchProjectDetailApi>(`/research-projects/${id}/leader`, payload);
}

export function changeResearchStatus(id: number, payload: { targetStatus: string; reason?: string; version: number }) {
  return apiPost<ResearchProjectDetailApi>(`/research-projects/${id}/status`, payload);
}

export function listResearchMembers(projectId: number) {
  return apiGet<ResearchMemberApi[]>(`/research-projects/${projectId}/members`);
}

export function addResearchMember(projectId: number, payload: { userId: number; memberRole: string; remark?: string }) {
  return apiPost<ResearchMemberApi>(`/research-projects/${projectId}/members`, payload);
}

export function updateResearchMember(projectId: number, userId: number, payload: { memberRole: string; remark?: string }) {
  return apiPut<ResearchMemberApi>(`/research-projects/${projectId}/members/${userId}`, payload);
}

export function removeResearchMember(projectId: number, userId: number) {
  return apiDelete<void>(`/research-projects/${projectId}/members/${userId}`);
}

export function bindResearchMaterial(projectId: number, payload: { fileId: number; fileUsage: string; remark?: string }) {
  return apiPost<number>(`/research-projects/${projectId}/materials`, payload);
}

export function removeResearchMaterial(projectId: number, fileId: number) {
  return apiDelete<void>(`/research-projects/${projectId}/materials/${fileId}`);
}

export function createResearchAchievement(payload: { achievementNo: string; projectId: number; achievementName: string; achievementType: string; achievementStage?: string; description?: string; fileId?: number }) {
  return apiPost<ResearchAchievementApi>("/research-achievements", payload);
}

export function updateResearchAchievement(id: number, payload: { achievementName: string; achievementType: string; achievementStage?: string; achievementStatus: string; description?: string; fileId?: number; version: number }) {
  return apiPut<ResearchAchievementApi>(`/research-achievements/${id}`, payload);
}

export function listResearchUsers() {
  return apiGet<ResearchUserCandidateApi[]>("/research-projects/candidate-users");
}

export function mapResearchProject(project: ResearchProjectApi): ResearchRecord {
  return {
    id: String(project.id),
    projectNo: project.projectNo,
    projectName: project.projectName,
    leader: project.leaderName ?? String(project.leaderId),
    leaderId: project.leaderId,
    period: `${project.startedAt?.slice(0, 7) ?? ""}—${project.endedAt?.slice(0, 7) ?? ""}`,
    status: project.projectStatus,
    updatedAt: project.updatedAt ?? "",
    version: project.version,
    detail: undefined,
  };
}

export function mapResearchDetail(project: ResearchProjectDetailApi): ResearchRecord {
  return {
    ...mapResearchProject(project),
    detail: {
      projectType: project.projectType,
      description: project.description ?? "",
      speciesId: project.speciesId,
      speciesName: project.speciesName,
      leaderId: project.leaderId,
      members: project.members,
      materials: project.materials.map((item) => ({ ...item, fileUrl: item.fileUrl ? toBrowserFileUrl(item.fileUrl) : undefined })),
      achievements: project.achievements,
    },
  };
}

export { getApiErrorMessage };
