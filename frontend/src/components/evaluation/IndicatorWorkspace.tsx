"use client";

import {
  App,
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Statistic,
  Table,
  type TableProps,
} from "antd";
import { Edit3, Plus, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  createIndicator,
  fetchIndicators,
  updateIndicator,
  type EvaluationIndicator,
  type EvaluationIndicatorPayload,
} from "@/lib/evaluation";
import { getApiErrorMessage } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import styles from "./evaluation.module.css";

type IndicatorFormValues = EvaluationIndicatorPayload;

const indicatorTypeOptions = [
  { value: "quality", label: "质量指标" },
  { value: "process", label: "过程指标" },
  { value: "result", label: "结果指标" },
  { value: "comprehensive", label: "综合指标" },
];

export function IndicatorWorkspace() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [form] = Form.useForm<IndicatorFormValues>();
  const [records, setRecords] = useState<EvaluationIndicator[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [keyword, setKeyword] = useState("");
  const [indicatorType, setIndicatorType] = useState<string>();
  const [status, setStatus] = useState<number>();
  const [editing, setEditing] = useState<EvaluationIndicator>();
  const [modalOpen, setModalOpen] = useState(false);
  const indicatorRequestId = useRef(0);

  const load = useCallback(
    async (requestedPage = page) => {
      const requestId = ++indicatorRequestId.current;
      setLoading(true);
      try {
        const data = await fetchIndicators({
          pageNum: requestedPage,
          pageSize,
          keyword: keyword || undefined,
          indicatorType,
          status,
        });
        if (requestId !== indicatorRequestId.current) return;
        setRecords(data.records);
        setTotal(data.total);
      } catch (error) {
        if (requestId === indicatorRequestId.current) {
          message.error(getApiErrorMessage(error, "评价指标加载失败"));
        }
      } finally {
        if (requestId === indicatorRequestId.current) setLoading(false);
      }
    },
    [indicatorType, keyword, message, page, pageSize, status],
  );

  useEffect(() => void load(), [load]);

  const weightTotal = useMemo(
    () => records.reduce((sum, item) => sum + Number(item.weight || 0), 0),
    [records],
  );
  const enabledCount = records.filter((item) => item.status !== 0).length;

  const openCreate = () => {
    setEditing(undefined);
    form.resetFields();
    form.setFieldsValue({ weight: 0, maxScore: 100, sortOrder: 0, status: 1 });
    setModalOpen(true);
  };

  const openEdit = (record: EvaluationIndicator) => {
    setEditing(record);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const submit = async (values: IndicatorFormValues) => {
    setSubmitting(true);
    try {
      if (editing) await updateIndicator(editing.id, values);
      else await createIndicator(values);
      message.success(editing ? "评价指标已更新" : "评价指标已创建");
      setModalOpen(false);
      form.resetFields();
      await load();
    } catch (error) {
      message.error(getApiErrorMessage(error, editing ? "更新指标失败" : "创建指标失败"));
    } finally {
      setSubmitting(false);
    }
  };

  const columns: TableProps<EvaluationIndicator>["columns"] = [
    {
      title: "指标编号",
      dataIndex: "indicatorNo",
      width: 140,
      render: (value) => value || "自动生成",
    },
    { title: "指标名称", dataIndex: "indicatorName", ellipsis: true },
    {
      title: "指标类型",
      dataIndex: "indicatorType",
      width: 120,
      render: (value) =>
        indicatorTypeOptions.find((item) => item.value === value)?.label ?? value ?? "未分类",
    },
    { title: "权重", dataIndex: "weight", width: 90, render: (value) => `${value ?? 0}%` },
    { title: "满分", dataIndex: "maxScore", width: 80 },
    { title: "排序", dataIndex: "sortOrder", width: 70 },
    {
      title: "状态",
      dataIndex: "status",
      width: 80,
      render: (value) => (value === 0 ? "停用" : "启用"),
    },
    { title: "评分说明", dataIndex: "scoreDesc", ellipsis: true, render: (value) => value || "-" },
    ...(hasPermission("evaluation:standard:manage")
      ? [
          {
            title: "操作",
            key: "action",
            width: 80,
            fixed: "right" as const,
            render: (_: unknown, row: EvaluationIndicator) => (
              <Button type="link" icon={<Edit3 size={15} />} onClick={() => openEdit(row)}>
                编辑
              </Button>
            ),
          },
        ]
      : []),
  ];

  return (
    <section className={styles.businessWorkspace}>
      <div className={styles.indicatorSummary}>
        <Statistic title="当前页指标" value={records.length} suffix={` / ${total}`} />
        <Statistic title="当前页启用" value={enabledCount} />
        <Statistic
          title="当前页权重合计"
          value={weightTotal}
          precision={2}
          suffix="%"
          valueStyle={{ color: weightTotal === 100 ? "#2f6f4e" : "#b26b21" }}
        />
      </div>
      <div className={styles.workspaceToolbar}>
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="指标编号或名称"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onSearch={() => {
              setPage(1);
              void load(1);
            }}
          />
          <Select
            allowClear
            placeholder="指标类型"
            value={indicatorType}
            onChange={(value) => {
              setIndicatorType(value);
              setPage(1);
            }}
            options={indicatorTypeOptions}
          />
          <Select
            allowClear
            placeholder="启用状态"
            value={status}
            onChange={(value) => {
              setStatus(value);
              setPage(1);
            }}
            options={[
              { value: 1, label: "启用" },
              { value: 0, label: "停用" },
            ]}
          />
        </Space>
        <Space>
          <Button icon={<RefreshCw size={15} />} onClick={() => void load()}>
            刷新
          </Button>
          {hasPermission("evaluation:standard:manage") ? (
            <Button type="primary" icon={<Plus size={16} />} onClick={openCreate}>
              新建指标
            </Button>
          ) : null}
        </Space>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={records}
        scroll={{ x: 950 }}
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
        title={editing ? "编辑评价指标" : "新建评价指标"}
        open={modalOpen}
        confirmLoading={submitting}
        onCancel={() => {
          setModalOpen(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        forceRender
        width={660}
      >
        <Form form={form} layout="vertical" onFinish={(values) => void submit(values)}>
          <div className={styles.formGrid}>
            <Form.Item
              label="指标名称"
              name="indicatorName"
              rules={[{ required: true, message: "请输入指标名称" }]}
            >
              <Input />
            </Form.Item>
            <Form.Item label="指标编号" name="indicatorNo">
              <Input placeholder="留空自动生成" />
            </Form.Item>
          </div>
          <div className={styles.formGrid}>
            <Form.Item label="指标类型" name="indicatorType">
              <Select allowClear options={indicatorTypeOptions} />
            </Form.Item>
            <Form.Item label="父指标 ID" name="parentId">
              <InputNumber min={1} precision={0} style={{ width: "100%" }} />
            </Form.Item>
          </div>
          <div className={styles.formGridThree}>
            <Form.Item
              label="权重（%）"
              name="weight"
              rules={[{ required: true, message: "请输入权重" }]}
            >
              <InputNumber min={0} max={100} precision={2} style={{ width: "100%" }} />
            </Form.Item>
            <Form.Item
              label="满分"
              name="maxScore"
              rules={[{ required: true, message: "请输入满分" }]}
            >
              <InputNumber min={0.01} precision={2} style={{ width: "100%" }} />
            </Form.Item>
            <Form.Item label="排序" name="sortOrder">
              <InputNumber min={0} precision={0} style={{ width: "100%" }} />
            </Form.Item>
          </div>
          <Form.Item label="评分说明" name="scoreDesc">
            <Input.TextArea rows={3} />
          </Form.Item>
          <div className={styles.formGrid}>
            <Form.Item label="状态" name="status">
              <Select
                options={[
                  { value: 1, label: "启用" },
                  { value: 0, label: "停用" },
                ]}
              />
            </Form.Item>
            <Form.Item label="备注" name="remark">
              <Input />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </section>
  );
}
