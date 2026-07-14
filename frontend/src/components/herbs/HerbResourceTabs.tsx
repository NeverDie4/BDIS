import { Tabs } from "antd";
import styles from "./herbs.module.css";

export type HerbTabKey = "species" | "categories" | "bases";

type HerbResourceTabsProps = {
  activeTab: HerbTabKey;
  onTabChange: (key: HerbTabKey) => void;
};

export function HerbResourceTabs({ activeTab, onTabChange }: HerbResourceTabsProps) {
  return (
    <div className={styles.tabsArea}>
      <Tabs
        activeKey={activeTab}
        className={styles.tabs}
        items={[
          { key: "species", label: "药材品种" },
          { key: "categories", label: "药材分类" },
          { key: "bases", label: "基地管理" },
        ]}
        onChange={(key) => onTabChange(key as HerbTabKey)}
      />
    </div>
  );
}
