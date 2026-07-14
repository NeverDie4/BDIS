import type { ReactNode } from "react";
import styles from "./evaluation.module.css";

type EvaluationViewportProps = { tabs: ReactNode; main: ReactNode; bottom: ReactNode };

export function EvaluationViewport({ tabs, main, bottom }: EvaluationViewportProps) {
  return <div className={styles.evaluationViewport}><section className={styles.evaluationMainColumn}>{tabs}{main}{bottom}</section></div>;
}
