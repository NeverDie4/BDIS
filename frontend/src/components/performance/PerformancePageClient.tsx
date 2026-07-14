"use client";

import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  FileAddOutlined,
  DownloadOutlined,
  PlusOutlined,
  SendOutlined,
  UploadOutlined,
} from "@ant-design/icons";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  App,
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
  Upload,
} from "antd";
import type { UploadProps } from "antd";
import dayjs from "dayjs";
import { useMemo, useState } from "react";
import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
import { deleteOwnUnboundUpload, uploadFile } from "@/lib/files";
import { resolveServerPagination, toPageRequest } from "@/lib/performance-pagination";
import { fetchProtectedFileBlob } from "@/lib/protected-files";
import { getApiErrorMessage } from "@/lib/request";
import {
  addParticipant,
  addPerformanceMaterial,
  auditPerformance,
  createPerformance,
  createStandard,
  createStandardVersion,
  disableStandard,
  fetchPerformance,
  fetchPerformances,
  fetchPerformanceStatistics,
  fetchPerformanceParticipantUsers,
  fetchStandards,
  publishStandard,
  removeParticipant,
  removePerformanceMaterial,
  submitPerformance,
  updateStandard,
  updatePerformance,
  type PerformanceRecord,
  type PerformanceMaterial,
  type PerformanceStandard,
  type StandardPayload,
} from "@/lib/performance";
import { useAuthStore } from "@/stores/auth-store";
import styles from "./performance.module.css";

const typeOptions = [
  { label: "科研", value: "RESEARCH" },
  { label: "教学", value: "TEACHING" },
  { label: "培训", value: "TRAINING" },
  { label: "应用", value: "APPLICATION" },
];

const statusLabels: Record<string, string> = {
  draft: "草稿",
  submitted: "待审核",
  approved: "已通过",
  rejected: "已退回",
};

const lifecycleLabels: Record<string, string> = {
  draft: "草稿",
  published: "已发布",
  disabled: "已停用",
};

type PerformanceFormValues = {
  performanceTitle: string;
  performanceType?: string;
  performanceLevel?: string;
  occurredAt?: dayjs.Dayjs;
  standardId?: number;
  sourceType?: string;
  sourceId?: number;
  remark?: string;
};

const sourceTypeOptions = [
  { label: "申报档案", value: "eval_application" },
  { label: "科研课题", value: "research_project" },
  { label: "实验课程", value: "edu_course" },
  { label: "实验记录", value: "edu_experiment_record" },
  { label: "培训计划", value: "edu_training_plan" },
];

type StandardFormValues = Omit<StandardPayload, "effectiveFrom" | "effectiveTo"> & {
  effectiveRange?: [dayjs.Dayjs, dayjs.Dayjs];
};

function toPerformancePayload(values: PerformanceFormValues) {
  return {
    ...values,
    occurredAt: values.occurredAt?.format("YYYY-MM-DDTHH:mm:ss"),
  };
}

function statusTag(status: string) {
  const color =
    status === "approved"
      ? "green"
      : status === "rejected"
        ? "red"
        : status === "submitted"
          ? "gold"
          : "default";
  return <Tag color={color}>{statusLabels[status] ?? status}</Tag>;
}

export function PerformancePageClient() {
  const { message, modal } = App.useApp();
  const queryClient = useQueryClient();
  const currentUser = useAuthStore((state) => state.user);
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const canCreate = hasPermission("performance:record:create");
  const canUpdate = hasPermission("performance:record:update");
  const canSubmit = hasPermission("performance:record:submit");
  const canAudit = hasPermission("performance:record:audit");
  const canManageStandards = hasPermission("performance:standard:manage");
  const [activeTab, setActiveTab] = useState("records");
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<string>();
  const [recordPagination, setRecordPagination] = useState({ current: 1, pageSize: 10 });
  const [standardPagination, setStandardPagination] = useState({ current: 1, pageSize: 10 });
  const [standardSearch, setStandardSearch] = useState("");
  const [recordModalOpen, setRecordModalOpen] = useState(false);
  const [standardModalOpen, setStandardModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<PerformanceRecord>();
  const [versionSource, setVersionSource] = useState<PerformanceStandard>();
  const [editingStandard, setEditingStandard] = useState<PerformanceStandard>();
  const [selectedId, setSelectedId] = useState<number>();
  const [reviewingRecord, setReviewingRecord] = useState<PerformanceRecord>();
  const [recordForm] = Form.useForm<PerformanceFormValues>();
  const [standardForm] = Form.useForm<StandardFormValues>();
  const materialRequired = Form.useWatch("materialRequired", standardForm);
  const [reviewForm] = Form.useForm<{ comment?: string }>();
  const [participantForm] = Form.useForm<{
    userId: number;
    participantRole: string;
    sortOrder?: number;
  }>();

  const records = useQuery({
    queryKey: ["performance", "records", keyword, status, recordPagination],
    queryFn: () =>
      fetchPerformances({
        ...toPageRequest(recordPagination),
        keyword: keyword || undefined,
        identifyStatus: status,
      }),
  });
  const standards = useQuery({
    queryKey: ["performance", "standards", standardPagination],
    queryFn: () => fetchStandards(toPageRequest(standardPagination)),
  });
  const publishedStandardOptions = useQuery({
    queryKey: ["performance", "published-standard-options", standardSearch],
    queryFn: () =>
      fetchStandards({
        pageNum: 1,
        pageSize: 20,
        keyword: standardSearch || undefined,
        lifecycleStatus: "published",
      }),
  });
  const users = useQuery({
    queryKey: ["performance", "participant-users", selectedId],
    queryFn: () => fetchPerformanceParticipantUsers(selectedId as number),
    enabled: canUpdate && Boolean(selectedId),
  });
  const statistics = useQuery({
    queryKey: ["performance", "statistics"],
    queryFn: () => fetchPerformanceStatistics({}),
  });
  const detail = useQuery({
    queryKey: ["performance", "detail", selectedId],
    queryFn: () => fetchPerformance(selectedId as number),
    enabled: Boolean(selectedId),
  });

  const publishedStandards = useMemo(
    () => publishedStandardOptions.data?.records ?? [],
    [publishedStandardOptions.data],
  );

  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["performance", "records"] }),
      queryClient.invalidateQueries({ queryKey: ["performance", "statistics"] }),
      queryClient.invalidateQueries({ queryKey: ["performance", "detail", selectedId] }),
    ]);
  };

  const openCreateRecord = () => {
    setEditingRecord(undefined);
    recordForm.resetFields();
    setRecordModalOpen(true);
  };

  const openEditRecord = (record: PerformanceRecord) => {
    setEditingRecord(record);
    recordForm.setFieldsValue({
      ...record,
      occurredAt: record.occurredAt ? dayjs(record.occurredAt) : undefined,
    });
    setRecordModalOpen(true);
  };

  const saveRecord = async () => {
    try {
      const values = await recordForm.validateFields();
      if (editingRecord) {
        await updatePerformance(editingRecord.id, toPerformancePayload(values));
        message.success("业绩已保存");
      } else {
        const created = await createPerformance(toPerformancePayload(values));
        message.success("业绩草稿已创建，请补充材料后提交");
        setSelectedId(created.id);
      }
      setRecordModalOpen(false);
      await refresh();
    } catch (error) {
      if (error && typeof error === "object" && "errorFields" in error) return;
      message.error(getApiErrorMessage(error, "保存业绩失败"));
    }
  };

  const submit = (record: PerformanceRecord) => {
    modal.confirm({
      title: "提交业绩审核",
      content: "提交后将冻结当前标准快照和材料清单，等待审核。",
      okText: "提交",
      onOk: async () => {
        try {
          await submitPerformance(record.id);
          message.success("业绩已提交审核");
          await refresh();
        } catch (error) {
          message.error(getApiErrorMessage(error, "提交失败"));
        }
      },
    });
  };

  const openReview = (record: PerformanceRecord) => {
    setReviewingRecord(record);
    reviewForm.resetFields();
  };

  const review = async (decision: "approved" | "rejected") => {
    if (!reviewingRecord) return;
    try {
      const values =
        decision === "rejected"
          ? await reviewForm.validateFields(["comment"])
          : reviewForm.getFieldsValue();
      await auditPerformance(reviewingRecord.id, decision, values.comment);
      message.success(decision === "approved" ? "业绩已通过认定" : "业绩已退回");
      setReviewingRecord(undefined);
      await refresh();
    } catch (error) {
      if (error && typeof error === "object" && "errorFields" in error) return;
      message.error(getApiErrorMessage(error, "审核失败"));
    }
  };

  const addParticipantToDetail = async () => {
    if (!selectedId) return;
    try {
      const values = await participantForm.validateFields();
      await addParticipant(selectedId, values);
      participantForm.resetFields();
      message.success("参与人已添加");
      await queryClient.invalidateQueries({ queryKey: ["performance", "detail", selectedId] });
    } catch (error) {
      if (error && typeof error === "object" && "errorFields" in error) return;
      message.error(getApiErrorMessage(error, "添加参与人失败"));
    }
  };

  const materialUploadProps: UploadProps = {
    showUploadList: false,
    customRequest: async (options) => {
      if (!selectedId) return;
      let uploadedFileId: number | undefined;
      try {
        const file = options.file as File;
        const uploaded = await uploadFile(file, { accessLevel: "private" });
        uploadedFileId = uploaded.id;
        await addPerformanceMaterial(selectedId, uploaded.id);
        options.onSuccess?.(uploaded);
        message.success("佐证材料已关联");
        await queryClient.invalidateQueries({ queryKey: ["performance", "detail", selectedId] });
      } catch (error) {
        if (uploadedFileId) {
          void deleteOwnUnboundUpload(uploadedFileId).catch(() => undefined);
        }
        options.onError?.(error as Error);
        message.error(getApiErrorMessage(error, "上传材料失败"));
      }
    },
  };

  const openProtectedFile = async (fileId: number, download = false) => {
    try {
      const blob = await fetchProtectedFileBlob(
        `/files/${fileId}/content?disposition=${download ? "attachment" : "inline"}`,
      );
      const objectUrl = URL.createObjectURL(blob);
      if (download) {
        const anchor = document.createElement("a");
        anchor.href = objectUrl;
        anchor.download = `业绩材料-${fileId}`;
        anchor.click();
      } else {
        window.open(objectUrl, "_blank", "noopener,noreferrer");
      }
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
    } catch (error) {
      message.error(getApiErrorMessage(error, "读取材料失败"));
    }
  };

  const openStandardModal = (source?: PerformanceStandard, edit = false) => {
    setVersionSource(edit ? undefined : source);
    setEditingStandard(edit ? source : undefined);
    standardForm.setFieldsValue(
      source
        ? {
            standardName: source.standardName,
            performanceType: source.performanceType,
            standardDesc: source.standardDesc,
            scoreRule: source.scoreRule,
            levelRule: source.levelRule,
            materialRequired: source.materialRequired === 1,
            minMaterialCount: source.minMaterialCount,
            sortOrder: source.sortOrder,
            remark: source.remark,
            effectiveRange:
              source.effectiveFrom && source.effectiveTo
                ? [dayjs(source.effectiveFrom), dayjs(source.effectiveTo)]
                : undefined,
          }
        : { materialRequired: true, minMaterialCount: 1 },
    );
    setStandardModalOpen(true);
  };

  const saveStandard = async () => {
    try {
      const values = await standardForm.validateFields();
      const { effectiveRange, ...rest } = values;
      const payload: StandardPayload = {
        ...rest,
        effectiveFrom: effectiveRange?.[0]?.format("YYYY-MM-DDTHH:mm:ss"),
        effectiveTo: effectiveRange?.[1]?.format("YYYY-MM-DDTHH:mm:ss"),
      };
      if (editingStandard) {
        await updateStandard(editingStandard.id, payload);
        message.success("标准草稿已更新");
      } else if (versionSource) {
        await createStandardVersion(versionSource.id, payload);
        message.success("新版本标准已创建为草稿");
      } else {
        await createStandard(payload);
        message.success("标准草稿已创建");
      }
      setStandardModalOpen(false);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["performance", "standards"] }),
        queryClient.invalidateQueries({ queryKey: ["performance", "published-standard-options"] }),
      ]);
    } catch (error) {
      if (error && typeof error === "object" && "errorFields" in error) return;
      message.error(getApiErrorMessage(error, "保存标准失败"));
    }
  };

  const changeStandardLifecycle = async (
    standard: PerformanceStandard,
    action: "publish" | "disable",
  ) => {
    try {
      if (action === "publish") {
        await publishStandard(standard.id);
        message.success("标准已发布");
      } else {
        await disableStandard(standard.id);
        message.success("标准已停用");
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["performance", "standards"] }),
        queryClient.invalidateQueries({ queryKey: ["performance", "published-standard-options"] }),
      ]);
    } catch (error) {
      message.error(
        getApiErrorMessage(error, action === "publish" ? "发布标准失败" : "停用标准失败"),
      );
    }
  };

  const recordColumns = [
    { title: "业绩编号", dataIndex: "performanceNo", width: 185 },
    { title: "业绩名称", dataIndex: "performanceTitle", ellipsis: true },
    { title: "类别", dataIndex: "performanceType", width: 100 },
    { title: "等级", dataIndex: "performanceLevel", width: 100 },
    {
      title: "发生时间",
      dataIndex: "occurredAt",
      width: 170,
      render: (value?: string) => (value ? dayjs(value).format("YYYY-MM-DD") : "-"),
    },
    { title: "状态", dataIndex: "identifyStatus", width: 100, render: statusTag },
    {
      title: "操作",
      key: "actions",
      width: 230,
      render: (_: unknown, record: PerformanceRecord) => {
        const editable = record.identifyStatus === "draft" || record.identifyStatus === "rejected";
        return (
          <Space size={4} wrap>
            <Button type="link" icon={<EyeOutlined />} onClick={() => setSelectedId(record.id)}>
              查看
            </Button>
            {canUpdate && editable ? (
              <Button type="link" icon={<EditOutlined />} onClick={() => openEditRecord(record)}>
                编辑
              </Button>
            ) : null}
            {canSubmit && editable && record.userId === currentUser?.userId ? (
              <Button type="link" icon={<SendOutlined />} onClick={() => submit(record)}>
                提交
              </Button>
            ) : null}
            {canAudit &&
            record.identifyStatus === "submitted" &&
            record.userId !== currentUser?.userId ? (
              <Button type="link" onClick={() => openReview(record)}>
                审核
              </Button>
            ) : null}
          </Space>
        );
      },
    },
  ];

  const standardColumns = [
    { title: "标准编号", dataIndex: "standardNo", width: 160 },
    {
      title: "版本",
      dataIndex: "standardVersion",
      width: 70,
      render: (value: number) => `v${value}`,
    },
    { title: "名称", dataIndex: "standardName", ellipsis: true },
    { title: "类别", dataIndex: "performanceType", width: 100 },
    {
      title: "材料",
      width: 110,
      render: (_: unknown, record: PerformanceStandard) =>
        record.materialRequired ? `至少 ${record.minMaterialCount} 份` : "非必填",
    },
    {
      title: "状态",
      dataIndex: "lifecycleStatus",
      width: 100,
      render: (value: string) => <Tag>{lifecycleLabels[value] ?? value}</Tag>,
    },
    {
      title: "操作",
      width: 220,
      render: (_: unknown, record: PerformanceStandard) =>
        canManageStandards ? (
          <Space size={4} wrap>
            {record.lifecycleStatus === "draft" ? (
              <>
                <Button
                  type="link"
                  icon={<EditOutlined />}
                  onClick={() => openStandardModal(record, true)}
                >
                  编辑
                </Button>
                <Button type="link" onClick={() => void changeStandardLifecycle(record, "publish")}>
                  发布
                </Button>
              </>
            ) : null}
            {record.lifecycleStatus === "published" ? (
              <Button
                danger
                type="link"
                onClick={() => void changeStandardLifecycle(record, "disable")}
              >
                停用
              </Button>
            ) : null}
            <Button type="link" onClick={() => openStandardModal(record)}>
              新版本
            </Button>
          </Space>
        ) : null,
    },
  ];

  const materialColumns = [
    { title: "文件 ID", dataIndex: "fileId" },
    { title: "用途", dataIndex: "fileUsage" },
    { title: "排序", dataIndex: "sortOrder" },
    {
      title: "操作",
      render: (_: unknown, material: PerformanceMaterial) => (
        <Space size={0}>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => void openProtectedFile(material.fileId)}
          >
            预览
          </Button>
          <Button
            type="link"
            icon={<DownloadOutlined />}
            onClick={() => void openProtectedFile(material.fileId, true)}
          >
            下载
          </Button>
          {canUpdate &&
          detail.data &&
          ["draft", "rejected"].includes(detail.data.performance.identifyStatus) ? (
            <Popconfirm
              title="删除该材料关联？"
              onConfirm={() =>
                void removePerformanceMaterial(selectedId as number, material.id).then(() =>
                  queryClient.invalidateQueries({
                    queryKey: ["performance", "detail", selectedId],
                  }),
                )
              }
            >
              <Button danger type="link" icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.page}>
      <ModuleHeroBanner
        eyebrow="PERFORMANCE RECOGNITION"
        sealText="认定"
        title="业绩认定"
        description="管理工作业绩、证明材料、标准版本和审核认定记录。"
        actions={
          canCreate ? (
            <Button icon={<PlusOutlined />} type="primary" onClick={openCreateRecord}>
              新增业绩
            </Button>
          ) : undefined
        }
      />
      <Row gutter={[12, 12]} className={styles.stats}>
        <Col xs={12} md={4}>
          <Card>
            <Statistic title="全部业绩" value={statistics.data?.totalCount ?? 0} />
          </Card>
        </Col>
        <Col xs={12} md={4}>
          <Card>
            <Statistic title="草稿" value={statistics.data?.draftCount ?? 0} />
          </Card>
        </Col>
        <Col xs={12} md={4}>
          <Card>
            <Statistic title="待审核" value={statistics.data?.submittedCount ?? 0} />
          </Card>
        </Col>
        <Col xs={12} md={4}>
          <Card>
            <Statistic title="已通过" value={statistics.data?.approvedCount ?? 0} />
          </Card>
        </Col>
        <Col xs={12} md={4}>
          <Card>
            <Statistic title="已退回" value={statistics.data?.rejectedCount ?? 0} />
          </Card>
        </Col>
      </Row>
      <Card className={styles.workspace}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: "records",
              label: "业绩工作台",
              children: (
                <>
                  <Space className={styles.filters} wrap>
                    <Input
                      allowClear
                      placeholder="搜索业绩名称"
                      value={keyword}
                      onChange={(event) => {
                        setKeyword(event.target.value);
                        setRecordPagination((current) => ({ ...current, current: 1 }));
                      }}
                    />
                    <Select
                      allowClear
                      placeholder="全部状态"
                      options={Object.entries(statusLabels).map(([value, label]) => ({
                        value,
                        label,
                      }))}
                      value={status}
                      onChange={(value) => {
                        setStatus(value);
                        setRecordPagination((current) => ({ ...current, current: 1 }));
                      }}
                    />
                  </Space>
                  <Table
                    rowKey="id"
                    columns={recordColumns}
                    dataSource={records.data?.records ?? []}
                    loading={records.isLoading}
                    pagination={{
                      current: recordPagination.current,
                      pageSize: recordPagination.pageSize,
                      total: records.data?.total ?? 0,
                      showSizeChanger: true,
                      onChange: (current, pageSize) =>
                        setRecordPagination((previous) =>
                          resolveServerPagination(
                            previous.current,
                            previous.pageSize,
                            current,
                            pageSize,
                          ),
                        ),
                    }}
                    scroll={{ x: 980 }}
                  />
                </>
              ),
            },
            {
              key: "standards",
              label: "认定标准",
              children: (
                <>
                  {canManageStandards ? (
                    <div className={styles.tableActions}>
                      <Button icon={<FileAddOutlined />} onClick={() => openStandardModal()}>
                        新增标准
                      </Button>
                    </div>
                  ) : null}
                  <Table
                    rowKey="id"
                    columns={standardColumns}
                    dataSource={standards.data?.records ?? []}
                    loading={standards.isLoading}
                    pagination={{
                      current: standardPagination.current,
                      pageSize: standardPagination.pageSize,
                      total: standards.data?.total ?? 0,
                      showSizeChanger: true,
                      onChange: (current, pageSize) =>
                        setStandardPagination((previous) =>
                          resolveServerPagination(
                            previous.current,
                            previous.pageSize,
                            current,
                            pageSize,
                          ),
                        ),
                    }}
                    scroll={{ x: 920 }}
                  />
                </>
              ),
            },
            {
              key: "statistics",
              label: "分类统计",
              children: (
                <div className={styles.typeCounts}>
                  {Object.entries(statistics.data?.typeCounts ?? {}).map(([type, count]) => (
                    <Card key={type}>
                      <Statistic
                        title={typeOptions.find((item) => item.value === type)?.label ?? type}
                        value={count}
                      />
                    </Card>
                  ))}
                </div>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title={editingRecord ? "编辑业绩" : "新增业绩"}
        open={recordModalOpen}
        okText="保存草稿"
        onCancel={() => setRecordModalOpen(false)}
        onOk={() => void saveRecord()}
      >
        <Form form={recordForm} layout="vertical">
          <Form.Item
            label="业绩名称"
            name="performanceTitle"
            rules={[{ required: true, message: "请输入业绩名称" }]}
          >
            <Input maxLength={200} />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="业绩类别" name="performanceType">
                <Select options={typeOptions} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="业绩等级" name="performanceLevel">
                <Input placeholder="例如：省部级" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="发生时间" name="occurredAt">
                <DatePicker className={styles.fullWidth} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="认定标准" name="standardId">
                <Select
                  allowClear
                  filterOption={false}
                  loading={publishedStandardOptions.isLoading}
                  options={publishedStandards.map((standard) => ({
                    value: standard.id,
                    label: `${standard.standardName} v${standard.standardVersion}`,
                  }))}
                  showSearch
                  onSearch={setStandardSearch}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="来源类型" name="sourceType">
                <Select allowClear options={sourceTypeOptions} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="来源 ID" name="sourceId">
                <InputNumber className={styles.fullWidth} min={1} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="说明" name="remark">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={
          editingStandard
            ? `编辑 ${editingStandard.standardNo} 草稿`
            : versionSource
              ? `创建 ${versionSource.standardNo} 新版本`
              : "新增认定标准"
        }
        open={standardModalOpen}
        okText="保存草稿"
        onCancel={() => setStandardModalOpen(false)}
        onOk={() => void saveStandard()}
      >
        <Form form={standardForm} layout="vertical">
          <Form.Item
            label="标准名称"
            name="standardName"
            rules={[{ required: true, message: "请输入标准名称" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            label="业绩类别"
            name="performanceType"
            rules={[{ required: true, message: "请选择类别" }]}
          >
            <Select options={typeOptions} />
          </Form.Item>
          <Form.Item label="适用时间" name="effectiveRange">
            <DatePicker.RangePicker className={styles.fullWidth} showTime />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="要求材料" name="materialRequired">
                <Select
                  options={[
                    { value: true, label: "必须提供" },
                    { value: false, label: "无需材料" },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="最少材料数" name="minMaterialCount">
                <InputNumber
                  className={styles.fullWidth}
                  min={materialRequired === false ? 0 : 1}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="认定规则" name="scoreRule">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="等级规则" name="levelRule">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="说明" name="standardDesc">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`审核：${reviewingRecord?.performanceTitle ?? ""}`}
        open={Boolean(reviewingRecord)}
        okText="通过"
        cancelText="取消"
        onCancel={() => setReviewingRecord(undefined)}
        onOk={() => void review("approved")}
        footer={(_, { OkBtn, CancelBtn }) => (
          <>
            <Button danger onClick={() => void review("rejected")}>
              退回
            </Button>
            <CancelBtn />
            <OkBtn />
          </>
        )}
      >
        <Form form={reviewForm} layout="vertical">
          <Form.Item
            label="审核意见"
            name="comment"
            rules={[{ required: true, message: "退回时必须填写审核意见" }]}
          >
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="业绩详情"
        open={Boolean(selectedId)}
        onClose={() => setSelectedId(undefined)}
        width={720}
      >
        {detail.data ? (
          <Space direction="vertical" size="large" className={styles.fullWidth}>
            <Descriptions
              bordered
              column={1}
              size="small"
              items={[
                { key: "no", label: "业绩编号", children: detail.data.performance.performanceNo },
                {
                  key: "title",
                  label: "业绩名称",
                  children: detail.data.performance.performanceTitle,
                },
                {
                  key: "status",
                  label: "认定状态",
                  children: statusTag(detail.data.performance.identifyStatus),
                },
                {
                  key: "standard",
                  label: "标准快照",
                  children: detail.data.performance.standardNameSnapshot
                    ? `${detail.data.performance.standardNameSnapshot} v${detail.data.performance.standardVersionSnapshot}`
                    : "提交前尚未冻结",
                },
              ]}
            />
            <section>
              <Typography.Title level={5}>佐证材料</Typography.Title>
              {canUpdate &&
              ["draft", "rejected"].includes(detail.data.performance.identifyStatus) ? (
                <Upload {...materialUploadProps}>
                  <Button icon={<UploadOutlined />}>上传并关联材料</Button>
                </Upload>
              ) : null}
              <Table
                size="small"
                rowKey="id"
                pagination={false}
                dataSource={detail.data.materials}
                columns={materialColumns}
              />
            </section>
            <section>
              <Typography.Title level={5}>参与人</Typography.Title>
              {canUpdate &&
              ["draft", "rejected"].includes(detail.data.performance.identifyStatus) ? (
                <Form
                  form={participantForm}
                  layout="inline"
                  onFinish={() => void addParticipantToDetail()}
                >
                  <Form.Item name="userId" rules={[{ required: true, message: "请选择参与人" }]}>
                    <Select
                      className={styles.participantSelect}
                      loading={users.isLoading}
                      options={(users.data ?? []).map((user) => ({
                        value: user.id,
                        label: `${user.realName || user.username} (${user.username})`,
                      }))}
                      placeholder="选择参与人"
                      showSearch
                      optionFilterProp="label"
                    />
                  </Form.Item>
                  <Form.Item
                    name="participantRole"
                    rules={[{ required: true, message: "请输入参与角色" }]}
                  >
                    <Input placeholder="参与角色" />
                  </Form.Item>
                  <Form.Item name="sortOrder">
                    <InputNumber min={0} placeholder="排序" />
                  </Form.Item>
                  <Button htmlType="submit" icon={<PlusOutlined />}>
                    添加
                  </Button>
                </Form>
              ) : null}
              <Table
                size="small"
                rowKey="id"
                pagination={false}
                dataSource={detail.data.participants}
                columns={[
                  { title: "用户 ID", dataIndex: "userId" },
                  { title: "角色", dataIndex: "participantRole" },
                  {
                    title: "负责人",
                    dataIndex: "isPrimary",
                    render: (value: number) => (value === 1 ? "是" : "否"),
                  },
                  {
                    title: "操作",
                    render: (_: unknown, participant: { id: number; isPrimary: number }) =>
                      canUpdate &&
                      participant.isPrimary !== 1 &&
                      ["draft", "rejected"].includes(detail.data.performance.identifyStatus) ? (
                        <Popconfirm
                          title="移除参与人？"
                          onConfirm={() =>
                            void removeParticipant(selectedId as number, participant.id).then(() =>
                              queryClient.invalidateQueries({
                                queryKey: ["performance", "detail", selectedId],
                              }),
                            )
                          }
                        >
                          <Button danger type="link">
                            移除
                          </Button>
                        </Popconfirm>
                      ) : null,
                  },
                ]}
              />
            </section>
            <section>
              <Typography.Title level={5}>审核历史</Typography.Title>
              <Table
                size="small"
                rowKey="id"
                pagination={false}
                dataSource={detail.data.auditRecords}
                columns={[
                  { title: "动作", dataIndex: "identifyAction" },
                  { title: "结果", dataIndex: "identifyResult", render: statusTag },
                  { title: "意见", dataIndex: "identifyComment" },
                  { title: "时间", dataIndex: "identifiedAt" },
                ]}
              />
            </section>
          </Space>
        ) : null}
      </Drawer>
    </div>
  );
}
