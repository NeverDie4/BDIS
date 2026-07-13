"use client";

import axios from "axios";
import { App, Button, Image } from "antd";
import { Copy, Download, FileSearch, Printer, QrCode } from "lucide-react";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import {
  getPublicGrowthTrace,
  resolveGrowthResourceUrl,
  type GrowthPublicTraceArchiveApi,
  type GrowthPublicTraceEventApi,
} from "@/lib/growth-records";
import type { ApiResult } from "@/types/api";
import styles from "./page.module.css";

const STATUS_META: Record<string, { label: string; className: string }> = {
  draft: { label: "草稿", className: styles.statusDraft },
  submitted: { label: "待审核", className: styles.statusSubmitted },
  approved: { label: "已通过", className: styles.statusApproved },
  rejected: { label: "已驳回", className: styles.statusRejected },
  archived: { label: "已归档", className: styles.statusDraft },
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

const TRACE_EVENT_LABELS: Record<string, string> = {
  create: "创建生长记录",
  update: "修改生长记录",
  submit: "提交审核",
  approve: "审核通过",
  reject: "审核驳回",
  trace_code_generated: "生成溯源码",
  trace_qrcode_generated: "生成溯源二维码",
  public_trace_enabled: "开启公开溯源",
  public_trace_disabled: "关闭公开溯源",
};

type ErrorKind = "missing" | "private" | "network";

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "-";
}

function statusLabel(status?: string) {
  return STATUS_META[status || ""]?.label || "状态未记录";
}

function statusTransition(before?: string, after?: string) {
  const beforeLabel = before && before !== "-" ? statusLabel(before) : undefined;
  const afterLabel = after && after !== "-" ? statusLabel(after) : undefined;
  if (!beforeLabel && afterLabel) return `初始状态：${afterLabel}`;
  if (beforeLabel && afterLabel) return `${beforeLabel} → ${afterLabel}`;
  return afterLabel || beforeLabel || "状态未变化";
}

function eventTitle(event: GrowthPublicTraceEventApi) {
  return event.eventTitle || TRACE_EVENT_LABELS[event.eventType] || "溯源事件";
}

function Metric({ label, value, unit }: { label: string; value?: number | string; unit?: string }) {
  return (
    <div className={styles.metricCard}>
      <span>{label}</span>
      <strong>
        {value ?? "-"}
        {value != null && unit ? <small>{unit}</small> : null}
      </strong>
    </div>
  );
}

export default function PublicGrowthTracePage() {
  const params = useParams<{ traceCode: string }>();
  const traceCode = Array.isArray(params.traceCode) ? params.traceCode[0] : params.traceCode;
  const { message } = App.useApp();
  const [archive, setArchive] = useState<GrowthPublicTraceArchiveApi>();
  const [loading, setLoading] = useState(true);
  const [errorKind, setErrorKind] = useState<ErrorKind>();

  useEffect(() => {
    let active = true;
    if (!traceCode) {
      setErrorKind("missing");
      setLoading(false);
      return;
    }
    setLoading(true);
    getPublicGrowthTrace(traceCode)
      .then((data) => {
        if (!active) return;
        setArchive(data);
        setErrorKind(undefined);
      })
      .catch((error: unknown) => {
        if (!active) return;
        if (axios.isAxiosError<ApiResult<unknown>>(error)) {
          const status = error.response?.status;
          const apiMessage = error.response?.data?.message || "";
          if (status === 403 || apiMessage.includes("暂未公开")) setErrorKind("private");
          else if (status === 404 || apiMessage.includes("不存在")) setErrorKind("missing");
          else setErrorKind("network");
        } else {
          setErrorKind("network");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [traceCode]);

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(window.location.href);
      message.success("溯源链接已复制");
    } catch {
      message.error("复制失败，请手动复制浏览器地址");
    }
  }

  async function downloadQrCode() {
    if (!archive?.qrCodeUrl) return;
    try {
      const response = await fetch(resolveGrowthResourceUrl(archive.qrCodeUrl));
      if (!response.ok) throw new Error("二维码下载失败");
      const objectUrl = URL.createObjectURL(await response.blob());
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = `growth-trace-${archive.traceCode}.png`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(objectUrl);
    } catch {
      message.error("二维码下载失败，请稍后重试");
    }
  }

  if (loading) {
    return (
      <main className={styles.statePage}>
        <div className={styles.loader} />
        <p>正在加载溯源档案...</p>
      </main>
    );
  }

  if (!archive || errorKind) {
    const errorCopy =
      errorKind === "private"
        ? ["该溯源档案暂未公开", "该记录尚未开启公开查询，请联系管理员开启公开溯源。"]
        : errorKind === "missing"
          ? ["未找到对应溯源档案", "请确认溯源码是否正确，或联系系统管理员。"]
          : ["溯源档案加载失败", "请稍后重试。"];
    return (
      <main className={styles.statePage}>
        <FileSearch size={42} />
        <h1>{errorCopy[0]}</h1>
        <p>{errorCopy[1]}</p>
      </main>
    );
  }

  const status = STATUS_META[archive.auditStatus || ""];
  const herbName = archive.herbName || archive.speciesName || "未命名药材";
  const images = archive.images || [];
  const timeline = archive.traceTimeline || [];

  return (
    <main className={styles.page}>
      <header className={styles.brandBar}>
        <div className={styles.brandMark}>本草</div>
        <div>
          <strong>本草研究院标本馆</strong>
          <span>Herbarium Research Hall</span>
        </div>
        <p>中药材数字溯源档案</p>
      </header>

      <div className={styles.content}>
        <section className={styles.cover}>
          <div>
            <span className={styles.eyebrow}>DIGITAL TRACE ARCHIVE</span>
            <h1>{herbName}</h1>
            <p>
              {archive.batchName || "采集批次未记录"} · {formatTime(archive.collectTime)}
            </p>
            <div className={styles.coverMeta}>
              <span>
                溯源码 <b>{archive.traceCode}</b>
              </span>
              <span className={status?.className}>{status?.label || "状态未记录"}</span>
              <span>数据来源：生物医药数字信息系统</span>
            </div>
          </div>
          <div className={styles.coverAside}>
            {archive.auditStatus === "approved" ? (
              <div className={styles.approvalStamp}>
                审核通过<small>数据可信</small>
              </div>
            ) : null}
            {archive.qrCodeUrl ? (
              <div className={styles.coverQr}>
                <Image
                  src={resolveGrowthResourceUrl(archive.qrCodeUrl)}
                  alt={`${herbName}溯源二维码`}
                  preview
                />
                <span>扫码查验档案</span>
              </div>
            ) : null}
          </div>
        </section>

        <div className={`${styles.actions} ${styles.noPrint}`}>
          <Button icon={<Copy size={16} />} onClick={() => void copyLink()}>
            复制溯源链接
          </Button>
          {archive.qrCodeUrl ? (
            <Button icon={<Download size={16} />} onClick={() => void downloadQrCode()}>
              下载二维码
            </Button>
          ) : null}
          <Button type="primary" icon={<Printer size={16} />} onClick={() => window.print()}>
            打印溯源档案
          </Button>
        </div>

        <section className={styles.section}>
          <header>
            <h2>基础档案</h2>
            <p>采集任务、批次与现场归属信息</p>
          </header>
          <dl className={styles.infoGrid}>
            {[
              ["药材名称", herbName],
              ["采集任务", archive.taskName],
              ["采集批次", archive.batchName],
              ["基地名称", archive.baseName],
              ["采集地点", archive.collectPlace],
              ["采集时间", formatTime(archive.collectTime)],
              ["采集员", archive.collectorName],
              ["生长阶段", archive.growthStage],
            ].map(([label, value]) => (
              <div key={label}>
                <dt>{label}</dt>
                <dd>{value || "-"}</dd>
              </div>
            ))}
          </dl>
        </section>

        <div className={styles.metricSections}>
          <section className={styles.section}>
            <header>
              <h2>环境指标</h2>
              <p>采集现场气候与土壤观测</p>
            </header>
            <div className={styles.metricGrid}>
              <Metric label="温度" value={archive.temperature} unit="℃" />
              <Metric label="湿度" value={archive.humidity} unit="%" />
              <Metric label="光照" value={archive.light} unit="lx" />
              <Metric label="土壤湿度" value={archive.soilMoisture} unit="%" />
              <Metric label="土壤 pH" value={archive.soilPh} />
              <Metric label="土壤类型" value={archive.soilType} />
            </div>
          </section>
          <section className={styles.section}>
            <header>
              <h2>生长指标</h2>
              <p>植株形态与现场生长评价</p>
            </header>
            <div className={styles.metricGrid}>
              <Metric label="株高" value={archive.plantHeight} unit="cm" />
              <Metric label="茎粗" value={archive.stemDiameter} unit="mm" />
              <Metric label="叶色" value={archive.leafColor} />
              <Metric label="开花情况" value={archive.floweringStatus} />
              <Metric label="采样重量" value={archive.sampleWeight} unit="g" />
              <Metric label="生长评价" value={archive.growthEvaluation} />
            </div>
          </section>
        </div>

        <section className={styles.section}>
          <header>
            <h2>现场图片证据</h2>
            <p>以下图片来自该采集批次，用于佐证本次生长记录。</p>
          </header>
          {images.length ? (
            <div className={styles.imageGrid}>
              {images.map((image, index) => (
                <figure key={`${image.imageUrl}-${index}`}>
                  <Image
                    src={resolveGrowthResourceUrl(image.imageUrl)}
                    alt={`${herbName}现场图片 ${index + 1}`}
                    preview
                  />
                  <figcaption>
                    <strong>
                      {IMAGE_TYPE_LABELS[image.imageRole || image.imageType || ""] || "现场图片"}
                    </strong>
                    <span>
                      {image.uploaderName || "采集人员"} · {formatTime(image.uploadTime)}
                    </span>
                  </figcaption>
                </figure>
              ))}
            </div>
          ) : (
            <div className={styles.empty}>
              <QrCode size={28} />
              <strong>暂无现场图片证据</strong>
            </div>
          )}
        </section>

        <section className={styles.section}>
          <header>
            <h2>审核信息</h2>
            <p>档案当前审核结论与最近一次审核记录</p>
          </header>
          {archive.latestAuditResult || archive.latestAuditTime ? (
            <dl className={styles.auditCard}>
              <div>
                <dt>当前状态</dt>
                <dd>{statusLabel(archive.auditStatus)}</dd>
              </div>
              <div>
                <dt>审核结果</dt>
                <dd>{archive.latestAuditResult || "-"}</dd>
              </div>
              <div>
                <dt>审核人</dt>
                <dd>{archive.reviewerName || "-"}</dd>
              </div>
              <div>
                <dt>审核时间</dt>
                <dd>{formatTime(archive.latestAuditTime)}</dd>
              </div>
            </dl>
          ) : (
            <div className={styles.empty}>
              <strong>暂无审核记录</strong>
            </div>
          )}
        </section>

        <section className={styles.section}>
          <header>
            <h2>溯源时间线</h2>
            <p>从记录创建到公开查询的完整档案链路</p>
          </header>
          {timeline.length ? (
            <ol className={styles.timeline}>
              {timeline.map((event, index) => (
                <li key={`${event.eventType}-${event.eventTime || index}`}>
                  <i />
                  <div>
                    <strong>{eventTitle(event)}</strong>
                    <span>{statusTransition(event.beforeStatus, event.afterStatus)}</span>
                    <small>
                      {event.operatorName || "系统"} · {formatTime(event.eventTime)}
                    </small>
                  </div>
                </li>
              ))}
            </ol>
          ) : (
            <div className={styles.empty}>
              <strong>暂无溯源事件</strong>
            </div>
          )}
        </section>

        <footer className={styles.footer}>
          本溯源档案由生物医药数字信息系统根据采集记录、审核记录和溯源事件自动生成。
        </footer>
      </div>
    </main>
  );
}
