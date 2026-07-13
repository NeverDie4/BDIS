"use client";

import { Button, Descriptions, Modal, Select, Typography } from "antd";
import type { TableProps } from "antd";
import { useMemo, useState } from "react";
import { ActionToolbar } from "@/components/common/ActionToolbar";
import { DataTable } from "@/components/common/DataTable";
import { DetailDrawer } from "@/components/common/DetailDrawer";
import { FilterPanel } from "@/components/common/FilterPanel";
import { ImageThumb } from "@/components/common/ImageThumb";
import { MetricCard } from "@/components/common/MetricCard";
import { SearchBar } from "@/components/common/SearchBar";
import { StatusTag } from "@/components/common/StatusTag";
import { TraceTimeline } from "@/components/feature/TraceTimeline";
import type { TraceStatus } from "@/components/feature/TraceTimeline";
import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
import { SiteLayout } from "@/components/layout/SiteLayout";
import { growthRecords, traceEvents } from "@/mocks/growth";
import type { GrowthRecord } from "@/types/growth";
import styles from "@/styles/mockPages.module.css";

function toTraceStatus(status: GrowthRecord["status"]): TraceStatus {
  if (status === "approved") return "approved";
  if (status === "archived") return "archived";
  if (status === "rejected") return "rejected";
  if (status === "pending") return "pending";
  return "recorded";
}

export default function GrowthPage() {
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<string>("all");
  const [selectedRecord, setSelectedRecord] = useState<GrowthRecord | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const filteredRecords = useMemo(() => {
    return growthRecords.filter((record) => {
      const matchedKeyword =
        keyword.length === 0 ||
        record.recordNo.includes(keyword) ||
        record.herbName.includes(keyword) ||
        record.locationName.includes(keyword);
      const matchedStatus = status === "all" || record.reviewStatus === status;

      return matchedKeyword && matchedStatus;
    });
  }, [keyword, status]);

  const selectedTraceItems = traceEvents
    .filter((event) => event.recordId === selectedRecord?.id)
    .map((event) => ({
      id: event.id,
      time: event.eventTime,
      actor: event.actor,
      action: event.action,
      status: toTraceStatus(event.status),
      description: event.description,
    }));

  const columns: TableProps<GrowthRecord>["columns"] = [
    { title: "采集编号", dataIndex: "recordNo", key: "recordNo", width: 170 },
    { title: "药材", dataIndex: "herbName", key: "herbName" },
    { title: "区县", dataIndex: "district", key: "district" },
    { title: "采集人", dataIndex: "collector", key: "collector" },
    { title: "采集时间", dataIndex: "collectedAt", key: "collectedAt", width: 170 },
    {
      title: "审核状态",
      key: "reviewStatus",
      render: (_, record) => <StatusTag status={record.reviewStatus} />,
    },
    {
      title: "操作",
      key: "actions",
      render: (_, record) => (
        <Button type="link" onClick={() => setSelectedRecord(record)}>
          查看详情
        </Button>
      ),
    },
  ];

  return (
    <SiteLayout contentMode="fluid">
      <div className={styles.pageStack}>
        <ModuleHeroBanner
          eyebrow="GROWTH RECORDS"
          sealText="观测"
          title="生长数据采集档案"
          description="承载中药材生长阶段、环境指标、定位、采集人快照和图片上传记录。"
        />

        <div className={styles.metricGrid}>
          <MetricCard description="当前 mock 生长采集记录。" title="采集记录" value={growthRecords.length} />
          <MetricCard
            description="待补充图片、定位或审核说明。"
            title="待完善"
            value={growthRecords.filter((record) => record.reviewStatus === "pending" || record.reviewStatus === "draft").length}
          />
          <MetricCard
            description="可进入资源库和课程案例。"
            title="已通过"
            value={growthRecords.filter((record) => record.reviewStatus === "approved").length}
          />
          <MetricCard description="现场图片 mock 附件。" title="现场图片" value={growthRecords.flatMap((record) => record.imageUrls).length} />
        </div>

        <FilterPanel
          description="当前为前端 mock 筛选。"
          title="采集记录筛选"
          onReset={() => {
            setKeyword("");
            setStatus("all");
          }}
        >
          <SearchBar placeholder="搜索编号、药材或地点" value={keyword} onChange={setKeyword} />
          <Select
            options={[
              { label: "全部状态", value: "all" },
              { label: "草稿", value: "draft" },
              { label: "待处理", value: "pending" },
              { label: "已通过", value: "approved" },
            ]}
            style={{ minWidth: 160 }}
            value={status}
            onChange={setStatus}
          />
        </FilterPanel>

        <ActionToolbar
          actions={
            <div className={styles.toolbarActions}>
              <Button onClick={() => setModalOpen(true)}>批量导出</Button>
              <Button type="primary" onClick={() => setModalOpen(true)}>
                新增采集记录
              </Button>
            </div>
          }
          description="点击查看详情可打开包含指标、图片和溯源的抽屉。"
          title="生长记录表格"
        />
        <DataTable<GrowthRecord> columns={columns} dataSource={filteredRecords} pagination={false} rowKey="id" scroll={{ x: 900 }} />
      </div>

      <DetailDrawer
        open={Boolean(selectedRecord)}
        title={selectedRecord?.recordNo ?? "采集详情"}
        width={620}
        onClose={() => setSelectedRecord(null)}
      >
        {selectedRecord ? (
          <div className={styles.sectionStack}>
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="药材">{selectedRecord.herbName}</Descriptions.Item>
              <Descriptions.Item label="地点">{selectedRecord.district} / {selectedRecord.locationName}</Descriptions.Item>
              <Descriptions.Item label="采集人">{selectedRecord.collector}</Descriptions.Item>
              <Descriptions.Item label="备注">{selectedRecord.remark}</Descriptions.Item>
            </Descriptions>

            <Descriptions bordered column={1} size="small" title="环境指标">
              <Descriptions.Item label="温度">{selectedRecord.environmentMetric?.temperature}℃</Descriptions.Item>
              <Descriptions.Item label="湿度">{selectedRecord.environmentMetric?.humidity}%</Descriptions.Item>
              <Descriptions.Item label="土壤 pH">{selectedRecord.environmentMetric?.soilPh}</Descriptions.Item>
              <Descriptions.Item label="天气">{selectedRecord.environmentMetric?.weather}</Descriptions.Item>
            </Descriptions>

            <Descriptions bordered column={1} size="small" title="形态指标">
              <Descriptions.Item label="生长阶段">{selectedRecord.morphologyMetric?.growthStage}</Descriptions.Item>
              <Descriptions.Item label="株高">{selectedRecord.morphologyMetric?.plantHeight} cm</Descriptions.Item>
              <Descriptions.Item label="叶片数">{selectedRecord.morphologyMetric?.leafCount}</Descriptions.Item>
              <Descriptions.Item label="样本说明">{selectedRecord.morphologyMetric?.sampleDescription}</Descriptions.Item>
            </Descriptions>

            <div className={styles.imageGrid}>
              {selectedRecord.imageUrls.map((image) => (
                <ImageThumb alt={`${selectedRecord.herbName}现场图片`} key={image} size={86} src={image} />
              ))}
            </div>

            <TraceTimeline items={selectedTraceItems} />
          </div>
        ) : null}
      </DetailDrawer>

      <Modal footer={null} open={modalOpen} title="采集操作占位" onCancel={() => setModalOpen(false)}>
        <Typography.Paragraph className={styles.mutedText}>
          新增采集、批量导出和提交审核将在表单与接口层完成后接入。
        </Typography.Paragraph>
      </Modal>
    </SiteLayout>
  );
}
