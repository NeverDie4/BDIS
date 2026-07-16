export type ApplicationStatus = "draft" | "pending" | "reviewing" | "approved" | "returned";

export type EvaluationTabKey = "workspace" | "tasks" | "indicators" | "archives";

export interface EvaluationTaskOption {
  id: string;
  taskNo: string;
  taskName: string;
  status: string;
  dueDate: string;
}

export interface EvaluationIndicator {
  id: string;
  name: string;
  score: number;
  weight: number;
}

export interface EvaluationResult {
  id: string;
  objectName: string;
  score: number;
  level: string;
  completedAt: string;
}

export interface EvaluationSummary {
  taskTotal: number;
  taskNewCount: number;
  ongoingTaskCount: number;
  nearDeadlineTaskCount: number;
  applicationTotal: number;
  draftCount: number;
  pendingCount: number;
  longestWaitingDays: number;
}

export interface MaterialCompletenessRule {
  key: string;
  label: string;
  required: boolean;
}

export interface EvaluationApplicantOption {
  id: string;
  name: string;
}

export interface ApplicationFilters {
  keyword: string;
  taskId?: string;
  applicationType?: string;
  applicantId?: string;
  status?: ApplicationStatus;
  submittedDateRange?: [string, string];
}

export interface ApplicationSourceSummary {
  herbArchiveCount: number;
  growthRecordCount: number;
  atlasCount: number;
  researchMaterialCount: number;
  evaluationResultCount: number;
}

export interface EvaluationAttachment {
  id: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  materialType: string;
  uploadedBy: string;
  uploadedAt: string;
}

export interface EvaluationReviewRecord {
  id: string;
  action: string;
  fromStatus?: ApplicationStatus;
  toStatus: ApplicationStatus;
  reviewerName: string;
  opinion?: string;
  reviewedAt: string;
}

export interface MaterialCompletenessItem {
  key: string;
  label: string;
  required: boolean;
  completed: boolean;
  missingCount?: number;
}

export interface EvaluationApplication {
  id: string;
  applicationNo: string;
  title: string;
  applicationType: string;
  taskId: string;
  taskName: string;
  applicantId: string;
  applicantName: string;
  organizationName: string;
  contactName: string;
  contactPhone: string;
  materialCount: number;
  status: ApplicationStatus;
  submittedAt?: string;
  reviewerName?: string;
  description?: string;
  coverImage?: string;
  sourceSummary: ApplicationSourceSummary;
  attachments: EvaluationAttachment[];
  reviewRecords: EvaluationReviewRecord[];
  completenessItems: MaterialCompletenessItem[];
}
