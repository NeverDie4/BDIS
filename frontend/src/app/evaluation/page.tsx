import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";

export default function EvaluationPage() {
  return (
    <SiteLayout contentMode="fluid">
      <ModuleHeroBanner
        eyebrow="EVALUATION & APPLICATION"
        sealText="评审"
        title="评价申报"
        description="围绕评价任务、指标体系、申报材料与审核流程，完成评价结果汇总与申报档案管理。"
      />
    </SiteLayout>
  );
}
