"use client";

import { Button, Descriptions, Modal, Select, Typography } from "antd";
import type { TableProps } from "antd";
import { useMemo, useState } from "react";
import { ActionToolbar } from "@/components/common/ActionToolbar";
import { DataTable } from "@/components/common/DataTable";
import { DetailDrawer } from "@/components/common/DetailDrawer";
import { FilterPanel } from "@/components/common/FilterPanel";
import { InfoCard } from "@/components/common/InfoCard";
import { SearchBar } from "@/components/common/SearchBar";
import { StatusTag } from "@/components/common/StatusTag";
import { HerbCard } from "@/components/feature/HerbCard";
import { PageBanner } from "@/components/layout/PageBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import { herbBases, herbCategories, herbSpeciesList } from "@/mocks/herbs";
import type { HerbSpecies } from "@/types/herb";
import styles from "@/styles/mockPages.module.css";

export default function HerbsPage() {
  const [keyword, setKeyword] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<string>("all");
  const [selectedHerb, setSelectedHerb] = useState<HerbSpecies | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const filteredHerbs = useMemo(() => {
    return herbSpeciesList.filter((herb) => {
      const matchedKeyword =
        keyword.length === 0 ||
        herb.herbName.includes(keyword) ||
        herb.herbNo.includes(keyword) ||
        herb.aliasNames.some((alias) => alias.includes(keyword));
      const matchedCategory = selectedCategory === "all" || herb.categoryId === selectedCategory;

      return matchedKeyword && matchedCategory;
    });
  }, [keyword, selectedCategory]);

  const columns: TableProps<HerbSpecies>["columns"] = [
    { title: "药材编号", dataIndex: "herbNo", key: "herbNo", width: 150 },
    { title: "药材名称", dataIndex: "herbName", key: "herbName" },
    { title: "分类", dataIndex: "categoryName", key: "categoryName" },
    { title: "药用部位", dataIndex: "medicinalPart", key: "medicinalPart" },
    {
      title: "状态",
      key: "status",
      render: (_, record) => <StatusTag status={record.status} />,
    },
    {
      title: "操作",
      key: "actions",
      render: (_, record) => (
        <Button type="link" onClick={() => setSelectedHerb(record)}>
          查看详情
        </Button>
      ),
    },
  ];

  return (
    <SiteLayout>
      <div className={styles.pageStack}>
        <PageBanner
          sealText="HERBARIUM RESOURCE"
          title="中药材资源中心"
          subtitle="以标本档案为线索，汇集中药材基础信息、分类、药用部位、产地说明和图像资源。"
        />

        <div className={styles.threeGrid}>
          {herbSpeciesList.slice(0, 3).map((herb) => (
            <HerbCard
              actions={[{ label: "查看详情", onClick: () => setSelectedHerb(herb) }]}
              alias={herb.aliasNames.join("、")}
              efficacy={herb.efficacy}
              image={herb.imageUrl}
              key={herb.id}
              medicinalPart={herb.medicinalPart}
              name={herb.herbName}
              tags={herb.tags}
            />
          ))}
        </div>

        <FilterPanel
          description="当前为前端 mock 筛选，后续接入 /api/herbs 分页查询。"
          extra={<Typography.Text type="secondary">共 {filteredHerbs.length} 条药材</Typography.Text>}
          title="药材检索"
          onReset={() => {
            setKeyword("");
            setSelectedCategory("all");
          }}
        >
          <SearchBar placeholder="搜索药材名称、编号或别名" value={keyword} onChange={setKeyword} />
          <Select
            options={[
              { label: "全部分类", value: "all" },
              ...herbCategories.map((category) => ({
                label: category.categoryName,
                value: String(category.id),
              })),
            ]}
            style={{ minWidth: 180 }}
            value={selectedCategory}
            onChange={setSelectedCategory}
          />
        </FilterPanel>

        <ActionToolbar
          actions={
            <div className={styles.toolbarActions}>
              <Button onClick={() => setModalOpen(true)}>导出药材目录</Button>
              <Button type="primary" onClick={() => setModalOpen(true)}>
                新增药材
              </Button>
            </div>
          }
          description="展示药材主数据，点击查看详情可打开抽屉。"
          title="药材表格"
        />
        <DataTable<HerbSpecies> columns={columns} dataSource={filteredHerbs} pagination={false} rowKey="id" />

        <div className={styles.twoGrid}>
          <InfoCard title="药材分类">
            <ul className={styles.compactList}>
              {herbCategories.map((category) => (
                <li key={category.id}>{category.categoryName}：{category.description}</li>
              ))}
            </ul>
          </InfoCard>
          <InfoCard title="基地信息">
            <ul className={styles.compactList}>
              {herbBases.map((base) => (
                <li key={base.id}>{base.baseName} / {base.district} / {base.manager}</li>
              ))}
            </ul>
          </InfoCard>
        </div>
      </div>

      <DetailDrawer
        open={Boolean(selectedHerb)}
        title={selectedHerb?.herbName ?? "药材详情"}
        onClose={() => setSelectedHerb(null)}
      >
        {selectedHerb ? (
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="药材编号">{selectedHerb.herbNo}</Descriptions.Item>
            <Descriptions.Item label="别名">{selectedHerb.aliasNames.join("、")}</Descriptions.Item>
            <Descriptions.Item label="拉丁名">{selectedHerb.latinName}</Descriptions.Item>
            <Descriptions.Item label="分类">{selectedHerb.categoryName}</Descriptions.Item>
            <Descriptions.Item label="药用部位">{selectedHerb.medicinalPart}</Descriptions.Item>
            <Descriptions.Item label="功效">{selectedHerb.efficacy}</Descriptions.Item>
            <Descriptions.Item label="适宜环境">{selectedHerb.suitableEnvironment}</Descriptions.Item>
          </Descriptions>
        ) : null}
      </DetailDrawer>

      <Modal footer={null} open={modalOpen} title="功能占位" onCancel={() => setModalOpen(false)}>
        <Typography.Paragraph className={styles.mutedText}>
          新增、编辑、导出等操作将在后续接口联调阶段接入真实表单与权限控制。
        </Typography.Paragraph>
      </Modal>
    </SiteLayout>
  );
}
