"use client";

import { Button, Card, Image, Space, Tag, Typography } from "antd";
import Link from "next/link";
import { featuredHerbs } from "@/mocks/home";
import overviewStyles from "./HomeOverviewGrid.module.css";
import styles from "./FeaturedHerbsPanel.module.css";

export function FeaturedHerbsPanel() {
  return (
    <Card
      className={`${overviewStyles.overviewCard} ${overviewStyles.paperPanel} ${styles.panel}`}
      extra={
        <Button className={styles.moreButton} type="link">
          <Link href="/herbs">查看全部 →</Link>
        </Button>
      }
      title="道地药材精选"
      variant="borderless"
    >
      <div className={styles.herbShelf}>
        {featuredHerbs.map((herb) => (
          <Link className={styles.herbTile} href={`/herbs?keyword=${encodeURIComponent(herb.name)}`} key={herb.id}>
            <div className={styles.herbImageBox}>
              {herb.imageSrc ? (
                <Image
                  alt={`${herb.name}药材图`}
                  className={styles.herbImage}
                  height={82}
                  preview={false}
                  src={herb.imageSrc}
                  width="100%"
                />
              ) : (
                <div className={styles.imagePlaceholder}>{herb.name.slice(0, 1)}</div>
              )}
            </div>
            <Typography.Title className={styles.herbName} level={4}>
              {herb.name}
            </Typography.Title>
            <Typography.Text className={styles.description}>
              {herb.descriptionLines.map((line) => (
                <span key={line}>{line}</span>
              ))}
            </Typography.Text>
            <Typography.Text className={styles.herbOrigin}>{herb.origin}</Typography.Text>
            <Space className={styles.tagRow} size={4} wrap>
              <Tag className={styles.paperTag}>{herb.level}</Tag>
              <Tag className={styles.paperTag}>{herb.category}</Tag>
            </Space>
          </Link>
        ))}
      </div>
    </Card>
  );
}
