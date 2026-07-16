import type { ID, StatusType } from "./common";

export type Course = {
  id: ID;
  courseNo: string;
  courseName: string;
  teacher: string;
  department: string;
  coverUrl?: string;
  description: string;
  tags: string[];
  status: StatusType;
  updatedAt: string;
};

export type CourseResource = {
  id: ID;
  courseId: ID;
  resourceName: string;
  resourceType: "课件" | "视频" | "实验指导书" | "图片资料" | "数据表";
  fileUrl?: string;
  durationText?: string;
  status: StatusType;
};

export type ExperimentStep = {
  id: ID;
  courseId: ID;
  stepNo: number;
  title: string;
  description: string;
  requiredMaterials: string[];
};

export type ResearchProject = {
  id: ID;
  projectNo: string;
  projectName: string;
  leader: string;
  researchDirection: string;
  startDate: string;
  endDate?: string;
  status: StatusType;
  relatedHerbs: string[];
  summary: string;
};

export type TrainingPlan = {
  id: ID;
  planNo: string;
  planName: string;
  targetGroup: string;
  trainer: string;
  startTime: string;
  endTime: string;
  status: StatusType;
  description: string;
};

export type TrainingRecord = {
  id: ID;
  planId: ID;
  traineeName: string;
  department: string;
  attendanceStatus: "已签到" | "缺勤" | "请假";
  score?: number;
  completedAt?: string;
};

export type TrainingFeedback = {
  id: ID;
  planId: ID;
  traineeName: string;
  rating: number;
  comment: string;
  submittedAt: string;
};
