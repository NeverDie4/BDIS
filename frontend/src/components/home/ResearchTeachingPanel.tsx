"use client";

import { Button, Card, List, Space, Tag, Typography } from "antd";
import Link from "next/link";
import type { CourseApi } from "@/lib/courses";
import styles from "./HomeOverviewGrid.module.css";

export function ResearchTeachingPanel({ courses }: { courses: CourseApi[] }) {
  return (
    <Card className={`${styles.overviewCard} ${styles.paperPanel}`} extra={<Button className={styles.moreButton} type="link"><Link href="/teaching">更多 →</Link></Button>} title="最新实验课程" variant="borderless">
      <List
        className={styles.newsList}
        dataSource={courses}
        locale={{ emptyText: "暂无可见课程" }}
        renderItem={(course) => <List.Item className={styles.newsItem}>
          <Link className={styles.newsLink} href="/teaching">
            <div className={styles.newsCover}><span>课</span></div>
            <div className={styles.newsContent}>
              <Space size={6}><Tag className={styles.paperTag}>{course.courseType || "实验课程"}</Tag><Typography.Text className={styles.newsDate}>{course.publishStatus}</Typography.Text></Space>
              <Typography.Title className={styles.newsTitle} level={4}>{course.courseName}</Typography.Title>
              <Typography.Text className={styles.newsSource}>{course.teacherName || "授课教师未配置"}</Typography.Text>
            </div>
          </Link>
        </List.Item>}
      />
      <Link className={styles.bottomLink} href="/teaching">查看全部课程 →</Link>
    </Card>
  );
}
