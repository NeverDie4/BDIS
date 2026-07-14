"use client";

import { CanvasRenderer } from "echarts/renderers";
import { LineChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { init, use as registerECharts, type EChartsCoreOption } from "echarts/core";
import axios from "axios";
import { App, Button, Empty, Form, Input, Modal, Select, Spin } from "antd";
import { Activity, BarChart3, Check, ChevronDown, Copy, Download, Edit3, ExternalLink, Eye, Filter, Layers, MapPin, Plus, QrCode, RefreshCw, Send, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { SecureImageThumb } from "@/components/common/SecureImageThumb";
import { SiteLayout } from "@/components/layout/SiteLayout";
import {
  approveGrowthRecord,
  buildGrowthChartData,
  createGrowthTask,
  disableGrowthPublicTrace,
  downloadGrowthTraceQrCode,
  enableGrowthPublicTrace,
  fetchAssignableGrowthCollectors,
  fetchGrowthAuditHistory,
  fetchGrowthBatchImages,
  fetchGrowthChart,
  fetchMyGrowthTasks,
  fetchGrowthRecordDetail,
  fetchGrowthTasks,
  fetchGrowthTrace,
  generateGrowthTraceCode,
  generateGrowthTraceQrCode,
  getGrowthTraceQrCode,
  publishGrowthTask,
  rejectGrowthRecord,
  submitGrowthRecord,
  type GrowthAuditHistoryApi,
  type GrowthBatchImageApi,
  type GrowthChartDatum,
  type GrowthChartEmptyReason,
  type GrowthCollectorOptionApi,
  type GrowthChartPointApi,
  type GrowthMetricKey,
  type GrowthRecordApi,
  type GrowthTaskApi,
  type GrowthTraceEventApi,
  type GrowthTraceQrCodeApi,
} from "@/lib/growth-records";
import { fetchEnabledHerbs, fetchHerbBases, type HerbBaseApi, type HerbSpeciesApi } from "@/lib/herbs";
import { getApiErrorMessage, isAuthRedirectError } from "@/lib/request";
import { useAuthStore } from "@/stores/auth-store";
import styles from "./page.module.css";

registerECharts([CanvasRenderer, LineChart, GridComponent, TooltipComponent]);

const heroIllustrationUrl = "";

type DetailTab = "base" | "metrics" | "images" | "trace";

const STATUS_META: Record<string, { label: string; color: string }> = {
  draft: { label: "草稿", color: "#8a8f89" },
  submitted: { label: "待审核", color: "#d97706" },
  approved: { label: "已通过", color: "#2f7d4f" },
  rejected: { label: "已驳回", color: "#b94a48" },
  archived: { label: "已归档", color: "#70877a" },
};

const IMAGE_TYPE_LABELS: Record<string, string> = {
  other: "其他",
  leaf: "叶片",
  root: "根部",
  stem: "茎部",
  flower: "花",
  fruit: "果实",
  whole_plant: "整株",
  medicinal_part: "药用部位",
  environment: "环境",
};

const METRICS: Record<GrowthMetricKey, { label: string; unit: string }> = {
  plantHeight: { label: "株高", unit: "cm" },
  temperature: { label: "温度", unit: "℃" },
  humidity: { label: "湿度", unit: "%" },
  soilMoisture: { label: "土壤湿度", unit: "%" },
  soilPh: { label: "土壤 pH", unit: "" },
  light: { label: "光照", unit: "lx" },
  sampleWeight: { label: "采样重量", unit: "g" },
};

const DEFAULT_METRIC_ORDER = [
  "plantHeight",
  "temperature",
  "humidity",
  "soilPh",
  "soilMoisture",
  "light",
  "sampleWeight",
] as const satisfies readonly GrowthMetricKey[];

const STATUS_OPTIONS = [
  { label: "全部状态", value: "all" },
  ...Object.entries(STATUS_META).map(([value, meta]) => ({ label: meta.label, value })),
];

const EMPTY_REASON_LABELS: Record<GrowthChartEmptyReason, string> = {
  "no-records": "当前任务下没有生长记录",
  "no-status-records": "当前状态下没有匹配记录",
  "no-metric-values": "当前指标暂无有效数据",
  "metric-missing": "当前接口暂未返回该指标字段",
};

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "-";
}

const ACTION_LABELS: Record<string, string> = {
  create: "创建记录",
  update: "更新记录",
  submit: "提交审核",
  approve: "审核通过",
  reject: "审核驳回",
};

const TRACE_EVENT_LABELS: Record<string, string> = {
  trace_code_generated: "生成溯源码",
  trace_qrcode_generated: "生成溯源二维码",
  public_trace_enabled: "开启公开溯源",
  public_trace_disabled: "关闭公开溯源",
};

function formatActionLabel(action?: string) {
  return ACTION_LABELS[action?.toLowerCase() || ""] || action || "记录操作";
}

function formatTraceEventTitle(event: GrowthTraceEventApi) {
  return event.eventTitle || TRACE_EVENT_LABELS[event.eventType] || TRACE_EVENT_LABELS[event.action] || event.action || "溯源事件";
}

function getPublicTracePageUrl(traceCode?: string) {
  if (!traceCode || typeof window === "undefined") return "";
  return `${window.location.origin}/trace/growth/${encodeURIComponent(traceCode)}`;
}

function formatStatusTransition(beforeStatus?: string, afterStatus?: string) {
  const before = beforeStatus && beforeStatus !== "-" ? STATUS_META[beforeStatus]?.label : undefined;
  const after = afterStatus && afterStatus !== "-" ? STATUS_META[afterStatus]?.label : undefined;
  if (!before && after) return `初始状态：${after}`;
  if (before && after) return `${before} → ${after}`;
  return after || before || "状态未记录";
}

function MetricReading({ value, unit }: { value?: number | null; unit?: string }) {
  if (value == null) return <span className={styles.metricEmpty}>暂无数据</span>;
  return (
    <span className={styles.metricReading}>
      <strong className={styles.metricNumber}>{Number(value).toLocaleString("zh-CN")}</strong>
      {unit ? <span className={styles.metricUnit}>{unit}</span> : null}
    </span>
  );
}

function escapeHtml(value?: string) {
  return (value || "-")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatTooltipMetric(value: number | null | undefined) {
  if (value == null || !Number.isFinite(Number(value))) return "-";
  return Number(value).toLocaleString("zh-CN");
}

function getTooltipStatus(status?: string) {
  const meta = STATUS_META[status || "draft"];
  if (!meta) return null;
  const styles: Record<string, { color: string; background: string; border: string }> = {
    draft: { color: "#6f7f72", background: "#f2f1ec", border: "#ded8cc" },
    submitted: { color: "#d97706", background: "#fff4df", border: "#f4d6a5" },
    approved: { color: "#2f7d4f", background: "#eef7ef", border: "#cce3d0" },
    rejected: { color: "#b94a48", background: "#fff0ee", border: "#efcfca" },
    archived: { color: "#5d7467", background: "#edf3ee", border: "#cfdbd1" },
  };
  return { ...meta, ...(styles[status || "draft"] || styles.draft) };
}

function hasMetricValue(point: GrowthChartPointApi, metric: GrowthMetricKey) {
  const value = point[metric];
  return typeof value === "number" && Number.isFinite(value);
}

function findFirstValidMetric(points: GrowthChartPointApi[]) {
  return DEFAULT_METRIC_ORDER.find((candidate) =>
    points.some((point) => hasMetricValue(point, candidate)),
  );
}

function StatusBadge({ status }: { status?: string }) {
  const meta = STATUS_META[status || "draft"] || STATUS_META.draft;
  return (
    <span
      className={styles.statusBadge}
      style={{ "--status-color": meta.color } as React.CSSProperties}
    >
      <i />
      {meta.label}
    </span>
  );
}

function GrowthTrendChart({
  data,
  metric,
  emptyDescription,
  emptyHint,
  showAuditContext,
  selectedId,
  onSelect,
}: {
  data: GrowthChartDatum[];
  metric: GrowthMetricKey;
  emptyDescription?: string;
  emptyHint?: string;
  showAuditContext: boolean;
  selectedId?: number;
  onSelect: (recordId: number) => void;
}) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    const chart = init(containerRef.current);
    const metricMeta = METRICS[metric];
    const option: EChartsCoreOption = {
      color: ["#315f3d"],
      animationDuration: 450,
      grid: { left: 56, right: 32, top: 36, bottom: 52, containLabel: true },
      tooltip: {
        trigger: "item",
        confine: true,
        appendToBody: true,
        backgroundColor: "#fffaf2",
        borderColor: "#eadfcd",
        borderWidth: 1,
        padding: 0,
        textStyle: {
          color: "#0f3d2e",
          fontFamily:
            "var(--font-sans), system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
        },
        extraCssText:
          "border-radius:14px;box-shadow:0 12px 30px rgba(15,61,46,0.14);overflow:hidden;min-width:240px;max-width:320px;",
        formatter: (params: unknown) => {
          const point = (params as { data?: GrowthChartDatum }).data;
          if (!point) return "";
          const status = showAuditContext ? getTooltipStatus(point.auditStatus) : null;
          const unit = metricMeta.unit;
          const summary = point.summary?.trim();
          const showSummary = summary && summary !== "-";
          const statusTag = status
            ? `<span style="flex:0 0 auto;padding:3px 8px;border:1px solid ${status.border};border-radius:999px;background:${status.background};color:${status.color};font-size:12px;font-weight:700;line-height:1.4;">${status.label}</span>`
            : "";
          return `
            <div style="box-sizing:border-box;width:100%;min-width:240px;max-width:320px;padding:14px 16px;background:#fffaf2;color:#0f3d2e;font-family:var(--font-sans),system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
              <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:12px;">
                <div style="min-width:0;color:#0f3d2e;font-size:15px;font-weight:800;line-height:1.45;overflow-wrap:anywhere;">${escapeHtml(point.batchName || "未命名批次")}</div>
                ${statusTag}
              </div>
              <div style="margin-bottom:13px;padding:12px 0;border-top:1px solid rgba(234,223,205,0.72);border-bottom:1px solid rgba(234,223,205,0.72);">
                <div style="color:#6f7f72;font-size:12px;font-weight:700;line-height:1.4;">${escapeHtml(metricMeta.label)}</div>
                <div style="margin-top:4px;color:#0f5132;font-size:27px;font-weight:800;line-height:1.1;">
                  ${escapeHtml(formatTooltipMetric(point.value))}
                  ${unit ? `<span style="margin-left:5px;color:#6f7f72;font-size:14px;font-weight:700;">${escapeHtml(unit)}</span>` : ""}
                </div>
              </div>
              <div style="display:grid;gap:8px;margin-bottom:${showSummary ? "12px" : "0"};">
                <div style="display:flex;gap:12px;align-items:flex-start;font-size:13px;line-height:1.55;">
                  <span style="flex:0 0 64px;color:#6f7f72;">采集时间</span>
                  <b style="min-width:0;color:#0f3d2e;font-weight:700;overflow-wrap:anywhere;">${escapeHtml(formatTime(point.collectTime))}</b>
                </div>
                <div style="display:flex;gap:12px;align-items:flex-start;font-size:13px;line-height:1.55;">
                  <span style="flex:0 0 64px;color:#6f7f72;">生长阶段</span>
                  <b style="min-width:0;color:#0f3d2e;font-weight:700;overflow-wrap:anywhere;">${escapeHtml(point.growthStage || "-")}</b>
                </div>
                <div style="display:flex;gap:12px;align-items:flex-start;font-size:13px;line-height:1.55;">
                  <span style="flex:0 0 64px;color:#6f7f72;">采集员</span>
                  <b style="min-width:0;color:#0f3d2e;font-weight:700;overflow-wrap:anywhere;">${escapeHtml(point.collectorName || "-")}</b>
                </div>
              </div>
              ${
                showSummary
                  ? `<div style="padding:10px 11px;border:1px solid rgba(15,81,50,0.10);border-radius:10px;background:#eef7ef;color:#53645a;">
                      <span style="display:block;margin-bottom:4px;color:#0f5132;font-size:12px;font-weight:800;line-height:1.4;">摘要</span>
                      <p style="margin:0;color:#53645a;font-size:13px;line-height:1.6;white-space:normal;overflow-wrap:anywhere;">${escapeHtml(summary)}</p>
                    </div>`
                  : ""
              }
            </div>
          `;
        },
      },
      xAxis: {
        type: "category",
        name: "采集时间",
        nameLocation: "middle",
        nameGap: 34,
        boundaryGap: false,
        data: data.length
          ? data.map((point) =>
              point.collectTime
                ? new Date(point.collectTime).toLocaleDateString("zh-CN", {
                    month: "2-digit",
                    day: "2-digit",
                  })
                : `#${point.recordId}`,
            )
          : ["暂无数据"],
        nameTextStyle: { color: "#647167", fontSize: 13 },
        axisLine: { show: true, lineStyle: { color: "#aeb8af" } },
        axisTick: { show: false },
        axisLabel: { show: true, color: "#677066", margin: 14, rotate: data.length > 7 ? 28 : 0 },
      },
      yAxis: {
        type: "value",
        name: `${metricMeta.label}${metricMeta.unit ? `(${metricMeta.unit})` : ""}`,
        min: data.length ? undefined : 0,
        max: data.length ? undefined : 1,
        nameTextStyle: { color: "#647167", fontSize: 13, padding: [0, 0, 8, 0] },
        axisLine: { show: true, lineStyle: { color: "#aeb8af" } },
        axisLabel: { show: true, color: "#677066" },
        splitLine: { show: true, lineStyle: { color: "rgba(69, 91, 72, 0.15)", type: "dashed" } },
      },
      series: [
        {
          type: "line",
          smooth: true,
          symbol: "circle",
          connectNulls: false,
          lineStyle: { color: "#315f3d", width: 3 },
          areaStyle: { color: "rgba(77, 123, 84, 0.08)" },
          data: data.map((point) => {
            const status = showAuditContext
              ? STATUS_META[point.auditStatus || "draft"] || STATUS_META.draft
              : { label: "", color: "#2f7d4f" };
            const isSelected = point.recordId === selectedId;
            const isPending = point.auditStatus === "submitted";
            return {
              ...point,
              symbolSize: isPending ? 13 : 9,
              itemStyle: {
                color: status.color,
                borderColor: isSelected || isPending ? "#fffaf0" : status.color,
                borderWidth: isSelected ? 4 : isPending ? 3 : 1,
                shadowBlur: isPending ? 10 : 0,
                shadowColor: isPending ? "rgba(217, 130, 43, 0.45)" : "transparent",
              },
            };
          }),
        },
      ],
      graphic: !data.length
        ? {
            type: "group",
            left: "center",
            top: "middle",
            silent: true,
            children: [
              {
                type: "text",
                x: 0,
                y: -32,
                style: {
                  text: "当前筛选条件下暂无趋势观测点",
                  fill: "#344b3a",
                  font: "600 16px sans-serif",
                  textAlign: "center",
                },
              },
              {
                type: "text",
                x: 0,
                y: 0,
                style: {
                  text: emptyDescription || "当前没有可绘制的数据。",
                  fill: "#667369",
                  font: "14px sans-serif",
                  textAlign: "center",
                },
              },
              {
                type: "text",
                x: 0,
                y: 30,
                style: {
                  text: emptyHint || "可切换为“全部状态”或选择其他观测指标。",
                  fill: "#7a847b",
                  font: "13px sans-serif",
                  textAlign: "center",
                },
              },
            ],
          }
        : undefined,
    };
    chart.setOption(option);
    chart.on("click", (params: unknown) => {
      const recordId = (params as { data?: GrowthChartDatum }).data?.recordId;
      if (recordId) onSelect(recordId);
    });
    const resizeObserver = new ResizeObserver(() => chart.resize());
    resizeObserver.observe(containerRef.current);
    return () => {
      resizeObserver.disconnect();
      chart.dispose();
    };
  }, [data, emptyDescription, emptyHint, metric, onSelect, selectedId, showAuditContext]);

  return (
    <div
      className={styles.chart}
      ref={containerRef}
      style={{ height: data.length ? 420 : 320 }}
    />
  );
}

type GrowthTaskFormValues = {
  taskCode: string;
  taskName: string;
  speciesId: number;
  baseId?: number;
  collectPlace?: string;
  plannedStartTime?: string;
  plannedEndTime?: string;
  collectorId: number;
  description?: string;
  remark?: string;
};

function createTaskCode() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  return `TASK_${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
}

function normalizeTaskDateTime(value?: string) {
  if (!value) return undefined;
  const normalized = value.replace("T", " ");
  return normalized.length === 16 ? `${normalized}:00` : normalized;
}

export default function GrowthPage() {
  const { message, modal } = App.useApp();
  const user = useAuthStore((state) => state.user);
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const roleCodes = user?.roleCodes || [];
  const showReviewWorkspace = roleCodes.some((role) =>
    ["ADMIN", "TEACHER", "REVIEWER"].includes(role),
  );
  const canCreateTask = roleCodes.some((role) => ["ADMIN", "TEACHER"].includes(role));
  const isCollectorOnly = roleCodes.includes("COLLECTOR") && !showReviewWorkspace;
  const canReview = showReviewWorkspace && hasPermission("growth:record:audit");
  const reviewerDefaultApplied = useRef(canReview);
  const [auditForm] = Form.useForm<{ comment?: string }>();
  const [taskForm] = Form.useForm<GrowthTaskFormValues>();
  const [tasks, setTasks] = useState<GrowthTaskApi[]>([]);
  const [taskId, setTaskId] = useState<number>();
  const [herbId, setHerbId] = useState<number>();
  const [baseId, setBaseId] = useState<number>();
  const [status, setStatus] = useState(canReview ? "submitted" : "all");
  const [metric, setMetric] = useState<GrowthMetricKey>("plantHeight");
  const [points, setPoints] = useState<GrowthChartPointApi[]>([]);
  const [selectedRecord, setSelectedRecord] = useState<GrowthRecordApi | null>(null);
  const [batchImages, setBatchImages] = useState<GrowthBatchImageApi[]>([]);
  const [auditHistory, setAuditHistory] = useState<GrowthAuditHistoryApi[]>([]);
  const [traceEvents, setTraceEvents] = useState<GrowthTraceEventApi[]>([]);
  const [traceQrCode, setTraceQrCode] = useState<GrowthTraceQrCodeApi>();
  const [traceOperating, setTraceOperating] = useState<string>();
  const [loadingTasks, setLoadingTasks] = useState(true);
  const [loadingChart, setLoadingChart] = useState(false);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [auditAction, setAuditAction] = useState<"approve" | "reject" | null>(null);
  const [auditSubmitting, setAuditSubmitting] = useState(false);
  const [autoMetricNotice, setAutoMetricNotice] = useState<string>();
  const [detailTab, setDetailTab] = useState<DetailTab>("base");
  const [collectorFilter, setCollectorFilter] = useState<string>();
  const [keyword, setKeyword] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [pageNumber, setPageNumber] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [advancedFiltersOpen, setAdvancedFiltersOpen] = useState(false);
  const [trendOpen, setTrendOpen] = useState(false);
  const [taskCreateOpen, setTaskCreateOpen] = useState(false);
  const [taskCreating, setTaskCreating] = useState(false);
  const [taskPublishing, setTaskPublishing] = useState(false);
  const [taskOptionsLoading, setTaskOptionsLoading] = useState(false);
  const [taskHerbs, setTaskHerbs] = useState<HerbSpeciesApi[]>([]);
  const [taskBases, setTaskBases] = useState<HerbBaseApi[]>([]);
  const [taskCollectors, setTaskCollectors] = useState<GrowthCollectorOptionApi[]>([]);

  useEffect(() => {
    if (canReview && !reviewerDefaultApplied.current) {
      reviewerDefaultApplied.current = true;
      setStatus("submitted");
    }
  }, [canReview]);

  useEffect(() => {
    if (isCollectorOnly) setStatus("all");
  }, [isCollectorOnly]);

  const loadTasks = useCallback(async () => {
    if (!user) return;
    setLoadingTasks(true);
    try {
      const page = await (isCollectorOnly ? fetchMyGrowthTasks() : fetchGrowthTasks());
      setTasks(page.records);
    } catch (error) {
      if (!isAuthRedirectError(error)) message.error(getApiErrorMessage(error, "采集任务加载失败"));
    } finally {
      setLoadingTasks(false);
    }
  }, [isCollectorOnly, message, user]);

  useEffect(() => {
    void loadTasks();
  }, [loadTasks]);

  async function openTaskCreate() {
    taskForm.resetFields();
    taskForm.setFieldsValue({ taskCode: createTaskCode() });
    setTaskCreateOpen(true);
    setTaskOptionsLoading(true);
    try {
      const [herbs, bases, collectors] = await Promise.all([
        fetchEnabledHerbs(),
        fetchHerbBases(),
        fetchAssignableGrowthCollectors(),
      ]);
      setTaskHerbs(herbs);
      setTaskBases(bases.records);
      setTaskCollectors(collectors);
    } catch (error) {
      message.error(getApiErrorMessage(error, "任务创建选项加载失败"));
    } finally {
      setTaskOptionsLoading(false);
    }
  }

  async function submitTaskCreate(values: GrowthTaskFormValues) {
    const collector = taskCollectors.find((item) => item.id === values.collectorId);
    if (!collector) {
      message.warning("请选择有效采集员");
      return;
    }
    if (values.plannedStartTime && values.plannedEndTime && values.plannedEndTime < values.plannedStartTime) {
      message.warning("计划结束时间不能早于开始时间");
      return;
    }
    const herb = taskHerbs.find((item) => item.id === values.speciesId);
    const base = taskBases.find((item) => item.id === values.baseId);
    setTaskCreating(true);
    try {
      const created = await createGrowthTask({
        taskCode: values.taskCode.trim(),
        taskName: values.taskName.trim(),
        speciesId: values.speciesId,
        speciesName: herb?.herbName,
        baseId: values.baseId,
        baseName: base?.baseName,
        collectPlace: values.collectPlace?.trim() || undefined,
        plannedStartTime: normalizeTaskDateTime(values.plannedStartTime),
        plannedEndTime: normalizeTaskDateTime(values.plannedEndTime),
        collectorId: values.collectorId,
        collectorName: collector.name,
        description: values.description?.trim() || undefined,
        remark: values.remark?.trim() || undefined,
      });
      message.success("采集任务已创建为草稿，请发布后再由采集员在手机端查看");
      setTaskCreateOpen(false);
      taskForm.resetFields();
      await loadTasks();
      setTaskId(created.id);
    } catch (error) {
      message.error(getApiErrorMessage(error, "采集任务创建失败"));
    } finally {
      setTaskCreating(false);
    }
  }

  async function publishSelectedTask() {
    if (!selectedTask || selectedTask.taskStatus !== "draft") {
      message.info("请选择一个草稿状态的采集任务");
      return;
    }
    setTaskPublishing(true);
    try {
      await publishGrowthTask(selectedTask.id);
      message.success("采集任务发布成功，指定采集员现在可在手机端查看");
      await loadTasks();
    } catch (error) {
      message.error(getApiErrorMessage(error, "采集任务发布失败"));
    } finally {
      setTaskPublishing(false);
    }
  }
  const filteredTasks = useMemo(
    () =>
      tasks.filter(
        (task) => (!herbId || task.speciesId === herbId) && (!baseId || task.baseId === baseId),
      ),
    [baseId, herbId, tasks],
  );

  useEffect(() => {
    if (!filteredTasks.length) {
      setTaskId(undefined);
      return;
    }
    if (!taskId || !filteredTasks.some((task) => task.id === taskId)) {
      setTaskId(filteredTasks[0].id);
    }
  }, [filteredTasks, taskId]);

  const loadChart = useCallback(async () => {
    if (!taskId) {
      setPoints([]);
      return;
    }
    setLoadingChart(true);
    try {
      setPoints(await fetchGrowthChart(taskId, "plantHeight"));
    } catch (error) {
      if (!isAuthRedirectError(error)) message.error(getApiErrorMessage(error, "生长趋势加载失败"));
      setPoints([]);
    } finally {
      setLoadingChart(false);
    }
  }, [message, taskId]);

  useEffect(() => {
    void loadChart();
  }, [loadChart]);

  const visiblePoints = useMemo(() => points, [points]);
  const statusPoints = useMemo(
    () =>
      status === "all"
        ? visiblePoints
        : visiblePoints.filter((point) => point.auditStatus === status),
    [status, visiblePoints],
  );

  useEffect(() => {
    if (!statusPoints.length || statusPoints.some((point) => hasMetricValue(point, metric))) return;
    const fallbackMetric = findFirstValidMetric(statusPoints);
    if (fallbackMetric && fallbackMetric !== metric) {
      setAutoMetricNotice(
        `当前任务暂无${METRICS[metric].label}数据，已自动切换为${METRICS[fallbackMetric].label}趋势。`,
      );
      setMetric(fallbackMetric);
    }
  }, [metric, statusPoints]);

  const chartResult = useMemo(
    () => buildGrowthChartData(visiblePoints, metric, status),
    [metric, status, visiblePoints],
  );
  const chartData = chartResult.validPoints;
  const validPointCount = chartData.length;
  const emptyDescription = useMemo(() => {
    if (!taskId) return "请先选择采集任务。";
    if (chartResult.reason === "no-status-records" && status === "submitted") {
      return "当前状态下没有待审核记录。";
    }
    return chartResult.reason ? EMPTY_REASON_LABELS[chartResult.reason] + "。" : undefined;
  }, [chartResult.reason, status, taskId]);
  const emptyHint = isCollectorOnly
    ? "可选择其他采集任务或观测指标。"
    : "可切换为“全部状态”或选择其他观测指标。";
  const submittedPointCount = chartData.filter(
    (point) => point.auditStatus === "submitted",
  ).length;

  const taskOverview = useMemo(() => {
    const countStatus = (target: string) =>
      visiblePoints.filter((point) => point.auditStatus === target).length;
    return {
      batchCount: new Set(visiblePoints.map((point) => point.batchId)).size,
      recordCount: visiblePoints.length,
      submittedCount: countStatus("submitted"),
      approvedCount: countStatus("approved"),
      rejectedCount: countStatus("rejected"),
    };
  }, [visiblePoints]);

  const selectedTask = tasks.find((task) => task.id === taskId);
  const collectorOptions = useMemo(
    () =>
      Array.from(new Set(visiblePoints.map((point) => point.collectorName).filter(Boolean))).map(
        (name) => ({ value: name!, label: name! }),
      ),
    [visiblePoints],
  );
  const tableRecords = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return statusPoints.filter((point) => {
      if (collectorFilter && point.collectorName !== collectorFilter) return false;
      if (startDate && (!point.collectTime || point.collectTime.slice(0, 10) < startDate)) return false;
      if (endDate && (!point.collectTime || point.collectTime.slice(0, 10) > endDate)) return false;
      if (!normalizedKeyword) return true;
      return [point.batchName, point.collectorName, point.growthStage, point.summary]
        .filter(Boolean)
        .some((value) => value!.toLowerCase().includes(normalizedKeyword));
    });
  }, [collectorFilter, endDate, keyword, startDate, statusPoints]);
  const totalPages = Math.max(1, Math.ceil(tableRecords.length / pageSize));
  const pagedRecords = useMemo(
    () => tableRecords.slice((pageNumber - 1) * pageSize, pageNumber * pageSize),
    [pageNumber, pageSize, tableRecords],
  );
  const herbOptions = useMemo(
    () =>
      Array.from(
        new Map(
          tasks.filter((task) => task.speciesId).map((task) => [task.speciesId, task.speciesName]),
        ),
      ).map(([value, label]) => ({ value, label: label || `药材 #${value}` })),
    [tasks],
  );
  const baseOptions = useMemo(
    () =>
      Array.from(
        new Map(tasks.filter((task) => task.baseId).map((task) => [task.baseId, task.baseName])),
      ).map(([value, label]) => ({ value, label: label || `基地 #${value}` })),
    [tasks],
  );

  const refreshSelected = useCallback(
    async (recordId: number) => {
      const detail = await fetchGrowthRecordDetail(recordId);
      const [history, trace, images, qrCode] = await Promise.all([
        showReviewWorkspace ? fetchGrowthAuditHistory(recordId) : Promise.resolve([]),
        fetchGrowthTrace(recordId),
        detail.batchId ? fetchGrowthBatchImages(detail.batchId) : Promise.resolve([]),
        getGrowthTraceQrCode(recordId).catch(() => undefined),
      ]);
      setSelectedRecord(detail);
      setBatchImages(images);
      setAuditHistory(history);
      setTraceEvents(trace);
      setTraceQrCode(qrCode);
    },
    [showReviewWorkspace],
  );

  const openDetail = useCallback(
    async (recordId: number) => {
      setDetailTab("base");
      setLoadingDetail(true);
      setSelectedRecord({ id: recordId } as GrowthRecordApi);
      setBatchImages([]);
      setAuditHistory([]);
      setTraceEvents([]);
      setTraceQrCode(undefined);
      try {
        await refreshSelected(recordId);
      } catch (error) {
        setSelectedRecord(null);
        message.error(getApiErrorMessage(error, "观测点详情加载失败"));
      } finally {
        setLoadingDetail(false);
      }
    },
    [message, refreshSelected],
  );

  function resetFilters() {
    setHerbId(undefined);
    setBaseId(undefined);
    setStatus(canReview ? "submitted" : "all");
    setMetric("plantHeight");
    setCollectorFilter(undefined);
    setKeyword("");
    setStartDate("");
    setEndDate("");
    setPageNumber(1);
    setAutoMetricNotice(undefined);
  }

  async function refreshTracePanel(recordId: number, qrCode?: GrowthTraceQrCodeApi) {
    const [nextQrCode, nextTrace] = await Promise.all([
      qrCode ? Promise.resolve(qrCode) : getGrowthTraceQrCode(recordId),
      fetchGrowthTrace(recordId),
    ]);
    setTraceQrCode(nextQrCode);
    setTraceEvents(nextTrace);
  }

  async function performTraceAction(
    action: string,
    operation: (recordId: number) => Promise<GrowthTraceQrCodeApi>,
    successMessage: string,
  ) {
    if (!selectedRecord?.id) return;
    setTraceOperating(action);
    try {
      const qrCode = await operation(selectedRecord.id);
      await refreshTracePanel(selectedRecord.id, qrCode);
      message.success(successMessage);
    } catch (error) {
      message.error(getApiErrorMessage(error, "溯源操作失败，请稍后重试"));
    } finally {
      setTraceOperating(undefined);
    }
  }

  async function copyTraceLink() {
    const traceUrl = getPublicTracePageUrl(traceQrCode?.traceCode);
    if (!traceUrl) {
      message.info("请先生成溯源码或二维码");
      return;
    }
    try {
      await navigator.clipboard.writeText(traceUrl);
      message.success("溯源链接已复制");
    } catch {
      message.error("复制失败，请手动复制溯源链接");
    }
  }

  async function openPublicTracePage() {
    const traceUrl = getPublicTracePageUrl(traceQrCode?.traceCode);
    if (!traceUrl) {
      message.info("请先生成溯源码或二维码");
      return;
    }
    const openPage = () => window.open(traceUrl, "_blank", "noopener,noreferrer");
    if (traceQrCode?.publicVisible !== 1) {
      if (!showReviewWorkspace || !selectedRecord?.id) {
        message.info("该溯源档案尚未公开，请联系管理员开启公开溯源");
        return;
      }
      modal.confirm({
        title: "溯源档案尚未公开",
        content: "开启后，外部用户可通过二维码和公开链接访问该档案，是否开启并打开？",
        okText: "开启并打开",
        cancelText: "取消",
        onOk: async () => {
          setTraceOperating("enable");
          try {
            const qrCode = await enableGrowthPublicTrace(selectedRecord.id);
            await refreshTracePanel(selectedRecord.id, qrCode);
            message.success("公开溯源已开启");
            openPage();
          } catch (error) {
            message.error(getApiErrorMessage(error, "开启公开溯源失败，请稍后重试"));
            throw error;
          } finally {
            setTraceOperating(undefined);
          }
        },
      });
      return;
    }
    openPage();
  }

  async function downloadTraceQrCode() {
    if (!traceQrCode?.qrCodeUrl) {
      message.info("请先生成二维码");
      return;
    }
    try {
      if (!selectedRecord?.id) throw new Error("未选择生长记录");
      const objectUrl = URL.createObjectURL(await downloadGrowthTraceQrCode(selectedRecord.id));
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = `growth-trace-${traceQrCode.traceCode || selectedRecord?.id}.png`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(objectUrl);
    } catch {
      message.error("二维码下载失败，请稍后重试");
    }
  }

  function confirmDisablePublicTrace() {
    modal.confirm({
      title: "关闭公开溯源",
      content: "关闭后，外部用户将无法通过二维码访问该溯源档案，是否继续？",
      okText: "确认关闭",
      okButtonProps: { danger: true },
      cancelText: "取消",
      onOk: () => performTraceAction("disable", disableGrowthPublicTrace, "公开溯源已关闭"),
    });
  }

  function exportRecords() {
    if (!tableRecords.length) {
      message.info("当前筛选条件下没有可导出的生长记录");
      return;
    }
    const rows = [
      ["记录编号", "药材", "批次", "采集人", "采集时间", "生长阶段", "审核状态"],
      ...tableRecords.map((point) => [
        point.recordId,
        selectedTask?.speciesName || "-",
        point.batchName || `批次 #${point.batchId}`,
        point.collectorName || "-",
        formatTime(point.collectTime),
        point.growthStage || "-",
        STATUS_META[point.auditStatus || "draft"]?.label || "草稿",
      ]),
    ];
    const csv = rows.map((row) => row.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(",")).join("\n");
    const url = URL.createObjectURL(new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8" }));
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `生长记录-${new Date().toISOString().slice(0, 10)}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  useEffect(() => {
    setPageNumber(1);
  }, [collectorFilter, endDate, keyword, pageSize, startDate, status, taskId]);

  useEffect(() => {
    if (pageNumber > totalPages) setPageNumber(totalPages);
  }, [pageNumber, totalPages]);

  function showAllStatuses() {
    if (status === "all") {
      message.info("当前已是全部状态");
      return;
    }
    setAutoMetricNotice(undefined);
    setStatus("all");
    message.success("已切换为全部状态");
  }

  async function refreshAfterAction(recordId: number) {
    await Promise.all([loadChart(), refreshSelected(recordId)]);
  }

  async function submitForReview() {
    if (!selectedRecord) return;
    try {
      await submitGrowthRecord(selectedRecord.id);
      message.success("已提交审核");
      await refreshAfterAction(selectedRecord.id);
    } catch (error) {
      message.error(getApiErrorMessage(error, "提交审核失败"));
    }
  }

  function auditErrorMessage(error: unknown) {
    if (axios.isAxiosError(error) && error.response?.status === 403) return "当前用户无审核权限";
    const detail = getApiErrorMessage(error, "");
    if (detail.includes("已提交") || detail.includes("状态")) return "当前状态不可审核";
    return "操作失败，请稍后重试";
  }

  async function submitAudit(values: { comment?: string }) {
    if (!selectedRecord || !auditAction) return;
    if (auditAction === "reject" && !values.comment?.trim()) {
      message.warning("请填写驳回原因");
      return;
    }
    setAuditSubmitting(true);
    try {
      if (auditAction === "approve") {
        await approveGrowthRecord(selectedRecord.id, { comment: values.comment?.trim() });
        message.success("审核通过成功");
      } else {
        await rejectGrowthRecord(selectedRecord.id, { comment: values.comment!.trim() });
        message.success("审核驳回成功");
      }
      setAuditAction(null);
      auditForm.resetFields();
      await refreshAfterAction(selectedRecord.id);
    } catch (error) {
      message.error(auditErrorMessage(error));
    } finally {
      setAuditSubmitting(false);
    }
  }

  const canAuditSelected = canReview && selectedRecord?.reviewStatus === "submitted";
  const canSubmitSelected =
    hasPermission("growth:record:submit") &&
    ["draft", "rejected"].includes(selectedRecord?.reviewStatus || "");
  return (
    <SiteLayout>
      <main className={styles.page}>
        <section
          className={styles.growthHero}
          aria-label="生长数据工作台"
          style={heroIllustrationUrl ? { backgroundImage: `url(${heroIllustrationUrl})` } : undefined}
        >
          <div className={styles.heroCopy}>
            <span>本草研究院标本馆</span>
            <h1>生长数据</h1>
            <p>管理移动端采集的中药材生长记录，汇聚现场图片、审核状态与溯源轨迹。</p>
          </div>
          <div className={styles.heroIllustrationPlaceholder} aria-hidden="true">
            <i /><i /><i />
          </div>
        </section>

        <div className={styles.workspaceLayout}>
          <div className={styles.workspaceMain}>

        <section className={styles.filters} aria-label="趋势筛选">
          <div className={styles.filterHeading}>
            <div>
              <h2>记录筛选</h2>
              <p>按任务、采集人和审核状态快速定位生长记录。</p>
            </div>
          </div>
          <div
            className={`${styles.filterGrid} ${isCollectorOnly ? styles.collectorFilterGrid : ""}`}
          >
            <label className={styles.taskFilter}>
              <span>采集任务</span>
              <Select
                showSearch
                optionFilterProp="label"
                loading={loadingTasks}
                title={selectedTask?.taskName}
                value={taskId}
                options={filteredTasks.map((task) => ({
                  value: task.id,
                  label: `${task.taskName}（${task.taskCode}）`,
                }))}
                onChange={setTaskId}
              />
            </label>
            <label>
              <span>药材</span>
              <Select
                allowClear
                placeholder="全部药材"
                value={herbId}
                options={herbOptions}
                onChange={setHerbId}
              />
            </label>
            {showReviewWorkspace ? (
              <label>
                <span>审核状态</span>
                <Select
                  value={status}
                  options={STATUS_OPTIONS}
                  onChange={(value) => {
                    setAutoMetricNotice(undefined);
                    setStatus(value);
                  }}
                />
              </label>
            ) : null}
            <label className={styles.keywordFilter}>
              <span>关键词</span>
              <Input
                allowClear
                placeholder="搜索批次、采集人、阶段或备注"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
              />
            </label>
          </div>
          {advancedFiltersOpen ? (
            <div className={styles.advancedFilterGrid}>
              <label>
                <span>采集人</span>
                <Select allowClear placeholder="全部采集人" value={collectorFilter} options={collectorOptions} onChange={setCollectorFilter} />
              </label>
              <label>
                <span>基地</span>
                <Select allowClear placeholder="全部基地" value={baseId} options={baseOptions} onChange={setBaseId} />
              </label>
              <label>
                <span>观测指标</span>
                <Select
                  value={metric}
                  options={Object.entries(METRICS).map(([value, meta]) => ({ value, label: meta.label }))}
                  onChange={(value) => {
                    setAutoMetricNotice(undefined);
                    setMetric(value);
                  }}
                />
              </label>
              <label className={styles.dateFilter}>
                <span>时间范围</span>
                <div>
                  <input aria-label="开始日期" type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
                  <b>至</b>
                  <input aria-label="结束日期" type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
                </div>
              </label>
              <label>
                <span>数据来源</span>
                <Select disabled placeholder="全部来源" title="后端暂未提供来源筛选字段" />
              </label>
            </div>
          ) : null}
          <div className={styles.filterActions}>
            <Button type="primary" icon={<Filter size={15} />} onClick={() => setPageNumber(1)}>查询</Button>
            <Button icon={<RefreshCw size={15} />} onClick={resetFilters}>重置</Button>
            <Button icon={<Download size={15} />} onClick={exportRecords}>导出</Button>
            <Button
              icon={<ChevronDown className={advancedFiltersOpen ? styles.chevronUp : undefined} size={15} />}
              onClick={() => setAdvancedFiltersOpen((value) => !value)}
            >
              {advancedFiltersOpen ? "收起筛选" : "展开筛选"}
            </Button>
          </div>
          <div className={styles.filterSummary}>
            <strong>当前条件</strong>
            <div className={styles.filterChips}>
              <span className={styles.filterChip} title={selectedTask?.taskName}>
                任务：<b>{selectedTask?.taskName || "未选择"}</b>
              </span>
              <span className={styles.filterChip}>
                指标：<b>{METRICS[metric].label}</b>
              </span>
              {herbId ? (
                <span className={styles.filterChip}>
                  药材：<b>{herbOptions.find((item) => item.value === herbId)?.label || "-"}</b>
                </span>
              ) : null}
              {baseId ? (
                <span className={styles.filterChip}>
                  基地：<b>{baseOptions.find((item) => item.value === baseId)?.label || "-"}</b>
                </span>
              ) : null}
              {showReviewWorkspace ? (
                <span className={styles.filterChip}>
                  状态：<b>{STATUS_OPTIONS.find((item) => item.value === status)?.label || "全部状态"}</b>
                </span>
              ) : null}
              <span className={`${styles.filterChip} ${styles.filterPointChip}`}>
                观测点：<b>{chartData.length}</b>
              </span>
            </div>
          </div>
        </section>

        <section className={styles.recordWorkspace} aria-label="生长记录列表">
          <div className={styles.recordToolbar}>
            <div>
              {canCreateTask ? (
                <>
                  <Button type="primary" icon={<Plus size={15} />} onClick={() => void openTaskCreate()}>
                    创建采集任务
                  </Button>
                  <Button
                    icon={<Send size={15} />}
                    disabled={selectedTask?.taskStatus !== "draft" || taskPublishing}
                    loading={taskPublishing}
                    onClick={() => void publishSelectedTask()}
                  >
                    发布任务
                  </Button>
                </>
              ) : null}
              {!showReviewWorkspace ? (
                <Button type="primary" icon={<Plus size={15} />} disabled title="请在移动端批次详情中创建生长记录">
                  新增记录
                </Button>
              ) : null}
              <Button
                icon={<Edit3 size={15} />}
                disabled={!selectedRecord || !["draft", "rejected"].includes(selectedRecord.reviewStatus || "")}
                title="生长记录编辑由移动端批次流程完成"
              >
                编辑
              </Button>
              <Button icon={<Send size={15} />} disabled={!canSubmitSelected} onClick={() => void submitForReview()}>
                提交审核
              </Button>
              <Button icon={<Download size={15} />} onClick={exportRecords}>导出</Button>
            </div>
            <span>共 <strong>{tableRecords.length}</strong> 条记录</span>
          </div>

          <div className={styles.tableScroller}>
            <table className={styles.recordTable}>
              <thead>
                <tr>
                  <th aria-label="选择" />
                  <th>序号</th>
                  <th>药材名称</th>
                  <th>采集人</th>
                  <th>采集地点</th>
                  <th>采集时间</th>
                  <th>生长阶段</th>
                  <th>审核状态</th>
                  <th>数据来源</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {pagedRecords.map((point, index) => (
                  <tr
                    key={point.recordId}
                    className={selectedRecord?.id === point.recordId ? styles.selectedRow : undefined}
                    onClick={() => void openDetail(point.recordId)}
                  >
                    <td><input type="radio" aria-label={`选择记录 ${point.recordId}`} checked={selectedRecord?.id === point.recordId} readOnly /></td>
                    <td>{(pageNumber - 1) * pageSize + index + 1}</td>
                    <td>
                      <span className={styles.herbIdentity}><i>{(selectedTask?.speciesName || "药").slice(0, 1)}</i><b>{selectedTask?.speciesName || "未记录药材"}</b></span>
                    </td>
                    <td>{point.collectorName || "-"}</td>
                    <td title={selectedTask?.collectPlace || selectedTask?.baseName}>{selectedTask?.collectPlace || selectedTask?.baseName || "-"}</td>
                    <td>{formatTime(point.collectTime)}</td>
                    <td><span className={styles.stageTag}>{point.growthStage || "-"}</span></td>
                    <td><StatusBadge status={point.auditStatus} /></td>
                    <td>-</td>
                    <td>
                      <div className={styles.rowActions}>
                        <button type="button" onClick={(event) => { event.stopPropagation(); void openDetail(point.recordId); }}>查看</button>
                        {["draft", "rejected"].includes(point.auditStatus || "") && !showReviewWorkspace ? <button type="button" disabled>编辑</button> : null}
                        <button type="button" disabled>更多</button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!pagedRecords.length ? (
                  <tr><td colSpan={10}><div className={styles.tableEmpty}><strong>暂无生长记录</strong><span>可调整筛选条件后重新查询。</span></div></td></tr>
                ) : null}
              </tbody>
            </table>
          </div>

          <div className={styles.paginationBar}>
            <span>共 {tableRecords.length} 条</span>
            <Select value={pageSize} options={[10, 20, 50].map((value) => ({ value, label: `${value} 条/页` }))} onChange={setPageSize} />
            <Button disabled={pageNumber <= 1} onClick={() => setPageNumber((value) => value - 1)}>上一页</Button>
            <span className={styles.pageIndicator}>{pageNumber} / {totalPages}</span>
            <Button disabled={pageNumber >= totalPages} onClick={() => setPageNumber((value) => value + 1)}>下一页</Button>
          </div>
        </section>

        <section className={styles.overviewPanel} aria-label="任务概览">
          <div className={styles.overviewHeading}>
            <div className={styles.overviewIdentity}>
              <span>{selectedTask?.taskCode || "TASK"}</span>
              <h2 title={selectedTask?.taskName}>{selectedTask?.taskName || "未选择采集任务"}</h2>
              <p className={styles.taskMetaLine}>
                {selectedTask?.speciesName || "药材未记录"} · {selectedTask?.baseName || "基地未记录"}
              </p>
            </div>
            <div className={styles.currentMetricTag}>
              当前指标：<strong>{METRICS[metric].label}</strong>
            </div>
          </div>
          <dl className={styles.overviewGrid}>
            <div><dt><span><Layers size={16} /></span>批次数</dt><dd>{taskOverview.batchCount}</dd></div>
            <div><dt><span><BarChart3 size={16} /></span>生长记录数</dt><dd>{taskOverview.recordCount}</dd></div>
            <div className={styles.activeMetricCount}>
              <dt><span><Activity size={16} /></span>当前指标有效观测点</dt><dd>{validPointCount}</dd>
            </div>
          </dl>
        </section>

        <section className={styles.trendDisclosure}>
          <header className={styles.trendEntryHeader}>
            <div>
              <BarChart3 size={19} />
              <span><strong>生长趋势分析</strong><small>按当前筛选任务展示多个采集批次形成的生长记录趋势。</small></span>
            </div>
            <Button onClick={() => setTrendOpen((value) => !value)}>
              {trendOpen ? "收起趋势图" : "展开趋势图"}
            </Button>
            <dl className={styles.trendFacts}>
              <div><dt>当前任务</dt><dd>{selectedTask?.taskName || "未选择"}</dd></div>
              <div><dt>当前指标</dt><dd>{METRICS[metric].label}</dd></div>
              <div><dt>观测点数量</dt><dd>{chartData.length}</dd></div>
            </dl>
          </header>
        {trendOpen ? <section className={styles.trendPanel}>
          <header
            className={`${styles.panelHeader} ${isCollectorOnly ? styles.collectorPanelHeader : ""}`}
          >
            <div className={styles.chartHeading}>
              <span className={styles.eyebrow}>{selectedTask?.taskCode || "COLLECTION TASK"}</span>
              <h2>{METRICS[metric].label}趋势</h2>
              <p>每个观测点对应一次采集批次下的生长记录。</p>
              <span className={styles.chartContext}>
                {[selectedTask?.taskName, selectedTask?.speciesName, selectedTask?.baseName]
                  .filter(Boolean)
                  .join(" · ") || "选择任务后查看生长趋势"}
              </span>
            </div>
            {showReviewWorkspace ? (
              <div className={styles.chartAside}>
                <div className={styles.legend} aria-label="审核状态图例">
                  {Object.entries(STATUS_META).map(([key, meta]) => (
                    <span key={key}>
                      <i style={{ background: meta.color }} />
                      {meta.label}
                    </span>
                  ))}
                </div>
                <p className={styles.reviewHint}>
                  {submittedPointCount
                    ? "橙色观测点表示待审核记录，点击观测点可查看详情并审核。"
                    : "当前筛选条件下暂无待审核观测点。"}
                </p>
              </div>
            ) : null}
          </header>
          {autoMetricNotice ? (
            <div className={styles.autoMetricNotice}>{autoMetricNotice}</div>
          ) : null}
          <Spin spinning={loadingChart}>
            <GrowthTrendChart
              data={chartData}
              metric={metric}
              emptyDescription={emptyDescription}
              emptyHint={emptyHint}
              showAuditContext={showReviewWorkspace}
              selectedId={selectedRecord?.id}
              onSelect={openDetail}
            />
          </Spin>
        </section> : null}
        </section>

        <section className={styles.summarySection}>
          <div className={styles.sectionTitle}>
            <div>
              <span>POINT SUMMARY</span>
              <h2>观测点摘要</h2>
              <p className={styles.summaryDescription}>
                每个观测点对应一次采集批次下的生长记录，可点击查看详情与审核记录。
              </p>
            </div>
            <strong>{chartData.length} 个观测点</strong>
          </div>
          {chartData.length ? (
            <div className={chartData.length === 1 ? styles.singlePointLayout : styles.pointGrid}>
              {chartData.map((point) => (
                <article
                  className={`${styles.pointCard} ${selectedRecord?.id === point.recordId ? styles.selectedCard : ""}`}
                  key={point.recordId}
                >
                  <header className={styles.pointCardHeader}>
                    <h3>{point.batchName || `批次 #${point.batchId}`}</h3>
                    <StatusBadge status={point.auditStatus} />
                  </header>
                  <div className={styles.pointMetric}>
                    <span>{METRICS[metric].label}</span>
                    <strong>
                      <b>{Number(point.value).toLocaleString()}</b>
                      {METRICS[metric].unit ? <small>{METRICS[metric].unit}</small> : null}
                    </strong>
                  </div>
                  <dl className={styles.pointDetails}>
                    <div>
                      <dt>采集时间</dt>
                      <dd>{formatTime(point.collectTime)}</dd>
                    </div>
                    <div>
                      <dt>药材名称</dt>
                      <dd>{selectedTask?.speciesName || "-"}</dd>
                    </div>
                    <div>
                      <dt>采集员</dt>
                      <dd>{point.collectorName || "-"}</dd>
                    </div>
                    <div>
                      <dt>生长阶段</dt>
                      <dd>{point.growthStage || "-"}</dd>
                    </div>
                  </dl>
                  <Button
                    className={styles.pointDetailButton}
                    icon={<Eye size={15} />}
                    onClick={() => void openDetail(point.recordId)}
                  >
                    查看详情
                  </Button>
                </article>
              ))}
              {chartData.length === 1 ? (
                <aside className={styles.trendGuideCard}>
                  <span>趋势形成提示</span>
                  <h3>当前仅有 1 个观测点</h3>
                  <p>继续创建新的采集批次并填写生长记录后，系统会自动将多个批次的观测数据连接成趋势线。</p>
                  <dl>
                    <div><dt>当前任务</dt><dd>{selectedTask?.taskName || "-"}</dd></div>
                    <div><dt>当前指标</dt><dd>{METRICS[metric].label}</dd></div>
                    <div><dt>最新采集时间</dt><dd>{formatTime(chartData[0].collectTime)}</dd></div>
                  </dl>
                </aside>
              ) : null}
            </div>
          ) : (
            <div className={styles.pointEmptyState}>
              <div className={styles.pointEmptyCopy}>
                <span>数据诊断</span>
                <h3>暂无观测点</h3>
                <p>当前筛选条件下没有可展示的生长记录。可尝试切换任务、切换观测指标或查看全部状态。</p>
              </div>
              <dl className={styles.diagnosticGrid}>
                <div><dt>当前任务</dt><dd>{selectedTask?.taskName || "未选择"}</dd></div>
                {showReviewWorkspace ? (
                  <div><dt>审核状态</dt><dd>{STATUS_OPTIONS.find((item) => item.value === status)?.label}</dd></div>
                ) : null}
                <div><dt>当前指标</dt><dd>{METRICS[metric].label}</dd></div>
                <div><dt>匹配记录数</dt><dd>{chartResult.matchedRecordCount}</dd></div>
                <div><dt>有效观测点数</dt><dd>{validPointCount}</dd></div>
                <div>
                  <dt>空状态原因</dt>
                  <dd>{chartResult.reason ? EMPTY_REASON_LABELS[chartResult.reason] : "暂无观测点"}</dd>
                </div>
              </dl>
              <div className={styles.pointEmptyActions}>
                <Select
                  aria-label="切换指标"
                  className={styles.metricDiagnosticSelect}
                  value={metric}
                  options={Object.entries(METRICS).map(([value, meta]) => ({
                    value,
                    label: meta.label,
                  }))}
                  onChange={(value) => setMetric(value)}
                />
                <Button onClick={resetFilters}>重置筛选</Button>
                {showReviewWorkspace ? (
                  <Button onClick={() => void showAllStatuses()}>查看全部状态</Button>
                ) : null}
              </div>
            </div>
          )}
        </section>
          </div>

      <aside className={styles.detailPanel} aria-label="生长记录详情">
        <header className={styles.detailPanelHeader}>
          <div>
            <span>{selectedRecord ? `生长记录 #${selectedRecord.id}` : "记录详情"}</span>
          </div>
          <div>
            {selectedRecord ? (
              <button
                type="button"
                aria-label="关闭详情"
                onClick={() => {
                  setSelectedRecord(null);
                  setBatchImages([]);
                  setAuditHistory([]);
                  setTraceEvents([]);
                  setTraceQrCode(undefined);
                }}
              ><X size={18} /></button>
            ) : null}
          </div>
        </header>
        <nav className={styles.detailTabs} aria-label="详情标签页">
          {([
            ["base", "基础信息"],
            ["metrics", "指标数据"],
            ["images", "现场图片"],
            ["trace", "地图与溯源"],
          ] as const).map(([value, label]) => (
            <button key={value} type="button" className={detailTab === value ? styles.activeDetailTab : undefined} onClick={() => setDetailTab(value)}>{label}</button>
          ))}
        </nav>
        <Spin spinning={loadingDetail}>
          {selectedRecord && !loadingDetail ? (
            <div className={styles.drawerContent}>
              <section className={`${styles.recordSummary} ${styles.archiveSummary}`}>
                <div className={styles.summaryIdentity}>
                  <span>当前选中记录</span>
                  <h2>{selectedRecord.speciesName || "未命名药材"}</h2>
                  <p>{selectedRecord.batchName || `批次 #${selectedRecord.batchId || "-"}`}</p>
                  <div className={`${styles.summaryMeta} ${styles.archiveMeta}`}>
                    <span>采集时间：{formatTime(selectedRecord.collectedAt)}</span>
                    <span>采集员：{selectedRecord.collectorName || "-"}</span>
                  </div>
                </div>
                <div className={styles.summaryTags}>
                  {showReviewWorkspace ? (
                    <StatusBadge status={selectedRecord.reviewStatus} />
                  ) : null}
                  <span className={styles.growthStageTag}>
                    {selectedRecord.growthStage || "阶段未记录"}
                  </span>
                </div>
              </section>

              <section className={`${styles.detailSection} ${detailTab !== "base" ? styles.hiddenTab : ""}`}>
                <header className={styles.detailSectionHeader}>
                  <h3>基础信息</h3>
                  <p>记录归属与本次现场采集信息</p>
                </header>
                <dl className={styles.basicInfoList}>
                  <div className={styles.basicInfoWide}>
                    <dt>所属任务</dt>
                    <dd>{selectedRecord.taskName || "-"}</dd>
                  </div>
                  <div className={styles.basicInfoWide}>
                    <dt>所属批次</dt>
                    <dd>{selectedRecord.batchName || "-"}</dd>
                  </div>
                  <div>
                    <dt>基地名称</dt>
                    <dd>{selectedRecord.baseName || "-"}</dd>
                  </div>
                  <div>
                    <dt>采集地点</dt>
                    <dd>{selectedRecord.collectPlace || "-"}</dd>
                  </div>
                  <div>
                    <dt>采集时间</dt>
                    <dd>{formatTime(selectedRecord.collectedAt)}</dd>
                  </div>
                  <div>
                    <dt>采集员</dt>
                    <dd>{selectedRecord.collectorName || "-"}</dd>
                  </div>
                  <div>
                    <dt>生长阶段</dt>
                    <dd>{selectedRecord.growthStage || "-"}</dd>
                  </div>
                  <div>
                    <dt>审核状态</dt>
                    <dd>{STATUS_META[selectedRecord.reviewStatus || "draft"]?.label || "-"}</dd>
                  </div>
                </dl>
              </section>
              <section className={`${styles.detailSection} ${detailTab !== "metrics" ? styles.hiddenTab : ""}`}>
                <header className={styles.detailSectionHeader}>
                  <h3>环境指标</h3>
                  <p>本次采集现场的气候与土壤观测</p>
                </header>
                <dl className={`${styles.detailCardGrid} ${styles.metricCardGrid}`}>
                  <div className={styles.metricTile}>
                    <dt>温度</dt>
                    <dd><MetricReading value={selectedRecord.temperature} unit="℃" /></dd>
                  </div>
                  <div className={styles.metricTile}>
                    <dt>湿度</dt>
                    <dd><MetricReading value={selectedRecord.humidity} unit="%" /></dd>
                  </div>
                  <div className={styles.metricTile}>
                    <dt>光照</dt>
                    <dd><MetricReading value={selectedRecord.light} unit="lx" /></dd>
                  </div>
                  <div className={styles.metricTile}>
                    <dt>土壤湿度</dt>
                    <dd><MetricReading value={selectedRecord.soilMoisture} unit="%" /></dd>
                  </div>
                  <div className={styles.metricTile}>
                    <dt>土壤 pH</dt>
                    <dd><MetricReading value={selectedRecord.soilPh} /></dd>
                  </div>
                  <div className={`${styles.metricTile} ${styles.textMetricTile}`}>
                    <dt>土壤类型</dt>
                    <dd>{selectedRecord.soilType || "-"}</dd>
                  </div>
                </dl>
              </section>
              <section className={`${styles.detailSection} ${detailTab !== "metrics" ? styles.hiddenTab : ""}`}>
                <header className={styles.detailSectionHeader}>
                  <h3>生长指标</h3>
                  <p>植株形态、生长阶段与现场评价</p>
                </header>
                <dl className={`${styles.detailCardGrid} ${styles.growthMetricGrid}`}>
                  <div className={`${styles.metricTile} ${styles.primaryMetricTile}`}>
                    <dt>株高</dt>
                    <dd><MetricReading value={selectedRecord.plantHeight} unit="cm" /></dd>
                  </div>
                  <div className={`${styles.metricTile} ${styles.primaryMetricTile}`}>
                    <dt>茎粗</dt>
                    <dd><MetricReading value={selectedRecord.stemDiameter} unit="mm" /></dd>
                  </div>
                  <div className={styles.textMetricTile}>
                    <dt>叶色</dt>
                    <dd>{selectedRecord.leafColor || "-"}</dd>
                  </div>
                  <div className={styles.textMetricTile}>
                    <dt>开花情况</dt>
                    <dd>{selectedRecord.floweringStatus || "-"}</dd>
                  </div>
                  <div className={`${styles.textMetricTile} ${styles.detailFieldWide}`}>
                    <dt>生长评价</dt>
                    <dd>{selectedRecord.growthEvaluation || selectedRecord.remark || "-"}</dd>
                  </div>
                </dl>
              </section>
              <section className={`${styles.detailSection} ${detailTab !== "images" ? styles.hiddenTab : ""}`}>
                <header className={styles.detailSectionHeader}>
                  <h3>现场图片证据</h3>
                  <p>以下图片来自当前采集批次，用于佐证本次生长记录。</p>
                </header>
                {batchImages.length ? (
                  <div className={styles.imageEvidenceGrid}>
                    {batchImages.map((image) => (
                      <figure className={styles.imageEvidenceItem} key={image.imageId || image.id}>
                        <div className={styles.imageEvidenceThumb}>
                          <SecureImageThumb
                            alt={image.imageName || image.imageCode || `批次图片 #${image.id}`}
                            src={image.imageUrl}
                          />
                        </div>
                        {image.imageType ? <figcaption>{IMAGE_TYPE_LABELS[image.imageType] || "其他"}</figcaption> : null}
                      </figure>
                    ))}
                  </div>
                ) : (
                  <div className={styles.imageEmptyState}>
                    <strong>暂无现场图片</strong>
                    <p>当前采集批次尚未上传可用于佐证的现场图片。</p>
                  </div>
                )}
              </section>
              <section className={`${styles.detailSection} ${detailTab !== "trace" ? styles.hiddenTab : ""}`}>
                <header className={styles.detailSectionHeader}>
                  <h3>地图定位</h3>
                  <p>本次采集记录的基地与地理位置信息</p>
                </header>
                <div className={styles.mapPreview}>
                  <MapPin size={24} />
                  <div>
                    <strong>{selectedRecord.collectPlace || selectedRecord.baseName || "暂无采集地点"}</strong>
                    <span>基地：{selectedRecord.baseName || "-"}</span>
                    <span>经度：{selectedRecord.longitude ?? "-"} · 纬度：{selectedRecord.latitude ?? "-"}</span>
                    {selectedRecord.longitude == null || selectedRecord.latitude == null ? (
                      <p>暂无精确经纬度，已展示采集地点文本信息。</p>
                    ) : null}
                  </div>
                </div>
              </section>
              <section className={`${styles.detailSection} ${detailTab !== "trace" ? styles.hiddenTab : ""}`}>
                <header className={styles.detailSectionHeader}>
                  <h3>溯源二维码</h3>
                  <p>生成二维码后，可通过扫码查看该生长记录的公开溯源档案。</p>
                </header>
                <div className={styles.traceQrCard}>
                  <div className={styles.traceQrStatusRow}>
                    <span className={traceQrCode?.publicVisible === 1 ? styles.tracePublic : styles.tracePrivate}>
                      {traceQrCode?.publicVisible === 1 ? "已公开" : "未公开"}
                    </span>
                    <small>{traceQrCode?.traceCode ? "溯源标识已生成" : "尚未生成溯源码"}</small>
                  </div>
                  {traceQrCode?.qrCodeUrl ? (
                    <div className={styles.traceQrImage}>
                      <SecureImageThumb src={traceQrCode.qrCodeUrl} alt={`生长记录 ${selectedRecord.id} 溯源二维码`} />
                      <span>扫码查看公开溯源档案</span>
                    </div>
                  ) : (
                    <div className={styles.traceQrEmpty}>
                      <QrCode size={30} />
                      <strong>{traceQrCode?.traceCode ? "尚未生成二维码" : "当前记录尚无公开溯源标识"}</strong>
                      <p>{traceQrCode?.traceCode ? "生成后即可扫码访问公开档案。" : "请先生成溯源码或直接生成二维码。"}</p>
                    </div>
                  )}
                  {traceQrCode?.traceCode ? (
                    <dl className={styles.traceQrMeta}>
                      <div><dt>溯源码</dt><dd>{traceQrCode.traceCode}</dd></div>
                      <div title={getPublicTracePageUrl(traceQrCode.traceCode)}>
                        <dt>公开链接</dt><dd>{getPublicTracePageUrl(traceQrCode.traceCode)}</dd>
                      </div>
                    </dl>
                  ) : null}
                  <div className={styles.traceQrActions}>
                    {showReviewWorkspace && !traceQrCode?.traceCode ? (
                      <Button loading={traceOperating === "code"} onClick={() => void performTraceAction("code", generateGrowthTraceCode, "溯源码生成成功")}>生成溯源码</Button>
                    ) : null}
                    {showReviewWorkspace && !traceQrCode?.qrCodeUrl ? (
                      <Button type="primary" loading={traceOperating === "qr"} icon={<QrCode size={15} />} onClick={() => void performTraceAction("qr", generateGrowthTraceQrCode, "二维码生成成功")}>生成二维码</Button>
                    ) : null}
                    {traceQrCode?.traceCode ? <Button icon={<Copy size={15} />} onClick={() => void copyTraceLink()}>复制链接</Button> : null}
                    {traceQrCode?.traceCode ? <Button icon={<ExternalLink size={15} />} onClick={openPublicTracePage}>打开公开页</Button> : null}
                    {traceQrCode?.qrCodeUrl ? <Button icon={<Download size={15} />} onClick={() => void downloadTraceQrCode()}>下载二维码</Button> : null}
                    {showReviewWorkspace && traceQrCode?.traceCode && traceQrCode.publicVisible !== 1 ? (
                      <Button type="primary" loading={traceOperating === "enable"} onClick={() => void performTraceAction("enable", enableGrowthPublicTrace, "公开溯源已开启")}>开启公开溯源</Button>
                    ) : null}
                    {showReviewWorkspace && traceQrCode?.publicVisible === 1 ? (
                      <Button danger loading={traceOperating === "disable"} onClick={confirmDisablePublicTrace}>关闭公开溯源</Button>
                    ) : null}
                  </div>
                </div>
              </section>
              {showReviewWorkspace ? (
                <section className={`${styles.detailSection} ${detailTab !== "trace" ? styles.hiddenTab : ""}`}>
                  <header className={styles.detailSectionHeader}>
                    <h3>审核历史</h3>
                    <p>记录每次提交与审核处理结果</p>
                  </header>
                  {auditHistory.length ? (
                    <ol className={`${styles.timelineList} ${styles.auditTimeline}`}>
                      {auditHistory.map((item, index) => (
                        <li
                          key={`${item.actionType}-${item.operateTime || index}`}
                          style={{
                            "--timeline-color": STATUS_META[item.afterStatus || ""]?.color || "#0f5132",
                          } as React.CSSProperties}
                        >
                          <i />
                          <div className={styles.timelineEntry}>
                            <strong>{formatActionLabel(item.actionType)}</strong>
                            <span className={styles.statusTransition}>
                              {formatStatusTransition(item.beforeStatus, item.afterStatus)}
                            </span>
                            <p>{item.comment || "无审核意见"}</p>
                            <small>{item.operatorName || "系统"} · {formatTime(item.operateTime)}</small>
                          </div>
                        </li>
                      ))}
                    </ol>
                  ) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无审核历史" />
                  )}
                </section>
              ) : null}
              <section className={`${styles.detailSection} ${detailTab !== "trace" ? styles.hiddenTab : ""}`}>
                <header className={styles.detailSectionHeader}>
                  <h3>溯源时间线</h3>
                  <p>从采集记录创建到当前状态的完整链路</p>
                </header>
                {traceEvents.length ? (
                  <ol className={`${styles.timelineList} ${styles.traceTimeline}`}>
                    {traceEvents.map((event, index) => (
                      <li
                        key={`${event.eventType}-${event.eventTime || index}`}
                        style={{
                          "--timeline-color": STATUS_META[event.afterStatus || ""]?.color || "#0f5132",
                        } as React.CSSProperties}
                      >
                        <i />
                        <div className={styles.timelineEntry}>
                          <strong>{formatTraceEventTitle(event)}</strong>
                          <span className={styles.statusTransition}>
                            {formatStatusTransition(event.beforeStatus, event.afterStatus)}
                          </span>
                          <p>{event.eventContent || event.comment || "-"}</p>
                          <small>{event.operatorName || "系统"} · {formatTime(event.eventTime)}</small>
                        </div>
                      </li>
                    ))}
                  </ol>
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无溯源事件" />
                )}
              </section>
              {canAuditSelected || canSubmitSelected ? (
                <footer className={styles.drawerActions}>
                  {canSubmitSelected ? (
                    <Button className={styles.submitButton} onClick={() => void submitForReview()}>
                      重新提交审核
                    </Button>
                  ) : null}
                  {canAuditSelected ? (
                    <>
                      <Button
                        className={styles.approveButton}
                        icon={<Check size={16} />}
                        onClick={() => {
                          auditForm.resetFields();
                          setAuditAction("approve");
                        }}
                      >
                        审核通过
                      </Button>
                      <Button
                        danger
                        className={styles.rejectButton}
                        icon={<X size={16} />}
                        onClick={() => {
                          auditForm.resetFields();
                          setAuditAction("reject");
                        }}
                      >
                        审核驳回
                      </Button>
                    </>
                  ) : null}
                </footer>
              ) : null}
            </div>
          ) : !loadingDetail ? (
            <div className={styles.detailEmpty}>
              <Eye size={28} />
              <strong>请选择生长记录</strong>
              <p>点击左侧表格中的“查看”，这里会显示完整采集信息、现场图片和溯源记录。</p>
            </div>
          ) : null}
        </Spin>
      </aside>
        </div>
      </main>

      <Modal
        className={styles.taskCreateModal}
        title="创建采集任务"
        open={taskCreateOpen}
        okText="创建任务"
        cancelText="取消"
        confirmLoading={taskCreating}
        width={720}
        destroyOnHidden
        onCancel={() => {
          setTaskCreateOpen(false);
          taskForm.resetFields();
        }}
        onOk={() => taskForm.submit()}
      >
        <Spin spinning={taskOptionsLoading}>
          <Form form={taskForm} layout="vertical" className={styles.taskCreateForm} onFinish={submitTaskCreate}>
            <div className={styles.taskFormGrid}>
              <Form.Item name="taskCode" label="任务编号" rules={[{ required: true, message: "请输入任务编号" }]}>
                <Input placeholder="请输入唯一任务编号" />
              </Form.Item>
              <Form.Item name="taskName" label="任务名称" rules={[{ required: true, message: "请输入任务名称" }]}>
                <Input placeholder="例如：岷县党参夏季连续观测" />
              </Form.Item>
              <Form.Item name="speciesId" label="药材" rules={[{ required: true, message: "请选择药材" }]}>
                <Select showSearch optionFilterProp="label" placeholder="请选择药材" options={taskHerbs.map((item) => ({ label: item.herbName, value: item.id }))} />
              </Form.Item>
              <Form.Item name="collectorId" label="指定采集员" rules={[{ required: true, message: "请选择采集员" }]}>
                <Select
                  showSearch
                  optionFilterProp="label"
                  placeholder="请选择负责本任务的采集员"
                  options={taskCollectors.map((item) => ({ label: item.name, value: item.id }))}
                  notFoundContent={taskOptionsLoading ? "加载中" : "当前范围内暂无可分配采集员"}
                />
              </Form.Item>
              <Form.Item name="baseId" label="采集基地">
                <Select allowClear showSearch optionFilterProp="label" placeholder="请选择采集基地" options={taskBases.map((item) => ({ label: item.baseName, value: item.id }))} />
              </Form.Item>
              <Form.Item name="collectPlace" label="采集地点">
                <Input placeholder="请输入详细采集地点" />
              </Form.Item>
              <Form.Item name="plannedStartTime" label="计划开始时间">
                <Input type="datetime-local" />
              </Form.Item>
              <Form.Item name="plannedEndTime" label="计划结束时间">
                <Input type="datetime-local" />
              </Form.Item>
              <Form.Item className={styles.taskFormWide} name="description" label="任务说明">
                <Input.TextArea rows={3} placeholder="填写本次采集任务的目标与要求" />
              </Form.Item>
              <Form.Item className={styles.taskFormWide} name="remark" label="备注">
                <Input.TextArea rows={2} placeholder="可选" />
              </Form.Item>
            </div>
          </Form>
        </Spin>
      </Modal>

      <Modal
        title={auditAction === "approve" ? "审核通过" : "审核驳回"}
        open={Boolean(auditAction)}
        okText={auditAction === "approve" ? "确认通过" : "确认驳回"}
        okButtonProps={{ danger: auditAction === "reject" }}
        confirmLoading={auditSubmitting}
        destroyOnHidden
        onCancel={() => {
          setAuditAction(null);
          auditForm.resetFields();
        }}
        onOk={() => auditForm.submit()}
      >
        <Form form={auditForm} layout="vertical" onFinish={submitAudit}>
          <Form.Item
            name="comment"
            label={auditAction === "reject" ? "驳回原因" : "审核意见"}
            rules={
              auditAction === "reject"
                ? [{ required: true, whitespace: true, message: "请填写驳回原因" }]
                : undefined
            }
          >
            <Input.TextArea
              rows={4}
              maxLength={500}
              showCount
              placeholder={auditAction === "reject" ? "请说明需要补充或修改的内容" : "审核意见可选"}
            />
          </Form.Item>
        </Form>
      </Modal>
    </SiteLayout>
  );
}
