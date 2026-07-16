import { apiDelete, apiGet, apiPost, apiPut, getApiErrorMessage } from "@/lib/request";
import { toBrowserFileUrl } from "@/lib/files";
import type { PageResult } from "@/types/api";
import type { CourseRecord } from "@/components/teaching/types";

export type CoursePublishStatus = "published" | "draft" | "offline";

export type CourseListApi = {
  id: number;
  courseNo: string;
  courseName: string;
  courseType: string;
  teacherId: number;
  teacherName?: string;
  publishStatus: CoursePublishStatus;
  publishedAt?: string;
  publishedBy?: number;
  publisherName?: string;
  createdBy?: number;
  version?: number;
  startedAt?: string;
  endedAt?: string;
  status?: number;
  updatedAt?: string;
};

export type CourseStepApi = {
  id: number;
  courseId: number;
  stepNo: string;
  stepTitle: string;
  stepContent?: string;
  expectedResult?: string;
  sortOrder?: number;
  version: number;
};

export type CourseResourceApi = {
  id: number;
  courseId: number;
  resourceName: string;
  resourceType?: string;
  fileId: number;
  fileName?: string;
  originalFilename?: string;
  fileType?: string;
  fileFormat?: string;
  fileSize?: number;
  fileUrl?: string;
  sortOrder?: number;
  status?: number;
};

export type CourseDetailApi = CourseListApi & {
  description?: string;
  videoUrl?: string;
  applicableMajors?: string[];
  hours?: number;
  credits?: number;
  prerequisites?: string[];
  prerequisiteCourseIds?: number[];
  teachingObjectives?: string[];
  teachingMethods?: string[];
  tags?: string[];
  publishedAt?: string;
  publishedBy?: number;
  remark?: string;
  createdAt?: string;
  createdBy?: number;
  version: number;
  steps: CourseStepApi[];
  resources: CourseResourceApi[];
  relatedHerbs?: CourseRelationOptionApi[];
  relatedProjects?: CourseRelationOptionApi[];
};

export type CourseRelationOptionApi = { id: number; code: string; name: string };
export type CourseRelationOptionsApi = { herbs: CourseRelationOptionApi[]; projects: CourseRelationOptionApi[] };

export type CoursePayload = {
  courseNo: string;
  courseName: string;
  courseType: string;
  teacherId: number;
  description?: string;
  videoUrl?: string;
  applicableMajors?: string[];
  hours?: number;
  credits?: number;
  prerequisites?: string[];
  prerequisiteCourseIds?: number[];
  teachingObjectives?: string[];
  teachingMethods?: string[];
  tags?: string[];
  startedAt?: string;
  endedAt?: string;
  remark?: string;
};

export type CourseUpdatePayload = CoursePayload & { version: number };

export type CourseStepPayload = {
  stepNo: string;
  stepTitle: string;
  stepContent?: string;
  expectedResult?: string;
  sortOrder?: number;
};

export type CourseResourcePayload = {
  fileId: number;
  resourceName: string;
  resourceType?: string;
  sortOrder?: number;
};

export async function listCourses(params?: Record<string, unknown>) {
  return apiGet<PageResult<CourseListApi>>("/courses", {
    pageNo: 1,
    pageSize: 50,
    ...params,
  });
}

export function getCourse(courseId: number) {
  return apiGet<CourseDetailApi>(`/courses/${courseId}`);
}

export function createCourse(payload: CoursePayload) {
  return apiPost<CourseDetailApi>("/courses", payload);
}

export function updateCourse(courseId: number, payload: CourseUpdatePayload) {
  return apiPut<CourseDetailApi>(`/courses/${courseId}`, payload);
}

export function getCourseRelationOptions(courseId: number) {
  return apiGet<CourseRelationOptionsApi>(`/courses/${courseId}/relation-options`);
}

export function updateCourseRelations(courseId: number, payload: { version: number; speciesIds: number[]; projectIds: number[] }) {
  return apiPut<CourseDetailApi>(`/courses/${courseId}/relations`, payload);
}

export function deleteCourse(courseId: number) {
  return apiDelete<void>(`/courses/${courseId}`);
}

export function publishCourse(courseId: number, version: number) {
  return apiPost<void>(`/courses/${courseId}/publish`, { version });
}

export function offlineCourse(courseId: number, version: number) {
  return apiPost<void>(`/courses/${courseId}/offline`, { version });
}

export type CourseEnrollmentApi = { id: number; courseId: number; enrollmentStatus: string; progress: number; score?: number };
export function enrollCourse(courseId: number) { return apiPost<CourseEnrollmentApi>(`/courses/${courseId}/enrollment`); }
export function listMyCourseEnrollments() { return apiGet<CourseEnrollmentApi[]>('/courses/enrollments'); }
export function saveCourseLearningProgress(courseId: number, payload: { itemType: 'step' | 'resource' | 'video'; itemId: number; progressValue?: number; progressSeconds?: number; totalSeconds?: number; completed: boolean }) { return apiPut(`/courses/${courseId}/learning`, payload); }
export function getCourseLearningSummary(courseId: number) { return apiGet(`/courses/${courseId}/learning/summary`); }

export function createCourseStep(courseId: number, payload: CourseStepPayload) {
  return apiPost<CourseStepApi>(`/courses/${courseId}/steps`, payload);
}

export function updateCourseStep(courseId: number, stepId: number, payload: CourseStepPayload & { version: number }) {
  return apiPut<CourseStepApi>(`/courses/${courseId}/steps/${stepId}`, payload);
}

export function deleteCourseStep(courseId: number, stepId: number) {
  return apiDelete<void>(`/courses/${courseId}/steps/${stepId}`);
}

export function bindCourseResource(courseId: number, payload: CourseResourcePayload) {
  return apiPost<CourseResourceApi>(`/courses/${courseId}/resources`, payload);
}

export function deleteCourseResource(courseId: number, resourceId: number) {
  return apiDelete<void>(`/courses/${courseId}/resources/${resourceId}`);
}

function formatFileSize(bytes?: number) {
  if (!bytes) return "";
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export function mapCourseList(course: CourseListApi): CourseRecord {
  return {
    id: String(course.id),
    courseNo: course.courseNo,
    courseName: course.courseName,
    category: course.courseType,
    subject: course.courseType,
    teacher: course.teacherName ?? String(course.teacherId),
    teacherId: course.teacherId,
    term: course.startedAt ? course.startedAt.slice(0, 7) : "",
    status: course.publishStatus,
    updatedAt: course.updatedAt ?? "",
    thumbnail: "/images/herbs/showcase/huangqi.png",
    description: "",
    version: course.version ?? 0,
    publisher: course.publisherName ?? (course.publishedBy ? String(course.publishedBy) : ""),
    createdBy: course.createdBy,
    detail: {
      applicableMajors: [],
      hours: 0,
      credits: 0,
      prerequisites: [],
      prerequisiteCourseIds: [],
      teachingObjectives: [],
      teachingMethods: [],
      publishedAt: "",
      tags: [],
      experimentSteps: [],
      resources: [],
      videos: [],
      relatedHerbs: [],
      relatedProjects: [],
      relatedHerbItems: [],
      relatedProjectItems: [],
      relatedCollections: [],
      experimentRecords: [],
      videoUrl: "",
    },
  };
}

export function mapCourseDetail(course: CourseDetailApi): CourseRecord {
  const record = mapCourseList(course);
  return {
    ...record,
    description: course.description ?? "",
    version: course.version,
    detail: {
      ...record.detail,
      applicableMajors: course.applicableMajors ?? [],
      hours: course.hours ?? 0,
      credits: course.credits ?? 0,
      prerequisites: course.prerequisites ?? [],
      prerequisiteCourseIds: course.prerequisiteCourseIds ?? [],
      teachingObjectives: course.teachingObjectives ?? [],
      teachingMethods: course.teachingMethods ?? [],
      tags: course.tags ?? [],
      videoUrl: course.videoUrl ? toBrowserFileUrl(course.videoUrl) : "",
      publishedAt: course.publishedAt ?? "尚未发布",
      experimentSteps: course.steps.map((step) => ({
        id: step.id,
        stepNo: step.stepNo,
        title: step.stepTitle,
        description: step.stepContent ?? "",
        expectedResult: step.expectedResult,
        version: step.version,
      })),
      resources: course.resources.filter((resource) => resource.resourceType !== "video").map((resource) => ({
        id: resource.id,
        fileId: resource.fileId,
        name: resource.resourceName,
        type: resource.resourceType ?? "课程资源",
        size: formatFileSize(resource.fileSize),
        url: resource.fileUrl,
      })),
      videos: [
        ...(course.videoUrl ? [{ title: "课程主视频", url: toBrowserFileUrl(course.videoUrl) }] : []),
        ...course.resources.filter((resource) => resource.resourceType === "video").map((resource) => ({
          id: resource.id,
          title: resource.resourceName,
          url: resource.fileUrl ? toBrowserFileUrl(resource.fileUrl) : undefined,
          size: formatFileSize(resource.fileSize),
          speaker: "课程视频",
          duration: formatFileSize(resource.fileSize),
        })),
      ],
      relatedHerbs: (course.relatedHerbs ?? []).map((item) => item.name),
      relatedProjects: (course.relatedProjects ?? []).map((item) => `${item.code} · ${item.name}`),
      relatedHerbItems: course.relatedHerbs ?? [],
      relatedProjectItems: course.relatedProjects ?? [],
    },
  };
}

export { getApiErrorMessage };
