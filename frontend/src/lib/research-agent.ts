"use client";

import type { PageResult } from "@/types/api";
import { apiGet, apiPost, apiPut } from "./request";

export type AgentStatus =
  | "CREATED" | "PLANNING" | "RUNNING" | "WAITING_CONFIRMATION"
  | "WAITING_FIELD_DATA" | "REANALYZING" | "COMPLETED" | "FAILED" | "CANCELLED";

export type AgentTarget = { type?: string; id?: number; name?: string };

export type AgentTaskSummary = {
  id: number;
  taskNo: string;
  goalType: string;
  goalText: string;
  status: AgentStatus;
  statusLabel: string;
  currentPhase?: string;
  progressPercent: number;
  target?: AgentTarget;
  createTime: string;
  updateTime?: string;
};

export type AgentStep = {
  id: number;
  stepNo: number;
  stepType: string;
  stepName: string;
  description?: string;
  status: "PENDING" | "RUNNING" | "WAITING" | "SUCCEEDED" | "FAILED" | "SKIPPED" | "CANCELLED";
  statusLabel: string;
  outputSummary?: string;
  errorCode?: string;
  errorMessage?: string;
  retryCount: number;
  maxRetryCount: number;
  startTime?: string;
  finishTime?: string;
  createTime: string;
};

export type AgentFinding = {
  id: number;
  stepId?: number;
  findingType: string;
  severity: "INFO" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  targetType?: string;
  targetId?: number;
  title: string;
  description?: string;
  suggestion?: string;
  status: "OPEN" | "ACKNOWLEDGED" | "RESOLVED" | "IGNORED";
  resolvedTime?: string;
  createTime: string;
};

export type AgentAction = {
  id: number;
  stepId?: number;
  actionType: string;
  targetType?: string;
  targetId?: number;
  actionName: string;
  actionDescription?: string;
  riskLevel: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  needConfirm: boolean;
  status: string;
  requestedTime: string;
  resultSummary?: string;
  errorMessage?: string;
};

export type AgentTaskDetail = AgentTaskSummary & {
  resultSummary?: string;
  errorCode?: string;
  errorMessage?: string;
  startTime?: string;
  finishTime?: string;
  cancelTime?: string;
  steps: AgentStep[];
  findings: AgentFinding[];
  pendingActions: AgentAction[];
};

export type RequiredMetric = {
  metricCode: string;
  metricName: string;
  required: boolean;
  reason?: string;
  inputHint?: string;
  unit?: string;
};

export type RequiredImage = {
  imageType: string;
  imageTypeName: string;
  minCount: number;
  shootingGuidance?: string;
  reason?: string;
};

export type FollowUpPlan = {
  objective: string;
  recommendedTimeType?: string;
  recommendedAfterDays?: number;
  recommendedStartTime?: string;
  recommendedEndTime?: string;
  requiredMetrics: RequiredMetric[];
  requiredImages: RequiredImage[];
  optionalItems: string[];
  completionCriteria: string[];
  sourceFindingIds: number[];
  rationale: string;
  uncertainty?: string;
  priority?: string;
};

export type AgentCollectionPlan = {
  id: number;
  planNo: string;
  agentTaskId: number;
  researchRound: number;
  collectionTaskId: number;
  status: string;
  statusLabel: string;
  planSource: string;
  plan: FollowUpPlan;
  createTime: string;
  updateTime?: string;
};

export type AgentEvidenceAnalysis = {
  available: boolean;
  status: string;
  message?: string;
  analysis?: {
    agentTaskId: number;
    collectionTaskId: number;
    stageCount: number;
    stages: Array<Record<string, unknown>>;
    metricTrends: Array<Record<string, unknown>>;
    findings: Array<Record<string, unknown>>;
    evidenceGaps: Array<Record<string, unknown>>;
    explanation?: {
      conclusion?: string;
      confirmedFacts?: string[];
      possibleAssociations?: string[];
      uncertainties?: string[];
      evidenceGaps?: string[];
      followUpSuggestions?: string[];
      confidenceLevel?: string;
    };
    generatedTime?: string;
  };
};

export type AgentWaitStatus = {
  followUpTaskId?: number;
  followUpTaskName?: string;
  status: string;
  deadline?: string;
  completedRequirements: string[];
  missingRequirements: string[];
  progressPercent: number;
  lastCheckTime?: string;
  nextActionHint?: string;
};

export type AgentReanalysis = {
  analysisRoundId: number;
  roundNo: number;
  resolvedFindings: AgentFinding[];
  remainingFindings: AgentFinding[];
  newFindings: AgentFinding[];
  metricChanges: Array<{ metricCode: string; beforeValue?: number; afterValue?: number; difference?: number }>;
  imageEvidenceChanges: Record<string, number>;
  recognitionChanges: Record<string, number>;
  completenessScoreBefore: number;
  completenessScoreAfter: number;
  archiveReadinessBefore?: string;
  archiveReadinessAfter?: string;
  conclusion?: string;
  outcome?: string;
};

export type AgentAnalysisRound = {
  id: number;
  roundNo: number;
  roundType: string;
  conclusion?: string;
  outcome?: string;
  status: string;
  startTime?: string;
  finishTime?: string;
};

export type AgentArchiveStatus = {
  agentTaskId: number;
  status: string;
  statusLabel: string;
  completenessScore: number;
  requiredCompletenessScore: number;
  prerequisitesSatisfied: boolean;
  integrityVerified: boolean;
  waitingPublicConfirmation: boolean;
  pendingActionId?: number;
  message: string;
};

export type AgentArchiveResult = {
  agentTaskId: number;
  archiveId: number;
  archiveNo: string;
  traceCode?: string;
  publicUrl?: string;
  qrCodeUrl?: string;
  stageCount: number;
  imageCount: number;
  completenessScore: number;
  integrityVerified: boolean;
  rootHashShort?: string;
  hashVersion?: string;
  publicVisible: boolean;
  generatedTime?: string;
  completedCapabilities: string[];
  limitations: string[];
};

export type AgentActionExecution = {
  actionId: number;
  actionStatus: string;
  collectionPlanId?: number;
  collectionTaskId?: number;
  collectionTaskNo?: string;
  collectionTaskStatus?: string;
  message: string;
};

const base = "/herb/assistant/agent";

export function createAgentTask(data: {
  goalType?: string;
  goalText: string;
  targetType?: string;
  targetId: number;
  collectionTaskId: number;
  sessionId?: string;
  pageContext?: string;
}) {
  return apiPost<AgentTaskSummary>(`${base}/tasks`, {
    goalType: "DIGITAL_TWIN_RESEARCH",
    targetType: "COLLECTION_TASK",
    ...data,
  });
}

export function fetchAgentTasks(params: Record<string, unknown> = {}) {
  return apiGet<PageResult<AgentTaskSummary>>(`${base}/tasks`, params);
}

export function fetchAgentTask(id: number) {
  return apiGet<AgentTaskDetail>(`${base}/tasks/${id}`);
}

export function startAgentTask(id: number) {
  return apiPost(`${base}/tasks/${id}/start`);
}

export function cancelAgentTask(id: number, reason: string) {
  return apiPost<AgentTaskDetail>(`${base}/tasks/${id}/cancel`, { reason });
}

export function fetchAgentEvidence(id: number) {
  return apiGet<AgentEvidenceAnalysis>(`${base}/tasks/${id}/evidence-analysis`);
}

export function fetchAgentPlan(id: number) {
  return apiGet<AgentCollectionPlan>(`${base}/tasks/${id}/collection-plan`);
}

export function generateAgentPlan(id: number, regenerate = false) {
  return apiPost<AgentCollectionPlan>(`${base}/tasks/${id}/collection-plan/generate?regenerate=${regenerate}`);
}

export function updateAgentPlan(agentTaskId: number, planId: number, plan: FollowUpPlan) {
  return apiPut<AgentCollectionPlan>(`${base}/tasks/${agentTaskId}/collection-plan/${planId}`, {
    objective: plan.objective,
    recommendedStartTime: plan.recommendedStartTime,
    recommendedEndTime: plan.recommendedEndTime,
    requiredMetrics: plan.requiredMetrics,
    requiredImages: plan.requiredImages,
    completionCriteria: plan.completionCriteria,
    rationale: plan.rationale,
  });
}

export function confirmAgentAction(id: number, publishAfterCreate = false) {
  return apiPost<AgentActionExecution>(`${base}/actions/${id}/confirm`, {
    comment: "用户在科研 Agent 工作台确认执行",
    publishAfterCreate,
  });
}

export function rejectAgentAction(id: number, reason: string) {
  return apiPost<AgentActionExecution>(`${base}/actions/${id}/reject`, { reason });
}

export function fetchAgentWaitStatus(id: number) {
  return apiGet<AgentWaitStatus>(`${base}/tasks/${id}/wait-status`);
}

export function fetchAgentReanalysis(id: number) {
  return apiGet<AgentReanalysis>(`${base}/tasks/${id}/reanalysis`);
}

export function fetchAgentAnalysisRounds(id: number) {
  return apiGet<AgentAnalysisRound[]>(`${base}/tasks/${id}/analysis-rounds`);
}

export function prepareAgentArchive(id: number) {
  return apiPost<AgentArchiveResult>(`${base}/tasks/${id}/archive/prepare`);
}

export function fetchAgentArchiveStatus(id: number) {
  return apiGet<AgentArchiveStatus>(`${base}/tasks/${id}/archive/status`);
}

export function fetchAgentArchiveResult(id: number) {
  return apiGet<AgentArchiveResult>(`${base}/tasks/${id}/archive/result`);
}

export function agentPollInterval(status?: AgentStatus) {
  if (!status || ["COMPLETED", "FAILED", "CANCELLED"].includes(status)) return false;
  return status === "WAITING_FIELD_DATA" ? 30_000 : 5_000;
}
