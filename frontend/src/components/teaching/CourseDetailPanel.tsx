import {
  CloseOutlined,
  ExperimentOutlined,
  EyeOutlined,
  FileTextOutlined,
  LinkOutlined,
  PlayCircleOutlined,
} from "@ant-design/icons";
import { Button, Tag, Tabs } from "antd";
import type { TabsProps } from "antd";
import Image from "next/image";
import { TeachingStatusTag } from "./TeachingStatusTag";
import type { CourseRecord } from "./types";
import styles from "./teaching.module.css";

type CourseDetailPanelProps = {
  course: CourseRecord;
  onClose: () => void;
};

export function CourseDetailPanel({ course, onClose }: CourseDetailPanelProps) {
  const detailItems: TabsProps["items"] = [
    {
      key: "basic",
      label: "基础信息",
      children: (
        <div className={styles.detailContent}>
          <section className={styles.detailIntroSection}>
            <h3>课程简介</h3>
            <p>{course.description}</p>
          </section>
          <dl className={styles.detailInfoList}>
            <div>
              <dt>适用专业</dt>
              <dd>{course.detail.applicableMajors.join("、")}</dd>
            </div>
            <div>
              <dt>学时学分</dt>
              <dd>{course.detail.hours} 学时 / {course.detail.credits} 学分</dd>
            </div>
            <div>
              <dt>先修课程</dt>
              <dd>{course.detail.prerequisites.join("、")}</dd>
            </div>
            <div className={styles.detailInfoLongRow}>
              <dt>教学目标</dt>
              <dd>
                <ol className={styles.detailObjectiveList}>
                  {course.detail.teachingObjectives.map((objective) => <li key={objective}>{objective}</li>)}
                </ol>
              </dd>
            </div>
            <div>
              <dt>教学方式</dt>
              <dd className={styles.detailTagGroup}>
                {course.detail.teachingMethods.map((method) => (
                  <Tag className={styles.detailTag} key={method}>{method}</Tag>
                ))}
              </dd>
            </div>
            <div>
              <dt>课程状态</dt>
              <dd><TeachingStatusTag status={course.status} /></dd>
            </div>
            <div>
              <dt>发布时间</dt>
              <dd>{course.detail.publishedAt}</dd>
            </div>
            <div>
              <dt>标签</dt>
              <dd className={styles.detailTagGroup}>
                {course.detail.tags.map((tag) => <Tag className={styles.detailTag} key={tag}>{tag}</Tag>)}
              </dd>
            </div>
          </dl>
        </div>
      ),
    },
    {
      key: "steps",
      label: "实验步骤",
      children: (
        <div className={styles.detailContent}>
          <ol className={styles.detailStepList}>
            {course.detail.experimentSteps.map((step, index) => (
              <li key={step.title}>
                <span className={styles.detailStepIndex}>{index + 1}</span>
                <div>
                  <div className={styles.detailListHeading}>
                    <strong>{step.title}</strong>
                    <span>{step.duration}</span>
                  </div>
                  <p>{step.description}</p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      ),
    },
    {
      key: "resources",
      label: "课程资源",
      children: (
        <div className={styles.detailContent}>
          <div className={styles.detailResourceList}>
            {course.detail.resources.map((resource) => (
              <div className={styles.detailResourceRow} key={resource.name}>
                <FileTextOutlined />
                <div>
                  <strong>{resource.name}</strong>
                  <span>{resource.type} · {resource.size}</span>
                </div>
                <Button className={styles.actionLink} type="link" onClick={() => undefined}>查看</Button>
              </div>
            ))}
          </div>
        </div>
      ),
    },
    {
      key: "videos",
      label: "视频资源",
      children: (
        <div className={styles.detailContent}>
          <div className={styles.detailResourceList}>
            {course.detail.videos.map((video) => (
              <div className={styles.detailResourceRow} key={video.title}>
                <PlayCircleOutlined />
                <div>
                  <strong>{video.title}</strong>
                  <span>{video.speaker} · {video.duration}</span>
                </div>
                <Button className={styles.actionLink} type="link" onClick={() => undefined}>查看</Button>
              </div>
            ))}
          </div>
        </div>
      ),
    },
    {
      key: "relations",
      label: "关联信息",
      children: (
        <div className={styles.detailContent}>
          <div className={styles.detailRelationGroup}>
            <section>
              <h3><ExperimentOutlined />关联药材</h3>
              <div className={styles.detailTagGroup}>
                {course.detail.relatedHerbs.map((herb) => <Tag className={styles.detailTag} key={herb}>{herb}</Tag>)}
              </div>
            </section>
            <section>
              <h3><LinkOutlined />关联课题</h3>
              <ul>
                {course.detail.relatedProjects.map((project) => <li key={project}>{project}</li>)}
              </ul>
            </section>
            <section>
              <h3><FileTextOutlined />关联采集数据</h3>
              <ul>
                {course.detail.relatedCollections.map((collection) => <li key={collection}>{collection}</li>)}
              </ul>
            </section>
          </div>
        </div>
      ),
    },
    {
      key: "records",
      label: "实验记录",
      children: (
        <div className={styles.detailContent}>
          <div className={styles.detailRecordList}>
            {course.detail.experimentRecords.map((record) => (
              <div className={styles.detailRecordRow} key={`${record.date}-${record.className}`}>
                <div className={styles.detailListHeading}>
                  <strong>{record.className}</strong>
                  <span>{record.date}</span>
                </div>
                <div className={styles.detailRecordMetrics}>
                  <span>参与人数<strong>{record.participantCount}</strong></span>
                  <span>完成率<strong>{record.completionRate}</strong></span>
                </div>
              </div>
            ))}
          </div>
        </div>
      ),
    },
  ];

  return (
    <aside className={styles.detailPanel} aria-label="课程详情">
      <div className={styles.detailHeader}>
        <h2>课程详情</h2>
        <div className={styles.detailHeaderActions}>
          <Button icon={<EyeOutlined />} size="small" onClick={() => undefined}>学生预览</Button>
          <Button
            aria-label="关闭课程详情"
            icon={<CloseOutlined />}
            size="small"
            type="text"
            onClick={onClose}
          />
        </div>
      </div>

      <section className={styles.courseSummary}>
        <Image
          alt={`${course.courseName}课程封面`}
          className={styles.detailCover}
          height={78}
          src={course.thumbnail}
          width={104}
        />
        <div className={styles.courseSummaryBody}>
          <div className={styles.courseSummaryTitle}>
            <h3>{course.courseName}</h3>
            <TeachingStatusTag status={course.status} />
          </div>
          <dl className={styles.courseSummaryMeta}>
            <div><dt>课程编号</dt><dd>{course.courseNo}</dd></div>
            <div><dt>学科方向</dt><dd>{course.subject}</dd></div>
            <div><dt>负责人</dt><dd>{course.teacher}</dd></div>
            <div><dt>更新时间</dt><dd>{course.updatedAt}</dd></div>
          </dl>
        </div>
      </section>

      <Tabs className={styles.detailTabs} items={detailItems} size="small" tabBarGutter={16} />
    </aside>
  );
}
