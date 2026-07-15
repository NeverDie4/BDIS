"use client";

import { LineChart } from "echarts/charts";
import {
  DataZoomComponent,
  GraphicComponent,
  GridComponent,
  TooltipComponent,
} from "echarts/components";
import { init, use as registerECharts, type EChartsCoreOption } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { useEffect, useMemo, useRef, useState } from "react";
import type { PublicDigitalLifeMetricsApi, PublicDigitalLifeStageApi } from "@/lib/digital-life";
import styles from "./page.module.css";

registerECharts([
  CanvasRenderer,
  LineChart,
  GraphicComponent,
  GridComponent,
  TooltipComponent,
  DataZoomComponent,
]);

type MetricKey = keyof Pick<
  PublicDigitalLifeMetricsApi,
  "plantHeight" | "stemDiameter" | "temperature" | "humidity" | "soilMoisture" | "soilPh" | "light"
>;

const METRICS: Record<MetricKey, { label: string; unit: string }> = {
  plantHeight: { label: "株高", unit: "cm" },
  stemDiameter: { label: "茎粗", unit: "mm" },
  temperature: { label: "温度", unit: "℃" },
  humidity: { label: "湿度", unit: "%" },
  soilMoisture: { label: "土壤湿度", unit: "%" },
  soilPh: { label: "土壤 pH", unit: "" },
  light: { label: "光照", unit: "lx" },
};

const METRIC_ORDER = Object.keys(METRICS) as MetricKey[];

type Props = {
  stages: PublicDigitalLifeStageApi[];
  currentStageIndex: number;
  onStageSelect: (index: number) => void;
};

type ChartDatum = {
  value: number | null;
  stageIndex: number;
  stage: PublicDigitalLifeStageApi;
};

function escapeHtml(value?: string) {
  return (value || "-")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function metricValue(stage: PublicDigitalLifeStageApi, metric: MetricKey) {
  const value = stage.metrics?.[metric];
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function defaultMetric(stages: PublicDigitalLifeStageApi[]) {
  return (
    METRIC_ORDER.find((metric) => stages.some((stage) => metricValue(stage, metric) != null)) ||
    "plantHeight"
  );
}

function formatDate(value?: string, full = false) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return full
    ? date.toLocaleString("zh-CN", { hour12: false })
    : date.toLocaleDateString("zh-CN", { month: "2-digit", day: "2-digit" });
}

function auditStatusLabel(status?: string) {
  return (
    (
      { approved: "已通过", submitted: "待审核", rejected: "已驳回", draft: "草稿" } as Record<
        string,
        string
      >
    )[status || ""] ||
    status ||
    "状态未记录"
  );
}

export default function DigitalLifeTrendChart({ stages, currentStageIndex, onStageSelect }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<ReturnType<typeof init> | null>(null);
  const [metric, setMetric] = useState<MetricKey>(() => defaultMetric(stages));
  const meta = METRICS[metric];
  const chartData = useMemo<ChartDatum[]>(
    () =>
      stages.map((stage, stageIndex) => {
        const value = metricValue(stage, metric);
        return { value: value == null ? null : value, stageIndex, stage };
      }),
    [metric, stages],
  );

  useEffect(() => {
    if (!containerRef.current) return;
    const chart = init(containerRef.current);
    chartRef.current = chart;
    const resizeObserver = new ResizeObserver(() => chart.resize());
    resizeObserver.observe(containerRef.current);
    return () => {
      resizeObserver.disconnect();
      chart.dispose();
      chartRef.current = null;
    };
  }, []);

  useEffect(() => {
    const chart = chartRef.current;
    if (!chart) return;
    const hasValues = chartData.some(({ value }) => value != null);
    const showZoom = stages.length > 8;
    const option: EChartsCoreOption = {
      animationDuration: 420,
      grid: { left: 50, right: 24, top: 28, bottom: showZoom ? 62 : 38, containLabel: true },
      tooltip: {
        trigger: "item",
        confine: true,
        appendToBody: true,
        backgroundColor: "#fffaf2",
        borderColor: "#eadfcd",
        borderWidth: 1,
        padding: 0,
        extraCssText:
          "border-radius:10px;box-shadow:0 10px 26px rgba(15,61,46,.14);overflow:hidden;",
        formatter: (params: unknown) => {
          const datum = (params as { data?: ChartDatum }).data;
          if (!datum) return "";
          const valueText =
            datum.value == null
              ? "本阶段暂无该指标"
              : `${datum.value.toLocaleString("zh-CN")} ${meta.unit}`.trim();
          return `<div style="min-width:220px;padding:13px 14px;background:#fffaf2;color:#315742;font-family:'Microsoft YaHei',sans-serif;">
            <strong style="display:block;color:#0f3d2e;font-size:15px;">${escapeHtml(datum.stage.growthStage || "阶段未填写")}</strong>
            <div style="margin:9px 0;padding:8px 0;border-top:1px solid #eadfcd;border-bottom:1px solid #eadfcd;color:#0f5132;font-size:17px;font-weight:700;">${escapeHtml(meta.label)}：${escapeHtml(valueText)}</div>
            <div style="display:grid;gap:5px;color:#6f7f72;font-size:12px;line-height:1.5;">
              <span>批次：${escapeHtml(datum.stage.batchName || datum.stage.batchCode || "批次未命名")}</span>
              <span>时间：${escapeHtml(formatDate(datum.stage.collectedAt, true))}</span>
              <span>采集员：${escapeHtml(datum.stage.collectorName || "采集员未知")}</span>
              <span>审核：${escapeHtml(auditStatusLabel(datum.stage.auditStatus))}</span>
            </div>
          </div>`;
        },
      },
      xAxis: {
        type: "category",
        boundaryGap: false,
        data: stages.map((stage, index) =>
          formatDate(stage.collectedAt) === "-"
            ? `阶段 ${index + 1}`
            : formatDate(stage.collectedAt),
        ),
        axisLine: { lineStyle: { color: "#aeb8af" } },
        axisTick: { show: false },
        axisLabel: { color: "#6f7f72", fontSize: 11, rotate: stages.length > 7 ? 25 : 0 },
      },
      yAxis: {
        type: "value",
        name: `${meta.label}${meta.unit ? ` (${meta.unit})` : ""}`,
        nameTextStyle: { color: "#6f7f72", fontSize: 11 },
        axisLabel: { color: "#6f7f72", fontSize: 11 },
        splitLine: { lineStyle: { color: "rgba(69,91,72,.14)", type: "dashed" } },
      },
      dataZoom: showZoom
        ? [
            { type: "inside", startValue: 0, endValue: 7 },
            {
              type: "slider",
              startValue: 0,
              endValue: 7,
              height: 14,
              bottom: 5,
              borderColor: "#d8dfd5",
              fillerColor: "rgba(15,81,50,.12)",
            },
          ]
        : [],
      series: [
        {
          type: "line",
          smooth: true,
          connectNulls: false,
          symbol: "circle",
          lineStyle: { color: "#315f3d", width: 3 },
          itemStyle: { color: "#7da283", borderColor: "#fffaf2", borderWidth: 2 },
          areaStyle: { color: "rgba(77,123,84,.07)" },
          data: chartData.map((datum) => ({
            ...datum,
            symbolSize: datum.stageIndex === currentStageIndex ? 14 : 8,
            itemStyle:
              datum.stageIndex === currentStageIndex
                ? {
                    color: "#0f5132",
                    borderColor: "#fffaf2",
                    borderWidth: 4,
                    shadowBlur: 10,
                    shadowColor: "rgba(15,81,50,.45)",
                  }
                : undefined,
          })),
        },
      ],
      graphic: hasValues
        ? []
        : {
            type: "text",
            left: "center",
            top: "middle",
            silent: true,
            style: { text: "当前指标暂无有效数据", fill: "#6f7f72", font: "13px sans-serif" },
          },
    };
    chart.setOption(option, { notMerge: false });
  }, [chartData, currentStageIndex, meta, stages]);

  useEffect(() => {
    const chart = chartRef.current;
    if (!chart || !stages.length) return;
    chart.dispatchAction({ type: "downplay", seriesIndex: 0 });
    chart.dispatchAction({ type: "highlight", seriesIndex: 0, dataIndex: currentStageIndex });
    chart.dispatchAction({ type: "showTip", seriesIndex: 0, dataIndex: currentStageIndex });
    if (stages.length > 8) {
      const startValue = Math.max(0, Math.min(currentStageIndex - 3, stages.length - 8));
      chart.dispatchAction({
        type: "dataZoom",
        startValue,
        endValue: Math.min(stages.length - 1, startValue + 7),
      });
    }
  }, [currentStageIndex, metric, stages.length]);

  useEffect(() => {
    const chart = chartRef.current;
    if (!chart) return;
    const handleClick = (params: unknown) => {
      const stageIndex = (params as { data?: ChartDatum }).data?.stageIndex;
      if (typeof stageIndex === "number") onStageSelect(stageIndex);
    };
    chart.on("click", handleClick);
    return () => {
      chart.off("click", handleClick);
    };
  }, [onStageSelect]);

  return (
    <div className={styles.trendChartWrap}>
      <label className={styles.metricSelect}>
        <span>观测指标</span>
        <select value={metric} onChange={(event) => setMetric(event.target.value as MetricKey)}>
          {METRIC_ORDER.map((key) => (
            <option key={key} value={key}>
              {METRICS[key].label}
            </option>
          ))}
        </select>
      </label>
      <div
        ref={containerRef}
        className={styles.trendChart}
        aria-label={`${meta.label}生长趋势图`}
      />
    </div>
  );
}
