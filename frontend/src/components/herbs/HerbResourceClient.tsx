"use client";

import { PlusOutlined } from "@ant-design/icons";
import { Button } from "antd";
import { useState } from "react";
import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
import { HerbActionToolbar } from "./HerbActionToolbar";
import { HerbDetailPanel } from "./HerbDetailPanel";
import { HerbFilterBar } from "./HerbFilterBar";
import { HerbResourceTabs, type HerbTabKey } from "./HerbResourceTabs";
import { HerbTable } from "./HerbTable";
import type { HerbTableRecord } from "./types";
import styles from "./herbs.module.css";

export function HerbResourceClient() {
  const [activeTab, setActiveTab] = useState<HerbTabKey>("species");
  const [selectedHerb, setSelectedHerb] = useState<HerbTableRecord | null>(null);

  return (
    <div className={styles.herbPage}>
      <div className={`${styles.workspace} ${selectedHerb ? styles.workspaceWithDetail : ""}`}>
        <section className={styles.leftWorkspace}>
          <ModuleHeroBanner
            actions={
              <Button disabled icon={<PlusOutlined />} title="功能开发中" type="primary">
                新增药材
              </Button>
            }
            description="管理中药材基础资料、分类、基地信息，支持药材资源全生命周期管理。"
            eyebrow="HERBAL RESOURCE CENTER"
            sealText="本草"
            title="中药材资源中心"
          />
          <section className={styles.managementPanel}>
            <HerbResourceTabs activeTab={activeTab} onTabChange={setActiveTab} />
            <HerbFilterBar />
            <HerbActionToolbar />
          </section>
          <section className={styles.tableArea}>
            <HerbTable onView={setSelectedHerb} />
          </section>
        </section>

        {selectedHerb ? (
          <HerbDetailPanel herb={selectedHerb} onClose={() => setSelectedHerb(null)} />
        ) : null}
      </div>
    </div>
  );
}
