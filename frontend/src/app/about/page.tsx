import {
  BookOpenCheck,
  Boxes,
  FileCheck2,
  Leaf,
  MapPinned,
  Network,
  ScanLine,
  ShieldCheck,
  Smartphone,
  Workflow,
} from "lucide-react";
import { SiteLayout } from "@/components/layout/SiteLayout";
import styles from "./page.module.css";

const capabilities = [
  {
    icon: MapPinned,
    title: "资源与生长数据",
    description: "统一维护药材档案、基地信息、分布地图与生长采集记录。",
  },
  {
    icon: ShieldCheck,
    title: "权限与过程管控",
    description: "以账号、角色、组织和数据范围约束业务访问，并保留关键操作审计。",
  },
  {
    icon: FileCheck2,
    title: "资料归档与审核",
    description: "让教学科研资料、申报材料和业绩证明可关联、可审核、可追溯。",
  },
  {
    icon: ScanLine,
    title: "溯源与数字档案",
    description: "通过二维码溯源和数字生命档案，让采集过程与业务结果更容易核验。",
  },
  {
    icon: Smartphone,
    title: "移动端协同采集",
    description: "为现场采集提供移动端入口，使位置、图片和记录能够汇入同一平台。",
  },
  {
    icon: Network,
    title: "数据交换演示",
    description: "提供本地 mock SOAP 对接链路，用于展示外部校内系统的数据交换过程。",
  },
];

const workflow = [
  "现场采集或业务录入",
  "校验、权限控制与文件关联",
  "审核、认定与过程归档",
  "地图查询、统计展示与公开溯源",
];

const technologyGroups = [
  { label: "前端", value: "Next.js · TypeScript · Ant Design" },
  { label: "后端", value: "Spring Boot · Java 21 · MyBatis-Plus" },
  { label: "数据", value: "MySQL · Flyway · Redis" },
  { label: "协同", value: "uni-app · FastAPI · Docker Compose" },
];

const projectFacts = [
  { value: "6", label: "业务功能域", description: "资源、教学、培训、评价、业绩与用户管理" },
  { value: "20", label: "模块化设计", description: "基础支撑、核心业务、集成与展示协同推进" },
  { value: "多端", label: "数据协同", description: "Web、移动采集与本地 SOAP 演示链路" },
];

export default function AboutPage() {
  return (
    <SiteLayout contentMode="fluid">
      <div className={styles.page}>
        <section className={styles.hero} aria-labelledby="about-title">
          <div className={styles.heroInner}>
            <div className={styles.heroContent}>
              <p className={styles.eyebrow}>
                <Leaf aria-hidden="true" size={16} />
                BDIS · 小组实训项目
              </p>
              <h1 id="about-title">让中药材数据更可见、可用、可追溯</h1>
              <p className={styles.lead}>
                生物医药数字信息系统面向中药材科研、教学与管理场景，围绕资源管理、生长采集、资料归档、审核认定与过程溯源，构建统一的数字化协同平台。
              </p>
              <div className={styles.heroTags}>
                <span>中药材科研</span>
                <span>教学资源协同</span>
                <span>全流程可追溯</span>
              </div>
            </div>
            <aside className={styles.heroAside} aria-label="项目概览">
              <span className={styles.seal}>实训项目</span>
              <dl className={styles.heroMeta}>
                <div>
                  <dt>项目定位</dt>
                  <dd>面向中药材科研、教学与管理的数字平台</dd>
                </div>
                <div>
                  <dt>交付重点</dt>
                  <dd>以真实业务流程验证跨模块协同闭环</dd>
                </div>
              </dl>
            </aside>
          </div>
        </section>

        <div className={styles.content}>
          <section className={styles.facts} aria-label="项目设计范围">
            {projectFacts.map((fact) => (
              <article className={styles.fact} key={fact.label}>
                <strong>{fact.value}</strong>
                <div>
                  <h2>{fact.label}</h2>
                  <p>{fact.description}</p>
                </div>
              </article>
            ))}
          </section>

          <section className={styles.section} aria-labelledby="background-title">
            <div className={styles.sectionIntro}>
              <p className={styles.sectionKicker}>PROJECT CONTEXT</p>
              <h2 id="background-title">从分散资料到统一的业务数据底座</h2>
            </div>
            <div className={styles.introGrid}>
              <p>
                中药材科研与教学工作涉及品种、基地、生长记录、图片、课程资料、申报材料等多类信息。分散存储会让资料复用、过程核验和权限管理变得困难。
              </p>
              <p>
                BDIS
                以统一身份、数据范围、文件资源与审计能力为基础，将采集、录入、审核、归档、查询和溯源串联为可追踪的业务过程。
              </p>
            </div>
          </section>

          <section className={styles.section} aria-labelledby="capability-title">
            <div className={styles.sectionIntro}>
              <p className={styles.sectionKicker}>WHAT WE DELIVER</p>
              <h2 id="capability-title">当前可演示的核心能力</h2>
              <p>以下能力以实训阶段的可运行链路为准，不将后续规划表述为已完成成果。</p>
            </div>
            <div className={styles.capabilityGrid}>
              {capabilities.map((capability, index) => {
                const Icon = capability.icon;
                return (
                  <article className={styles.capabilityCard} key={capability.title}>
                    <span className={styles.capabilityIndex}>
                      {String(index + 1).padStart(2, "0")}
                    </span>
                    <span className={styles.cardIcon}>
                      <Icon aria-hidden="true" size={21} />
                    </span>
                    <h3>{capability.title}</h3>
                    <p>{capability.description}</p>
                  </article>
                );
              })}
            </div>
          </section>

          <section
            className={`${styles.section} ${styles.workflowSection}`}
            aria-labelledby="workflow-title"
          >
            <div className={styles.sectionIntro}>
              <p className={styles.sectionKicker}>CORE WORKFLOW</p>
              <h2 id="workflow-title">围绕真实业务形成数据闭环</h2>
            </div>
            <ol className={styles.workflow}>
              {workflow.map((step, index) => (
                <li key={step}>
                  <span>{String(index + 1).padStart(2, "0")}</span>
                  <p>{step}</p>
                </li>
              ))}
            </ol>
          </section>

          <section className={styles.lowerGrid}>
            <article className={styles.technologyCard} aria-labelledby="technology-title">
              <div className={styles.cardHeading}>
                <Boxes aria-hidden="true" size={21} />
                <div>
                  <p className={styles.sectionKicker}>TECHNOLOGY</p>
                  <h2 id="technology-title">工程化协同实现</h2>
                </div>
              </div>
              <dl className={styles.technologyList}>
                {technologyGroups.map((group) => (
                  <div key={group.label}>
                    <dt>{group.label}</dt>
                    <dd>{group.value}</dd>
                  </div>
                ))}
              </dl>
            </article>

            <article className={styles.boundaryCard} aria-labelledby="boundary-title">
              <div className={styles.cardHeading}>
                <Workflow aria-hidden="true" size={21} />
                <div>
                  <p className={styles.sectionKicker}>PROJECT BOUNDARY</p>
                  <h2 id="boundary-title">实训阶段的取舍与延展</h2>
                </div>
              </div>
              <p>
                本期优先验证业务闭环和跨模块协同。校内 SOAP 系统采用本地 mock
                服务演示对接过程；真实校内系统联调、IoT
                实时采集、复杂报表与深度智能分析作为下一阶段方向。
              </p>
            </article>
          </section>

          <section className={styles.teamSection} aria-labelledby="team-title">
            <div className={styles.teamIcon}>
              <BookOpenCheck aria-hidden="true" size={24} />
            </div>
            <div>
              <p className={styles.sectionKicker}>TEAM PRACTICUM</p>
              <h2 id="team-title">以协作交付一个可运行的项目成果</h2>
              <p>
                团队围绕需求分析、前端交互、后端服务、数据库迁移、移动端接入、测试验证与项目文档协作推进。成员与指导信息由项目组在正式展示时按实际情况补充。
              </p>
            </div>
          </section>
        </div>
      </div>
    </SiteLayout>
  );
}
