import {
  DownloadOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SettingOutlined,
  UnorderedListOutlined,
} from "@ant-design/icons";
import { Button, Space, Tooltip } from "antd";
import styles from "./teaching.module.css";

type ResearchToolbarProps = {
  hasSelection: boolean;
};

export function ResearchToolbar({ hasSelection }: ResearchToolbarProps) {
  return (
    <div className={styles.researchToolbar}>
      <strong>课题研究</strong>
      <div className={styles.researchToolbarGroups}>
        <Space className={styles.researchToolbarLeft} size={8} wrap>
          <Button icon={<PlusOutlined />} type="primary" onClick={() => undefined}>
            新增
          </Button>
          <Button disabled={!hasSelection} icon={<EditOutlined />} onClick={() => undefined}>
            编辑
          </Button>
          <Button icon={<DownloadOutlined />} onClick={() => undefined}>
            导出
          </Button>
        </Space>
        <Space className={styles.researchToolbarRight} size={4}>
          <Tooltip title="刷新">
            <Button aria-label="刷新课题列表" icon={<ReloadOutlined />} onClick={() => undefined} />
          </Tooltip>
          <Tooltip title="列表视图">
            <Button aria-label="切换课题列表视图" icon={<UnorderedListOutlined />} onClick={() => undefined} />
          </Tooltip>
          <Tooltip title="列表设置">
            <Button aria-label="打开课题列表设置" icon={<SettingOutlined />} onClick={() => undefined} />
          </Tooltip>
        </Space>
      </div>
    </div>
  );
}
