import { Tabs } from "antd";
import { useAuthStore } from "@/stores/auth-store";
import type { EvaluationTabKey } from "./types";
import styles from "./evaluation.module.css";

type EvaluationModuleTabsProps = {
  activeKey: EvaluationTabKey;
  onChange: (key: EvaluationTabKey) => void;
};

export function EvaluationModuleTabs({ activeKey, onChange }: EvaluationModuleTabsProps) {
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const items = [
    { key: "workspace", label: "评价工作台", visible: true },
    { key: "tasks", label: "评价任务", visible: hasPermission("evaluation:task:view") },
    { key: "indicators", label: "指标体系", visible: hasPermission("evaluation:standard:view") },
    { key: "archives", label: "申报档案", visible: hasPermission("declaration:application:view") },
  ].filter((item) => item.visible);

  return (
    <section className={styles.tabsPanel}>
      <Tabs
        activeKey={activeKey}
        className={styles.moduleTabs}
        items={items}
        onChange={(key) => onChange(key as EvaluationTabKey)}
      />
    </section>
  );
}
