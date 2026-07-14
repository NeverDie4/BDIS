import {
  CheckCircleOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  StopOutlined,
  TableOutlined,
} from "@ant-design/icons";
import { Button, Space } from "antd";
import styles from "./herbs.module.css";

export function HerbActionToolbar() {
  return (
    <div className={styles.toolbar}>
      <Space className={styles.toolbarLeft} size={8} wrap>
        <Button disabled icon={<PlusOutlined />} title="功能开发中" type="primary">
          新增
        </Button>
        <Button disabled icon={<EditOutlined />} onClick={() => undefined}>
          编辑
        </Button>
        <Button disabled icon={<CheckCircleOutlined />} onClick={() => undefined}>
          启用
        </Button>
        <Button disabled icon={<StopOutlined />} onClick={() => undefined}>
          停用
        </Button>
        <Button danger disabled icon={<DeleteOutlined />} onClick={() => undefined}>
          删除
        </Button>
        <Button icon={<DownloadOutlined />} onClick={() => undefined}>
          导出
        </Button>
      </Space>
      <Space className={styles.toolbarRight} size={8} wrap>
        <Button icon={<ReloadOutlined />} onClick={() => undefined}>
          刷新
        </Button>
        <Button icon={<TableOutlined />} onClick={() => undefined}>
          表格视图
        </Button>
      </Space>
    </div>
  );
}
