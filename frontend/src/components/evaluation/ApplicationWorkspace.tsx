import { Button } from "antd";
import { ActionToolbar } from "@/components/common/ActionToolbar";
import { ApplicationFilterPanel } from "./ApplicationFilterPanel";
import { ApplicationTable } from "./ApplicationTable";
import type {
  ApplicationFilters,
  EvaluationApplication,
  EvaluationApplicantOption,
  EvaluationTaskOption,
} from "./types";
import styles from "./evaluation.module.css";

type ApplicationWorkspaceProps = {
  filters: ApplicationFilters;
  taskOptions: EvaluationTaskOption[];
  applicantOptions: EvaluationApplicantOption[];
  records: EvaluationApplication[];
  total: number;
  pageNo: number;
  pageSize: number;
  selectedRowKeys: React.Key[];
  selectedApplications: EvaluationApplication[];
  onFiltersChange: (filters: ApplicationFilters) => void;
  onSearch: () => void;
  onReset: () => void;
  onSelectionChange: (keys: React.Key[]) => void;
  onPageChange: (page: number, pageSize: number) => void;
  onView: (application: EvaluationApplication) => void;
};

export function ApplicationWorkspace({
  filters,
  taskOptions,
  applicantOptions,
  records,
  total,
  pageNo,
  pageSize,
  selectedRowKeys,
  selectedApplications,
  onFiltersChange,
  onSearch,
  onReset,
  onSelectionChange,
  onPageChange,
  onView,
}: ApplicationWorkspaceProps) {
  return (
    <section className={styles.workspace}>
      <ApplicationFilterPanel
        applicantOptions={applicantOptions}
        onChange={onFiltersChange}
        onReset={onReset}
        onSearch={onSearch}
        taskOptions={taskOptions}
        value={filters}
      />
      <ActionToolbar
        variant="compact"
        actions={
          <>
            <Button disabled type="primary" title="后续实现">新建申报</Button>
            <Button disabled title="后续实现">编辑</Button>
            <Button disabled title="后续实现">提交申报</Button>
            <Button disabled title="后续实现">审核</Button>
            <Button disabled title="后续实现">导出</Button>
          </>
        }
        description={`申报档案结构占位，当前已选择 ${selectedApplications.length} 条记录。`}
        title="申报档案"
      />
      <ApplicationTable
        onPageChange={onPageChange}
        onSelectionChange={onSelectionChange}
        onView={onView}
        pageNo={pageNo}
        pageSize={pageSize}
        records={records}
        selectedRowKeys={selectedRowKeys}
        total={total}
      />
    </section>
  );
}
