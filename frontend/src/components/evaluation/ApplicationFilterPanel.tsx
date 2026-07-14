import { Button, DatePicker, Input, Select } from "antd";
import { DownloadOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { FilterPanel } from "@/components/common/FilterPanel";
import type { ApplicationFilters, ApplicationStatus, EvaluationApplicantOption, EvaluationTaskOption } from "./types";
import styles from "./evaluation.module.css";

type ApplicationFilterPanelProps = {
  value: ApplicationFilters;
  taskOptions: EvaluationTaskOption[];
  applicantOptions: EvaluationApplicantOption[];
  onChange: (filters: ApplicationFilters) => void;
  onSearch: () => void;
  onReset: () => void;
};

const statusOptions: Array<{ label: string; value: ApplicationStatus }> = [
  { label: "草稿", value: "draft" },
  { label: "待审核", value: "pending" },
  { label: "审核中", value: "reviewing" },
  { label: "已通过", value: "approved" },
  { label: "已退回", value: "returned" },
];

export function ApplicationFilterPanel({ value, taskOptions, applicantOptions, onChange, onSearch, onReset }: ApplicationFilterPanelProps) {
  const update = (patch: Partial<ApplicationFilters>) => onChange({ ...value, ...patch });
  const rangeValue = value.submittedDateRange?.map((date) => dayjs(date)) as [dayjs.Dayjs, dayjs.Dayjs] | undefined;

  return (
    <FilterPanel
      className={styles.filterPanel}
      variant="compact"
      onQuery={onSearch}
      onReset={onReset}
      title="申报档案筛选"
    >
      <div className={styles.filterGrid}>
        <Input placeholder="请输入标题、编号、申报人或任务" value={value.keyword} onChange={(event) => update({ keyword: event.target.value })} />
        <Select allowClear options={taskOptions.map((task) => ({ label: task.taskName, value: task.id }))} placeholder="评价任务" value={value.taskId} onChange={(taskId) => update({ taskId })} />
        <Select allowClear options={["非遗申报", "质量评价", "道地评价", "生态评价", "标准化评价"].map((item) => ({ label: item, value: item }))} placeholder="申报类型" value={value.applicationType} onChange={(applicationType) => update({ applicationType })} />
        <Select allowClear options={applicantOptions.map((applicant) => ({ label: applicant.name, value: applicant.id }))} placeholder="申报人" value={value.applicantId} onChange={(applicantId) => update({ applicantId })} />
        <Select allowClear options={statusOptions} placeholder="审核状态" value={value.status} onChange={(status) => update({ status })} />
        <DatePicker.RangePicker
          className={styles.dateRange}
          placeholder={["开始日期", "结束日期"]}
          value={rangeValue}
          onChange={(dates) => update({ submittedDateRange: dates?.[0] && dates?.[1] ? [dates[0].format("YYYY-MM-DD"), dates[1].format("YYYY-MM-DD")] : undefined })}
        />
        <Button className={styles.filterExport} disabled icon={<DownloadOutlined />} title="后续实现">导出</Button>
      </div>
    </FilterPanel>
  );
}
