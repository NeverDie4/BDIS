"use client";

import { App } from "antd";
import { useCallback, useEffect, useState } from "react";
import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
import {
  fetchDeclarations,
  fetchEvaluationTasks,
  fetchIndicators,
  type Declaration,
  type EvaluationIndicator,
  type EvaluationTask,
} from "@/lib/evaluation";
import { getApiErrorMessage } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import { DeclarationWorkspace } from "./DeclarationWorkspace";
import { EvaluationModuleTabs } from "./EvaluationModuleTabs";
import { EvaluationTaskWorkspace } from "./EvaluationTaskWorkspace";
import { EvaluationWorkbench } from "./EvaluationWorkbench";
import { IndicatorWorkspace } from "./IndicatorWorkspace";
import type { EvaluationTabKey } from "./types";
import styles from "./evaluation.module.css";

export function EvaluationPageClient() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [activeTab, setActiveTab] = useState<EvaluationTabKey>("workspace");
  const [loading, setLoading] = useState(true);
  const [tasks, setTasks] = useState<EvaluationTask[]>([]);
  const [indicators, setIndicators] = useState<EvaluationIndicator[]>([]);
  const [declarations, setDeclarations] = useState<Declaration[]>([]);

  const loadWorkbench = useCallback(async () => {
    setLoading(true);
    try {
      const [taskPage, indicatorPage, declarationPage] = await Promise.all([
        hasPermission("evaluation:task:view")
          ? fetchEvaluationTasks({ pageNum: 1, pageSize: 200 })
          : Promise.resolve({ records: [] as EvaluationTask[] }),
        hasPermission("evaluation:standard:view")
          ? fetchIndicators({ pageNum: 1, pageSize: 200 })
          : Promise.resolve({ records: [] as EvaluationIndicator[] }),
        hasPermission("declaration:application:view")
          ? fetchDeclarations({ pageNum: 1, pageSize: 200 })
          : Promise.resolve({ records: [] as Declaration[] }),
      ]);
      setTasks(taskPage.records);
      setIndicators(indicatorPage.records);
      setDeclarations(declarationPage.records);
    } catch (error) {
      message.error(getApiErrorMessage(error, "评价工作台加载失败"));
    } finally {
      setLoading(false);
    }
  }, [hasPermission, message]);

  useEffect(() => {
    if (activeTab === "workspace") void loadWorkbench();
  }, [activeTab, loadWorkbench]);

  const navigate = (tab: EvaluationTabKey) => {
    const requiredPermission: Partial<Record<EvaluationTabKey, string>> = {
      tasks: "evaluation:task:view",
      indicators: "evaluation:standard:view",
      archives: "declaration:application:view",
    };
    const permission = requiredPermission[tab];
    if (permission && !hasPermission(permission)) {
      message.warning("当前账号没有访问该业务页的权限");
      return;
    }
    setActiveTab(tab);
  };

  return (
    <div className={styles.evaluationPage}>
      <ModuleHeroBanner
        variant="compact"
        description="统一管理评价指标、评价任务、评分结果与申报审核档案。"
        eyebrow="EVALUATION & APPLICATION"
        sealText="评审"
        title="评价申报"
      />
      <section className={styles.evaluationSurface}>
        <EvaluationModuleTabs activeKey={activeTab} onChange={navigate} />
        <div className={styles.tabContent}>
          {activeTab === "workspace" ? (
            <EvaluationWorkbench
              loading={loading}
              tasks={tasks}
              indicators={indicators}
              declarations={declarations}
              onNavigate={navigate}
            />
          ) : null}
          {activeTab === "tasks" ? <EvaluationTaskWorkspace /> : null}
          {activeTab === "indicators" ? <IndicatorWorkspace /> : null}
          {activeTab === "archives" ? <DeclarationWorkspace /> : null}
        </div>
      </section>
    </div>
  );
}
