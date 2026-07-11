import { PortalPage } from "@/components/common/PortalPage";
import { SiteLayout } from "@/components/layout/SiteLayout";

export default function EvaluationPage() {
  return (
    <SiteLayout>
      <PortalPage
        eyebrow="EVALUATION ARCHIVE"
        title="评价申报工作台"
        description="预留评价指标、评价任务、申报档案和佐证材料归档入口，后续承接评价标准版本、申报材料完整性和审核流转。"
        tags={["评价任务", "申报档案", "材料归档"]}
        cards={[
          {
            title: "评价标准",
            description: "预留指标体系、标准版本、适用范围和启用状态管理入口。",
          },
          {
            title: "申报材料",
            description: "预留材料上传、业务对象绑定、完整性提示和归档袋汇总。",
          },
          {
            title: "过程追踪",
            description: "预留提交、退回、确认、归档和历史版本追溯展示。",
          },
        ]}
      />
    </SiteLayout>
  );
}
