"use client";

import { useEffect, useMemo, useState } from "react";
import type { Key } from "react";
import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
import {
  evaluationApplicants,
  evaluationApplications,
  evaluationIndicators,
  evaluationResults,
  evaluationTasks,
} from "@/mocks/evaluation";
import { ApplicationDetailDrawer } from "./ApplicationDetailDrawer";
import { ApplicationWorkspace } from "./ApplicationWorkspace";
import { EvaluationBottomGrid } from "./EvaluationBottomGrid";
import { EvaluationModuleTabs } from "./EvaluationModuleTabs";
import { EvaluationSummaryGrid } from "./EvaluationSummaryGrid";
import { EvaluationViewport } from "./EvaluationViewport";
import { buildEvaluationSummary } from "./summary";
import type { ApplicationFilters, EvaluationApplication, EvaluationTabKey } from "./types";
import { filterEvaluationApplications } from "./utils";
import styles from "./evaluation.module.css";

const emptyFilters: ApplicationFilters = { keyword: "" };

export function EvaluationPageClient() {
  const [activeTab, setActiveTab] = useState<EvaluationTabKey>("archives");
  const [draftFilters, setDraftFilters] = useState<ApplicationFilters>(emptyFilters);
  const [appliedFilters, setAppliedFilters] = useState<ApplicationFilters>(emptyFilters);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [selectedApplication, setSelectedApplication] = useState<EvaluationApplication | null>(null);

  const filteredRecords = useMemo(
    () => filterEvaluationApplications(evaluationApplications, appliedFilters),
    [appliedFilters],
  );
  const pagedRecords = useMemo(
    () => filteredRecords.slice((pageNo - 1) * pageSize, pageNo * pageSize),
    [filteredRecords, pageNo, pageSize],
  );
  const selectedApplications = useMemo(
    () => evaluationApplications.filter((application) => selectedRowKeys.includes(application.id)),
    [selectedRowKeys],
  );
  const summary = useMemo(() => buildEvaluationSummary(evaluationTasks, evaluationApplications), []);
  const recentResults = useMemo(
    () => [...evaluationResults].sort((left, right) => right.completedAt.localeCompare(left.completedAt)),
    [],
  );
  const bottomApplication = selectedApplication ?? undefined;

  useEffect(() => {
    const maxPage = Math.max(1, Math.ceil(filteredRecords.length / pageSize));
    if (pageNo > maxPage) setPageNo(maxPage);
  }, [filteredRecords.length, pageNo, pageSize]);

  const search = () => {
    setAppliedFilters({ ...draftFilters });
    setPageNo(1);
  };

  const reset = () => {
    setDraftFilters(emptyFilters);
    setAppliedFilters(emptyFilters);
    setPageNo(1);
  };

  return (
    <div className={styles.evaluationPage}>
      <ModuleHeroBanner
        description="围绕评价任务、指标体系、申报材料与审核流程，完成评价结果汇总与申报档案管理。"
        eyebrow="EVALUATION & APPLICATION"
        sealText="评审"
        title="评价申报"
      />
      <EvaluationSummaryGrid summary={summary} />
      <EvaluationViewport
        tabs={<EvaluationModuleTabs activeKey={activeTab} onChange={setActiveTab} />}
        main={activeTab === "archives" ? <ApplicationWorkspace
            applicantOptions={evaluationApplicants}
            filters={draftFilters}
            onFiltersChange={setDraftFilters}
            onPageChange={(nextPage, nextPageSize) => {
              setPageNo(nextPageSize !== pageSize ? 1 : nextPage);
              setPageSize(nextPageSize);
            }}
            onReset={reset}
            onSearch={search}
            onSelectionChange={setSelectedRowKeys}
            onView={setSelectedApplication}
            pageNo={pageNo}
            pageSize={pageSize}
            records={pagedRecords}
            selectedApplications={selectedApplications}
            selectedRowKeys={selectedRowKeys}
            taskOptions={evaluationTasks}
            total={filteredRecords.length}
          /> : <div className={styles.placeholderPanel}>当前模块将在后续轮次接入真实数据与业务流程。</div>}
        bottom={activeTab === "archives" ? <EvaluationBottomGrid application={bottomApplication} indicators={evaluationIndicators} results={recentResults} /> : null}
      />
      <ApplicationDetailDrawer application={selectedApplication} open={selectedApplication !== null} onClose={() => setSelectedApplication(null)} />
    </div>
  );
}
