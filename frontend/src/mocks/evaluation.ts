import type {
  EvaluationApplication,
  EvaluationApplicantOption,
  EvaluationIndicator,
  EvaluationResult,
  EvaluationTaskOption,
  MaterialCompletenessRule,
} from "@/components/evaluation/types";

export const evaluationTasks: EvaluationTaskOption[] = [
  { id: "task-huangqi", taskNo: "EVAL-TASK-001", taskName: "黄芪品质综合评价", status: "进行中", dueDate: "2024-06-15" },
  { id: "task-danshen", taskNo: "EVAL-TASK-002", taskName: "丹参质量评价", status: "进行中", dueDate: "2024-06-12" },
  { id: "task-danggui", taskNo: "EVAL-TASK-003", taskName: "当归道地评价", status: "已完成", dueDate: "2024-05-20" },
  { id: "task-dangshen", taskNo: "EVAL-TASK-004", taskName: "党参综合评价", status: "已完成", dueDate: "2024-05-18" },
  { id: "task-gouqi", taskNo: "EVAL-TASK-005", taskName: "枸杞生态评价", status: "进行中", dueDate: "2024-06-20" },
];

export const evaluationApplicants: EvaluationApplicantOption[] = [
  { id: "applicant-li", name: "李明" },
  { id: "applicant-wang", name: "王芳" },
  { id: "applicant-liu", name: "刘洋" },
  { id: "applicant-chen", name: "陈露" },
  { id: "applicant-sun", name: "孙强" },
  { id: "applicant-zhou", name: "周磊" },
  { id: "applicant-zhao", name: "赵敏" },
];

export const evaluationIndicators: EvaluationIndicator[] = [
  { id: "indicator-authenticity", name: "道地性指标", score: 86, weight: 25 },
  { id: "indicator-safety", name: "安全性指标", score: 92, weight: 25 },
  { id: "indicator-efficacy", name: "有效性指标", score: 88, weight: 30 },
  { id: "indicator-stability", name: "质量稳定性指标", score: 85, weight: 20 },
];

export const evaluationResults: EvaluationResult[] = [
  { id: "result-001", objectName: "黄芪品质综合评价", score: 87.6, level: "优", completedAt: "2024-05-16" },
  { id: "result-002", objectName: "丹参质量评价", score: 84.3, level: "良", completedAt: "2024-05-15" },
  { id: "result-003", objectName: "当归道地评价", score: 91.2, level: "优", completedAt: "2024-05-14" },
];

export const materialCompletenessRules: MaterialCompletenessRule[] = [
  { key: "basic", label: "基本信息", required: true },
  { key: "evaluation", label: "评价结果", required: true },
  { key: "atlas", label: "图谱资料", required: true },
  { key: "research", label: "研究材料", required: false },
];

const applicationSeeds = [
  ["重庆道地黄芪非遗申报", "非遗申报", "task-huangqi", "李明", "draft"],
  ["川产丹参质量评价档案", "质量评价", "task-danshen", "王芳", "pending"],
  ["党参产地标准化评价申报", "标准化评价", "task-dangshen", "刘洋", "reviewing"],
  ["当归道地产地评价申报", "道地评价", "task-danggui", "陈露", "approved"],
  ["枸杞生态种植评价申报", "生态评价", "task-gouqi", "孙强", "returned"],
  ["石斛药材质量提升评价", "质量提升", "task-danshen", "周磊", "pending"],
  ["白术炮制工艺评价申报", "工艺评价", "task-huangqi", "赵敏", "approved"],
  ["金银花道地品质评价", "道地评价", "task-gouqi", "李明", "reviewing"],
  ["川贝母资源质量评价", "质量评价", "task-dangshen", "王芳", "draft"],
  ["天麻产地溯源评价申报", "溯源评价", "task-danggui", "刘洋", "pending"],
  ["麦冬生态种植评价", "生态评价", "task-gouqi", "陈露", "approved"],
  ["三七药材标准评价申报", "标准评价", "task-danshen", "孙强", "returned"],
  ["川芎质量稳定性评价", "稳定性评价", "task-huangqi", "周磊", "reviewing"],
  ["茯苓基地综合评价申报", "基地评价", "task-danggui", "赵敏", "pending"],
  ["黄连药材质量提升申报", "质量提升", "task-danshen", "李明", "approved"],
  ["菊花药材规范化评价", "规范化评价", "task-gouqi", "王芳", "draft"],
  ["川续断资源评价申报", "资源评价", "task-dangshen", "刘洋", "returned"],
  ["泽泻产地综合评价", "综合评价", "task-danggui", "陈露", "pending"],
  ["厚朴炮制质量评价", "工艺评价", "task-huangqi", "孙强", "reviewing"],
  ["半夏种植质量评价申报", "种植评价", "task-gouqi", "周磊", "approved"],
] as const;

const statusValues = ["draft", "pending", "reviewing", "approved", "returned"] as const;

function createApplication(seed: (typeof applicationSeeds)[number], index: number): EvaluationApplication {
  const [title, applicationType, taskId, applicantName, status] = seed;
  const task = evaluationTasks.find((item) => item.id === taskId) ?? evaluationTasks[0];
  const applicant = evaluationApplicants.find((item) => item.name === applicantName) ?? evaluationApplicants[0];
  const materialCount = 7 + (index * 3) % 9;
  const hasSubmission = status !== "draft";
  const attachmentCount = 2 + (index % 3);
  const attachments = Array.from({ length: attachmentCount }, (_, attachmentIndex) => ({
    id: `attachment-${index + 1}-${attachmentIndex + 1}`,
    fileName: `${title.slice(0, 6)}-${attachmentIndex + 1}.${attachmentIndex === 0 ? "pdf" : attachmentIndex === 1 ? "docx" : "zip"}`,
    fileType: attachmentIndex === 0 ? "PDF" : attachmentIndex === 1 ? "Word" : "压缩包",
    fileSize: 1.2 + attachmentIndex * 2.4 + index * 0.1,
    materialType: attachmentIndex === 0 ? "评价报告" : attachmentIndex === 1 ? "研究材料" : "附件资料",
    uploadedBy: applicantName,
    uploadedAt: `2024-05-${String(10 + (index % 10)).padStart(2, "0")} 09:30`,
  }));
  const completenessItems = materialCompletenessRules.map((rule, ruleIndex) => ({
    ...rule,
    completed: ruleIndex < 2 ? status !== "draft" || index % 2 === 0 : index % (ruleIndex + 2) !== 0,
    missingCount: ruleIndex === 2 && index % 3 === 0 ? 1 : undefined,
  }));
  const reviewRecords = status === "draft" ? [] : Array.from({ length: Math.min(4, 2 + (index % 3)) }, (_, recordIndex) => ({
    id: `review-${index + 1}-${recordIndex + 1}`,
    action: ["创建申报", "提交审核", "专家评分", "结果确认"][recordIndex],
    fromStatus: recordIndex === 0 ? undefined : statusValues[(statusValues.indexOf(status) + recordIndex - 1) % statusValues.length],
    toStatus: recordIndex === Math.min(3, 1 + (index % 3)) ? status : "pending",
    reviewerName: recordIndex === 0 ? applicantName : ["张华", "赵磊", "陈老师"][index % 3],
    opinion: recordIndex === 0 ? "已创建申报档案。" : recordIndex % 2 === 0 ? "材料已进入评审流程。" : "请继续补充相关证明材料。",
    reviewedAt: `2024-05-${String(12 + recordIndex + (index % 5)).padStart(2, "0")} ${String(9 + recordIndex).padStart(2, "0")}:20`,
  }));

  return {
    id: `evaluation-${String(index + 1).padStart(3, "0")}`,
    applicationNo: `EVAPP-202405-${String(index + 1).padStart(3, "0")}`,
    title,
    applicationType,
    taskId,
    taskName: task.taskName,
    applicantId: applicant.id,
    applicantName,
    organizationName: index % 2 === 0 ? "重庆本草研究院" : "西南中药材评价中心",
    contactName: applicantName,
    contactPhone: `138****${String(5678 + index).slice(-4)}`,
    materialCount,
    status,
    submittedAt: hasSubmission ? `2024-05-${String(10 + (index % 9)).padStart(2, "0")} ${String(9 + (index % 8)).padStart(2, "0")}:20` : undefined,
    reviewerName: hasSubmission ? ["张华", "赵磊", "陈老师"][index % 3] : undefined,
    description: `围绕${title}完成评价材料汇总、指标分析与申报档案整理。`,
    sourceSummary: {
      herbArchiveCount: 1 + (index % 4),
      growthRecordCount: 8 + (index % 7),
      atlasCount: index % 3 === 0 ? 0 : 2 + (index % 4),
      researchMaterialCount: 2 + (index % 5),
      evaluationResultCount: index % 4 === 0 ? 0 : 1 + (index % 2),
    },
    attachments,
    reviewRecords,
    completenessItems,
  };
}

export const evaluationApplications: EvaluationApplication[] = applicationSeeds.map(createApplication);
