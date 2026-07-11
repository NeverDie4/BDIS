"use client";

import { App, Button, Descriptions, Form, Input, Modal, Select, Typography } from "antd";
import type { TableProps } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
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
import { fetchDictionaryOptions, type DictionaryOption } from "@/lib/dictionaries";
import {
  createHerbSpecies,
  fetchHerbBases,
  fetchHerbSpecies,
  type HerbBaseApi,
  type HerbSpeciesApi,
  type HerbSpeciesPayload,
} from "@/lib/herbs";
import { getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import styles from "@/styles/mockPages.module.css";

export default function HerbsPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [form] = Form.useForm<HerbSpeciesPayload>();
  const [keyword, setKeyword] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<string>("all");
  const [herbs, setHerbs] = useState<HerbSpeciesApi[]>([]);
  const [bases, setBases] = useState<HerbBaseApi[]>([]);
  const [categoryOptions, setCategoryOptions] = useState<DictionaryOption[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [selectedHerb, setSelectedHerb] = useState<HerbSpeciesApi | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [herbPage, basePage, dictionaryCategories] = await Promise.all([
        fetchHerbSpecies({
          pageNum: 1,
          pageSize: 100,
          keyword: keyword || undefined,
          category: selectedCategory === "all" ? undefined : selectedCategory,
        }),
        fetchHerbBases(),
        fetchDictionaryOptions("herb_category"),
      ]);
      setHerbs(herbPage.records);
      setTotal(herbPage.total);
      setBases(basePage.records);
      setCategoryOptions(dictionaryCategories);
    } catch (error) {
      if (!isAuthRedirectError(error)) message.error(getApiErrorMessage(error, "药材数据加载失败"));
    } finally {
      setLoading(false);
    }
  }, [keyword, message, selectedCategory]);

  useEffect(() => {
    void load();
  }, [load]);

  const categories = useMemo(() => {
    const labels = new Map(categoryOptions.map((option) => [option.value, option.label]));
    herbs.forEach((herb) => {
      if (herb.category && !labels.has(herb.category)) labels.set(herb.category, herb.category);
    });
    return [...labels].map(([value, label]) => ({ label, value }));
  }, [categoryOptions, herbs]);

  const columns: TableProps<HerbSpeciesApi>["columns"] = [
    { title: "药材编号", dataIndex: "herbCode", key: "herbCode", width: 150 },
    { title: "药材名称", dataIndex: "herbName", key: "herbName" },
    { title: "分类", dataIndex: "category", key: "category", render: (value) => value || "未分类" },
    {
      title: "药用部位",
      dataIndex: "medicinalPart",
      key: "medicinalPart",
      render: (value) => value || "-",
    },
    {
      title: "状态",
      key: "status",
      render: (_, record) => <StatusTag status={record.status === 0 ? "disabled" : "normal"} />,
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

  async function submit(values: HerbSpeciesPayload) {
    try {
      await createHerbSpecies({ ...values, status: 1 });
      message.success("药材已创建");
      setModalOpen(false);
      form.resetFields();
      await load();
    } catch (error) {
      message.error(getApiErrorMessage(error, "新增药材失败"));
    }
  }

  return (
    <SiteLayout>
      <div className={styles.pageStack}>
        <PageBanner
          sealText="HERBARIUM RESOURCE"
          title="中药材资源中心"
          subtitle="药材主数据、分类信息、药用部位与种植基地均来自业务接口。"
        />
        <div className={styles.threeGrid}>
          {herbs.slice(0, 3).map((herb) => (
            <HerbCard
              actions={[{ label: "查看详情", onClick: () => setSelectedHerb(herb) }]}
              alias={herb.aliasName}
              efficacy={herb.efficacy}
              key={herb.id}
              medicinalPart={herb.medicinalPart}
              name={herb.herbName}
              tags={herb.category ? [herb.category] : []}
            />
          ))}
        </div>
        <FilterPanel
          description="筛选条件会直接查询后端药材分页接口。"
          extra={<Typography.Text type="secondary">共 {total} 条药材</Typography.Text>}
          title="药材检索"
          onReset={() => {
            setKeyword("");
            setSelectedCategory("all");
          }}
        >
          <SearchBar placeholder="搜索药材名称、编号或别名" value={keyword} onChange={setKeyword} />
          <Select
            options={[{ label: "全部分类", value: "all" }, ...categories]}
            style={{ minWidth: 180 }}
            value={selectedCategory}
            onChange={setSelectedCategory}
          />
        </FilterPanel>
        <ActionToolbar
          actions={
            hasPermission("herb:species:create") ? (
              <Button type="primary" onClick={() => setModalOpen(true)}>
                新增药材
              </Button>
            ) : undefined
          }
          description="数据来自 /api/herb/species/page，写操作按当前用户权限显示。"
          title="药材表格"
        />
        <DataTable<HerbSpeciesApi>
          columns={columns}
          dataSource={herbs}
          loading={loading}
          pagination={false}
          rowKey="id"
        />
        <InfoCard title="启用基地">
          <ul className={styles.compactList}>
            {bases.length ? (
              bases.map((base) => (
                <li key={base.id}>
                  {base.baseName} / {base.regionName || base.address || "区域未配置"} /{" "}
                  {base.contactName || "联系人未配置"}
                </li>
              ))
            ) : (
              <li>暂无基地数据</li>
            )}
          </ul>
        </InfoCard>
      </div>
      <DetailDrawer
        open={Boolean(selectedHerb)}
        title={selectedHerb?.herbName ?? "药材详情"}
        onClose={() => setSelectedHerb(null)}
      >
        {selectedHerb ? (
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="药材编号">{selectedHerb.herbCode}</Descriptions.Item>
            <Descriptions.Item label="别名">{selectedHerb.aliasName || "-"}</Descriptions.Item>
            <Descriptions.Item label="拉丁名">{selectedHerb.latinName || "-"}</Descriptions.Item>
            <Descriptions.Item label="分类">{selectedHerb.category || "未分类"}</Descriptions.Item>
            <Descriptions.Item label="药用部位">
              {selectedHerb.medicinalPart || "-"}
            </Descriptions.Item>
            <Descriptions.Item label="功效">{selectedHerb.efficacy || "-"}</Descriptions.Item>
            <Descriptions.Item label="说明">{selectedHerb.description || "-"}</Descriptions.Item>
          </Descriptions>
        ) : null}
      </DetailDrawer>
      <Modal
        title="新增药材"
        open={modalOpen}
        footer={null}
        destroyOnHidden
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item
            name="herbCode"
            label="药材编号"
            rules={[{ required: true, message: "请输入药材编号" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="herbName"
            label="药材名称"
            rules={[{ required: true, message: "请输入药材名称" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="aliasName" label="别名">
            <Input />
          </Form.Item>
          <Form.Item name="latinName" label="拉丁名">
            <Input />
          </Form.Item>
          <Form.Item name="category" label="分类编码">
            {categoryOptions.length ? (
              <Select allowClear options={categoryOptions} showSearch optionFilterProp="label" />
            ) : (
              <Input />
            )}
          </Form.Item>
          <Form.Item name="medicinalPart" label="药用部位">
            <Input />
          </Form.Item>
          <Form.Item name="efficacy" label="功效">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Button block htmlType="submit" type="primary">
            保存
          </Button>
        </Form>
      </Modal>
    </SiteLayout>
  );
}
