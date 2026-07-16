import {
  CheckCircleOutlined,
  CloseOutlined,
  DeleteOutlined,
  EditOutlined,
  ExperimentOutlined,
  EyeOutlined,
  FileTextOutlined,
  LinkOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  RollbackOutlined,
} from "@ant-design/icons";
import { App, Button, Empty, Input, InputNumber, Modal, Select, Tag, Tabs } from "antd";
import type { TabsProps } from "antd";
import Image from "next/image";
import { useEffect, useState } from "react";
import {
  archiveExperimentRecord,
  deleteExperimentRecord,
  gradeExperimentRecord,
  getExperimentRecord,
  listExperimentRecords,
  submitExperimentRecord,
  type ExperimentRecordDetailApi,
  type ExperimentRecordListApi,
} from "@/lib/experiment-records";
import { getApiErrorMessage } from "@/lib/request";
import {
  getCourseRelationOptions,
  updateCourseRelations,
  type CourseDetailApi,
  type CourseRelationOptionsApi,
} from "@/lib/courses";
import { ExperimentRecordEditorModal } from "./ExperimentRecordEditorModal";
import { TeachingStatusTag } from "./TeachingStatusTag";
import type { CourseRecord } from "./types";
import styles from "./teaching.module.css";

type CourseDetailPanelProps = {
  course: CourseRecord;
  onClose: () => void;
  canRecordAdd?: boolean;
  canRecordList?: boolean;
  canRecordUpdate?: boolean;
  canRecordSubmit?: boolean;
  canRecordArchive?: boolean;
  canRecordDelete?: boolean;
  canGrade?: boolean;
  canEdit?: boolean;
  onCourseUpdated?: (course: CourseDetailApi) => void;
};

export function CourseDetailPanel({
  course,
  onClose,
  canRecordAdd,
  canRecordList,
  canRecordUpdate,
  canRecordSubmit,
  canRecordArchive,
  canRecordDelete,
  canGrade,
  canEdit,
  onCourseUpdated,
}: CourseDetailPanelProps) {
  const { message, modal } = App.useApp();
  const [records, setRecords] = useState<ExperimentRecordListApi[]>([]);
  const [recordsLoading, setRecordsLoading] = useState(false);
  const [recordEditorOpen, setRecordEditorOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<ExperimentRecordDetailApi | null>(null);
  const [gradingRecord, setGradingRecord] = useState<ExperimentRecordListApi | null>(null);
  const [gradingRecordId, setGradingRecordId] = useState<number | null>(null);
  const [gradeValue, setGradeValue] = useState<number | null>(null);
  const [gradeComment, setGradeComment] = useState("");
  const [relationEditorOpen, setRelationEditorOpen] = useState(false);
  const [relationSaving, setRelationSaving] = useState(false);
  const [relationOptions, setRelationOptions] = useState<CourseRelationOptionsApi>({ herbs: [], projects: [] });
  const [selectedHerbIds, setSelectedHerbIds] = useState<number[]>([]);
  const [selectedProjectIds, setSelectedProjectIds] = useState<number[]>([]);

  async function openRelationEditor() {
    try {
      const options = await getCourseRelationOptions(Number(course.id));
      setRelationOptions(options);
      setSelectedHerbIds(course.detail.relatedHerbItems.map((item) => item.id));
      setSelectedProjectIds(course.detail.relatedProjectItems.map((item) => item.id));
      setRelationEditorOpen(true);
    } catch (error) {
      message.error(getApiErrorMessage(error, "关联候选数据加载失败"));
    }
  }

  async function saveRelations() {
    try {
      setRelationSaving(true);
      const detail = await updateCourseRelations(Number(course.id), {
        version: course.version ?? 0,
        speciesIds: selectedHerbIds,
        projectIds: selectedProjectIds,
      });
      onCourseUpdated?.(detail);
      setRelationEditorOpen(false);
      message.success("课程关联已保存");
    } catch (error) {
      message.error(getApiErrorMessage(error, "课程关联保存失败"));
    } finally {
      setRelationSaving(false);
    }
  }

  async function reloadRecords() {
    if (!canRecordList) {
      setRecords([]);
      return;
    }
    setRecordsLoading(true);
    try {
      const result = await listExperimentRecords(Number(course.id));
      setRecords(result.records);
    } catch (error) {
      message.error(getApiErrorMessage(error, "实验记录加载失败"));
    } finally {
      setRecordsLoading(false);
    }
  }

  useEffect(() => {
    void reloadRecords();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [course.id, canRecordList]);

  async function openEditRecord(record: ExperimentRecordListApi) {
    try {
      const detail = await getExperimentRecord(record.id);
      setEditingRecord(detail);
      setRecordEditorOpen(true);
    } catch (error) {
      message.error(getApiErrorMessage(error, "实验记录详情加载失败"));
    }
  }

  function confirmDelete(record: ExperimentRecordListApi) {
    modal.confirm({
      title: "确认删除实验记录？",
      content: `删除后不可恢复：${record.experimentTitle}`,
      okText: "删除",
      cancelText: "取消",
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deleteExperimentRecord(record.id);
          message.success("实验记录已删除");
          await reloadRecords();
        } catch (error) {
          message.error(getApiErrorMessage(error, "实验记录删除失败"));
        }
      },
    });
  }

  async function handleSubmitRecord(record: ExperimentRecordListApi) {
    try {
      const detail = await getExperimentRecord(record.id);
      await submitExperimentRecord(detail.id, detail.version);
      message.success("实验记录已提交");
      await reloadRecords();
    } catch (error) {
      message.error(getApiErrorMessage(error, "实验记录提交失败"));
    }
  }

  function confirmArchive(record: ExperimentRecordListApi) {
    modal.confirm({
      title: "确认归档实验记录？",
      content: "归档后记录将不能再编辑或添加附件。",
      okText: "归档",
      cancelText: "取消",
      onOk: async () => {
        try {
          const detail = await getExperimentRecord(record.id);
          await archiveExperimentRecord(detail.id, detail.version);
          message.success("实验记录已归档");
          await reloadRecords();
        } catch (error) {
          message.error(getApiErrorMessage(error, "实验记录归档失败"));
        }
      },
    });
  }

  const archiveStatusLabel: Record<string, string> = {
    draft: "草稿",
    submitted: "已提交",
    archived: "已归档",
  };

  function openGradingRecord(record: ExperimentRecordListApi) {
    setGradingRecord(record);
    setGradingRecordId(record.id);
    setGradeValue(record.score ?? null);
    setGradeComment(record.gradeComment ?? "");
  }

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
            <div><dt>适用专业</dt><dd>{course.detail.applicableMajors.join("、")}</dd></div>
            <div><dt>学时学分</dt><dd>{course.detail.hours} 学时 / {course.detail.credits} 学分</dd></div>
            <div><dt>先修课程</dt><dd>{course.detail.prerequisites.join("、")}</dd></div>
            <div className={styles.detailInfoLongRow}>
              <dt>教学目标</dt>
              <dd><ol className={styles.detailObjectiveList}>{course.detail.teachingObjectives.map((item) => <li key={item}>{item}</li>)}</ol></dd>
            </div>
            <div><dt>教学方式</dt><dd className={styles.detailTagGroup}>{course.detail.teachingMethods.map((item) => <Tag className={styles.detailTag} key={item}>{item}</Tag>)}</dd></div>
            <div><dt>课程状态</dt><dd><TeachingStatusTag status={course.status} /></dd></div>
            <div><dt>发布时间</dt><dd>{course.detail.publishedAt}</dd></div>
            <div><dt>标签</dt><dd className={styles.detailTagGroup}>{course.detail.tags.map((item) => <Tag className={styles.detailTag} key={item}>{item}</Tag>)}</dd></div>
          </dl>
        </div>
      ),
    },
    {
      key: "steps",
      label: "实验步骤",
      children: <div className={styles.detailContent}><ol className={styles.detailStepList}>{course.detail.experimentSteps.map((step, index) => <li key={step.id ?? step.title}><span className={styles.detailStepIndex}>{index + 1}</span><div><div className={styles.detailListHeading}><strong>{step.title}</strong><span>{step.duration}</span></div><p>{step.description}</p></div></li>)}</ol></div>,
    },
    {
      key: "resources",
      label: "课程资源",
      children: <div className={styles.detailContent}>{course.detail.videoUrl ? <video className={styles.detailVideo} controls preload="metadata" src={course.detail.videoUrl} /> : null}<div className={styles.detailResourceList}>{course.detail.resources.map((resource) => <div className={styles.detailResourceRow} key={resource.id ?? resource.name}><FileTextOutlined /><div><strong>{resource.name}</strong><span>{resource.type} · {resource.size}</span></div><Button className={styles.actionLink} type="link" href={resource.url} target="_blank">查看</Button></div>)}</div>{!course.detail.videoUrl && course.detail.videos.length === 0 ? <p>暂无视频资源</p> : null}</div>,
    },
    {
      key: "videos",
      label: "视频资源",
      children: <div className={styles.detailContent}><div className={styles.detailResourceList}>{course.detail.videos.map((video) => <div className={styles.detailResourceRow} key={video.title}><PlayCircleOutlined /><div><strong>{video.title}</strong><span>{video.speaker} · {video.duration}</span></div><Button className={styles.actionLink} type="link">查看</Button></div>)}</div></div>,
    },
    {
      key: "relations",
      label: "关联信息",
      children: <div className={styles.detailContent}><div className={styles.detailRelationGroup}><section><h3><ExperimentOutlined />关联药材</h3><div className={styles.detailTagGroup}>{course.detail.relatedHerbs.map((item) => <Tag className={styles.detailTag} key={item}>{item}</Tag>)}</div></section><section><h3><LinkOutlined />关联课题</h3><ul>{course.detail.relatedProjects.map((item) => <li key={item}>{item}</li>)}</ul></section><section><h3><FileTextOutlined />关联采集数据</h3><ul>{course.detail.relatedCollections.map((item) => <li key={item}>{item}</li>)}</ul></section></div></div>,
    },
    {
      key: "records",
      label: "实验记录",
      children: <div className={styles.detailContent}><div className={styles.recordToolbar}><span>{recordsLoading ? "正在加载…" : `共 ${records.length} 条记录`}</span>{canRecordAdd ? <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => { setEditingRecord(null); setRecordEditorOpen(true); }}>新增记录</Button> : null}</div><div className={styles.detailRecordList}>{records.map((record) => <div className={styles.detailRecordRow} key={record.id}><div className={styles.detailListHeading}><strong>{record.experimentTitle}</strong><span>{record.score == null ? "未评分" : `成绩 ${record.score} 分`}</span><Tag className={styles.detailTag}>{archiveStatusLabel[record.archiveStatus] ?? record.archiveStatus}</Tag></div><div className={styles.recordMetaLine}><span>{record.recordNo}</span><span>{record.recorderName ?? "未知记录人"}</span><span>{record.recordedAt?.replace("T", " ") ?? "未填写时间"}</span></div><div className={styles.recordActions}>{record.archiveStatus === "draft" && canRecordUpdate ? <Button type="link" className={styles.actionLink} icon={<EditOutlined />} onClick={() => void openEditRecord(record)}>编辑</Button> : null}{record.archiveStatus === "draft" && canRecordSubmit ? <Button type="link" className={styles.actionLink} icon={<CheckCircleOutlined />} onClick={() => void handleSubmitRecord(record)}>提交</Button> : null}{record.archiveStatus === "submitted" && canRecordArchive ? <Button type="link" className={styles.actionLink} icon={<RollbackOutlined />} onClick={() => confirmArchive(record)}>归档</Button> : null}{record.archiveStatus === "draft" && canRecordDelete ? <Button type="link" danger className={styles.actionLink} icon={<DeleteOutlined />} onClick={() => confirmDelete(record)}>删除</Button> : null}</div></div>)}{!recordsLoading && records.length === 0 ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无实验记录" /> : null}</div></div>,
    },
  ];

  const renderedDetailItems = detailItems.map((item) => {
    if (item.key === "resources") {
      return {
        ...item,
        children: <div className={styles.detailContent}><div className={styles.detailResourceList}>{course.detail.resources.map((resource) => <div className={styles.detailResourceRow} key={resource.id ?? resource.name}><FileTextOutlined /><div><strong>{resource.name}</strong><span>{resource.type} · {resource.size}</span></div>{resource.url ? <Button className={styles.actionLink} type="link" href={resource.url} target="_blank">查看</Button> : null}</div>)}</div>{course.detail.resources.length === 0 ? <p>暂无课程文件</p> : null}</div>,
      };
    }
    if (item.key === "videos") {
      return {
        ...item,
        children: <div className={styles.detailContent}><div className={styles.detailResourceList}>{course.detail.videos.map((video) => <div className={styles.detailResourceRow} key={video.id ?? video.title}><PlayCircleOutlined /><div><strong>{video.title}</strong><span>{video.size ?? "课程视频"}</span></div>{video.url ? <Button className={styles.actionLink} type="link" href={video.url} target="_blank">播放</Button> : null}</div>)}</div>{course.detail.videos.length === 0 ? <p>暂无视频资源</p> : null}</div>,
      };
    }
    if (item.key === "relations") {
      return {
        ...item,
        children: <div className={styles.detailContent}>{canEdit ? <div className={styles.recordToolbar}><Button size="small" icon={<EditOutlined />} onClick={() => void openRelationEditor()}>编辑关联</Button></div> : null}<div className={styles.detailRelationGroup}><section><h3><ExperimentOutlined />关联药材</h3><div className={styles.detailTagGroup}>{course.detail.relatedHerbs.map((name) => <Tag className={styles.detailTag} key={name}>{name}</Tag>)}</div></section><section><h3><LinkOutlined />关联课题</h3><ul>{course.detail.relatedProjects.map((name) => <li key={name}>{name}</li>)}</ul></section></div></div>,
      };
    }
    return item;
  });

  return (
    <aside className={styles.detailPanel} aria-label="课程详情">
      <div className={styles.detailHeader}><h2>课程详情</h2><div className={styles.detailHeaderActions}><Button icon={<EyeOutlined />} size="small">学生预览</Button>{canGrade ? <Button size="small" onClick={() => { const record = records.find((item) => item.archiveStatus === "submitted"); if (record) { openGradingRecord(record); } else message.info("暂无待批阅的实验报告"); }}>批阅报告</Button> : null}<Button aria-label="关闭课程详情" icon={<CloseOutlined />} size="small" type="text" onClick={onClose} /></div></div>
      <section className={styles.courseSummary}><Image alt={`${course.courseName}课程封面`} className={styles.detailCover} height={78} src={course.thumbnail} width={104} /><div className={styles.courseSummaryBody}><div className={styles.courseSummaryTitle}><h3>{course.courseName}</h3><TeachingStatusTag status={course.status} /></div><dl className={styles.courseSummaryMeta}><div><dt>课程编号</dt><dd>{course.courseNo}</dd></div><div><dt>学科方向</dt><dd>{course.subject}</dd></div><div><dt>负责人</dt><dd>{course.teacher}</dd></div><div><dt>更新时间</dt><dd>{course.updatedAt}</dd></div></dl></div></section>
      <Tabs className={styles.detailTabs} items={renderedDetailItems} size="small" tabBarGutter={16} />
      <ExperimentRecordEditorModal open={recordEditorOpen} courseId={Number(course.id)} record={editingRecord} onCancel={() => setRecordEditorOpen(false)} onSaved={async () => { setRecordEditorOpen(false); await reloadRecords(); }} />
      <Modal open={relationEditorOpen} title="编辑课程关联" confirmLoading={relationSaving} okText="保存关联" cancelText="取消" onCancel={() => setRelationEditorOpen(false)} onOk={() => void saveRelations()}>
        <p>关联药材</p>
        <Select mode="multiple" value={selectedHerbIds} onChange={setSelectedHerbIds} options={relationOptions.herbs.map((item) => ({ value: item.id, label: `${item.code} · ${item.name}` }))} style={{ width: "100%" }} />
        <p style={{ marginTop: 16 }}>关联课题</p>
        <Select mode="multiple" value={selectedProjectIds} onChange={setSelectedProjectIds} options={relationOptions.projects.map((item) => ({ value: item.id, label: `${item.code} · ${item.name}` }))} style={{ width: "100%" }} />
      </Modal>
      <Modal open={Boolean(gradingRecord)} title="批阅实验报告" okText="保存评分" cancelText="取消" onCancel={() => setGradingRecord(null)} onOk={async () => { if (!gradingRecord || gradeValue == null) return; try { const detail = await getExperimentRecord(gradingRecord.id); await gradeExperimentRecord(gradingRecord.id, { version: detail.version, score: gradeValue, gradeComment: gradeComment.trim() || undefined }); message.success("评分已保存"); setGradingRecord(null); await reloadRecords(); } catch (error) { message.error(getApiErrorMessage(error, "评分保存失败")); } }}>
        <p>{gradingRecord?.experimentTitle} · {gradingRecord?.recorderName ?? "学生"}</p>
        <Select value={gradingRecordId ?? undefined} placeholder="选择要批阅的学生报告" style={{ width: "100%", marginBottom: 12 }} options={records.filter((record) => record.archiveStatus === "submitted").map((record) => ({ value: record.id, label: `${record.experimentTitle} · ${record.recorderName ?? "学生"}` }))} onChange={(value) => { const record = records.find((item) => item.id === value); if (record) openGradingRecord(record); }} />
        <InputNumber min={0} max={100} precision={1} value={gradeValue} onChange={(value) => setGradeValue(value)} addonAfter="分" style={{ width: "100%" }} />
        <Input.TextArea value={gradeComment} onChange={(event) => setGradeComment(event.target.value)} maxLength={1000} rows={4} placeholder="填写批阅意见（可选）" style={{ marginTop: 12 }} />
      </Modal>
    </aside>
  );
}
