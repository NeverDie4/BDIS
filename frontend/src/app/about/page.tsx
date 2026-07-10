import { PortalPage } from "@/components/common/PortalPage";
import { SiteLayout } from "@/components/layout/SiteLayout";

export default function AboutPage() {
  return (
    <SiteLayout>
      <PortalPage
        eyebrow="ABOUT BDIS"
        title="关于本草研究院标本馆"
        description="本系统面向生物医药教学科研与中药材资源管理场景，服务数据集中管理、资料规范归档、流程在线办理和全过程审计。"
        tags={["教学科研", "资源治理", "审计追溯"]}
        cards={[
          {
            title: "建设目标",
            description: "建立统一身份认证、资源管理、文件归档和业务流转支撑能力。",
          },
          {
            title: "服务对象",
            description: "面向教师、学生、科研人员、采集人员、评价申报人员和系统管理员。",
          },
          {
            title: "阶段边界",
            description: "初版优先完成可演示闭环，AI 识别、复杂图谱和 SOAP 真接入作为扩展。",
          },
        ]}
      />
    </SiteLayout>
  );
}
