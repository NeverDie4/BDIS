"use client";

import { CloudOutlined, EnvironmentOutlined, FireOutlined, ThunderboltOutlined } from "@ant-design/icons";
import { Button, Card, Divider, Space, Statistic, Tooltip, Typography } from "antd";
import Link from "next/link";
import { growthObservation } from "@/mocks/home";
import styles from "./HomeOverviewGrid.module.css";

function buildTrendPoints() {
  const values = growthObservation.trend.map((item) => item.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = Math.max(max - min, 1);

  return growthObservation.trend
    .map((item, index) => {
      const x = 18 + index * 38;
      const y = 88 - ((item.value - min) / span) * 54;
      return `${x},${y}`;
    })
    .join(" ");
}

export function GrowthObservationPanel() {
  const trendPoints = buildTrendPoints();

  return (
    <Card
      className={`${styles.overviewCard} ${styles.paperPanel}`}
      extra={
        <Button className={styles.moreButton} type="link">
          <Link href="/growth">更多 →</Link>
        </Button>
      }
      title="生长观测动态"
      variant="borderless"
    >
      <Space className={styles.stationLine} size={8}>
        <EnvironmentOutlined />
        <Typography.Text>{growthObservation.baseName}</Typography.Text>
      </Space>
      <Typography.Text className={styles.updatedAt}>更新时间：{growthObservation.updatedAt}</Typography.Text>

      <div className={styles.observationGrid}>
        <Statistic
          prefix={<FireOutlined />}
          suffix="℃"
          title="温度"
          value={growthObservation.temperature}
          valueStyle={{ color: "var(--color-text)" }}
        />
        <Statistic suffix="%RH" title="湿度" value={growthObservation.humidity} />
        <Statistic prefix={<ThunderboltOutlined />} suffix="Lux" title="光照" value={growthObservation.light} />
        <Statistic prefix={<CloudOutlined />} suffix="mm" title="降雨" value={growthObservation.rainfall} />
      </div>

      <Divider className={styles.softDivider} />

      <div className={styles.trendHeader}>
        <Typography.Text>近 7 日温度趋势（℃）</Typography.Text>
        <Tooltip title="首页摘要使用 SVG polyline，后续可升级为 ECharts">
          <span className={styles.trendBadge}>示意</span>
        </Tooltip>
      </div>
      <svg className={styles.trendChart} role="img" viewBox="0 0 260 110">
        <title>近 7 日温度趋势</title>
        <line className={styles.axisLine} x1="18" x2="246" y1="92" y2="92" />
        <line className={styles.axisLine} x1="18" x2="18" y1="24" y2="92" />
        <polyline className={styles.trendLine} points={trendPoints} />
        {growthObservation.trend.map((item, index) => {
          const [x, y] = trendPoints.split(" ")[index].split(",");
          return (
            <g key={item.date}>
              <circle className={styles.trendDot} cx={x} cy={y} r="3.2" />
              <text className={styles.trendLabel} x={x} y="106">
                {item.date}
              </text>
            </g>
          );
        })}
      </svg>
    </Card>
  );
}
