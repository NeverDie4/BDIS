"use client";

import { useState } from "react";
import { HerbActionToolbar } from "./HerbActionToolbar";
import { HerbDetailPanel } from "./HerbDetailPanel";
import { HerbFilterBar } from "./HerbFilterBar";
import { HerbPageHeader } from "./HerbPageHeader";
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
          <HerbPageHeader />
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
