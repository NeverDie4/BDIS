"use client";

import { Card, Tag } from "antd";
import dayjs from "dayjs";
import Link from "next/link";
import type { DashboardRecentGrowth, DashboardSummary } from "@/lib/dashboard";
import styles from "./HomeOverviewGrid.module.css";

const reviewStatusMap = {
  draft: { label: "草稿", className: styles.growthStatusDraft },
  submitted: { label: "待审核", className: styles.growthStatusSubmitted },
  approved: { label: "已通过", className: styles.growthStatusApproved },
  rejected: { label: "已驳回", className: styles.growthStatusRejected },
} as const;

function getPlaceholderCharacter(name?: string) {
  return name?.match(/[\u4e00-\u9fff]/)?.[0] ?? "药";
}

function getCollectedTime(value?: string) {
  if (!value) return "";
  const date = dayjs(value);
  return date.isValid() ? date.format("YYYY-MM-DD HH:mm") : "";
}

function getTimestamp(value?: string) {
  if (!value) return 0;
  const timestamp = dayjs(value).valueOf();
  return Number.isFinite(timestamp) ? timestamp : 0;
}

export function GrowthObservationPanel({ records, summary }: { records: DashboardRecentGrowth[]; summary: DashboardSummary | null }) {
  const visibleRecords = [...records]
    .sort((left, right) => getTimestamp(right.collectedAt) - getTimestamp(left.collectedAt) || right.recordId - left.recordId)
    .slice(0, 3);

  return (
    <Card
      className={`${styles.overviewCard} ${styles.paperPanel}`}
      extra={<Link className={styles.moreButton} href="/growth">更多 →</Link>}
      title="生长观测动态"
      variant="borderless"
    >
      <div className={styles.growthSummary}>
        <div className={styles.growthSummaryItem}>
          <span>观测记录</span>
          <strong>{summary?.growthRecordCount ?? 0}</strong>
        </div>
        <div className={styles.growthSummaryItem}>
          <span>待审核</span>
          <strong>{summary?.pendingGrowthReviewCount ?? 0}</strong>
        </div>
      </div>

      {visibleRecords.length > 0 ? (
        <div className={styles.growthActivityList}>
          {visibleRecords.map((record) => {
            const status = reviewStatusMap[record.reviewStatus as keyof typeof reviewStatusMap] ?? {
              label: "状态未知",
              className: styles.growthStatusDraft,
            };
            const collectedTime = getCollectedTime(record.collectedAt);

            return (
              <Link className={styles.growthActivityRow} href="/growth" key={record.recordId}>
                <div className={styles.growthAvatar}>{getPlaceholderCharacter(record.herbName)}</div>
                <div className={styles.growthActivityContent}>
                  <strong className={styles.growthHerbName}>{record.herbName || "药材名称待完善"}</strong>
                  <div className={styles.growthMeta}>
                    <span>{record.baseName || "基地未关联"}</span>
                    <span aria-hidden>·</span>
                    <span>{record.collectorName || "采集员未知"}</span>
                  </div>
                  <div className={styles.growthDetail}>
                    {collectedTime ? <><span>{collectedTime}</span><span aria-hidden>·</span></> : null}
                    <span>阶段未填写</span>
                  </div>
                </div>
                <Tag className={`${styles.growthStatus} ${status.className}`}>{status.label}</Tag>
              </Link>
            );
          })}
        </div>
      ) : (
        <div className={styles.growthEmpty}>
          <strong>暂无生长观测记录</strong>
          <span>移动端提交的生长数据将在此同步展示</span>
        </div>
      )}
    </Card>
  );
}
