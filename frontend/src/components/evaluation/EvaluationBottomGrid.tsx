import type { EvaluationApplication, EvaluationIndicator, EvaluationResult } from "./types";
import { IndicatorOverviewCard } from "./IndicatorOverviewCard";
import { RecentEvaluationResultCard } from "./RecentEvaluationResultCard";
import { ReviewProcessCard } from "./ReviewProcessCard";
import styles from "./evaluation.module.css";

type EvaluationBottomGridProps = {
  indicators: EvaluationIndicator[];
  results: EvaluationResult[];
  application?: EvaluationApplication;
};

export function EvaluationBottomGrid({ indicators, results, application }: EvaluationBottomGridProps) {
  return <section className={styles.bottomGrid}><IndicatorOverviewCard indicators={indicators} /><RecentEvaluationResultCard results={results} /><ReviewProcessCard application={application} /></section>;
}
