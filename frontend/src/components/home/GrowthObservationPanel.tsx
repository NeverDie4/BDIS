"use client";

import { Button, Card, List, Space, Statistic, Tag, Typography } from "antd";
import Link from "next/link";
import type { DashboardRecentGrowth, DashboardSummary } from "@/lib/dashboard";
import styles from "./HomeOverviewGrid.module.css";

export function GrowthObservationPanel({ records, summary }: { records: DashboardRecentGrowth[]; summary: DashboardSummary | null }) {
  return (
    <Card className={`${styles.overviewCard} ${styles.paperPanel}`} extra={<Button className={styles.moreButton} type="link"><Link href="/growth">更多 →</Link></Button>} title="生长观测动态" variant="borderless">
      <Space size="large" wrap>
        <Statistic title="采集总数" value={summary?.growthRecordCount ?? 0} />
        <Statistic title="待审核" value={summary?.pendingGrowthReviewCount ?? 0} />
      </Space>
      <List
        dataSource={records}
        locale={{ emptyText: "暂无近期采集记录" }}
        renderItem={(record) => <List.Item>
          <div>
            <Typography.Text strong>{record.herbName || `记录 #${record.recordId}`}</Typography.Text>
            <Typography.Paragraph type="secondary">{record.baseName || "未关联基地"} / {record.collectorName || "采集人未知"}</Typography.Paragraph>
          </div>
          <Tag>{record.reviewStatus || "draft"}</Tag>
        </List.Item>}
      />
    </Card>
  );
}
