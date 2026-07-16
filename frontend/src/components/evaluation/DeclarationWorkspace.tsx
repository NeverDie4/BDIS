"use client";

import {
  App,
  Button,
  DatePicker,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Steps,
  Table,
  Tabs,
  Upload,
  type TableProps,
  type UploadFile,
} from "antd";
import type { Dayjs } from "dayjs";
import {
  Archive,
  CheckCircle2,
  Download,
  Eye,
  FileUp,
  Plus,
  RefreshCw,
  RotateCcw,
  Search,
  Send,
} from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  addDeclarationMaterial,
  createDeclaration,
  fetchDeclarationDetail,
  fetchDeclarationSummary,
  fetchDeclarations,
  generateDeclarationArchive,
  reviewDeclaration,
  submitDeclaration,
  type Declaration,
  type DeclarationDetail,
  type DeclarationSummary,
} from "@/lib/evaluation";
import { deleteOwnUnboundUpload, uploadFile } from "@/lib/files";
import { escapeCsvCell } from "@/lib/csv";
import { getApiErrorMessage } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import {
  applicationTypeLabel,
  applicationTypeOptions,
  EvaluationStatus,
  formatDateTime,
} from "./display";
import styles from "./evaluation.module.css";

type CreateValues = { applicationTitle: string; applicationType?: string; remark?: string };
type ReviewValues = { reviewAction: "approve" | "reject"; reviewComment?: string; remark?: string };
type MaterialValues = { remark?: string };
type DeclarationFilters = {
  keyword: string;
  applicationType?: string;
  status?: string;
  applicantId?: number;
  submittedDateRange?: [Dayjs, Dayjs];
};

const emptyFilters: DeclarationFilters = { keyword: "" };

function formatFileSize(value?: number) {
  if (value == null) return "-";
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

function declarationQuery(filters: DeclarationFilters, pageNum: number, pageSize: number) {
  return {
    pageNum,
    pageSize,
    keyword: filters.keyword || undefined,
    status: filters.status,
    applicationType: filters.applicationType,
    applicantId: filters.applicantId,
    submittedStartDate: filters.submittedDateRange?.[0].format("YYYY-MM-DD"),
    submittedEndDate: filters.submittedDateRange?.[1].format("YYYY-MM-DD"),
  };
}

export function DeclarationWorkspace() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [createForm] = Form.useForm<CreateValues>();
  const [reviewForm] = Form.useForm<ReviewValues>();
  const [materialForm] = Form.useForm<MaterialValues>();
  const [records, setRecords] = useState<Declaration[]>([]);
  const [detail, setDetail] = useState<DeclarationDetail>();
  const [summaries, setSummaries] = useState<Record<number, DeclarationSummary>>({});
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [draftFilters, setDraftFilters] = useState<DeclarationFilters>(emptyFilters);
  const [appliedFilters, setAppliedFilters] = useState<DeclarationFilters>(emptyFilters);
  const [detailOpen, setDetailOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [reviewOpen, setReviewOpen] = useState(false);
  const [materialOpen, setMaterialOpen] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const declarationRequestId = useRef(0);

  const load = useCallback(async () => {
    const requestId = ++declarationRequestId.current;
    setLoading(true);
    try {
      const data = await fetchDeclarations(declarationQuery(appliedFilters, page, pageSize));
      if (requestId !== declarationRequestId.current) return;
      setRecords(data.records);
      setTotal(data.total);
      const settled = await Promise.allSettled(
        data.records.map(
          async (record) => [record.id, await fetchDeclarationSummary(record.id)] as const,
        ),
      );
      if (requestId !== declarationRequestId.current) return;
      setSummaries(
        Object.fromEntries(
          settled.flatMap((result) => (result.status === "fulfilled" ? [result.value] : [])),
        ),
      );
    } catch (error) {
      if (requestId === declarationRequestId.current) {
        message.error(getApiErrorMessage(error, "申报档案加载失败"));
      }
    } finally {
      if (requestId === declarationRequestId.current) setLoading(false);
    }
  }, [appliedFilters, message, page, pageSize]);

  useEffect(() => void load(), [load]);

  const openDetail = useCallback(
    async (record: Declaration) => {
      try {
        setDetail(await fetchDeclarationDetail(record.id));
        setDetailOpen(true);
      } catch (error) {
        message.error(getApiErrorMessage(error, "申报详情加载失败"));
      }
    },
    [message],
  );

  const refreshDetail = async () => {
    if (detail) setDetail(await fetchDeclarationDetail(detail.declaration.id));
  };

  const perform = async (action: () => Promise<unknown>, success: string) => {
    setSubmitting(true);
    try {
      await action();
      message.success(success);
      try {
        await Promise.all([load(), refreshDetail()]);
      } catch (error) {
        message.warning(getApiErrorMessage(error, "操作已完成，但页面数据刷新失败"));
      }
      return true;
    } catch (error) {
      message.error(getApiErrorMessage(error));
      return false;
    } finally {
      setSubmitting(false);
    }
  };

  const submitCreate = async (values: CreateValues) => {
    if (await perform(() => createDeclaration(values), "申报档案已创建")) {
      setCreateOpen(false);
      createForm.resetFields();
    }
  };

  const submitReview = async (values: ReviewValues) => {
    if (!detail) return;
    if (
      await perform(
        () =>
          reviewDeclaration(detail.declaration.id, {
            ...values,
            reviewStatus: values.reviewAction === "approve" ? "approved" : "rejected",
          }),
        values.reviewAction === "approve" ? "申报已审核通过" : "申报已退回",
      )
    ) {
      setReviewOpen(false);
      reviewForm.resetFields();
    }
  };

  const submitMaterial = async (values: MaterialValues) => {
    const rawFile = fileList[0]?.originFileObj;
    if (!detail || !rawFile) {
      message.warning("请选择需要上传的申报材料");
      return;
    }
    let uploadedFileId: number | undefined;
    let materialBound = false;
    setSubmitting(true);
    try {
      const uploaded = await uploadFile(rawFile, {
        fileUsage: "application_material",
        accessLevel: "private",
      });
      uploadedFileId = uploaded.id;
      await addDeclarationMaterial(detail.declaration.id, uploaded.id, values.remark);
      materialBound = true;
      message.success("申报材料已上传并关联");
      setMaterialOpen(false);
      setFileList([]);
      materialForm.resetFields();
      try {
        await Promise.all([load(), refreshDetail()]);
      } catch (error) {
        message.warning(getApiErrorMessage(error, "材料已关联，但页面数据刷新失败"));
      }
    } catch (error) {
      if (uploadedFileId && !materialBound) {
        try {
          await deleteOwnUnboundUpload(uploadedFileId);
        } catch {
          message.warning("材料关联失败，未绑定文件清理失败，请联系管理员处理");
        }
      }
      message.error(getApiErrorMessage(error, "申报材料上传失败"));
    } finally {
      setSubmitting(false);
    }
  };

  const applyFilters = () => {
    setPage(1);
    setAppliedFilters({ ...draftFilters });
  };

  const resetFilters = () => {
    setDraftFilters(emptyFilters);
    setAppliedFilters(emptyFilters);
    setPage(1);
  };

  const exportDeclarations = async () => {
    setExporting(true);
    try {
      const firstPage = await fetchDeclarations(declarationQuery(appliedFilters, 1, 200));
      const pages = Math.max(1, firstPage.pages);
      const records = [...firstPage.records];
      for (let current = 2; current <= pages; current += 1) {
        const nextPage = await fetchDeclarations(declarationQuery(appliedFilters, current, 200));
        records.push(...nextPage.records);
      }
      if (records.length !== firstPage.total) {
        throw new Error("导出数据不完整，请重试");
      }
      const statusLabels: Record<string, string> = {
        draft: "草稿",
        submitted: "待审核",
        approved: "已通过",
        rejected: "已退回",
        archived: "已归档",
      };
      const rows = [
        [
          "申报编号",
          "申报标题",
          "申报类型",
          "申报人ID",
          "审核状态",
          "提交时间",
          "审核人ID",
          "审核时间",
        ],
        ...records.map((record) => [
          record.applicationNo,
          record.applicationTitle,
          applicationTypeLabel(record.applicationType),
          record.applicantId,
          statusLabels[record.reviewStatus] ?? record.reviewStatus,
          formatDateTime(record.submittedAt),
          record.reviewerId ?? "",
          formatDateTime(record.reviewedAt),
        ]),
      ];
      const csv = rows.map((row) => row.map((value) => escapeCsvCell(value)).join(",")).join("\n");
      const url = URL.createObjectURL(
        new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8" }),
      );
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `申报档案-${new Date().toISOString().slice(0, 10)}.csv`;
      anchor.click();
      URL.revokeObjectURL(url);
      message.success(`已导出 ${records.length} 条申报档案`);
    } catch (error) {
      message.error(getApiErrorMessage(error, "申报档案导出失败"));
    } finally {
      setExporting(false);
    }
  };

  const columns: TableProps<Declaration>["columns"] = [
    {
      title: "序号",
      key: "index",
      width: 64,
      render: (_value, _row, index) => (page - 1) * pageSize + index + 1,
    },
    { title: "申报编号", dataIndex: "applicationNo", width: 170, ellipsis: true },
    { title: "申报标题", dataIndex: "applicationTitle", ellipsis: true },
    { title: "申报类型", dataIndex: "applicationType", width: 150, render: applicationTypeLabel },
    {
      title: "申报人",
      dataIndex: "applicantId",
      width: 90,
      render: (value) => `用户 #${value}`,
    },
    {
      title: "材料数",
      key: "materialCount",
      width: 80,
      align: "center",
      render: (_, row) => summaries[row.id]?.materialCount ?? "-",
    },
    {
      title: "状态",
      dataIndex: "reviewStatus",
      width: 90,
      render: (value) => <EvaluationStatus value={value} />,
    },
    { title: "提交时间", dataIndex: "submittedAt", width: 150, render: formatDateTime },
    { title: "审核时间", dataIndex: "reviewedAt", width: 150, render: formatDateTime },
    {
      title: "审核人",
      dataIndex: "reviewerId",
      width: 90,
      render: (value) => (value ? `用户 #${value}` : "未分配"),
    },
    {
      title: "操作",
      key: "actions",
      width: 90,
      fixed: "right",
      render: (_, row) => (
        <Button type="link" icon={<Eye size={15} />} onClick={() => void openDetail(row)}>
          详情
        </Button>
      ),
    },
  ];

  const currentPageStatusCounts = records.reduce<Record<string, number>>((counts, record) => {
    counts[record.reviewStatus] = (counts[record.reviewStatus] ?? 0) + 1;
    return counts;
  }, {});

  const declaration = detail?.declaration;
  const canEditMaterial =
    declaration &&
    ["draft", "rejected"].includes(declaration.reviewStatus) &&
    hasPermission("declaration:application:update");
  const canSubmit =
    declaration &&
    ["draft", "rejected"].includes(declaration.reviewStatus) &&
    hasPermission("declaration:application:submit");
  const canReview =
    declaration?.reviewStatus === "submitted" && hasPermission("declaration:application:audit");
  const canArchive =
    declaration?.reviewStatus === "approved" && hasPermission("declaration:application:archive");
  const detailSummary = declaration ? summaries[declaration.id] : undefined;
  const processStep = detail
    ? detail.archive
      ? 5
      : declaration?.reviewStatus === "approved" || declaration?.reviewStatus === "archived"
        ? 4
        : declaration?.reviewStatus === "submitted" || declaration?.reviewStatus === "rejected"
          ? 3
          : detail.materials.length
            ? 1
            : 0
    : 0;

  return (
    <section className={styles.businessWorkspace}>
      <div className={styles.declarationOverview}>
        <div>
          <span>档案总数</span>
          <strong>{total}</strong>
          <small>当前权限范围</small>
        </div>
        <div>
          <span>草稿</span>
          <strong>{currentPageStatusCounts.draft ?? 0}</strong>
          <small>当前页待完善</small>
        </div>
        <div>
          <span>待审核</span>
          <strong>{currentPageStatusCounts.submitted ?? 0}</strong>
          <small>当前页待处理</small>
        </div>
        <div>
          <span>已通过</span>
          <strong>{currentPageStatusCounts.approved ?? 0}</strong>
          <small>当前页审核完成</small>
        </div>
      </div>
      <div className={styles.declarationFilterPanel}>
        <label className={styles.declarationFilterField}>
          <span>关键词</span>
          <Input
            allowClear
            prefix={<Search size={15} />}
            placeholder="请输入标题或编号"
            value={draftFilters.keyword}
            onChange={(event) =>
              setDraftFilters((current) => ({ ...current, keyword: event.target.value }))
            }
            onPressEnter={applyFilters}
          />
        </label>
        <label className={styles.declarationFilterField}>
          <span>申报类型</span>
          <Select
            allowClear
            placeholder="全部类型"
            value={draftFilters.applicationType}
            onChange={(value) =>
              setDraftFilters((current) => ({ ...current, applicationType: value }))
            }
            options={applicationTypeOptions}
          />
        </label>
        <label className={styles.declarationFilterField}>
          <span>申报人</span>
          <InputNumber
            min={1}
            precision={0}
            placeholder="输入申报人 ID"
            value={draftFilters.applicantId}
            onChange={(value) =>
              setDraftFilters((current) => ({ ...current, applicantId: value ?? undefined }))
            }
          />
        </label>
        <label className={styles.declarationFilterField}>
          <span>审核状态</span>
          <Select
            allowClear
            placeholder="全部状态"
            value={draftFilters.status}
            onChange={(value) => setDraftFilters((current) => ({ ...current, status: value }))}
            options={[
              { value: "draft", label: "草稿" },
              { value: "submitted", label: "待审核" },
              { value: "approved", label: "已通过" },
              { value: "rejected", label: "已退回" },
              { value: "archived", label: "已归档" },
            ]}
          />
        </label>
        <label className={`${styles.declarationFilterField} ${styles.declarationDateField}`}>
          <span>提交时间</span>
          <DatePicker.RangePicker
            value={draftFilters.submittedDateRange}
            placeholder={["开始日期", "结束日期"]}
            onChange={(dates) =>
              setDraftFilters((current) => ({
                ...current,
                submittedDateRange: dates?.[0] && dates[1] ? [dates[0], dates[1]] : undefined,
              }))
            }
          />
        </label>
        <div className={styles.declarationFilterActions}>
          <Button type="primary" icon={<Search size={15} />} onClick={applyFilters}>
            查询
          </Button>
          <Button icon={<RotateCcw size={15} />} onClick={resetFilters}>
            重置
          </Button>
          <Button
            icon={<Download size={15} />}
            loading={exporting}
            onClick={() => void exportDeclarations()}
          >
            导出
          </Button>
        </div>
      </div>
      <div className={styles.declarationActionBar}>
        {hasPermission("declaration:application:create") ? (
          <Button type="primary" icon={<Plus size={16} />} onClick={() => setCreateOpen(true)}>
            新建申报
          </Button>
        ) : null}
        <Button icon={<RefreshCw size={15} />} onClick={() => void load()}>
          刷新
        </Button>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={records}
        scroll={{ x: 1350 }}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          showTotal: (value) => `共 ${value} 条`,
          onChange: (next, size) => {
            setPage(next);
            setPageSize(size);
          },
        }}
      />

      <Modal
        title="新建申报档案"
        open={createOpen}
        confirmLoading={submitting}
        onCancel={() => setCreateOpen(false)}
        onOk={() => createForm.submit()}
        destroyOnHidden
      >
        <Form form={createForm} layout="vertical" onFinish={(values) => void submitCreate(values)}>
          <Form.Item
            label="申报标题"
            name="applicationTitle"
            rules={[{ required: true, message: "请输入申报标题" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item label="申报类型" name="applicationType">
            <Select allowClear options={applicationTypeOptions} />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={declaration?.applicationTitle ?? "申报详情"}
        width={760}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        extra={
          <Space wrap>
            {canEditMaterial ? (
              <Button icon={<FileUp size={15} />} onClick={() => setMaterialOpen(true)}>
                添加材料
              </Button>
            ) : null}
            {canSubmit ? (
              <Popconfirm
                title="确认提交该申报？"
                description="提交后将进入审核流程。"
                onConfirm={() =>
                  void perform(() => submitDeclaration(declaration.id), "申报已提交")
                }
              >
                <Button type="primary" icon={<Send size={15} />}>
                  提交
                </Button>
              </Popconfirm>
            ) : null}
            {canReview ? (
              <Button
                type="primary"
                icon={<CheckCircle2 size={15} />}
                onClick={() => setReviewOpen(true)}
              >
                审核
              </Button>
            ) : null}
            {canArchive ? (
              <Popconfirm
                title="重新同步申报档案袋？"
                onConfirm={() =>
                  void perform(
                    () => generateDeclarationArchive(declaration.id),
                    "档案袋已生成并同步",
                  )
                }
              >
                <Button icon={<Archive size={15} />}>同步档案</Button>
              </Popconfirm>
            ) : null}
          </Space>
        }
      >
        {detail ? (
          <div className={styles.detailSections}>
            <div className={styles.declarationDetailSummary}>
              <div>
                <span>申报材料</span>
                <strong>{detailSummary?.materialCount ?? detail.materials.length}</strong>
              </div>
              <div>
                <span>流程记录</span>
                <strong>{detailSummary?.reviewRecordCount ?? detail.reviewRecords.length}</strong>
              </div>
              <div>
                <span>档案项目</span>
                <strong>{detailSummary?.archiveItemCount ?? detail.archiveItems.length}</strong>
              </div>
            </div>
            <Steps
              size="small"
              current={processStep}
              status={declaration?.reviewStatus === "rejected" ? "error" : "process"}
              items={[
                { title: "创建" },
                { title: "材料" },
                { title: "提交" },
                { title: "审核" },
                { title: "归档" },
              ]}
            />
            <Tabs
              items={[
                {
                  key: "basic",
                  label: "基本信息",
                  children: (
                    <Descriptions
                      bordered
                      size="small"
                      column={2}
                      items={[
                        { key: "no", label: "申报编号", children: declaration?.applicationNo },
                        {
                          key: "status",
                          label: "状态",
                          children: <EvaluationStatus value={declaration?.reviewStatus} />,
                        },
                        {
                          key: "type",
                          label: "申报类型",
                          children: applicationTypeLabel(declaration?.applicationType),
                        },
                        {
                          key: "applicant",
                          label: "申报人",
                          children: `用户 #${declaration?.applicantId}`,
                        },
                        {
                          key: "reviewer",
                          label: "审核人",
                          children: declaration?.reviewerId
                            ? `用户 #${declaration.reviewerId}`
                            : "未分配",
                        },
                        {
                          key: "created",
                          label: "创建时间",
                          children: formatDateTime(declaration?.createdAt),
                        },
                        {
                          key: "updated",
                          label: "更新时间",
                          children: formatDateTime(declaration?.updatedAt),
                        },
                        {
                          key: "submit",
                          label: "提交时间",
                          children: formatDateTime(declaration?.submittedAt),
                        },
                        {
                          key: "review",
                          label: "审核时间",
                          children: formatDateTime(declaration?.reviewedAt),
                        },
                        {
                          key: "archiveNo",
                          label: "档案袋编号",
                          children: detailSummary?.archiveNo ?? "尚未生成",
                        },
                        {
                          key: "comment",
                          label: "审核意见",
                          span: 2,
                          children: declaration?.reviewComment || "-",
                        },
                        {
                          key: "remark",
                          label: "申报备注",
                          span: 2,
                          children: declaration?.remark || "-",
                        },
                      ]}
                    />
                  ),
                },
                {
                  key: "materials",
                  label: `申报材料（${detail.materials.length}）`,
                  children: detail.materials.length ? (
                    <Table
                      size="small"
                      rowKey="id"
                      pagination={false}
                      dataSource={detail.materials}
                      columns={[
                        { title: "文件名称", dataIndex: "fileName", ellipsis: true },
                        {
                          title: "类型",
                          dataIndex: "fileType",
                          width: 100,
                          render: (value) => value || "-",
                        },
                        { title: "大小", dataIndex: "fileSize", width: 90, render: formatFileSize },
                        {
                          title: "上传时间",
                          dataIndex: "uploadedAt",
                          width: 150,
                          render: formatDateTime,
                        },
                      ]}
                    />
                  ) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无申报材料" />
                  ),
                },
                {
                  key: "reviews",
                  label: `审核记录（${detail.reviewRecords.length}）`,
                  children: detail.reviewRecords.length ? (
                    <Table
                      size="small"
                      rowKey="id"
                      pagination={false}
                      dataSource={detail.reviewRecords}
                      columns={[
                        {
                          title: "动作",
                          dataIndex: "reviewAction",
                          width: 90,
                          render: (value) =>
                            ({ submit: "提交", approve: "通过", reject: "退回" })[
                              value as string
                            ] ?? value,
                        },
                        {
                          title: "状态变化",
                          render: (_, row) => (
                            <Space size={4}>
                              <EvaluationStatus value={row.beforeStatus} />
                              <span>→</span>
                              <EvaluationStatus value={row.reviewStatus} />
                            </Space>
                          ),
                        },
                        {
                          title: "审核人",
                          dataIndex: "reviewerId",
                          width: 90,
                          render: (value) => `#${value}`,
                        },
                        {
                          title: "意见",
                          dataIndex: "reviewComment",
                          ellipsis: true,
                          render: (value) => value || "-",
                        },
                        {
                          title: "时间",
                          dataIndex: "reviewedAt",
                          width: 150,
                          render: formatDateTime,
                        },
                      ]}
                    />
                  ) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无审核记录" />
                  ),
                },
                {
                  key: "archive",
                  label: `档案袋（${detail.archiveItems.length}）`,
                  children: detail.archive ? (
                    <div className={styles.archivePanel}>
                      <Descriptions
                        size="small"
                        column={1}
                        items={[
                          { key: "no", label: "档案编号", children: detail.archive.archiveNo },
                          {
                            key: "title",
                            label: "档案标题",
                            children: detail.archive.archiveTitle,
                          },
                          {
                            key: "time",
                            label: "生成时间",
                            children: formatDateTime(detail.archive.generatedAt),
                          },
                          {
                            key: "status",
                            label: "档案状态",
                            children: detail.archive.archiveStatus,
                          },
                          {
                            key: "owner",
                            label: "档案所有人",
                            children: `用户 #${detail.archive.ownerId}`,
                          },
                        ]}
                      />
                      <Table
                        size="small"
                        rowKey="id"
                        pagination={false}
                        dataSource={detail.archiveItems}
                        columns={[
                          {
                            title: "档案项",
                            dataIndex: "itemName",
                            render: (value) => value || "-",
                          },
                          { title: "来源类型", dataIndex: "sourceType", width: 120 },
                          { title: "来源 ID", dataIndex: "sourceId", width: 90 },
                        ]}
                      />
                    </div>
                  ) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="尚未生成档案袋" />
                  ),
                },
              ]}
            />
          </div>
        ) : null}
      </Drawer>

      <Modal
        title="上传申报材料"
        open={materialOpen}
        confirmLoading={submitting}
        onCancel={() => {
          setMaterialOpen(false);
          setFileList([]);
        }}
        onOk={() => materialForm.submit()}
        destroyOnHidden
      >
        <Form
          form={materialForm}
          layout="vertical"
          onFinish={(values) => void submitMaterial(values)}
        >
          <Form.Item label="材料文件" required>
            <Upload.Dragger
              maxCount={1}
              fileList={fileList}
              beforeUpload={() => false}
              onChange={({ fileList: next }) => setFileList(next)}
            >
              <p>
                <FileUp size={28} />
              </p>
              <p>点击或拖拽文件到此处</p>
            </Upload.Dragger>
          </Form.Item>
          <Form.Item label="材料说明" name="remark">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="审核申报"
        open={reviewOpen}
        confirmLoading={submitting}
        onCancel={() => setReviewOpen(false)}
        onOk={() => reviewForm.submit()}
        destroyOnHidden
      >
        <Form form={reviewForm} layout="vertical" onFinish={(values) => void submitReview(values)}>
          <Form.Item
            label="审核结论"
            name="reviewAction"
            rules={[{ required: true, message: "请选择审核结论" }]}
          >
            <Select
              options={[
                { value: "approve", label: "审核通过" },
                { value: "reject", label: "退回修改" },
              ]}
            />
          </Form.Item>
          <Form.Item
            label="审核意见"
            name="reviewComment"
            rules={[{ required: true, message: "请输入审核意见" }]}
          >
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input prefix={<RotateCcw size={14} />} />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  );
}
