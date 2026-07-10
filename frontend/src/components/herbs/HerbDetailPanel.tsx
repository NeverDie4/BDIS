import { Button, Descriptions, Tabs, Tag } from "antd";
import type { HerbTableRecord } from "./types";
import styles from "./herbs.module.css";

type HerbDetailPanelProps = {
  herb: HerbTableRecord;
  onClose: () => void;
};

export function HerbDetailPanel({ herb, onClose }: HerbDetailPanelProps) {
  return (
    <aside className={styles.detailPanel}>
      <div className={styles.detailHeader}>
        <div className={styles.detailTitleGroup}>
          <h2>{herb.herbName}</h2>
          <p>{herb.aliasName}</p>
        </div>
        <Button type="text" aria-label="关闭药材详情" onClick={onClose}>
          ×
        </Button>
      </div>

      <Tabs
        className={styles.detailTabs}
        items={[
          {
            key: "basic",
            label: "基本信息",
            children: (
              <div className={styles.detailContent}>
                <Descriptions
                  colon
                  column={1}
                  size="small"
                  items={[
                    {
                      key: "name",
                      label: "药材名称",
                      children: herb.herbName,
                    },
                    {
                      key: "alias",
                      label: "别名",
                      children: herb.aliasName,
                    },
                    {
                      key: "category",
                      label: "所属分类",
                      children: herb.categoryName,
                    },
                    {
                      key: "part",
                      label: "药用部位",
                      children: herb.medicinalPart,
                    },
                    {
                      key: "status",
                      label: "状态",
                      children: (
                        <Tag color={herb.status === "enabled" ? "success" : "default"}>
                          {herb.status === "enabled" ? "启用" : "停用"}
                        </Tag>
                      ),
                    },
                    {
                      key: "region",
                      label: "分布地区",
                      children: herb.region,
                    },
                  ]}
                />
              </div>
            ),
          },
          {
            key: "images",
            label: "图片图谱",
            children: <div className={styles.detailContent} />,
          },
          {
            key: "relations",
            label: "关联数据",
            children: <div className={styles.detailContent} />,
          },
          {
            key: "attachments",
            label: "附件资料",
            children: <div className={styles.detailContent} />,
          },
        ]}
      />
    </aside>
  );
}
