import type {
  Course,
  CourseResource,
  ExperimentStep,
  ResearchProject,
  TrainingFeedback,
  TrainingPlan,
  TrainingRecord,
} from "@/types/teaching";

export const courses: Course[] = [
  {
    id: "course-001",
    courseNo: "COURSE-2026-001",
    courseName: "药用植物标本识别",
    teacher: "陈老师",
    department: "生物医药教研室",
    coverUrl: "/mock/courses/herb-identification.jpg",
    description: "围绕标本观察、形态特征和药用部位建立基础课程资源。",
    tags: ["标本识别", "药用部位", "教学演示"],
    status: "published",
    updatedAt: "2026-07-08 13:00:00",
  },
  {
    id: "course-002",
    courseNo: "COURSE-2026-002",
    courseName: "中药材生长环境采集",
    teacher: "李老师",
    department: "数字采集实训组",
    coverUrl: "/mock/courses/growth-collection.jpg",
    description: "训练采集人员记录温湿度、定位、图片和现场备注。",
    tags: ["移动采集", "环境指标", "生长记录"],
    status: "published",
    updatedAt: "2026-07-08 14:30:00",
  },
  {
    id: "course-003",
    courseNo: "COURSE-2026-003",
    courseName: "科研资料归档规范",
    teacher: "周老师",
    department: "科研管理办公室",
    coverUrl: "/mock/courses/archive-standard.jpg",
    description: "面向课题材料、成果附件和申报佐证材料的规范化归档。",
    tags: ["资料归档", "课题管理", "申报材料"],
    status: "draft",
    updatedAt: "2026-07-08 15:10:00",
  },
];

export const experimentSteps: ExperimentStep[] = [
  {
    id: "step-001",
    courseId: "course-001",
    stepNo: 1,
    title: "观察标本外形",
    description: "记录叶、花、根茎等关键形态特征。",
    requiredMaterials: ["标本夹", "放大镜", "记录表"],
  },
  {
    id: "step-002",
    courseId: "course-001",
    stepNo: 2,
    title: "比对药用部位",
    description: "结合课程图片和药材档案确认药用部位。",
    requiredMaterials: ["药材档案", "图像资料"],
  },
  {
    id: "step-003",
    courseId: "course-002",
    stepNo: 1,
    title: "填写环境指标",
    description: "记录温度、湿度、土壤 pH 和天气情况。",
    requiredMaterials: ["温湿度计", "土壤 pH 计", "移动采集页面"],
  },
];

export const courseResources: CourseResource[] = [
  {
    id: "res-001",
    courseId: "course-001",
    resourceName: "药用植物标本识别课件",
    resourceType: "课件",
    fileUrl: "/mock/resources/herb-identification.pptx",
    status: "published",
  },
  {
    id: "res-002",
    courseId: "course-002",
    resourceName: "移动采集操作演示视频",
    resourceType: "视频",
    fileUrl: "/mock/resources/mobile-collection.mp4",
    durationText: "08:36",
    status: "published",
  },
  {
    id: "res-003",
    courseId: "course-003",
    resourceName: "科研资料归档模板",
    resourceType: "实验指导书",
    fileUrl: "/mock/resources/archive-template.docx",
    status: "draft",
  },
];

export const videoResources: CourseResource[] = courseResources.filter(
  (resource) => resource.resourceType === "视频",
);

export const researchProjects: ResearchProject[] = [
  {
    id: "project-001",
    projectNo: "RP-2026-001",
    projectName: "重庆道地中药材数字化标本资源建设",
    leader: "陈老师",
    researchDirection: "中药材资源数字化",
    startDate: "2026-07-01",
    endDate: "2026-09-30",
    status: "pending",
    relatedHerbs: ["黄连", "党参", "金银花"],
    summary: "围绕重庆典型药材建立标本、图像、采集和课程资源关联。",
  },
  {
    id: "project-002",
    projectNo: "RP-2026-002",
    projectName: "山区药材生长环境观测数据采集研究",
    leader: "李老师",
    researchDirection: "生长环境与数据采集",
    startDate: "2026-07-05",
    status: "draft",
    relatedHerbs: ["党参", "厚朴"],
    summary: "利用移动 H5 采集页面沉淀温湿度、定位和形态指标数据。",
  },
];

export const trainingPlans: TrainingPlan[] = [
  {
    id: "plan-001",
    planNo: "TRAIN-2026-001",
    planName: "移动采集页面使用培训",
    targetGroup: "采集协作员",
    trainer: "系统管理员",
    startTime: "2026-07-10 09:00:00",
    endTime: "2026-07-10 11:00:00",
    status: "published",
    description: "讲解药材选择、定位、图片上传和暂存提交流程。",
  },
  {
    id: "plan-002",
    planNo: "TRAIN-2026-002",
    planName: "科研资料归档规范培训",
    targetGroup: "科研教师",
    trainer: "资料管理员",
    startTime: "2026-07-12 14:00:00",
    endTime: "2026-07-12 16:00:00",
    status: "draft",
    description: "围绕课题材料、成果附件和申报证明进行规范说明。",
  },
];

export const trainingRecords: TrainingRecord[] = [
  {
    id: "train-rec-001",
    planId: "plan-001",
    traineeName: "采集员A",
    department: "数字采集实训组",
    attendanceStatus: "已签到",
    score: 92,
    completedAt: "2026-07-10 11:00:00",
  },
  {
    id: "train-rec-002",
    planId: "plan-001",
    traineeName: "采集员B",
    department: "数字采集实训组",
    attendanceStatus: "已签到",
    score: 88,
    completedAt: "2026-07-10 11:00:00",
  },
];

export const trainingFeedback: TrainingFeedback[] = [
  {
    id: "feedback-001",
    planId: "plan-001",
    traineeName: "采集员A",
    rating: 5,
    comment: "移动采集表单字段清晰，图片上传入口容易理解。",
    submittedAt: "2026-07-10 11:20:00",
  },
  {
    id: "feedback-002",
    planId: "plan-001",
    traineeName: "采集员B",
    rating: 4,
    comment: "希望后续增加离线暂存状态提示。",
    submittedAt: "2026-07-10 11:25:00",
  },
];
