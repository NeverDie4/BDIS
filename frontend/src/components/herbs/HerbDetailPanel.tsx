import { Button, Descriptions, Tabs, Tag } from "antd";
import type { HerbTableRecord } from "./types";
import styles from "./herbs.module.css";

type HerbDetailPanelProps = {
  herb: HerbTableRecord;
  onClose: () => void;
};

function valueOrDash(value?: string | number) {
  return value == null || value === "" ? "-" : value;
}

export function HerbDetailPanel({ herb, onClose }: HerbDetailPanelProps) {
  return (
    <aside className={styles.detailPanel}>
      <div className={styles.detailHeader}>
        <div className={styles.detailTitleGroup}>
          <h2>{herb.herbName}</h2>
          <p>{herb.aliasName || herb.herbCode}</p>
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
                      key: "code",
                      label: "药材编号",
                      children: valueOrDash(herb.herbCode),
                    },
                    {
                      key: "name",
                      label: "药材名称",
                      children: valueOrDash(herb.herbName),
                    },
                    {
                      key: "latin",
                      label: "拉丁名",
                      children: valueOrDash(herb.latinName),
                    },
                    {
                      key: "alias",
                      label: "别名",
                      children: valueOrDash(herb.aliasName),
                    },
                    {
                      key: "category",
                      label: "所属分类",
                      children: valueOrDash(herb.categoryName || herb.category),
                    },
                    {
                      key: "part",
                      label: "药用部位",
                      children: valueOrDash(herb.medicinalPart),
                    },
                    {
                      key: "efficacy",
                      label: "功效",
                      children: valueOrDash(herb.efficacy),
                    },
                    {
                      key: "status",
                      label: "状态",
                      children: (
                        <Tag color={herb.status === 1 ? "success" : "default"}>
                          {herb.statusText || (herb.status === 1 ? "启用" : "停用")}
                        </Tag>
                      ),
                    },
                    {
                      key: "region",
                      label: "分布地区",
                      children: valueOrDash(herb.distributionRegionText),
                    },
                    {
                      key: "description",
                      label: "描述",
                      children: valueOrDash(herb.description),
                    },
                  ]}
                />
              </div>
            ),
          },
          {
            key: "images",
            label: "图片图鉴",
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