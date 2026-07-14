import {
  CheckCircleOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  MinusCircleOutlined,
  PlusOutlined,
  BookOutlined,
} from "@ant-design/icons";
import { Button, Space } from "antd";
import styles from "./teaching.module.css";

type CourseActionToolbarProps = {
  hasSelection: boolean;
  canAdd?: boolean;
  canEdit?: boolean;
  canPublish?: boolean;
  canDelete?: boolean;
  canEnroll?: boolean;
  viewMode?: "all" | "mine";
  onViewModeChange?: (mode: "all" | "mine") => void;
  onAdd: () => void;
  onEdit: () => void;
  onPublish: () => void;
  onOffline: () => void;
  onDelete: () => void;
};

export function CourseActionToolbar({
  hasSelection,
  canAdd,
  canEdit,
  canPublish,
  canDelete,
  onAdd,
  onEdit,
  onPublish,
  onOffline,
  onDelete,
  viewMode = "all",
  onViewModeChange,
}: CourseActionToolbarProps) {
  return (
    <div className={styles.sectionToolbar}>
      <strong>实验课程管理</strong>
      <Space size={8} wrap>
        <Button
          icon={<BookOutlined />}
          type={viewMode === "mine" ? "primary" : "default"}
          onClick={() => onViewModeChange?.(viewMode === "mine" ? "all" : "mine")}
        >
          我的课程
        </Button>
        <Button disabled={!canAdd} icon={<PlusOutlined />} type="primary" onClick={onAdd}>
          新增课程
        </Button>
        <Button disabled={!hasSelection || !canEdit} icon={<EditOutlined />} onClick={onEdit}>
          编辑
        </Button>
        <Button disabled={!hasSelection || !canPublish} icon={<CheckCircleOutlined />} onClick={onPublish}>
          发布
        </Button>
        <Button disabled={!hasSelection || !canPublish} icon={<MinusCircleOutlined />} onClick={onOffline}>
          下架
        </Button>
        <Button danger disabled={!hasSelection || !canDelete} icon={<DeleteOutlined />} onClick={onDelete}>
          删除
        </Button>
        <Button icon={<DownloadOutlined />} onClick={() => undefined}>
          导出
        </Button>
      </Space>
    </div>
  );
}
