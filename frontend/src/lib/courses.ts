import { apiGet, apiPost, request } from "@/lib/request";
import type { PageResult } from "@/types/api";

export type ExperimentStepApi = {
  id: number;
  courseId: number;
  stepNo: string;
  stepTitle: string;
  stepContent?: string;
  expectedResult?: string;
  sortOrder?: number;
  status?: number;
};

export type CourseResourceApi = {
  id: number;
  courseId: number;
  resourceName: string;
  resourceType?: string;
  fileId?: number;
  fileUrl?: string;
  fileSize?: number;
  fileFormat?: string;
  status?: number;
};

export type CourseApi = {
  id: number;
  courseNo: string;
  courseName: string;
  courseType?: string;
  teacherId?: number;
  teacherName?: string;
  description?: string;
  videoUrl?: string;
  publishStatus: "draft" | "published" | "archived";
  startedAt?: string;
  endedAt?: string;
  status?: number;
  updatedAt?: string;
  steps: ExperimentStepApi[];
  resources: CourseResourceApi[];
};

export type CoursePayload = {
  courseNo: string;
  courseName: string;
  courseType?: string;
  description?: string;
  videoUrl?: string;
  startedAt?: string;
  endedAt?: string;
  status?: number;
};

export function fetchCoursePage() {
  return apiGet<PageResult<CourseApi>>("/courses", { page: 1, size: 100 });
}

export function fetchCourseDetail(courseId: number) {
  return apiGet<CourseApi>(`/courses/${courseId}`);
}

export function createCourse(payload: CoursePayload) {
  return apiPost<number>("/courses", payload);
}

export function addCourseStep(
  courseId: number,
  payload: Pick<
    ExperimentStepApi,
    "stepNo" | "stepTitle" | "stepContent" | "expectedResult" | "sortOrder"
  >,
) {
  return apiPost<ExperimentStepApi>(`/courses/${courseId}/steps`, payload);
}

export function addCourseResource(
  courseId: number,
  payload: { resourceName: string; resourceType?: string; fileId: number },
) {
  return apiPost<CourseResourceApi>(`/courses/${courseId}/resources`, payload);
}

export async function changeCoursePublishStatus(
  courseId: number,
  status: CourseApi["publishStatus"],
) {
  const response = await request.patch(`/courses/${courseId}/publish-status`, undefined, {
    params: { status },
  });
  return response.data.data as CourseApi;
}

export async function downloadCourseResource(resource: CourseResourceApi) {
  if (!resource.fileId) return;
  const response = await request.get<Blob>(`/files/${resource.fileId}/content`, {
    params: { disposition: "attachment" },
    responseType: "blob",
  });
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `${resource.resourceName}${resource.fileFormat ? `.${resource.fileFormat}` : ""}`;
  anchor.click();
  URL.revokeObjectURL(url);
}
