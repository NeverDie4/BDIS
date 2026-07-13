import { Button } from "antd";
import { ActionToolbar } from "@/components/common/ActionToolbar";
import { ApplicationFilterPanel } from "./ApplicationFilterPanel";
import { ApplicationTable } from "./ApplicationTable";
import type { ApplicationFilters, EvaluationApplication, EvaluationApplicantOption, EvaluationTaskOption } from "./types";
import { applicationStatusMeta } from "./utils";
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
  const selected = selectedApplications.length === 1 ? selectedApplications[0] : undefined;
  const editEnabled = Boolean(selected && applicationStatusMeta[selected.status].canEdit);
  const submitEnabled = Boolean(selected && applicationStatusMeta[selected.status].canSubmit);
  const reviewEnabled = Boolean(selected && applicationStatusMeta[selected.status].canReview);
  const hasSelection = selectedApplications.length > 0;

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
            <Button disabled={!editEnabled} title={editEnabled ? undefined : "请选择一条可编辑记录"}>编辑</Button>
            <Button disabled={!submitEnabled} title={submitEnabled ? undefined : "请选择一条草稿或已退回记录"}>提交申报</Button>
            <Button disabled={!reviewEnabled} title={reviewEnabled ? undefined : "请选择一条待审核记录"}>审核</Button>
            <Button disabled={!hasSelection} title={hasSelection ? undefined : "请先选择记录"}>导出</Button>
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
