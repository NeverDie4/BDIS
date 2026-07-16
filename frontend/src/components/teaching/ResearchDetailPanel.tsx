import { CloseOutlined, EditOutlined } from "@ant-design/icons";
import { Button, Descriptions, Empty, Tabs, Tag } from "antd";
import type { ResearchRecord } from "./types";
import styles from "./teaching.module.css";

type Props = { project: ResearchRecord; canEdit?: boolean; onEdit: (project: ResearchRecord) => void; onClose: () => void };

export function ResearchDetailPanel({ project, canEdit, onEdit, onClose }: Props) {
  const detail = project.detail;
  return (
    <aside className={styles.detailPanel} aria-label="课题详情">
      <div className={styles.detailHeader}>
        <h2>课题详情</h2>
        <div className={styles.detailHeaderActions}>
          {canEdit ? <Button icon={<EditOutlined />} size="small" onClick={() => onEdit(project)}>编辑</Button> : null}
          <Button aria-label="关闭课题详情" icon={<CloseOutlined />} size="small" type="text" onClick={onClose} />
        </div>
      </div>
      <div className={styles.detailContent}>
        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="课题编号">{project.projectNo}</Descriptions.Item>
          <Descriptions.Item label="课题名称">{project.projectName}</Descriptions.Item>
          <Descriptions.Item label="负责人">{project.leader}</Descriptions.Item>
          <Descriptions.Item label="研究周期">{project.period || "—"}</Descriptions.Item>
          <Descriptions.Item label="状态"><Tag>{project.status}</Tag></Descriptions.Item>
          <Descriptions.Item label="研究对象">{detail?.speciesName || "—"}</Descriptions.Item>
        </Descriptions>
        <Tabs className={styles.detailTabs} size="small" items={[
          { key: "basic", label: "基本信息", children: detail?.description ? <p>{detail.description}</p> : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无课题简介" /> },
          { key: "members", label: "参与成员", children: detail?.members?.length ? <ul>{detail.members.map((member) => <li key={member.id}>{member.realName || member.username || member.userId} · {member.memberRole}</li>)}</ul> : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无成员" /> },
          { key: "materials", label: "过程资料", children: detail?.materials?.length ? <ul>{detail.materials.map((item) => <li key={item.bindingId}>{item.fileName || `文件 ${item.fileId}`}</li>)}</ul> : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无资料" /> },
        ]} />
      </div>
    </aside>
  );
}
