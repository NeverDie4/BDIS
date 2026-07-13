import { Tabs } from "antd";
import type { EvaluationTabKey } from "./types";
import styles from "./evaluation.module.css";

type EvaluationModuleTabsProps = {
  activeKey: EvaluationTabKey;
  onChange: (key: EvaluationTabKey) => void;
};

const items = [
  { key: "workspace", label: "评价工作台", disabled: true },
  { key: "tasks", label: "评价任务（开发中）", disabled: true },
  { key: "indicators", label: "指标体系（开发中）", disabled: true },
  { key: "archives", label: "申报档案" },
];

export function EvaluationModuleTabs({ activeKey, onChange }: EvaluationModuleTabsProps) {
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
