import {
  CheckCircleOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  MinusCircleOutlined,
  PlusOutlined,
} from "@ant-design/icons";
import { Button, Space } from "antd";
import styles from "./teaching.module.css";

type CourseActionToolbarProps = {
  hasSelection: boolean;
};

export function CourseActionToolbar({ hasSelection }: CourseActionToolbarProps) {
  return (
    <div className={styles.sectionToolbar}>
      <strong>实验课程管理</strong>
      <Space size={8} wrap>
        <Button icon={<PlusOutlined />} type="primary" onClick={() => undefined}>
          新增课程
        </Button>
        <Button disabled={!hasSelection} icon={<EditOutlined />} onClick={() => undefined}>
          编辑
        </Button>
        <Button disabled={!hasSelection} icon={<CheckCircleOutlined />} onClick={() => undefined}>
          发布
        </Button>
        <Button disabled={!hasSelection} icon={<MinusCircleOutlined />} onClick={() => undefined}>
          下架
        </Button>
        <Button danger disabled={!hasSelection} icon={<DeleteOutlined />} onClick={() => undefined}>
          删除
        </Button>
        <Button icon={<DownloadOutlined />} onClick={() => undefined}>
          导出
        </Button>
      </Space>
    </div>
  );
}
