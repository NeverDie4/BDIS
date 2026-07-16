import type { ApplicationFilters, ApplicationStatus, EvaluationApplication, MaterialCompletenessItem } from "./types";

export const applicationStatusMeta: Record<ApplicationStatus, {
  label: string;
  tagStatus: "draft" | "pending" | "approved" | "rejected";
  canEdit: boolean;
  canSubmit: boolean;
  canReview: boolean;
}> = {
  draft: { label: "草稿", tagStatus: "draft", canEdit: true, canSubmit: true, canReview: false },
  pending: { label: "待审核", tagStatus: "pending", canEdit: false, canSubmit: false, canReview: true },
  reviewing: { label: "审核中", tagStatus: "pending", canEdit: false, canSubmit: false, canReview: true },
  approved: { label: "已通过", tagStatus: "approved", canEdit: false, canSubmit: false, canReview: false },
  returned: { label: "已退回", tagStatus: "rejected", canEdit: true, canSubmit: true, canReview: false },
};

function normalize(value: string | undefined) {
  return value?.trim().toLocaleLowerCase() ?? "";
}

export function filterEvaluationApplications(records: EvaluationApplication[], filters: ApplicationFilters) {
  const keyword = normalize(filters.keyword);
  return records.filter((record) => {
    const keywordMatch = !keyword || [record.applicationNo, record.title, record.applicantName, record.taskName]
      .some((value) => normalize(value).includes(keyword));
    const taskMatch = !filters.taskId || record.taskId === filters.taskId;
    const typeMatch = !filters.applicationType || record.applicationType === filters.applicationType;
    const applicantMatch = !filters.applicantId || record.applicantId === filters.applicantId;
    const statusMatch = !filters.status || record.status === filters.status;
    const submittedDate = record.submittedAt?.slice(0, 10);
    const startDate = filters.submittedDateRange?.[0];
    const endDate = filters.submittedDateRange?.[1];
    const dateMatch = !startDate || !endDate || (Boolean(submittedDate) && submittedDate! >= startDate && submittedDate! <= endDate);
    return keywordMatch && taskMatch && typeMatch && applicantMatch && statusMatch && dateMatch;
  });
}

export function calculateMaterialCompleteness(items: MaterialCompletenessItem[]) {
  const requiredItems = items.filter((item) => item.required);
  const completedItems = requiredItems.filter((item) => item.completed);
  const missingItems = requiredItems.filter((item) => !item.completed);
  const percent = requiredItems.length === 0 ? 100 : Math.round((completedItems.length / requiredItems.length) * 100);
  return { percent, missingItems, missingCount: missingItems.length };
}

export function formatFileSize(sizeInMb: number) {
  return `${sizeInMb.toFixed(1)} MB`;
}
