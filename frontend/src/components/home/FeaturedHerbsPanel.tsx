"use client";

import { Button, Card, Space, Tag, Typography } from "antd";
import Link from "next/link";
import type { HerbSpeciesApi } from "@/lib/herbs";
import overviewStyles from "./HomeOverviewGrid.module.css";
import styles from "./FeaturedHerbsPanel.module.css";

export function FeaturedHerbsPanel({ herbs }: { herbs: HerbSpeciesApi[] }) {
  return (
    <Card
      className={`${overviewStyles.overviewCard} ${overviewStyles.paperPanel} ${styles.panel}`}
      extra={<Button className={styles.moreButton} type="link"><Link href="/herbs">查看全部 →</Link></Button>}
      title="道地药材精选" variant="borderless"
    >
      <div className={styles.herbShelf}>
        {herbs.map((herb) => (
          <Link className={styles.herbTile} href={`/herbs?keyword=${encodeURIComponent(herb.herbName)}`} key={herb.id}>
            <div className={styles.herbImageBox}><div className={styles.imagePlaceholder}>{herb.herbName.slice(0, 1)}</div></div>
            <Typography.Title className={styles.herbName} level={4}>{herb.herbName}</Typography.Title>
            <Typography.Text className={styles.description}>
              <span>{herb.medicinalPart ? `药用部位：${herb.medicinalPart}` : "药用部位待完善"}</span>
              <span>{herb.efficacy || herb.description || "药材档案待完善"}</span>
            </Typography.Text>
            <Typography.Text className={styles.herbOrigin}>{herb.aliasName || herb.latinName || "BDIS 药材档案"}</Typography.Text>
            <Space className={styles.tagRow} size={4} wrap><Tag className={styles.paperTag}>{herb.category || "未分类"}</Tag></Space>
          </Link>
        ))}
        {herbs.length === 0 ? <Typography.Text type="secondary">暂无可展示药材</Typography.Text> : null}
      </div>
    </Card>
  );
}
