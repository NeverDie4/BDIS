import { Tag } from "antd";

const statusLabels: Record<string, { label: string; color: string }> = {
  draft: { label: "草稿", color: "default" },
  scoring: { label: "评分中", color: "processing" },
  confirmed: { label: "已确认", color: "success" },
  submitted: { label: "待审核", color: "processing" },
  approved: { label: "已通过", color: "success" },
  rejected: { label: "已退回", color: "error" },
  archived: { label: "已归档", color: "purple" },
};

export function EvaluationStatus({ value }: { value?: string }) {
  const status = statusLabels[value ?? ""] ?? { label: value || "未知", color: "default" };
  return <Tag color={status.color}>{status.label}</Tag>;
}

export function formatDateTime(value?: string) {
  return value ? value.replace("T", " ").slice(0, 16) : "-";
}

export function resultLevelLabel(value?: string) {
  return (
    {
      excellent: "优秀",
      good: "良好",
      qualified: "合格",
      unqualified: "不合格",
    }[value ?? ""] ??
    value ??
    "-"
  );
}

export const targetTypeLabels: Record<string, string> = {
  herb: "药材品种",
  herb_species: "药材品种",
  herb_growth_record: "生长记录",
  application: "申报档案",
  eval_application: "申报档案",
  performance: "业绩记录",
  perf_record: "业绩记录",
};

export const applicationTypeOptions = [
  { value: "intangible_heritage", label: "非物质文化遗产" },
  { value: "geographical_indication", label: "地理标志" },
  { value: "quality_certification", label: "质量认证" },
  { value: "research_project", label: "科研项目" },
];

export function applicationTypeLabel(value?: string) {
  return applicationTypeOptions.find((item) => item.value === value)?.label ?? value ?? "未分类";
}
