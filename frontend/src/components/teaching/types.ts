export type TeachingTabKey = "course" | "research" | "training";

export type TeachingStatus =
  | "published"
  | "draft"
  | "offline"
  | "ongoing"
  | "completed"
  | "applying";

export interface CourseExperimentStep {
  title: string;
  description: string;
  duration: string;
}

export interface CourseResourceItem {
  name: string;
  type: string;
  size: string;
}

export interface CourseVideoItem {
  title: string;
  duration: string;
  speaker: string;
}

export interface CourseExperimentRecord {
  date: string;
  className: string;
  participantCount: number;
  completionRate: string;
}

export interface CourseDetailData {
  applicableMajors: string[];
  hours: number;
  credits: number;
  prerequisites: string[];
  teachingObjectives: string[];
  teachingMethods: string[];
  publishedAt: string;
  tags: string[];
  experimentSteps: CourseExperimentStep[];
  resources: CourseResourceItem[];
  videos: CourseVideoItem[];
  relatedHerbs: string[];
  relatedProjects: string[];
  relatedCollections: string[];
  experimentRecords: CourseExperimentRecord[];
}

export interface CourseRecord {
  id: string;
  courseNo: string;
  courseName: string;
  category: string;
  subject: string;
  teacher: string;
  term: string;
  status: "published" | "draft" | "offline";
  updatedAt: string;
  thumbnail: string;
  description: string;
  detail: CourseDetailData;
}

export interface ResearchRecord {
  id: string;
  projectNo: string;
  projectName: string;
  leader: string;
  period: string;
  status: "ongoing" | "completed" | "applying";
  updatedAt: string;
}
