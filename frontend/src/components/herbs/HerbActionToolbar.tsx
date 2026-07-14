import {
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import { Button, Space } from "antd";
import styles from "./herbs.module.css";

type HerbActionToolbarProps = {
  canCreate?: boolean;
  canEdit?: boolean;
  canDelete?: boolean;
  selectedCount?: number;
  createLabel?: string;
  onCreate: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onExport: () => void;
  onRefresh: () => void;
};

export function HerbActionToolbar({
  canCreate = false,
  canEdit = false,
  canDelete = false,
  selectedCount = 0,
  createLabel = "新增",
  onCreate,
  onEdit,
  onDelete,
  onExport,
  onRefresh,
}: HerbActionToolbarProps) {
  const hasSingleSelection = selectedCount === 1;
  const hasSelection = selectedCount > 0;

  return (
    <div className={styles.toolbar}>
      <Space className={styles.toolbarLeft} size={8} wrap>
        {canCreate ? (
          <Button icon={<PlusOutlined />} onClick={onCreate} type="primary">
            {createLabel}
          </Button>
        ) : null}
        {canEdit ? (
          <Button disabled={!hasSingleSelection} icon={<EditOutlined />} onClick={onEdit}>
            编辑
          </Button>
        ) : null}
        {canDelete ? (
          <Button danger disabled={!hasSelection} icon={<DeleteOutlined />} onClick={onDelete}>
            删除
          </Button>
        ) : null}
        <Button icon={<DownloadOutlined />} onClick={onExport}>
          导出
        </Button>
      </Space>
      <Space className={styles.toolbarRight} size={8} wrap>
        <Button icon={<ReloadOutlined />} onClick={onRefresh}>
          刷新
        </Button>
      </Space>
    </div>
  );
}
