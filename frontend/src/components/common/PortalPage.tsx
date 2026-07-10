"use client";

import { Card, Typography } from "antd";
import { PageHeader } from "@/components/layout/PageHeader";
import styles from "./PortalPage.module.css";

type PortalPageProps = {
  eyebrow: string;
  title: string;
  description: string;
  tags?: string[];
  cards: Array<{
    title: string;
    description: string;
  }>;
};

export function PortalPage({ eyebrow, title, description, tags = [], cards }: PortalPageProps) {
  return (
    <section className={styles.page}>
      <PageHeader eyebrow={eyebrow} title={title} description={description} tags={tags} />

      <div className={styles.cardGrid}>
        {cards.map((card) => (
          <Card className={styles.card} key={card.title} bordered={false}>
            <Typography.Title level={3}>{card.title}</Typography.Title>
            <Typography.Paragraph>{card.description}</Typography.Paragraph>
          </Card>
        ))}
      </div>
    </section>
  );
}
