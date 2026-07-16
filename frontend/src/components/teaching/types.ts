export type TeachingTabKey = "course" | "research" | "training";

export type TeachingStatus =
  | "published"
  | "draft"
  | "offline"
  | "ongoing"
  | "planning"
  | "suspended"
  | "completed"
  | "applying";

export interface CourseExperimentStep {
  id?: number;
  stepNo?: string;
  title: string;
  description: string;
  duration?: string;
  expectedResult?: string;
  version?: number;
}

export interface CourseResourceItem {
  id?: number;
  fileId?: number;
  name: string;
  type: string;
  size?: string;
  url?: string;
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
  videoUrl?: string;
}

export interface CourseRecord {
  id: string;
  courseNo: string;
  courseName: string;
  category: string;
  subject: string;
  teacher: string;
  teacherId?: number;
  term: string;
  status: "published" | "draft" | "offline";
  updatedAt: string;
  thumbnail: string;
  description: string;
  version?: number;
  createdBy?: number;
  publisher?: string;
  detail: CourseDetailData;
  enrollmentStatus?: "enrolled" | "available" | "closed";
  score?: number;
}

export interface ResearchRecord {
  id: string;
  projectNo: string;
  projectName: string;
  leader: string;
  period: string;
  leaderId?: number;
  status: "planning" | "ongoing" | "suspended" | "completed" | "applying";
  updatedAt: string;
  version?: number;
  detail?: ResearchDetailData;
}

export interface ResearchDetailData {
  projectType: string;
  description: string;
  speciesId?: number;
  speciesName?: string;
  leaderId: number;
  members: Array<{ id: number; userId: number; username?: string; realName?: string; memberRole: string; memberStatus: string }>;
  materials: Array<{ bindingId: number; fileId: number; fileName?: string; fileUsage: string; fileUrl?: string }>;
  achievements: Array<{ id: number; achievementNo: string; achievementName: string; achievementType: string; achievementStage?: string; achievementStatus: string; version: number; fileName?: string }>;
}
