"use client";

import { Card, Tag } from "antd";
import Link from "next/link";
import type { HerbSpeciesApi } from "@/lib/herbs";
import overviewStyles from "./HomeOverviewGrid.module.css";
import styles from "./FeaturedHerbsPanel.module.css";

function getPlaceholderCharacter(name?: string) {
  return name?.match(/[\u4e00-\u9fff]/)?.[0] ?? "药";
}

function getHerbHref(name?: string) {
  const keyword = name?.trim();
  return keyword ? `/herbs?keyword=${encodeURIComponent(keyword)}` : "/herbs";
}

export function FeaturedHerbsPanel({ herbs }: { herbs: HerbSpeciesApi[] }) {
  return (
    <Card
      className={`${overviewStyles.overviewCard} ${overviewStyles.paperPanel} ${styles.panel}`}
      extra={<Link className={styles.moreButton} href="/herbs">查看全部 →</Link>}
      title="道地药材精选"
      variant="borderless"
    >
      <div className={styles.herbShelf}>
        {herbs.map((herb) => {
          const displayName = herb.herbName?.trim() || "药材名称待完善";
          const medicinalPart = herb.medicinalPart?.trim() || "药用部位待完善";
          const description = herb.efficacy?.trim() || herb.description?.trim() || "药材档案信息待完善";
          const category = herb.categoryName || herb.category || "暂未分类";

          return (
            <Link className={styles.herbTile} href={getHerbHref(herb.herbName)} key={herb.id}>
              <div className={styles.herbImageBox}>
                {herb.coverImageUrl ? (
                  <img className={styles.herbImage} src={herb.coverImageUrl} alt={displayName} />
                ) : (
                  <div className={styles.imagePlaceholder}>{getPlaceholderCharacter(herb.herbName)}</div>
                )}
              </div>
              <div className={styles.herbContent}>
                <h3 className={styles.herbName} title={displayName}>{displayName}</h3>
                <p className={styles.medicinalPart} title={medicinalPart}>药用部位：{medicinalPart}</p>
                <p className={styles.description}>{description}</p>
              </div>
              <div className={styles.cardFooter}>
                <Tag className={styles.paperTag}>{category}</Tag>
                <span className={styles.archiveLink}>查看档案 →</span>
              </div>
            </Link>
          );
        })}
        {herbs.length === 0 ? (
          <div className={styles.emptyState}>
            <strong>暂无推荐药材</strong>
            <span>药材档案完善后将在此展示</span>
          </div>
        ) : null}
      </div>
    </Card>
  );
}
