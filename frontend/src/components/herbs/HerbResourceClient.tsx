"use client";

import { App, Button, Form, Input, InputNumber, Modal, Select, Table } from "antd";
import type { TableColumnsType } from "antd";
import type { Key } from "react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { FileUploadField } from "@/components/file/FileUploadField";
import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
import { escapeCsvCell } from "@/lib/csv";
import {
  createHerbBase,
  createHerbCategory,
  createHerbSpecies,
  deleteHerbBase,
  deleteHerbCategory,
  deleteHerbSpecies,
  fetchHerbBases,
  fetchHerbCategories,
  fetchHerbSpecies,
  updateHerbBase,
  updateHerbCategory,
  updateHerbSpecies,
  type DictItemApi,
  type DictItemPayload,
  type HerbBaseApi,
  type HerbBasePayload,
  type HerbSpeciesUpdatePayload,
} from "@/lib/herbs";
import { getApiErrorMessage } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import { HerbActionToolbar } from "./HerbActionToolbar";
import { HerbDetailPanel } from "./HerbDetailPanel";
import { HerbFilterBar, type HerbFilterValues } from "./HerbFilterBar";
import { HerbResourceTabs, type HerbTabKey } from "./HerbResourceTabs";
import { HerbTable } from "./HerbTable";
import type { HerbTableRecord } from "./types";
import styles from "./herbs.module.css";

type ModalMode = "create" | "edit";
type EditableRecord = HerbTableRecord | DictItemApi | HerbBaseApi;
type HerbResourceClientProps = {
  initialKeyword?: string;
};

const T = {
  create: "\u65b0\u589e",
  edit: "\u7f16\u8f91",
  save: "\u4fdd\u5b58",
  cancel: "\u53d6\u6d88",
  delete: "\u5220\u9664",
  selectOne: "\u8bf7\u5148\u9009\u62e9\u4e00\u6761\u8bb0\u5f55",
  selectDelete: "\u8bf7\u5148\u9009\u62e9\u8981\u5220\u9664\u7684\u8bb0\u5f55",
  confirmDelete: "\u786e\u8ba4\u5220\u9664",
  deleteSuccess: "\u5220\u9664\u6210\u529f",
  deleteFail: "\u5220\u9664\u5931\u8d25",
  saveSuccess: "\u4fdd\u5b58\u6210\u529f",
  createSuccess: "\u65b0\u589e\u6210\u529f",
  saveFail: "\u4fdd\u5b58\u5931\u8d25",
  herb: "\u836f\u6750",
  category: "\u5206\u7c7b",
  base: "\u57fa\u5730",
  title: "\u4e2d\u836f\u6750\u8d44\u6e90\u4e2d\u5fc3",
  desc: "\u7ba1\u7406\u4e2d\u836f\u6750\u57fa\u7840\u8d44\u6599\u3001\u5206\u7c7b\u3001\u57fa\u5730\u4fe1\u606f\uff0c\u652f\u6301\u836f\u6750\u8d44\u6e90\u5168\u751f\u547d\u5468\u671f\u7ba1\u7406\u3002",
  seal: "\u672c\u8349",
  speciesLoadFail: "\u836f\u6750\u5217\u8868\u52a0\u8f7d\u5931\u8d25",
  categoryLoadFail: "\u836f\u6750\u5206\u7c7b\u52a0\u8f7d\u5931\u8d25",
  categoryOptionFail: "\u836f\u6750\u5206\u7c7b\u9009\u9879\u52a0\u8f7d\u5931\u8d25",
  baseLoadFail: "\u57fa\u5730\u5217\u8868\u52a0\u8f7d\u5931\u8d25",
  exportedHerb: "\u5df2\u5bfc\u51fa\u5f53\u524d\u836f\u6750\u5217\u8868",
  exportedCategory: "\u5df2\u5bfc\u51fa\u5f53\u524d\u5206\u7c7b\u5217\u8868",
  exportedBase: "\u5df2\u5bfc\u51fa\u5f53\u524d\u57fa\u5730\u5217\u8868",
  index: "\u5e8f\u53f7",
  categoryCode: "\u5206\u7c7b\u7f16\u7801",
  categoryName: "\u5206\u7c7b\u540d\u79f0",
  itemValue: "\u5b57\u5178\u503c",
  sortOrder: "\u6392\u5e8f",
  remark: "\u5907\u6ce8",
  action: "\u64cd\u4f5c",
  baseNo: "\u57fa\u5730\u7f16\u53f7",
  baseName: "\u57fa\u5730\u540d\u79f0",
  baseType: "\u57fa\u5730\u7c7b\u578b",
  region: "\u6240\u5728\u5730\u533a",
  address: "\u5730\u5740",
  contact: "\u8054\u7cfb\u4eba",
  phone: "\u8054\u7cfb\u7535\u8bdd",
  herbCode: "\u836f\u6750\u7f16\u53f7",
  herbName: "\u836f\u6750\u540d\u79f0",
  latinName: "\u62c9\u4e01\u540d",
  aliasName: "\u522b\u540d",
  medicinalPart: "\u836f\u7528\u90e8\u4f4d",
  efficacy: "\u529f\u6548",
  description: "\u63cf\u8ff0",
  regionId: "\u5730\u533a ID",
  longitude: "\u7ecf\u5ea6",
  latitude: "\u7eac\u5ea6",
  requiredHerbCode: "\u8bf7\u8f93\u5165\u836f\u6750\u7f16\u53f7",
  requiredHerbName: "\u8bf7\u8f93\u5165\u836f\u6750\u540d\u79f0",
  requiredCategoryCode: "\u8bf7\u8f93\u5165\u5206\u7c7b\u7f16\u7801",
  requiredCategoryName: "\u8bf7\u8f93\u5165\u5206\u7c7b\u540d\u79f0",
  requiredBaseNo: "\u8bf7\u8f93\u5165\u57fa\u5730\u7f16\u53f7",
  requiredBaseName: "\u8bf7\u8f93\u5165\u57fa\u5730\u540d\u79f0",
};

const baseTypeOptions = [
  { label: "\u79cd\u690d\u57fa\u5730", value: "planting" },
  { label: "\u793a\u8303\u57fa\u5730", value: "demo" },
  { label: "\u4fdd\u62a4\u57fa\u5730", value: "protection" },
  { label: "\u79d1\u7814\u57fa\u5730", value: "research" },
];

function flattenCategories(items: DictItemApi[]): DictItemApi[] {
  return items.flatMap((item) => [item, ...flattenCategories(item.children ?? [])]);
}

function compactObject<T extends Record<string, unknown>>(value: T): T {
  return Object.fromEntries(
    Object.entries(value).filter(([, item]) => item !== undefined && item !== ""),
  ) as T;
}

function downloadCsv(
  filename: string,
  rows: Array<Record<string, string | number | undefined>>,
) {
  if (typeof window === "undefined") return;
  const headers = Object.keys(rows[0] ?? {});
  const csvRows = [
    headers.map((header) => escapeCsvCell(header)).join(","),
    ...rows.map((row) =>
      headers.map((header) => escapeCsvCell(row[header])).join(","),
    ),
  ];
  const blob = new Blob([`\uFEFF${csvRows.join("\n")}`], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export function HerbResourceClient({ initialKeyword }: HerbResourceClientProps) {
  const { message, modal } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [form] = Form.useForm();
  const [activeTab, setActiveTab] = useState<HerbTabKey>("species");
  const [selectedHerb, setSelectedHerb] = useState<HerbTableRecord | null>(null);
  const [records, setRecords] = useState<HerbTableRecord[]>([]);
  const [categories, setCategories] = useState<DictItemApi[]>([]);
  const [categoryChoices, setCategoryChoices] = useState<DictItemApi[]>([]);
  const [bases, setBases] = useState<HerbBaseApi[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [editingRecord, setEditingRecord] = useState<EditableRecord | null>(null);
  const [modalMode, setModalMode] = useState<ModalMode>("create");
  const [formOpen, setFormOpen] = useState(false);
  const [filters, setFilters] = useState<HerbFilterValues>(() =>
    initialKeyword?.trim() ? { keyword: initialKeyword.trim() } : {},
  );
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  const canCreate =
    activeTab === "species"
      ? hasPermission("herb:species:create")
      : activeTab === "categories"
        ? hasPermission("dictionary:manage")
        : hasPermission("map:base:manage");
  const canEdit =
    activeTab === "species"
      ? hasPermission("herb:species:update")
      : activeTab === "categories"
        ? hasPermission("dictionary:manage")
        : hasPermission("map:base:manage");
  const canDelete =
    activeTab === "species"
      ? hasPermission("herb:species:delete")
      : activeTab === "categories"
        ? hasPermission("dictionary:manage")
        : hasPermission("map:base:manage");

  const queryParams = useMemo(
    () => ({
      pageNum: page,
      pageSize,
      keyword: filters.keyword,
      category: filters.category,
      medicinalPart: filters.medicinalPart,
    }),
    [filters.category, filters.keyword, filters.medicinalPart, page, pageSize],
  );

  const categoryOptions = useMemo(
    () => categoryChoices.map((item) => ({ label: item.itemName, value: item.itemCode })),
    [categoryChoices],
  );

  const currentSelection = useMemo(() => {
    const selectedKey = selectedRowKeys[0];
    if (selectedKey === undefined) return null;
    const id = Number(selectedKey);
    if (activeTab === "species") return records.find((item) => item.id === id) ?? null;
    if (activeTab === "categories") return categories.find((item) => item.id === id) ?? null;
    return bases.find((item) => item.id === id) ?? null;
  }, [activeTab, bases, categories, records, selectedRowKeys]);

  const loadSpecies = useCallback(async () => {
    setLoading(true);
    try {
      const result = await fetchHerbSpecies(queryParams);
      const nextRecords = result.records;
      setRecords(nextRecords);
      setTotal(result.total);
      setSelectedHerb((current) => (current ? nextRecords.find((item) => item.id === current.id) || null : current));
    } catch (error) {
      message.error(getApiErrorMessage(error, T.speciesLoadFail));
    } finally {
      setLoading(false);
    }
  }, [message, queryParams]);

  const loadCategories = useCallback(async () => {
    setLoading(true);
    try {
      const result = flattenCategories(await fetchHerbCategories());
      setCategories(result);
      setCategoryChoices(result);
      setTotal(result.length);
    } catch (error) {
      message.error(getApiErrorMessage(error, T.categoryLoadFail));
    } finally {
      setLoading(false);
    }
  }, [message]);

  const loadCategoryChoices = useCallback(async () => {
    try {
      setCategoryChoices(flattenCategories(await fetchHerbCategories()));
    } catch (error) {
      message.error(getApiErrorMessage(error, T.categoryOptionFail));
    }
  }, [message]);

  const loadBases = useCallback(async () => {
    setLoading(true);
    try {
      const result = await fetchHerbBases({ page, size: pageSize });
      setBases(result.records);
      setTotal(result.total);
    } catch (error) {
      message.error(getApiErrorMessage(error, T.baseLoadFail));
    } finally {
      setLoading(false);
    }
  }, [message, page, pageSize]);

  const reloadCurrentTab = useCallback(async () => {
    if (activeTab === "species") return loadSpecies();
    if (activeTab === "categories") return loadCategories();
    return loadBases();
  }, [activeTab, loadBases, loadCategories, loadSpecies]);

  useEffect(() => {
    void loadCategoryChoices();
  }, [loadCategoryChoices]);

  useEffect(() => {
    const keyword = initialKeyword?.trim() || undefined;
    setFilters((current) =>
      current.keyword === keyword ? current : { ...current, keyword },
    );
    setPage(1);
  }, [initialKeyword]);

  useEffect(() => {
    setSelectedRowKeys([]);
    setSelectedHerb(null);
    setPage(1);
  }, [activeTab]);

  useEffect(() => {
    void reloadCurrentTab();
  }, [reloadCurrentTab]);

  function handleSearch(values: HerbFilterValues) {
    setFilters(values);
    setPage(1);
  }

  function handlePageChange(nextPage: number, nextPageSize: number) {
    setPage(nextPage);
    setPageSize(nextPageSize);
  }

  function openCreateModal() {
    setModalMode("create");
    setEditingRecord(null);
    form.resetFields();
    setFormOpen(true);
  }

  function openEditModal(record: EditableRecord | null = currentSelection) {
    if (!record) {
      message.warning(T.selectOne);
      return;
    }
    setModalMode("edit");
    setEditingRecord(record);
    form.resetFields();
    form.setFieldsValue(record);
    setFormOpen(true);
  }

  function handleDelete() {
    if (selectedRowKeys.length === 0) {
      message.warning(T.selectDelete);
      return;
    }
    modal.confirm({
      title: T.confirmDelete,
      content: `\u5c06\u5220\u9664\u9009\u4e2d\u7684 ${selectedRowKeys.length} \u6761\u8bb0\u5f55\uff0c\u5220\u9664\u540e\u4e0d\u53ef\u6062\u590d\u3002`,
      okText: T.delete,
      okButtonProps: { danger: true },
      cancelText: T.cancel,
      onOk: async () => {
        const results = await Promise.allSettled(
          selectedRowKeys.map((key) => {
            const id = Number(key);
            if (activeTab === "species") return deleteHerbSpecies(id);
            if (activeTab === "categories") return deleteHerbCategory(id);
            return deleteHerbBase(id);
          }),
        );
        const successCount = results.filter((result) => result.status === "fulfilled").length;
        const failedCount = results.length - successCount;

        if (failedCount === 0) {
          message.success(T.deleteSuccess);
        } else if (successCount > 0) {
          message.warning(`成功删除 ${successCount} 条，${failedCount} 条删除失败`);
        } else {
          const firstFailure = results.find((result) => result.status === "rejected");
          message.error(
            getApiErrorMessage(
              firstFailure?.status === "rejected" ? firstFailure.reason : undefined,
              T.deleteFail,
            ),
          );
        }
        setSelectedRowKeys([]);
        await reloadCurrentTab();
      },
    });
  }

  async function handleSubmit() {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      if (activeTab === "species") {
        const payload = compactObject<HerbSpeciesUpdatePayload>({
          herbName: values.herbName,
          latinName: values.latinName,
          aliasName: values.aliasName,
          category: values.category,
          medicinalPart: values.medicinalPart,
          efficacy: values.efficacy,
          description: values.description,
          coverImageUrl: values.coverImageUrl,
        });
        if (modalMode === "edit" && editingRecord) await updateHerbSpecies(editingRecord.id, payload);
        else await createHerbSpecies({ ...payload, herbCode: values.herbCode });
      } else if (activeTab === "categories") {
        const payload = compactObject<DictItemPayload>({
          itemCode: values.itemCode,
          itemName: values.itemName,
          itemValue: values.itemValue,
          parentId: values.parentId,
          sortOrder: values.sortOrder,

          remark: values.remark,
        });
        if (modalMode === "edit" && editingRecord) await updateHerbCategory(editingRecord.id, payload);
        else await createHerbCategory(payload);
      } else {
        const payload = compactObject<HerbBasePayload>({
          baseNo: values.baseNo,
          baseName: values.baseName,
          baseType: values.baseType,
          regionId: values.regionId,
          address: values.address,
          longitude: values.longitude,
          latitude: values.latitude,
          contactName: values.contactName,
          contactPhone: values.contactPhone,
          description: values.description,
          remark: values.remark,
        });
        if (modalMode === "edit" && editingRecord) await updateHerbBase(editingRecord.id, payload);
        else await createHerbBase(payload);
      }
      message.success(modalMode === "edit" ? T.saveSuccess : T.createSuccess);
      setFormOpen(false);
      setSelectedRowKeys([]);
      await reloadCurrentTab();
    } catch (error) {
      if (error && typeof error === "object" && "errorFields" in error) return;
      message.error(getApiErrorMessage(error, T.saveFail));
    } finally {
      setSubmitting(false);
    }
  }

  function handleExport() {
    if (activeTab === "species") {
      downloadCsv(
        "herb-species.csv",
        records.map((item) => ({
          [T.herbCode]: item.herbCode,
          [T.herbName]: item.herbName,
          [T.aliasName]: item.aliasName,
          [T.categoryName]: item.categoryName || item.category,
          [T.medicinalPart]: item.medicinalPart,
          ["\u5206\u5e03\u5730\u533a"]: item.distributionRegionText,
        })),
      );
      message.success(T.exportedHerb);
      return;
    }
    if (activeTab === "categories") {
      downloadCsv(
        "herb-categories.csv",
        categories.map((item) => ({
          [T.categoryCode]: item.itemCode,
          [T.categoryName]: item.itemName,
          [T.itemValue]: item.itemValue,
          [T.sortOrder]: item.sortOrder,

          [T.remark]: item.remark,
        })),
      );
      message.success(T.exportedCategory);
      return;
    }
    downloadCsv(
      "herb-bases.csv",
      bases.map((item) => ({
        [T.baseNo]: item.baseNo,
        [T.baseName]: item.baseName,
        [T.baseType]: item.baseType,
        [T.region]: item.regionName,
        [T.address]: item.address,
        [T.contact]: item.contactName,
        [T.phone]: item.contactPhone,
      })),
    );
    message.success(T.exportedBase);
  }

  const categoryColumns: TableColumnsType<DictItemApi> = [
    { title: T.index, key: "index", width: "5%", render: (_value, _record, index) => index + 1 },
    { title: T.categoryCode, dataIndex: "itemCode", key: "itemCode", width: "15%" },
    { title: T.categoryName, dataIndex: "itemName", key: "itemName", width: "16%" },
    { title: T.itemValue, dataIndex: "itemValue", key: "itemValue", width: "14%", render: (value?: string) => value || "-" },
    { title: T.sortOrder, dataIndex: "sortOrder", key: "sortOrder", width: "8%", render: (value?: number) => value ?? "-" },

    { title: T.remark, dataIndex: "remark", key: "remark", ellipsis: true, width: "28%", render: (value?: string) => value || "-" },
    { title: T.action, key: "actions", fixed: "right", width: "10%", render: (_value, record) => canEdit ? <Button type="link" onClick={() => openEditModal(record)}>{T.edit}</Button> : "-" },
  ];

  const baseColumns: TableColumnsType<HerbBaseApi> = [
    { title: T.index, key: "index", width: "5%", render: (_value, _record, index) => (page - 1) * pageSize + index + 1 },
    { title: T.baseNo, dataIndex: "baseNo", key: "baseNo", width: "11%" },
    { title: T.baseName, dataIndex: "baseName", key: "baseName", width: "13%" },
    { title: T.baseType, dataIndex: "baseType", key: "baseType", width: "10%", render: (value?: string) => value || "-" },
    { title: T.region, dataIndex: "regionName", key: "regionName", width: "11%", render: (value?: string) => value || "-" },
    { title: T.address, dataIndex: "address", key: "address", ellipsis: true, width: "18%", render: (value?: string) => value || "-" },
    { title: T.contact, dataIndex: "contactName", key: "contactName", width: "9%", render: (value?: string) => value || "-" },
    { title: T.phone, dataIndex: "contactPhone", key: "contactPhone", width: "11%", render: (value?: string) => value || "-" },
    { title: T.action, key: "actions", fixed: "right", width: "8%", render: (_value, record) => canEdit ? <Button type="link" onClick={() => openEditModal(record)}>{T.edit}</Button> : "-" },
  ];

  function renderTable() {
    if (activeTab === "species") {
      return <HerbTable canEdit={canEdit} loading={loading} records={records} page={page} pageSize={pageSize} total={total} selectedRowKeys={selectedRowKeys} onSelectionChange={setSelectedRowKeys} onPageChange={handlePageChange} onView={setSelectedHerb} onEdit={openEditModal} />;
    }
    if (activeTab === "categories") {
      return <Table<DictItemApi> bordered={false} className={styles.table} columns={categoryColumns} dataSource={categories} loading={loading} pagination={false} rowKey="id" rowSelection={{ columnWidth: "4%", selectedRowKeys, onChange: setSelectedRowKeys }} scroll={{ x: 900 }} size="middle" sticky tableLayout="fixed" />;
    }
    return <Table<HerbBaseApi> bordered={false} className={styles.table} columns={baseColumns} dataSource={bases} loading={loading} pagination={{ current: page, pageSize, total, showSizeChanger: true, onChange: handlePageChange }} rowKey="id" rowSelection={{ columnWidth: "4%", selectedRowKeys, onChange: setSelectedRowKeys }} scroll={{ x: 1200 }} size="middle" sticky tableLayout="fixed" />;
  }

  function renderFormFields() {
    if (activeTab === "species") {
      return <>
        <Form.Item name="herbCode" label={T.herbCode} rules={[{ required: true, message: T.requiredHerbCode }]}><Input disabled={modalMode === "edit"} maxLength={64} /></Form.Item>
        <Form.Item name="herbName" label={T.herbName} rules={[{ required: true, message: T.requiredHerbName }]}><Input maxLength={100} /></Form.Item>
        <Form.Item name="latinName" label={T.latinName}><Input maxLength={150} /></Form.Item>
        <Form.Item name="aliasName" label={T.aliasName}><Input maxLength={150} /></Form.Item>
        <Form.Item name="category" label={T.categoryName}><Select allowClear options={categoryOptions} /></Form.Item>
        <Form.Item name="medicinalPart" label={T.medicinalPart}><Input maxLength={100} /></Form.Item>
        <Form.Item name="efficacy" label={T.efficacy}><Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} /></Form.Item>
        <Form.Item name="description" label={T.description}><Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} /></Form.Item>
        <Form.Item name="coverImageUrl" label="封面图片">
          <FileUploadField
            accept="image/*"
            accessLevel="private"
            buttonText="上传药材图片"
            cleanupUnboundOnUnmount={false}
            fileUsage="cover"
            maxSizeMB={10}
          />
        </Form.Item>
      </>;
    }
    if (activeTab === "categories") {
      return <>
        <Form.Item name="itemCode" label={T.categoryCode} rules={[{ required: true, message: T.requiredCategoryCode }]}><Input maxLength={64} /></Form.Item>
        <Form.Item name="itemName" label={T.categoryName} rules={[{ required: true, message: T.requiredCategoryName }]}><Input maxLength={100} /></Form.Item>
        <Form.Item name="itemValue" label={T.itemValue}><Input maxLength={100} /></Form.Item>
        <Form.Item name="sortOrder" label={T.sortOrder}><InputNumber min={0} precision={0} style={{ width: "100%" }} /></Form.Item>

        <Form.Item name="remark" label={T.remark}><Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} /></Form.Item>
      </>;
    }
    return <>
      <Form.Item name="baseNo" label={T.baseNo} rules={[{ required: true, message: T.requiredBaseNo }]}><Input maxLength={64} /></Form.Item>
      <Form.Item name="baseName" label={T.baseName} rules={[{ required: true, message: T.requiredBaseName }]}><Input maxLength={150} /></Form.Item>
      <Form.Item name="baseType" label={T.baseType}><Select allowClear options={baseTypeOptions} /></Form.Item>
      <Form.Item name="regionId" label={T.regionId}><InputNumber min={1} precision={0} style={{ width: "100%" }} /></Form.Item>
      <Form.Item name="address" label={T.address}><Input /></Form.Item>
      <Form.Item name="longitude" label={T.longitude}><InputNumber precision={6} style={{ width: "100%" }} /></Form.Item>
      <Form.Item name="latitude" label={T.latitude}><InputNumber precision={6} style={{ width: "100%" }} /></Form.Item>
      <Form.Item name="contactName" label={T.contact}><Input maxLength={100} /></Form.Item>
      <Form.Item name="contactPhone" label={T.phone}><Input maxLength={50} /></Form.Item>
      <Form.Item name="description" label={T.description}><Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} /></Form.Item>
      <Form.Item name="remark" label={T.remark}><Input.TextArea autoSize={{ minRows: 2, maxRows: 4 }} /></Form.Item>
    </>;
  }

  const tabName = activeTab === "species" ? T.herb : activeTab === "categories" ? T.category : T.base;

  return (
    <div className={styles.herbPage}>
      <div className={`${styles.workspace} ${selectedHerb ? styles.workspaceWithDetail : ""}`}>
        <section className={styles.leftWorkspace}>
          <ModuleHeroBanner description={T.desc} eyebrow="HERBAL RESOURCE CENTER" sealText={T.seal} title={T.title} />
          <section className={styles.managementPanel}>
            <HerbResourceTabs activeTab={activeTab} onTabChange={setActiveTab} />
            {activeTab === "species" ? <HerbFilterBar categoryOptions={categoryOptions} initialKeyword={initialKeyword} onSearch={handleSearch} /> : null}
            <HerbActionToolbar canCreate={canCreate} canDelete={canDelete} canEdit={canEdit} selectedCount={selectedRowKeys.length} createLabel={`${T.create}${tabName}`} onCreate={openCreateModal} onEdit={() => openEditModal()} onDelete={handleDelete} onExport={handleExport} onRefresh={() => void reloadCurrentTab()} />
          </section>
          <section className={styles.tableArea}>{renderTable()}</section>
        </section>
        {selectedHerb ? <HerbDetailPanel herb={selectedHerb} onClose={() => setSelectedHerb(null)} /> : null}
      </div>
      <Modal title={`${modalMode === "edit" ? T.edit : T.create}${tabName}`} open={formOpen} confirmLoading={submitting} okText={T.save} cancelText={T.cancel} destroyOnHidden onCancel={() => setFormOpen(false)} onOk={() => void handleSubmit()}>
        <Form form={form} layout="vertical" preserve={false}>{renderFormFields()}</Form>
      </Modal>
    </div>
  );
}
