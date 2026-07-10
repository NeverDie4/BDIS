import { HerbActionToolbar } from "./HerbActionToolbar";
import { HerbFilterBar } from "./HerbFilterBar";
import { HerbTable } from "./HerbTable";
import type { HerbTableRecord } from "./types";
import styles from "./herbs.module.css";

type HerbSpeciesPanelProps = {
  onView: (record: HerbTableRecord) => void;
};

export function HerbSpeciesPanel({ onView }: HerbSpeciesPanelProps) {
  return (
    <div className={styles.speciesPanel}>
      <HerbFilterBar />
      <HerbActionToolbar />
      <HerbTable onView={onView} />
    </div>
  );
}
