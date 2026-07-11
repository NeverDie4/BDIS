"use client";

import { Button, Card, Image, List, Space, Tag, Typography } from "antd";
import Link from "next/link";
import { researchNews } from "@/mocks/home";
import styles from "./HomeOverviewGrid.module.css";

const typeClassName: Record<string, string> = {
  研究论文: styles.newsResearch,
  科研项目: styles.newsProject,
  教学课程: styles.newsCourse,
};

export function ResearchTeachingPanel() {
  return (
    <Card
      className={`${styles.overviewCard} ${styles.paperPanel}`}
      extra={
        <Button className={styles.moreButton} type="link">
          <Link href="/teaching">更多 →</Link>
        </Button>
      }
      title="最新研究与教学"
      variant="borderless"
    >
      <List
        className={styles.newsList}
        dataSource={researchNews}
        renderItem={(item) => (
          <List.Item className={styles.newsItem}>
            <Link className={styles.newsLink} href="/teaching">
              <div className={styles.newsCover}>
                {item.cover ? (
                  <Image alt={item.title} height={58} preview={false} src={item.cover} width={58} />
                ) : (
                  <span>{item.type.slice(0, 1)}</span>
                )}
              </div>
              <div className={styles.newsContent}>
                <Space size={6}>
                  <Tag className={`${styles.paperTag} ${typeClassName[item.type] ?? ""}`}>{item.type}</Tag>
                  <Typography.Text className={styles.newsDate}>{item.date}</Typography.Text>
                </Space>
                <Typography.Title className={styles.newsTitle} level={4}>
                  {item.title}
                </Typography.Title>
                <Typography.Text className={styles.newsSource}>{item.source}</Typography.Text>
              </div>
            </Link>
          </List.Item>
        )}
      />
      <Link className={styles.bottomLink} href="/teaching">
        查看全部研究成果 →
      </Link>
    </Card>
  );
}
