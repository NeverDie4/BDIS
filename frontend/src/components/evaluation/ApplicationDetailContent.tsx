import { DownloadOutlined, FileTextOutlined } from "@ant-design/icons";
import { Button, Descriptions, Empty, Progress, Tabs, Timeline } from "antd";
import { StatusTag } from "@/components/common/StatusTag";
import type { EvaluationApplication } from "./types";
import { applicationStatusMeta, calculateMaterialCompleteness, formatFileSize } from "./utils";
import styles from "./evaluation.module.css";

export function ApplicationDetailContent({ application }: { application: EvaluationApplication }) {
  const statusMeta = applicationStatusMeta[application.status];
  const completeness = calculateMaterialCompleteness(application.completenessItems);

  return (
    <div className={styles.detailBody}>
      <div className={styles.detailSummary}>
        <div className={styles.thumbnailPlaceholder}><FileTextOutlined /></div>
        <div>
          <h3>{application.title}</h3>
          <p>{application.applicationNo}</p>
          <StatusTag label={statusMeta.label} status={statusMeta.tagStatus} />
        </div>
      </div>
      <Tabs
        items={[
          { key: "basic", label: "基本信息", children: <Descriptions column={1} size="small">
            <Descriptions.Item label="申报编号">{application.applicationNo}</Descriptions.Item>
            <Descriptions.Item label="申报类型">{application.applicationType}</Descriptions.Item>
            <Descriptions.Item label="申报单位">{application.organizationName}</Descriptions.Item>
            <Descriptions.Item label="联系人">{application.contactName}</Descriptions.Item>
            <Descriptions.Item label="联系电话">{application.contactPhone}</Descriptions.Item>
            <Descriptions.Item label="当前状态">{statusMeta.label}</Descriptions.Item>
            <Descriptions.Item label="当前审核人">{application.reviewerName ?? "未分配"}</Descriptions.Item>
            <Descriptions.Item label="提交时间">{application.submittedAt ?? "未提交"}</Descriptions.Item>
            <Descriptions.Item label="说明">{application.description ?? "暂无说明"}</Descriptions.Item>
          </Descriptions> },
          { key: "source", label: "来源数据", children: <div className={styles.sourceGrid}>
            {Object.entries({ 药材档案: application.sourceSummary.herbArchiveCount, 生长记录: application.sourceSummary.growthRecordCount, 图谱资料: application.sourceSummary.atlasCount, 研究材料: application.sourceSummary.researchMaterialCount, 评价结果: application.sourceSummary.evaluationResultCount }).map(([label, count]) => <div key={label}><span>{label}</span><strong>{count} 项</strong></div>)}
          </div> },
          { key: "attachments", label: "附件材料", children: application.attachments.length > 0 ? <div className={styles.attachmentList}>
            {application.attachments.map((attachment) => <div className={styles.attachmentItem} key={attachment.id} title={attachment.fileName}><FileTextOutlined /><div><strong>{attachment.fileName}</strong><span>{attachment.materialType} · {attachment.fileType} · {formatFileSize(attachment.fileSize)}</span><span>{attachment.uploadedBy} · {attachment.uploadedAt}</span></div><Button disabled icon={<DownloadOutlined />} title="Mock 数据不可下载" type="text" /></div>)}
          </div> : <Empty description="暂无附件材料" /> },
          { key: "review", label: "审核记录", children: application.reviewRecords.length > 0 ? <Timeline items={application.reviewRecords.map((record) => ({ children: <div><strong>{record.action}</strong><p>{record.opinion ?? "暂无审核意见"}</p><span>{record.reviewerName} · {record.reviewedAt}</span></div>, color: record.toStatus === "approved" ? "green" : record.toStatus === "returned" ? "red" : "blue" }))} /> : <Empty description="暂无审核记录" /> },
          { key: "completeness", label: "材料完整度", children: <div className={styles.completenessPanel}><Progress type="circle" percent={completeness.percent} /><p>缺失必填项 {completeness.missingCount} 项</p><ul>{application.completenessItems.map((item) => <li key={item.key}><span>{item.label} · {item.required ? "必填" : "选填"}</span><strong className={item.completed ? styles.complete : styles.incomplete}>{item.completed ? "已完成" : `缺失 ${item.missingCount ?? 1} 项`}</strong></li>)}</ul></div> },
        ]}
      />
    </div>
  );
}
