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
  Select,
  Space,
  Table,
  Tag,
  type TableProps,
} from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { CheckCircle2, Eye, Plus, RefreshCw, Star } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  confirmEvaluationResult,
  createEvaluationTask,
  fetchEvaluationTaskDetail,
  fetchEvaluationTasks,
  fetchIndicators,
  saveEvaluationScore,
  type EvaluationIndicator,
  type EvaluationTask,
  type EvaluationTaskDetail,
} from "@/lib/evaluation";
import { getApiErrorMessage } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import { EvaluationStatus, formatDateTime, resultLevelLabel, targetTypeLabels } from "./display";
import styles from "./evaluation.module.css";

type TaskFormValues = {
  taskName: string;
  taskType?: string;
  targetType: string;
  targetId: number;
  period?: [Dayjs, Dayjs];
  remark?: string;
};

type ScoreFormValues = { indicatorId: number; score: number; scoreComment?: string };
type ConfirmFormValues = { resultDesc?: string; remark?: string };

const targetOptions = [
  { value: "herb_species", label: "药材品种" },
  { value: "herb_growth_record", label: "生长记录" },
  { value: "eval_application", label: "申报档案" },
  { value: "perf_record", label: "业绩记录" },
];

export function EvaluationTaskWorkspace() {
  const { message } = App.useApp();
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const [createForm] = Form.useForm<TaskFormValues>();
  const [scoreForm] = Form.useForm<ScoreFormValues>();
  const [confirmForm] = Form.useForm<ConfirmFormValues>();
  const [records, setRecords] = useState<EvaluationTask[]>([]);
  const [indicators, setIndicators] = useState<EvaluationIndicator[]>([]);
  const [detail, setDetail] = useState<EvaluationTaskDetail>();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<string>();
  const [targetType, setTargetType] = useState<string>();
  const [createOpen, setCreateOpen] = useState(false);
  const [scoreOpen, setScoreOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedIndicatorId, setSelectedIndicatorId] = useState<number>();

  const loadTasks = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchEvaluationTasks({
        pageNum: page,
        pageSize,
        keyword: keyword || undefined,
        status,
        targetType,
      });
      setRecords(data.records);
      setTotal(data.total);
    } catch (error) {
      message.error(getApiErrorMessage(error, "评价任务加载失败"));
    } finally {
      setLoading(false);
    }
  }, [keyword, message, page, pageSize, status, targetType]);

  const loadIndicators = useCallback(async () => {
    try {
      const data = await fetchIndicators({ pageNum: 1, pageSize: 200, status: 1 });
      setIndicators(data.records);
    } catch (error) {
      message.error(getApiErrorMessage(error, "评价指标加载失败"));
    }
  }, [message]);

  useEffect(() => void loadTasks(), [loadTasks]);
  useEffect(() => void loadIndicators(), [loadIndicators]);

  const openDetail = useCallback(
    async (task: EvaluationTask) => {
      try {
        const data = await fetchEvaluationTaskDetail(task.id);
        setDetail(data);
        setDetailOpen(true);
      } catch (error) {
        message.error(getApiErrorMessage(error, "任务详情加载失败"));
      }
    },
    [message],
  );

  const refreshDetail = async () => {
    if (!detail) return;
    setDetail(await fetchEvaluationTaskDetail(detail.task.id));
  };

  const columns: TableProps<EvaluationTask>["columns"] = [
    { title: "任务编号", dataIndex: "taskNo", width: 150, ellipsis: true },
    { title: "任务名称", dataIndex: "taskName", ellipsis: true },
    {
      title: "评价对象",
      width: 170,
      render: (_, row) => `${targetTypeLabels[row.targetType] ?? row.targetType} #${row.targetId}`,
    },
    {
      title: "状态",
      dataIndex: "taskStatus",
      width: 90,
      render: (value) => <EvaluationStatus value={value} />,
    },
    { title: "开始时间", dataIndex: "startedAt", width: 150, render: formatDateTime },
    { title: "截止时间", dataIndex: "endedAt", width: 150, render: formatDateTime },
    {
      title: "操作",
      key: "actions",
      width: 92,
      fixed: "right",
      render: (_, row) => (
        <Button type="link" icon={<Eye size={15} />} onClick={() => void openDetail(row)}>
          详情
        </Button>
      ),
    },
  ];

  const indicatorById = useMemo(
    () => new Map(indicators.map((item) => [item.id, item])),
    [indicators],
  );
  const selectedIndicator = indicatorById.get(selectedIndicatorId ?? -1);
  const latestRecordId = detail?.scores[0]?.id;

  const submitCreate = async (values: TaskFormValues) => {
    setSubmitting(true);
    try {
      await createEvaluationTask({
        taskName: values.taskName,
        taskType: values.taskType,
        targetType: values.targetType,
        targetId: values.targetId,
        startedAt: values.period?.[0].format("YYYY-MM-DDTHH:mm:ss"),
        endedAt: values.period?.[1].format("YYYY-MM-DDTHH:mm:ss"),
        remark: values.remark,
      });
      message.success("评价任务已创建");
      setCreateOpen(false);
      createForm.resetFields();
      setPage(1);
      await loadTasks();
    } catch (error) {
      message.error(getApiErrorMessage(error, "创建任务失败"));
    } finally {
      setSubmitting(false);
    }
  };

  const submitScore = async (values: ScoreFormValues) => {
    if (!detail) return;
    setSubmitting(true);
    try {
      await saveEvaluationScore({ taskId: detail.task.id, ...values });
      message.success("评分已保存");
      setScoreOpen(false);
      scoreForm.resetFields();
      setSelectedIndicatorId(undefined);
      await Promise.all([refreshDetail(), loadTasks()]);
    } catch (error) {
      message.error(getApiErrorMessage(error, "评分保存失败"));
    } finally {
      setSubmitting(false);
    }
  };

  const submitConfirmation = async (values: ConfirmFormValues) => {
    if (!latestRecordId) return;
    setSubmitting(true);
    try {
      await confirmEvaluationResult(latestRecordId, values);
      message.success("评价结果已确认");
      setConfirmOpen(false);
      confirmForm.resetFields();
      await Promise.all([refreshDetail(), loadTasks()]);
    } catch (error) {
      message.error(getApiErrorMessage(error, "结果确认失败"));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className={styles.businessWorkspace}>
      <div className={styles.workspaceToolbar}>
        <Space wrap>
          <Input.Search
            allowClear
            placeholder="任务编号或名称"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onSearch={() => {
              setPage(1);
              void loadTasks();
            }}
          />
          <Select
            allowClear
            placeholder="任务状态"
            value={status}
            onChange={(value) => {
              setStatus(value);
              setPage(1);
            }}
            options={[
              { value: "draft", label: "草稿" },
              { value: "scoring", label: "评分中" },
              { value: "confirmed", label: "已确认" },
            ]}
          />
          <Select
            allowClear
            placeholder="评价对象"
            value={targetType}
            onChange={(value) => {
              setTargetType(value);
              setPage(1);
            }}
            options={targetOptions}
          />
        </Space>
        <Space>
          <Button icon={<RefreshCw size={15} />} onClick={() => void loadTasks()}>
            刷新
          </Button>
          {hasPermission("evaluation:task:create") ? (
            <Button type="primary" icon={<Plus size={16} />} onClick={() => setCreateOpen(true)}>
              新建任务
            </Button>
          ) : null}
        </Space>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={records}
        scroll={{ x: 980 }}
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
        title="新建评价任务"
        open={createOpen}
        confirmLoading={submitting}
        onCancel={() => setCreateOpen(false)}
        onOk={() => createForm.submit()}
        destroyOnHidden
      >
        <Form
          form={createForm}
          layout="vertical"
          onFinish={(values) => void submitCreate(values)}
          initialValues={{ taskType: "comprehensive" }}
        >
          <Form.Item
            label="任务名称"
            name="taskName"
            rules={[{ required: true, message: "请输入任务名称" }]}
          >
            <Input />
          </Form.Item>
          <div className={styles.formGrid}>
            <Form.Item label="任务类型" name="taskType">
              <Select
                options={[
                  { value: "comprehensive", label: "综合评价" },
                  { value: "special", label: "专项评价" },
                ]}
              />
            </Form.Item>
            <Form.Item
              label="评价对象类型"
              name="targetType"
              rules={[{ required: true, message: "请选择评价对象类型" }]}
            >
              <Select options={targetOptions} />
            </Form.Item>
          </div>
          <Form.Item
            label="评价对象 ID"
            name="targetId"
            rules={[{ required: true, message: "请输入真实业务对象 ID" }]}
          >
            <InputNumber min={1} precision={0} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item label="评价周期" name="period">
            <DatePicker.RangePicker
              showTime
              style={{ width: "100%" }}
              disabledDate={(date) => date.isBefore(dayjs().startOf("day"))}
            />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={detail?.task.taskName ?? "任务详情"}
        width={720}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        extra={
          <Space>
            {detail &&
            detail.task.taskStatus !== "confirmed" &&
            hasPermission("evaluation:score:create") ? (
              <Button icon={<Star size={15} />} onClick={() => setScoreOpen(true)}>
                录入评分
              </Button>
            ) : null}
            {detail &&
            !detail.result &&
            latestRecordId &&
            hasPermission("evaluation:result:confirm") ? (
              <Button
                type="primary"
                icon={<CheckCircle2 size={15} />}
                onClick={() => setConfirmOpen(true)}
              >
                确认结果
              </Button>
            ) : null}
          </Space>
        }
      >
        {detail ? (
          <div className={styles.detailSections}>
            <Descriptions
              bordered
              size="small"
              column={2}
              items={[
                { key: "no", label: "任务编号", children: detail.task.taskNo },
                {
                  key: "status",
                  label: "状态",
                  children: <EvaluationStatus value={detail.task.taskStatus} />,
                },
                {
                  key: "target",
                  label: "评价对象",
                  children: `${targetTypeLabels[detail.task.targetType] ?? detail.task.targetType} #${detail.task.targetId}`,
                },
                { key: "owner", label: "负责人 ID", children: detail.task.ownerId },
                {
                  key: "start",
                  label: "开始时间",
                  children: formatDateTime(detail.task.startedAt),
                },
                { key: "end", label: "截止时间", children: formatDateTime(detail.task.endedAt) },
              ]}
            />
            <div>
              <h3>评分明细</h3>
              {detail.scores.length ? (
                <Table
                  size="small"
                  rowKey="id"
                  pagination={false}
                  dataSource={detail.scores}
                  columns={[
                    {
                      title: "指标",
                      dataIndex: "indicatorId",
                      render: (id) => indicatorById.get(id)?.indicatorName ?? `指标 #${id}`,
                    },
                    {
                      title: "评分人",
                      dataIndex: "evaluatorId",
                      width: 90,
                      render: (id) => `#${id}`,
                    },
                    { title: "得分", dataIndex: "score", width: 80 },
                    {
                      title: "意见",
                      dataIndex: "scoreComment",
                      ellipsis: true,
                      render: (value) => value || "-",
                    },
                    {
                      title: "评分时间",
                      dataIndex: "scoredAt",
                      width: 150,
                      render: formatDateTime,
                    },
                  ]}
                />
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无评分" />
              )}
            </div>
            <div>
              <h3>确认结果</h3>
              {detail.result ? (
                <Descriptions
                  bordered
                  size="small"
                  column={2}
                  items={[
                    {
                      key: "score",
                      label: "总分",
                      children: <strong>{detail.result.totalScore}</strong>,
                    },
                    {
                      key: "level",
                      label: "等级",
                      children: (
                        <Tag color="green">{resultLevelLabel(detail.result.resultLevel)}</Tag>
                      ),
                    },
                    {
                      key: "desc",
                      label: "结果说明",
                      span: 2,
                      children: detail.result.resultDesc || "-",
                    },
                    {
                      key: "time",
                      label: "确认时间",
                      children: formatDateTime(detail.result.confirmedAt),
                    },
                    { key: "user", label: "确认人 ID", children: detail.result.confirmedBy },
                  ]}
                />
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="结果尚未确认" />
              )}
            </div>
          </div>
        ) : null}
      </Drawer>

      <Modal
        title="录入评价分数"
        open={scoreOpen}
        confirmLoading={submitting}
        onCancel={() => {
          setScoreOpen(false);
          scoreForm.resetFields();
          setSelectedIndicatorId(undefined);
        }}
        onOk={() => scoreForm.submit()}
        destroyOnHidden
      >
        <Form
          form={scoreForm}
          layout="vertical"
          onFinish={(values) => void submitScore(values)}
          onValuesChange={(changed: Partial<ScoreFormValues>) => {
            if ("indicatorId" in changed) setSelectedIndicatorId(changed.indicatorId);
          }}
        >
          <Form.Item
            label="评价指标"
            name="indicatorId"
            rules={[{ required: true, message: "请选择评价指标" }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              options={indicators.map((item) => ({
                value: item.id,
                label: `${item.indicatorName}（满分 ${item.maxScore}）`,
              }))}
            />
          </Form.Item>
          <Form.Item
            label="得分"
            name="score"
            rules={[{ required: true, message: "请输入得分" }]}
            extra={selectedIndicator ? `该指标满分 ${selectedIndicator.maxScore}` : undefined}
          >
            <InputNumber
              min={0}
              max={selectedIndicator?.maxScore}
              precision={2}
              style={{ width: "100%" }}
            />
          </Form.Item>
          <Form.Item label="评分意见" name="scoreComment">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="确认评价结果"
        open={confirmOpen}
        confirmLoading={submitting}
        onCancel={() => setConfirmOpen(false)}
        onOk={() => confirmForm.submit()}
        okText="确认并冻结"
      >
        <Form
          form={confirmForm}
          layout="vertical"
          onFinish={(values) => void submitConfirmation(values)}
        >
          <Form.Item label="结果说明" name="resultDesc">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </section>
  );
}
