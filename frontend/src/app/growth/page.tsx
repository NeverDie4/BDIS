"use client";

import { App, Button, Descriptions, Form, Input, InputNumber, Modal, Select, Upload } from "antd";
import { ImageUp } from "lucide-react";
import type { TableProps } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ActionToolbar } from "@/components/common/ActionToolbar";
import { DataTable } from "@/components/common/DataTable";
import { DetailDrawer } from "@/components/common/DetailDrawer";
import { FilterPanel } from "@/components/common/FilterPanel";
import { MetricCard } from "@/components/common/MetricCard";
import { StatusTag } from "@/components/common/StatusTag";
import { SecureImageThumb } from "@/components/common/SecureImageThumb";
import { TraceTimeline, type TraceStatus } from "@/components/feature/TraceTimeline";
import { PageBanner } from "@/components/layout/PageBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import { fetchDictionaryOptions, type DictionaryOption } from "@/lib/dictionaries";
import {
  createGrowthRecord,
  fetchGrowthRecordDetail,
  fetchGrowthRecordPage,
  fetchGrowthTrace,
  submitGrowthRecord,
  uploadGrowthImage,
  type GrowthRecordApi,
  type GrowthRecordPayload,
  type GrowthTraceEventApi,
} from "@/lib/growth-records";
import { fetchEnabledHerbs, type HerbSpeciesApi } from "@/lib/herbs";
import { getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import styles from "@/styles/mockPages.module.css";

function displayStatus(status?: string) {
  if (status === "submitted") return "pending" as const;
  if (status === "approved" || status === "rejected" || status === "archived" || status === "draft")
    return status;
  return "draft" as const;
}

function traceStatus(status?: string): TraceStatus {
  if (status === "approved" || status === "rejected" || status === "archived") return status;
  if (status === "submitted") return "pending";
  return "recorded";
}

export default function GrowthPage() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [form] = Form.useForm<GrowthRecordPayload>();
  const [records, setRecords] = useState<GrowthRecordApi[]>([]);
  const [herbs, setHerbs] = useState<HerbSpeciesApi[]>([]);
  const [growthStageOptions, setGrowthStageOptions] = useState<DictionaryOption[]>([]);
  const [soilTypeOptions, setSoilTypeOptions] = useState<DictionaryOption[]>([]);
  const [weatherOptions, setWeatherOptions] = useState<DictionaryOption[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState("all");
  const [selectedRecord, setSelectedRecord] = useState<GrowthRecordApi | null>(null);
  const [trace, setTrace] = useState<GrowthTraceEventApi[]>([]);
  const [modalOpen, setModalOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [page, species, growthStages, soilTypes, weatherTypes] = await Promise.all([
        fetchGrowthRecordPage({
          page: 1,
          size: 100,
          reviewStatus: status === "all" ? undefined : status,
        }),
        fetchEnabledHerbs(),
        fetchDictionaryOptions("growth_stage"),
        fetchDictionaryOptions("soil_type"),
        fetchDictionaryOptions("weather"),
      ]);
      setRecords(page.records);
      setTotal(page.total);
      setHerbs(species);
      setGrowthStageOptions(growthStages);
      setSoilTypeOptions(soilTypes);
      setWeatherOptions(weatherTypes);
    } catch (error) {
      if (!isAuthRedirectError(error)) message.error(getApiErrorMessage(error, "生长记录加载失败"));
    } finally {
      setLoading(false);
    }
  }, [message, status]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!selectedRecord) {
      setTrace([]);
      return;
    }
    fetchGrowthTrace(selectedRecord.id)
      .then(setTrace)
      .catch((error) => message.error(getApiErrorMessage(error, "溯源记录加载失败")));
  }, [message, selectedRecord]);

  const traceItems = useMemo(
    () =>
      trace.map((event, index) => ({
        id: `${event.eventType}-${event.eventTime ?? index}`,
        time: event.eventTime ? new Date(event.eventTime).toLocaleString() : "-",
        actor: event.operatorName || "系统",
        action: event.action,
        status: traceStatus(event.afterStatus),
        description: event.comment,
      })),
    [trace],
  );

  const columns: TableProps<GrowthRecordApi>["columns"] = [
    { title: "记录 ID", dataIndex: "id", key: "id", width: 100 },
    {
      title: "药材",
      dataIndex: "speciesName",
      key: "speciesName",
      render: (value) => value || "-",
    },
    {
      title: "采集人",
      dataIndex: "collectorName",
      key: "collectorName",
      render: (value) => value || "-",
    },
    {
      title: "采集时间",
      dataIndex: "collectedAt",
      key: "collectedAt",
      width: 180,
      render: (value) => (value ? new Date(value).toLocaleString() : "-"),
    },
    {
      title: "来源",
      dataIndex: "dataSource",
      key: "dataSource",
      render: (value) => value || "manual",
    },
    {
      title: "审核状态",
      key: "reviewStatus",
      render: (_, record) => <StatusTag status={displayStatus(record.reviewStatus)} />,
    },
    {
      title: "操作",
      key: "actions",
      render: (_, record) => (
        <Button type="link" onClick={() => void openDetail(record.id)}>
          查看详情
        </Button>
      ),
    },
  ];

  async function openDetail(recordId: number) {
    try {
      setSelectedRecord(await fetchGrowthRecordDetail(recordId));
    } catch (error) {
      message.error(getApiErrorMessage(error, "采集详情加载失败"));
    }
  }

  async function uploadImage(file: File) {
    if (!selectedRecord) return false;
    try {
      await uploadGrowthImage(selectedRecord, file);
      setSelectedRecord(await fetchGrowthRecordDetail(selectedRecord.id));
      message.success("现场图片已关联到采集记录");
    } catch (error) {
      message.error(getApiErrorMessage(error, "现场图片上传失败"));
    }
    return false;
  }

  async function submit(values: GrowthRecordPayload) {
    try {
      await createGrowthRecord(values);
      message.success("采集记录已创建");
      setModalOpen(false);
      form.resetFields();
      await load();
    } catch (error) {
      message.error(getApiErrorMessage(error, "新增采集记录失败"));
    }
  }

  async function submitForAudit() {
    if (!selectedRecord) return;
    try {
      const updated = await submitGrowthRecord(selectedRecord.id);
      setSelectedRecord(updated);
      message.success("已提交审核");
      await load();
    } catch (error) {
      message.error(getApiErrorMessage(error, "提交审核失败"));
    }
  }

  return (
    <SiteLayout>
      <div className={styles.pageStack}>
        <PageBanner
          sealText="GROWTH RECORDS"
          title="生长数据采集档案"
          subtitle="生长指标、采集来源、审核状态和溯源事件均来自生长采集服务。"
        />
        <div className={styles.metricGrid}>
          <MetricCard description="符合当前数据权限的记录。" title="采集记录" value={total} />
          <MetricCard
            description="当前列表中的草稿与待审核记录。"
            title="待处理"
            value={
              records.filter((record) => ["draft", "submitted"].includes(record.reviewStatus || ""))
                .length
            }
          />
          <MetricCard
            description="当前列表中审核通过的记录。"
            title="已通过"
            value={records.filter((record) => record.reviewStatus === "approved").length}
          />
          <MetricCard
            description="当前列表中来自 SOAP 的记录。"
            title="SOAP 导入"
            value={records.filter((record) => record.externalSource === "SOAP").length}
          />
        </div>
        <FilterPanel
          description="筛选条件直接作用于后端查询。"
          title="采集记录筛选"
          onReset={() => setStatus("all")}
        >
          <Select
            options={[
              { label: "全部状态", value: "all" },
              { label: "草稿", value: "draft" },
              { label: "待审核", value: "submitted" },
              { label: "已通过", value: "approved" },
              { label: "已驳回", value: "rejected" },
              { label: "已归档", value: "archived" },
            ]}
            style={{ minWidth: 160 }}
            value={status}
            onChange={setStatus}
          />
        </FilterPanel>
        <ActionToolbar
          actions={
            hasPermission("growth:record:create") ? (
              <Button type="primary" onClick={() => setModalOpen(true)}>
                新增采集记录
              </Button>
            ) : undefined
          }
          description="列表按角色数据范围过滤，详情可查看完整流程轨迹。"
          title="生长记录表格"
        />
        <DataTable<GrowthRecordApi>
          columns={columns}
          dataSource={records}
          loading={loading}
          pagination={false}
          rowKey="id"
          scroll={{ x: 900 }}
        />
      </div>
      <DetailDrawer
        open={Boolean(selectedRecord)}
        title={selectedRecord ? `采集记录 #${selectedRecord.id}` : "采集详情"}
        width={620}
        onClose={() => setSelectedRecord(null)}
      >
        {selectedRecord ? (
          <div className={styles.sectionStack}>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="药材">
                {selectedRecord.speciesName || selectedRecord.speciesId}
              </Descriptions.Item>
              <Descriptions.Item label="采集人">
                {selectedRecord.collectorName || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="地图点位">
                {selectedRecord.distributionId || "未关联"}
              </Descriptions.Item>
              <Descriptions.Item label="位置">
                {selectedRecord.longitude && selectedRecord.latitude
                  ? `${selectedRecord.longitude}, ${selectedRecord.latitude}`
                  : "-"}
              </Descriptions.Item>
              <Descriptions.Item label="来源">
                {selectedRecord.externalNo
                  ? `${selectedRecord.externalSource} / ${selectedRecord.externalNo}`
                  : selectedRecord.dataSource || "manual"}
              </Descriptions.Item>
              <Descriptions.Item label="备注">{selectedRecord.remark || "-"}</Descriptions.Item>
            </Descriptions>
            <Descriptions bordered column={1} size="small" title="环境与采样指标">
              <Descriptions.Item label="生长阶段">
                {selectedRecord.growthStage || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="温度">
                {selectedRecord.temperature ?? "-"} ℃
              </Descriptions.Item>
              <Descriptions.Item label="湿度">{selectedRecord.humidity ?? "-"} %</Descriptions.Item>
              <Descriptions.Item label="土壤 pH">{selectedRecord.soilPh ?? "-"}</Descriptions.Item>
              <Descriptions.Item label="天气">{selectedRecord.weather || "-"}</Descriptions.Item>
              <Descriptions.Item label="样本重量">
                {selectedRecord.sampleWeight ?? "-"} g
              </Descriptions.Item>
            </Descriptions>
            <div>
              <div className={styles.toolbarActions}>
                <strong>现场图片</strong>
                {hasPermission("herb:identification:execute") ? (
                  <Upload accept="image/*" beforeUpload={uploadImage} showUploadList={false}>
                    <Button icon={<ImageUp size={16} />}>上传图片</Button>
                  </Upload>
                ) : null}
              </div>
              <div className={styles.imageGrid}>
                {(selectedRecord.images || []).map((image) => (
                  <SecureImageThumb
                    alt={image.imageName || image.imageCode}
                    key={image.id}
                    src={image.imageUrl}
                  />
                ))}
              </div>
            </div>
            {hasPermission("growth:record:submit") &&
            ["draft", "rejected"].includes(selectedRecord.reviewStatus || "") ? (
              <Button type="primary" onClick={submitForAudit}>
                提交审核
              </Button>
            ) : null}
            <TraceTimeline items={traceItems} />
          </div>
        ) : null}
      </DetailDrawer>
      <Modal
        title="新增采集记录"
        open={modalOpen}
        footer={null}
        destroyOnHidden
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item
            name="speciesId"
            label="药材品种"
            rules={[{ required: true, message: "请选择药材品种" }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              options={herbs.map((herb) => ({
                label: `${herb.herbName}（${herb.herbCode}）`,
                value: herb.id,
              }))}
            />
          </Form.Item>
          <Form.Item name="growthStage" label="生长阶段">
            {growthStageOptions.length ? (
              <Select allowClear options={growthStageOptions} />
            ) : (
              <Input />
            )}
          </Form.Item>
          <Form.Item name="soilType" label="土壤类型">
            {soilTypeOptions.length ? <Select allowClear options={soilTypeOptions} /> : <Input />}
          </Form.Item>
          <Form.Item name="weather" label="天气">
            {weatherOptions.length ? <Select allowClear options={weatherOptions} /> : <Input />}
          </Form.Item>
          <Form.Item name="temperature" label="温度">
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="humidity" label="湿度">
            <InputNumber min={0} max={100} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="soilPh" label="土壤 pH">
            <InputNumber min={0} max={14} step={0.1} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="sampleWeight" label="样本重量（g）">
            <InputNumber min={0} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Button block htmlType="submit" type="primary">
            保存草稿
          </Button>
        </Form>
      </Modal>
    </SiteLayout>
  );
}
