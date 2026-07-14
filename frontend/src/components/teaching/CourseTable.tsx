import type { Key } from "react";
import { DownOutlined } from "@ant-design/icons";
import { Button, Dropdown, Empty, Space, Table } from "antd";
import type { MenuProps, TableColumnsType } from "antd";
import Image from "next/image";
import { TeachingStatusTag } from "./TeachingStatusTag";
import type { CourseDetailData, CourseRecord } from "./types";
import styles from "./teaching.module.css";

type CourseTableProps = {
  courses: CourseRecord[];
  loading?: boolean;
  canEdit?: boolean;
  canPublish?: boolean;
  canDelete?: boolean;
  canEnroll?: boolean;
  viewMode?: "all" | "mine";
  onEnrollCourse?: (course: CourseRecord) => void;
  selectedRowKeys: Key[];
  onSelectionChange: (keys: Key[]) => void;
  onViewCourse: (course: CourseRecord) => void;
  onEditCourse: (course: CourseRecord) => void;
  onPublishCourse: (course: CourseRecord) => void;
  onOfflineCourse: (course: CourseRecord) => void;
  onDeleteCourse: (course: CourseRecord) => void;
};

const baseCourseDetail: CourseDetailData = {
  applicableMajors: ["中药学", "药学", "药用植物学"],
  hours: 32,
  credits: 2,
  prerequisites: ["中药学基础", "药用植物学"],
  teachingObjectives: [
    "掌握常用中药材的性状与显微鉴定方法。",
    "能够规范记录实验过程并形成可复核的实验结论。",
    "理解药材来源、采集环境与质量评价之间的关联。",
  ],
  teachingMethods: ["实验演示", "分组实践", "案例研讨"],
  publishedAt: "2026-03-01 09:00",
  tags: ["中药鉴定", "虚拟仿真", "实践教学"],
  experimentSteps: [
    { title: "实验准备", description: "核对药材样品、显微设备与实验记录表。", duration: "20 分钟" },
    { title: "性状观察", description: "观察并记录药材外形、断面、气味等性状特征。", duration: "45 分钟" },
    { title: "显微鉴定", description: "完成切片制备、显微观察与关键特征标注。", duration: "70 分钟" },
    { title: "结果分析", description: "对照标准图谱，完成鉴定结论与实验报告。", duration: "45 分钟" },
  ],
  resources: [
    { name: "课程教学大纲.pdf", type: "教学文档", size: "1.8 MB" },
    { name: "实验操作指导书.pdf", type: "实验指导", size: "4.2 MB" },
    { name: "常用药材显微图谱.zip", type: "图像资料", size: "28.6 MB" },
  ],
  videos: [
    { title: "中药材性状鉴定示范", duration: "18:26", speaker: "周老师" },
    { title: "显微切片制备规范", duration: "24:10", speaker: "实验中心" },
    { title: "实验报告填写说明", duration: "09:35", speaker: "课程组" },
  ],
  relatedHerbs: ["黄芪", "当归", "丹参", "黄连"],
  relatedProjects: ["川渝道地药材资源研究", "中药标本数字化规范"],
  relatedCollections: ["2026 春季课堂采集", "武陵山区药材样本", "显微图像数据集"],
  experimentRecords: [
    { date: "2026-06-18", className: "中药学 2024-1 班", participantCount: 42, completionRate: "100%" },
    { date: "2026-06-12", className: "中药学 2024-2 班", participantCount: 39, completionRate: "97%" },
    { date: "2026-05-29", className: "药学 2024-1 班", participantCount: 45, completionRate: "98%" },
  ],
};

const courseData: CourseRecord[] = [
  {
    id: "course-001",
    courseNo: "TC-001",
    courseName: "中药材鉴定实验",
    category: "实验课程",
    subject: "中药鉴定",
    teacher: "周老师",
    term: "2026 春",
    status: "published",
    updatedAt: "2026-07-08 14:20",
    thumbnail: "/images/herbs/showcase/huangqi.png",
    description: "围绕中药材性状、显微特征与标准图谱开展基础鉴定实验，训练规范观察、记录与分析能力。",
    detail: baseCourseDetail,
  },
  {
    id: "course-002",
    courseNo: "TC-002",
    courseName: "药用植物资源调查",
    category: "实践课程",
    subject: "资源调查",
    teacher: "李老师",
    term: "2026 春",
    status: "published",
    updatedAt: "2026-07-06 10:15",
    thumbnail: "/images/herbs/showcase/danggui.png",
    description: "围绕药用植物资源分布、样方调查与采集记录开展野外实践。",
    detail: {
      ...baseCourseDetail,
      applicableMajors: ["药用植物学", "中药资源与开发"],
      hours: 40,
      credits: 2.5,
      prerequisites: ["植物分类学", "野外调查方法"],
      teachingMethods: ["野外调查", "样方实践", "成果汇报"],
      tags: ["资源调查", "野外实践", "数据采集"],
    },
  },
  {
    id: "course-003",
    courseNo: "TC-003",
    courseName: "中药质量评价",
    category: "实验课程",
    subject: "质量评价",
    teacher: "陈老师",
    term: "2026 秋",
    status: "draft",
    updatedAt: "2026-07-04 16:40",
    thumbnail: "/images/herbs/showcase/danshen.png",
    description: "搭建中药质量指标、检测方法与综合评价的实验框架。",
    detail: {
      ...baseCourseDetail,
      hours: 36,
      credits: 2,
      publishedAt: "尚未发布",
      prerequisites: ["分析化学", "中药化学"],
      teachingMethods: ["仪器分析", "案例研讨", "实验报告"],
      tags: ["质量评价", "成分分析", "实验课程"],
    },
  },
  {
    id: "course-004",
    courseNo: "TC-004",
    courseName: "标本数字化采集",
    category: "实践课程",
    subject: "数字资源",
    teacher: "赵老师",
    term: "2026 秋",
    status: "offline",
    updatedAt: "2026-06-28 09:30",
    thumbnail: "/images/herbs/showcase/lingzhi.png",
    description: "完成标本图像、标签、采集地点和基础信息的标准化采集。",
    detail: {
      ...baseCourseDetail,
      applicableMajors: ["中药学", "生物信息学"],
      hours: 24,
      credits: 1.5,
      prerequisites: ["中药标本学"],
      teachingMethods: ["流程演示", "上机实践", "质量互检"],
      tags: ["标本数字化", "图像采集", "数据规范"],
    },
  },
  {
    id: "course-005",
    courseNo: "TC-005",
    courseName: "生长环境监测",
    category: "实验课程",
    subject: "生长监测",
    teacher: "王老师",
    term: "2026 秋",
    status: "published",
    updatedAt: "2026-06-25 11:50",
    thumbnail: "/images/herbs/showcase/shihu.png",
    description: "观察温湿度、光照和土壤因子与药材生长状态之间的关系。",
    detail: {
      ...baseCourseDetail,
      applicableMajors: ["中药资源与开发", "生态学"],
      hours: 28,
      credits: 1.5,
      prerequisites: ["植物生理学", "基础生态学"],
      teachingMethods: ["传感器实操", "连续观测", "数据分析"],
      tags: ["环境监测", "生长评价", "传感数据"],
    },
  },
];

export function CourseTable({
  courses,
  loading,
  canEdit,
  canPublish,
  canDelete,
  canEnroll,
  viewMode,
  onEnrollCourse,
  selectedRowKeys,
  onSelectionChange,
  onViewCourse,
  onEditCourse,
  onPublishCourse,
  onOfflineCourse,
  onDeleteCourse,
}: CourseTableProps) {
  const columns: TableColumnsType<CourseRecord> = [
    {
      title: "序号",
      key: "index",
      width: 58,
      render: (_value, _record, index) => index + 1,
    },
    {
      title: "课程名称",
      dataIndex: "courseName",
      key: "courseName",
      width: 250,
      render: (_value, record) => (
        <div className={styles.courseNameCell}>
          <Image
            alt=""
            className={styles.courseThumbnail}
            height={48}
            src={record.thumbnail}
            width={48}
          />
          <div className={styles.courseNameText}>
            <strong title={record.courseName}>{record.courseName}</strong>
            <span>{record.category} · {record.subject}</span>
          </div>
        </div>
      ),
    },
    { title: "学科方向", dataIndex: "subject", key: "subject", ellipsis: true, width: 110 },
    { title: "负责人", dataIndex: "teacher", key: "teacher", width: 90 },
    ...(viewMode === "mine" ? [{
      title: "我的成绩",
      dataIndex: "score",
      key: "score",
      width: 90,
      render: (score: number | undefined) => score == null ? "待批阅" : `${score} 分`,
    }] : []),
    {
      title: "课程状态",
      dataIndex: "status",
      key: "status",
      width: 90,
      render: (status: CourseRecord["status"]) => <TeachingStatusTag status={status} />,
    },
    { title: "更新时间", dataIndex: "updatedAt", key: "updatedAt", ellipsis: true, width: 142 },
    {
      title: "操作",
      key: "actions",
      fixed: "right",
      width: 176,
      render: (_value, record) => (
        <Space size={2}>
          <Button className={styles.actionLink} type="link" onClick={() => onViewCourse(record)}>查看</Button>
          {canEnroll && record.enrollmentStatus === "available" ? <Button className={styles.actionLink} type="link" onClick={() => onEnrollCourse?.(record)}>选课</Button> : null}
          {canEdit ? <Button className={styles.actionLink} type="link" onClick={() => onEditCourse(record)}>编辑</Button> : null}
          <Dropdown
            menu={{
              items: [
                record.status === "published" && canPublish ? { key: "offline", label: "下线" } : null,
                record.status !== "published" && canPublish ? { key: "publish", label: "发布" } : null,
                canDelete ? { key: "delete", label: "删除", danger: true } : null,
              ].filter(Boolean) as MenuProps["items"],
              onClick: ({ key }) => {
                if (key === "publish") onPublishCourse(record);
                if (key === "offline") onOfflineCourse(record);
                if (key === "delete") onDeleteCourse(record);
              },
            }}
            trigger={["click"]}
          >
            <Button className={styles.actionLink} type="link" onClick={(event) => event.preventDefault()}>
              更多 <DownOutlined />
            </Button>
          </Dropdown>
        </Space>
      ),
    },
  ];

  return (
    <Table<CourseRecord>
      className={styles.dataTable}
      columns={columns}
      dataSource={courses}
      locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无课程" /> }}
      loading={loading}
      pagination={{ pageSize: 5, showSizeChanger: false, showTotal: (total) => `共 ${total} 条` }}
      rowKey="id"
      rowSelection={{ selectedRowKeys, onChange: onSelectionChange }}
      scroll={{ x: "max-content" }}
      size="small"
    />
  );
}
