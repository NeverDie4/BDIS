"use client";

import axios from "axios";
import dynamic from "next/dynamic";
import Image from "next/image";
import {
  ArrowLeft,
  BadgeCheck,
  CalendarDays,
  Camera,
  FileClock,
  Leaf,
  MapPin,
  Pause,
  Play,
  Presentation,
  Printer,
  QrCode,
  RotateCcw,
  ShieldCheck,
  SkipBack,
  SkipForward,
  Sprout,
  TrendingUp,
  X,
} from "lucide-react";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import {
  getPublicDigitalLifeArchive,
  getPublicDigitalLifeIntegrity,
  resolveDigitalLifeResourceUrl,
  type PublicDigitalLifeArchiveApi,
  type PublicDigitalLifeIntegrityApi,
  type PublicDigitalLifeMetricsApi,
  type PublicDigitalLifeStageApi,
} from "@/lib/digital-life";
import type { ApiResult } from "@/types/api";
import DigitalLifeStageEvidence from "./DigitalLifeStageEvidence";
import styles from "./page.module.css";

const DigitalLifeStageMap = dynamic(() => import("./DigitalLifeStageMap"), {
  ssr: false,
  loading: () => <div className={styles.mapLoading}>地图加载中</div>,
});
const DigitalLifeTrendChart = dynamic(() => import("./DigitalLifeTrendChart"), {
  ssr: false,
  loading: () => <div className={styles.trendLoading}>趋势图加载中</div>,
});

const EMPTY_VALUE = "-";
const STAGE_DURATION_MS = 4_000;

const DATA_STATUS_LABELS: Record<string, string> = {
  complete: "数据完整",
  missing_growth_record: "缺少生长记录",
  missing_images: "缺少现场影像",
  incomplete: "数据待完善",
};

type PageError = "missing" | "private" | "network";
type StageChangeSource = "autoplay" | "timeline" | "previous" | "next" | "chart" | "map";

function formatDateTime(value?: string, dateOnly = false) {
  if (!value) return EMPTY_VALUE;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return EMPTY_VALUE;
  return dateOnly
    ? date.toLocaleDateString("zh-CN")
    : date.toLocaleString("zh-CN", { hour12: false });
}

function displayValue(value?: string | number | null) {
  return value == null || value === "" ? EMPTY_VALUE : value;
}

function shortHash(value?: string | null) {
  if (!value) return "-";
  return value.length > 16 ? `${value.slice(0, 8)}...${value.slice(-8)}` : value;
}

function hasMetrics(metrics?: PublicDigitalLifeMetricsApi | null) {
  return Boolean(metrics && Object.values(metrics).some((value) => value != null && value !== ""));
}

function stageDataStatus(stage: PublicDigitalLifeStageApi) {
  if (stage.dataStatus) return stage.dataStatus;
  if (!hasMetrics(stage.metrics)) return "incomplete";
  if (!stage.images?.length) return "missing_images";
  return "complete";
}

function statusLabel(status?: string) {
  if (status === "已通过" || status === "approved") return "已通过";
  if (status === "submitted") return "待审核";
  if (status === "rejected") return "已驳回";
  if (status === "draft") return "草稿";
  return status || "状态未记录";
}

function handleBackNavigation() {
  if (document.referrer) {
    const previousPage = new URL(document.referrer);
    if (previousPage.origin === window.location.origin && window.history.length > 1) {
      window.history.back();
      return;
    }
  }
  window.location.assign("/");
}

export default function PublicDigitalLifeArchivePage() {
  const params = useParams<{ traceCode: string }>();
  const traceCode = Array.isArray(params.traceCode) ? params.traceCode[0] : params.traceCode;
  const [archiveData, setArchiveData] = useState<PublicDigitalLifeArchiveApi>();
  const [integrityData, setIntegrityData] = useState<PublicDigitalLifeIntegrityApi>();
  const [stages, setStages] = useState<PublicDigitalLifeStageApi[]>([]);
  const [currentStageIndex, setCurrentStageIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [playbackSpeed, setPlaybackSpeed] = useState(1);
  const [progress, setProgress] = useState(0);
  const [hasFinished, setHasFinished] = useState(false);
  const [demoMode, setDemoMode] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<PageError>();
  const animationFrameRef = useRef<number | null>(null);
  const elapsedRef = useRef(0);
  const lastFrameTimeRef = useRef<number | null>(null);
  const currentStage = useMemo(() => stages[currentStageIndex], [currentStageIndex, stages]);

  useEffect(() => {
    let active = true;
    if (!traceCode) {
      setError("missing");
      setLoading(false);
      return;
    }
    setLoading(true);
    Promise.all([
      getPublicDigitalLifeArchive(traceCode),
      getPublicDigitalLifeIntegrity(traceCode).catch(() => undefined),
    ])
      .then(([archive, integrity]) => {
        if (!active) return;
        const nextStages = archive.stages || [];
        const firstValidIndex = nextStages.findIndex(
          (stage) => stageDataStatus(stage) === "complete",
        );
        setArchiveData(archive);
        setIntegrityData(integrity);
        setStages(nextStages);
        setCurrentStageIndex(firstValidIndex >= 0 ? firstValidIndex : 0);
        elapsedRef.current = 0;
        lastFrameTimeRef.current = null;
        setProgress(0);
        setHasFinished(false);
        setIsPlaying(false);
        setError(undefined);
      })
      .catch((reason: unknown) => {
        if (!active) return;
        if (axios.isAxiosError<ApiResult<unknown>>(reason)) {
          if (reason.response?.status === 403) setError("private");
          else if (reason.response?.status === 404) setError("missing");
          else setError("network");
        } else {
          setError("network");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [traceCode]);

  const pausePlayback = useCallback(() => {
    if (animationFrameRef.current != null) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }
    lastFrameTimeRef.current = null;
    setIsPlaying(false);
  }, []);

  const setActiveStage = useCallback(
    (index: number, source: StageChangeSource) => {
      if (!stages.length) return;
      const nextIndex = Math.min(Math.max(index, 0), stages.length - 1);
      if (source !== "autoplay") pausePlayback();
      elapsedRef.current = 0;
      lastFrameTimeRef.current = null;
      setProgress(0);
      setHasFinished(false);
      setCurrentStageIndex(nextIndex);
    },
    [pausePlayback, stages.length],
  );

  const playFromBeginning = useCallback(() => {
    if (!stages.length) return;
    setActiveStage(0, "autoplay");
    setIsPlaying(true);
  }, [setActiveStage, stages.length]);

  const handleMapStageSelect = useCallback(
    (index: number) => {
      setActiveStage(index, "map");
    },
    [setActiveStage],
  );

  const handleChartStageSelect = useCallback(
    (index: number) => {
      setActiveStage(index, "chart");
    },
    [setActiveStage],
  );

  const togglePlayback = useCallback(() => {
    if (!stages.length) return;
    if (isPlaying) {
      pausePlayback();
      return;
    }
    if (hasFinished) {
      playFromBeginning();
      return;
    }
    lastFrameTimeRef.current = null;
    setIsPlaying(true);
  }, [hasFinished, isPlaying, pausePlayback, playFromBeginning, stages.length]);

  const toggleDemoMode = useCallback(() => {
    if (demoMode) {
      setDemoMode(false);
      pausePlayback();
      return;
    }
    setDemoMode(true);
    setPlaybackSpeed(1);
    playFromBeginning();
  }, [demoMode, pausePlayback, playFromBeginning]);

  useEffect(() => {
    if (!isPlaying || !stages.length) return;

    const advance = (time: number) => {
      if (lastFrameTimeRef.current == null) lastFrameTimeRef.current = time;
      const elapsed = time - lastFrameTimeRef.current;
      lastFrameTimeRef.current = time;
      elapsedRef.current = Math.min(
        STAGE_DURATION_MS,
        elapsedRef.current + elapsed * playbackSpeed,
      );
      setProgress((elapsedRef.current / STAGE_DURATION_MS) * 100);

      if (elapsedRef.current >= STAGE_DURATION_MS) {
        if (currentStageIndex >= stages.length - 1) {
          animationFrameRef.current = null;
          setProgress(100);
          setHasFinished(true);
          setIsPlaying(false);
          return;
        }
        setActiveStage(currentStageIndex + 1, "autoplay");
        return;
      }
      animationFrameRef.current = requestAnimationFrame(advance);
    };

    animationFrameRef.current = requestAnimationFrame(advance);
    return () => {
      if (animationFrameRef.current != null) {
        cancelAnimationFrame(animationFrameRef.current);
        animationFrameRef.current = null;
      }
    };
  }, [currentStageIndex, isPlaying, playbackSpeed, setActiveStage, stages.length]);

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden) pausePlayback();
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => document.removeEventListener("visibilitychange", handleVisibilityChange);
  }, [pausePlayback]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (target?.matches("input, textarea, select, [contenteditable='true']")) return;
      if (event.code === "Space") {
        event.preventDefault();
        togglePlayback();
      } else if (event.code === "ArrowLeft") {
        setActiveStage(currentStageIndex - 1, "previous");
      } else if (event.code === "ArrowRight") {
        setActiveStage(currentStageIndex + 1, "next");
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [currentStageIndex, setActiveStage, togglePlayback]);

  if (loading) {
    return (
      <main className={styles.statePage}>
        <div className={styles.loader} />
        <strong>正在调取数字生命档案</strong>
        <span>本草研究院标本馆</span>
      </main>
    );
  }

  if (!archiveData || error) {
    const copy =
      error === "private"
        ? ["档案尚未公开", "该数字生命档案暂不提供公开查阅。"]
        : error === "missing"
          ? ["未找到数字生命档案", "请核对档案编号或溯源码。"]
          : ["档案加载失败", "网络连接异常，请稍后重新访问。"];
    return (
      <main className={styles.statePage}>
        <Leaf size={44} />
        <h1>{copy[0]}</h1>
        <p>{copy[1]}</p>
      </main>
    );
  }

  const audited =
    stages.length > 0 && stages.every((stage) => statusLabel(stage.auditStatus) === "已通过");
  const hasIntegrityChain = Boolean(integrityData?.rootHash);
  const metrics = currentStage?.metrics;
  const finalStage = stages[stages.length - 1];
  const stageSummary =
    currentStage?.aiNarration ||
    currentStage?.recognition?.conclusion ||
    metrics?.growthEvaluation ||
    (currentStage
      ? `本阶段记录于${formatDateTime(currentStage.collectedAt, true)}，生长阶段为${currentStage.growthStage || "待填写"}。`
      : "暂无可公开的连续观测阶段。");

  return (
    <main className={`${styles.page} ${demoMode ? styles.demoMode : ""}`}>
      {demoMode ? (
        <button type="button" className={styles.exitDemo} onClick={toggleDemoMode}>
          <X size={16} />
          退出演示
        </button>
      ) : null}
      <header className={styles.brandBar}>
        <button
          type="button"
          className={styles.backButton}
          onClick={handleBackNavigation}
          aria-label="返回上一页"
        >
          <ArrowLeft size={17} />
          <span>返回</span>
        </button>
        <div className={styles.brandSeal}>本草</div>
        <div>
          <strong>本草研究院标本馆</strong>
          <span>HERBARIUM DIGITAL ARCHIVE</span>
        </div>
        <p>中药材数字生命档案</p>
      </header>

      <div className={styles.content}>
        <section className={styles.hero}>
          <div className={styles.paperTexture} aria-hidden="true" />
          <div className={styles.heroContent}>
            <span className={styles.eyebrow}>DIGITAL LIFE ARCHIVE</span>
            <h1>{archiveData.speciesName || "未命名药材"}</h1>
            <p className={styles.subtitle}>
              记录中药材从连续观测、现场采集、图像识别到最终审核的完整数字生命过程。
            </p>
            <dl className={styles.heroFacts}>
              <div>
                <dt>采集任务</dt>
                <dd>{displayValue(archiveData.taskName)}</dd>
              </div>
              <div>
                <dt>观测基地</dt>
                <dd>{displayValue(archiveData.baseName)}</dd>
              </div>
              <div>
                <dt>档案编号</dt>
                <dd>{archiveData.traceCode}</dd>
              </div>
              <div>
                <dt>观测周期</dt>
                <dd>
                  {formatDateTime(archiveData.startTime, true)} 至{" "}
                  {formatDateTime(archiveData.endTime, true)}
                </dd>
              </div>
            </dl>
          </div>
          <aside className={styles.heroStats} aria-label="档案认证摘要">
            {audited ? (
              <div className={styles.approvalStamp}>
                数字档案<small>已审核</small>
              </div>
            ) : null}
            <div className={styles.heroCounters}>
              <div>
                <strong>{archiveData.stageCount ?? stages.length}</strong>
                <span>观测阶段</span>
              </div>
              <div>
                <strong>{archiveData.imageCount ?? 0}</strong>
                <span>现场影像</span>
              </div>
            </div>
            <div className={styles.auditState}>
              <BadgeCheck size={19} />
              <div>
                <span>档案审核状态</span>
                <strong>{audited ? "全部阶段已通过" : "审核信息待完善"}</strong>
              </div>
            </div>
            <code>{archiveData.traceCode}</code>
          </aside>
        </section>

        <section className={styles.trustCard} aria-label="档案可信状态">
          <header className={styles.sectionHeading}>
            <div>
              <span>馆藏认证</span>
              <h2>档案可信状态</h2>
              <p>公开范围、连续观测完整度与防篡改证据链摘要。</p>
            </div>
          </header>
          <div className={styles.trustStrip}>
            <div>
              <ShieldCheck size={22} />
              <span>公开状态</span>
              <strong>已公开</strong>
            </div>
            <div>
              <FileClock size={22} />
              <span>档案完整度</span>
              <strong>{archiveData.archiveStatus === "complete" ? "完整" : "待完善"}</strong>
            </div>
            <div>
              <Camera size={22} />
              <span>有效阶段</span>
              <strong>
                {archiveData.validStageCount ?? 0} / {archiveData.stageCount ?? stages.length}
              </strong>
            </div>
          </div>

          {integrityData ? (
            <div
              className={`${styles.integrityBanner} ${integrityData.verified ? styles.integrityVerified : hasIntegrityChain ? styles.integrityFailed : styles.integrityUnknown}`}
            >
              <ShieldCheck size={25} />
              <div>
                <h3>
                  {integrityData.verified
                    ? "防篡改档案校验通过"
                    : hasIntegrityChain
                      ? "档案完整性校验异常"
                      : "尚未生成哈希证据链"}
                </h3>
                <p>
                  {integrityData.verified
                    ? `已校验 ${integrityData.eventCount} 个关键事件 · 根哈希 ${shortHash(integrityData.rootHash)}`
                    : hasIntegrityChain
                      ? "该档案需要重新审核并生成新的证据链版本。"
                      : "当前档案内容可查阅，但尚无防篡改校验快照。"}
                </p>
              </div>
              <details>
                <summary>查看校验详情</summary>
                <dl>
                  <div>
                    <dt>校验结果</dt>
                    <dd>{integrityData.message}</dd>
                  </div>
                  <div>
                    <dt>哈希版本</dt>
                    <dd>{displayValue(integrityData.hashVersion)}</dd>
                  </div>
                  <div>
                    <dt>生成时间</dt>
                    <dd>{formatDateTime(integrityData.generatedTime || undefined)}</dd>
                  </div>
                  <div>
                    <dt>根哈希</dt>
                    <dd>{displayValue(integrityData.rootHash)}</dd>
                  </div>
                </dl>
              </details>
            </div>
          ) : (
            <div className={`${styles.integrityBanner} ${styles.integrityUnknown}`}>
              <ShieldCheck size={25} />
              <div>
                <h3>档案校验状态暂不可用</h3>
                <p>档案主体内容仍可继续查阅。</p>
              </div>
            </div>
          )}
        </section>

        <section className={styles.playbackDeck} aria-label="数字生命回放控制">
          <header className={styles.sectionHeading}>
            <div>
              <span>PLAYBACK CONSOLE</span>
              <h2>数字生命回放</h2>
              <p>按时间顺序回放中药材各阶段的采集位置、生长指标、现场影像和阶段解说。</p>
            </div>
          </header>
          <div className={styles.playbackBar}>
            <div className={styles.playbackControls}>
              <button type="button" onClick={playFromBeginning} disabled={!stages.length}>
                <RotateCcw size={17} />
                {hasFinished ? "重新播放" : "从头播放"}
              </button>
              <button
                type="button"
                onClick={() => setActiveStage(currentStageIndex - 1, "previous")}
                disabled={!stages.length || currentStageIndex === 0}
                title="上一阶段"
              >
                <SkipBack size={18} />
                <span>上一阶段</span>
              </button>
              <button
                type="button"
                className={styles.primaryPlayback}
                onClick={togglePlayback}
                disabled={!stages.length}
              >
                {isPlaying ? <Pause size={19} /> : <Play size={19} />}
                {isPlaying ? "暂停" : "播放"}
              </button>
              <button
                type="button"
                onClick={() => setActiveStage(currentStageIndex + 1, "next")}
                disabled={!stages.length || currentStageIndex === stages.length - 1}
                title="下一阶段"
              >
                <SkipForward size={18} />
                <span>下一阶段</span>
              </button>
              <label>
                <span>播放速度</span>
                <select
                  value={playbackSpeed}
                  onChange={(event) => setPlaybackSpeed(Number(event.target.value))}
                >
                  <option value={0.5}>0.5x</option>
                  <option value={1}>1x</option>
                  <option value={1.5}>1.5x</option>
                  <option value={2}>2x</option>
                </select>
              </label>
              <button type="button" onClick={toggleDemoMode} disabled={!stages.length}>
                <Presentation size={18} />
                演示模式
              </button>
            </div>
            <div className={styles.playbackProgress}>
              <div>
                <span>当前回放节点</span>
                <strong>
                  {hasFinished
                    ? "数字生命回放完成"
                    : `第 ${stages.length ? currentStageIndex + 1 : 0} / ${stages.length} 阶段`}
                </strong>
                <b>
                  {currentStage
                    ? `${formatDateTime(currentStage.collectedAt, true)} · ${currentStage.growthStage || "阶段未填写"}`
                    : "暂无可播放阶段"}
                </b>
              </div>
              <div
                className={styles.progressTrack}
                role="progressbar"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={Math.round(progress)}
              >
                <i style={{ width: `${progress}%` }} />
              </div>
            </div>
          </div>
        </section>

        {currentStage ? (
          <section className={`${styles.stage} ${styles.stageShell}`}>
            <header className={styles.sectionHeading}>
              <div>
                <span>数字生命主舞台</span>
                <h2>
                  第 {currentStage.sequence || currentStageIndex + 1} 阶段 ·{" "}
                  {currentStage.growthStage || "阶段未填写"}
                </h2>
                <p>当前阶段的空间位置、连续趋势、现场影像和档案解说同步展示。</p>
              </div>
              <div className={styles.stageMeta}>
                <time>{formatDateTime(currentStage.collectedAt, true)}</time>
                <span className={styles.stageStatus}>{statusLabel(currentStage.auditStatus)}</span>
              </div>
            </header>

            <div
              key={`${currentStage.batchCode || "stage"}-${currentStageIndex}`}
              className={styles.stageGrid}
            >
              <div className={styles.stageLeft}>
                <section className={styles.mapPanel}>
                  <header>
                    <div>
                      <MapPin size={19} />
                      <div>
                        <span>SPATIAL ARCHIVE</span>
                        <h3>生长时空轨迹</h3>
                      </div>
                    </div>
                    <strong>
                      {currentStage.latitude != null && currentStage.longitude != null
                        ? "地点已记录"
                        : "暂无精确坐标"}
                    </strong>
                  </header>
                  <DigitalLifeStageMap
                    stages={stages}
                    currentStageIndex={currentStageIndex}
                    onStageSelect={handleMapStageSelect}
                  />
                  <footer>
                    {currentStage.latitude != null && currentStage.longitude != null
                      ? `${currentStage.locationName || currentStage.baseName || "观测地点"} · 坐标已归档`
                      : `本阶段暂无精确坐标，当前展示观测基地范围：${currentStage.baseName || "基地未关联"}`}
                  </footer>
                </section>

                <section className={styles.trendPanel}>
                  <header>
                    <div>
                      <TrendingUp size={19} />
                      <div>
                        <span>GROWTH CURVE</span>
                        <h3>生长趋势</h3>
                      </div>
                    </div>
                    <strong>{stages.length} 个观测阶段</strong>
                  </header>
                  <DigitalLifeTrendChart
                    stages={stages}
                    currentStageIndex={currentStageIndex}
                    onStageSelect={handleChartStageSelect}
                  />
                </section>
              </div>

              <aside className={styles.stageRight}>
                <section className={styles.stageNarrative}>
                  <span className={styles.narrativeSource}>
                    {currentStage.aiNarration
                      ? currentStage.narrationSource === "ai"
                        ? "AI 阶段解说"
                        : "规则摘要"
                      : "阶段解读"}
                  </span>
                  <h3>{currentStage.batchName || `第 ${currentStage.sequence} 次连续观测`}</h3>
                  <p>{stageSummary}</p>
                  <small>
                    <CalendarDays size={14} />
                    {currentStage.narrationGeneratedTime
                      ? `解说生成于 ${formatDateTime(currentStage.narrationGeneratedTime)}`
                      : formatDateTime(currentStage.collectedAt)}
                  </small>
                </section>

                <section className={styles.evidencePanel}>
                  <header>
                    <div>
                      <Camera size={19} />
                      <div>
                        <span>FIELD EVIDENCE</span>
                        <h3>阶段现场影像与观测指标</h3>
                      </div>
                    </div>
                    <strong>{currentStage.images?.length || 0} 张影像</strong>
                  </header>
                  <DigitalLifeStageEvidence
                    stages={stages}
                    currentStageIndex={currentStageIndex}
                    speciesName={archiveData.speciesName}
                  />
                </section>
              </aside>
            </div>
          </section>
        ) : (
          <section className={styles.archiveEmpty}>
            <Sprout size={34} />
            <h2>档案暂无有效阶段</h2>
            <p>该连续观测档案尚未形成可公开的阶段数据。</p>
          </section>
        )}

        {hasFinished ? (
          <section className={styles.finalNode}>
            <header>
              <BadgeCheck size={30} />
              <div>
                <span>数字生命回放完成</span>
                <h2>连续观测档案最终节点</h2>
              </div>
            </header>
            <div className={styles.finalNodeBody}>
              <dl>
                <div>
                  <dt>最终审核状态</dt>
                  <dd>{statusLabel(finalStage?.auditStatus)}</dd>
                </div>
                <div>
                  <dt>审核人</dt>
                  <dd>审核人信息未公开</dd>
                </div>
                <div>
                  <dt>审核时间</dt>
                  <dd>{formatDateTime(finalStage?.reviewedAt || undefined)}</dd>
                </div>
                <div>
                  <dt>档案阶段总数</dt>
                  <dd>{archiveData.stageCount ?? stages.length}</dd>
                </div>
                <div>
                  <dt>图片证据总数</dt>
                  <dd>{archiveData.imageCount ?? 0}</dd>
                </div>
                <div>
                  <dt>防篡改校验</dt>
                  <dd>
                    {integrityData?.verified
                      ? "校验通过"
                      : hasIntegrityChain
                        ? "校验异常"
                        : "尚未生成"}
                  </dd>
                </div>
                <div className={styles.rootHash}>
                  <dt>根哈希</dt>
                  <dd>{shortHash(integrityData?.rootHash)}</dd>
                </div>
              </dl>
              <div className={styles.finalIdentity}>
                {archiveData.qrCodeUrl ? (
                  <Image
                    src={resolveDigitalLifeResourceUrl(archiveData.qrCodeUrl)}
                    alt="数字生命档案二维码"
                    width={112}
                    height={112}
                    unoptimized
                  />
                ) : (
                  <div>
                    <QrCode size={42} />
                    <span>二维码尚未生成</span>
                  </div>
                )}
                <code>{archiveData.traceCode}</code>
              </div>
            </div>
            <footer>
              <button type="button" onClick={playFromBeginning}>
                <RotateCcw size={16} />
                重新播放
              </button>
              <button type="button" onClick={() => window.print()}>
                <Printer size={16} />
                打印数字档案
              </button>
            </footer>
          </section>
        ) : null}

        <section className={styles.timelineSection}>
          <header className={styles.sectionHeading}>
            <div>
              <span>LIFE STAGE TIMELINE</span>
              <h2>阶段时间轴</h2>
              <p>点击阶段可查看对应的采集位置、生长指标、现场影像和阶段解说。</p>
            </div>
            <strong>
              共 {stages.length} 个阶段 · 当前 {currentStageIndex + (stages.length ? 1 : 0)}
            </strong>
          </header>
          {stages.length ? (
            <div
              className={styles.timeline}
              style={{ "--stage-count": Math.max(stages.length, 1) } as CSSProperties}
            >
              {stages.map((stage, index) => {
                const status = stageDataStatus(stage);
                return (
                  <button
                    key={`${stage.batchCode || "stage"}-${index}`}
                    type="button"
                    className={index === currentStageIndex ? styles.timelineActive : ""}
                    onClick={() => setActiveStage(index, "timeline")}
                  >
                    <i>
                      {index < currentStageIndex ? (
                        <BadgeCheck size={17} />
                      ) : (
                        String(stage.sequence || index + 1).padStart(2, "0")
                      )}
                    </i>
                    <span>{formatDateTime(stage.collectedAt, true)}</span>
                    <strong>{stage.growthStage || "阶段未填写"}</strong>
                    <small>
                      {statusLabel(stage.auditStatus)} ·{" "}
                      {DATA_STATUS_LABELS[status] || "数据待完善"}
                    </small>
                  </button>
                );
              })}
            </div>
          ) : (
            <div className={styles.inlineEmpty}>暂无阶段时间轴</div>
          )}
        </section>

        <section className={styles.archiveDetails}>
          <div>
            <header>
              <i />
              <div>
                <span>ARCHIVE PROFILE</span>
                <h2>档案基础信息</h2>
                <p>药材、任务、基地与连续观测周期。</p>
              </div>
            </header>
            <dl>
              <div>
                <dt>药材名称</dt>
                <dd>{displayValue(archiveData.speciesName)}</dd>
              </div>
              <div>
                <dt>任务名称</dt>
                <dd>{displayValue(archiveData.taskName)}</dd>
              </div>
              <div>
                <dt>任务编码</dt>
                <dd>{displayValue(archiveData.taskCode)}</dd>
              </div>
              <div>
                <dt>基地名称</dt>
                <dd>{displayValue(archiveData.baseName)}</dd>
              </div>
              <div>
                <dt>起始时间</dt>
                <dd>{formatDateTime(archiveData.startTime)}</dd>
              </div>
              <div>
                <dt>最后观测</dt>
                <dd>{formatDateTime(archiveData.endTime)}</dd>
              </div>
            </dl>
          </div>
          <div>
            <header>
              <i />
              <div>
                <span>TRUST RECORD</span>
                <h2>审核与溯源信息</h2>
                <p>公开审核、完整度与识别归档摘要。</p>
              </div>
            </header>
            <dl>
              <div>
                <dt>公开审核</dt>
                <dd>{audited ? "全部公开阶段已通过" : "审核信息待完善"}</dd>
              </div>
              <div>
                <dt>阶段完整度</dt>
                <dd>
                  {archiveData.validStageCount ?? 0} / {archiveData.stageCount ?? stages.length}
                </dd>
              </div>
              <div>
                <dt>档案状态</dt>
                <dd>{archiveData.archiveStatus === "complete" ? "完整档案" : "数据待完善"}</dd>
              </div>
              <div>
                <dt>识别结论</dt>
                <dd>{displayValue(currentStage?.recognition?.speciesName)}</dd>
              </div>
            </dl>
          </div>
          <div className={styles.archiveIdentity}>
            <header>
              <i />
              <div>
                <span>ARCHIVE IDENTITY</span>
                <h2>二维码与档案编号</h2>
                <p>用于公开查阅与档案身份核验。</p>
              </div>
            </header>
            {archiveData.qrCodeUrl ? (
              <div className={styles.qrPlaceholder}>
                <Image
                  src={resolveDigitalLifeResourceUrl(archiveData.qrCodeUrl)}
                  alt="数字生命档案二维码"
                  width={164}
                  height={164}
                  unoptimized
                />
                <strong>扫码查阅数字生命档案</strong>
              </div>
            ) : (
              <div className={`${styles.qrPlaceholder} ${styles.qrEmpty}`}>
                <div>
                  <QrCode size={28} />
                </div>
                <strong>任务级二维码尚未生成</strong>
                <span>当前任务级数字生命档案尚未生成独立二维码。</span>
              </div>
            )}
            <small>档案编号</small>
            <code>{archiveData.traceCode}</code>
          </div>
        </section>
        <footer className={styles.pageFooter}>
          本档案由生物医药数字信息系统根据连续观测记录、现场影像、审核结果与防篡改证据链自动生成。
        </footer>
      </div>
    </main>
  );
}
