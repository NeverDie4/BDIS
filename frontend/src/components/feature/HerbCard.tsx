"use client";

import { Button, Card, Space, Tag, Typography } from "antd";
import { ImageThumb } from "@/components/common/ImageThumb";
import styles from "./HerbCard.module.css";

type HerbCardAction = {
  label: string;
  onClick?: () => void;
};

type HerbCardProps = {
  name: string;
  alias?: string;
  medicinalPart?: string;
  efficacy?: string;
  image?: string;
  tags?: string[];
  actions?: HerbCardAction[];
  className?: string;
};

export function HerbCard({
  name,
  alias,
  medicinalPart,
  efficacy,
  image,
  tags = [],
  actions = [],
  className,
}: HerbCardProps) {
  return (
    <Card className={`${styles.card} ${className ?? ""}`} variant="borderless">
      <div className={styles.content}>
        <ImageThumb alt={`${name}标本图`} className={styles.image} shape="rounded" size={96} src={image} />
        <div className={styles.copy}>
          <Space className={styles.titleLine} align="start" wrap>
            <div>
              <Typography.Title level={3}>{name}</Typography.Title>
              {alias ? <Typography.Text className={styles.alias}>别名：{alias}</Typography.Text> : null}
            </div>
          </Space>
          <div className={styles.meta}>
            {medicinalPart ? <span>药用部位：{medicinalPart}</span> : null}
            {efficacy ? <span>功效：{efficacy}</span> : null}
          </div>
          {tags.length > 0 ? (
            <Space size={[6, 6]} wrap>
              {tags.map((tag) => (
                <Tag className={styles.tag} key={tag}>
                  {tag}
                </Tag>
              ))}
            </Space>
          ) : null}
        </div>
      </div>

      {actions.length > 0 ? (
        <Space className={styles.actions} size={8} wrap>
          {actions.map((action) => (
            <Button key={action.label} type="link" onClick={action.onClick}>
              {action.label}
            </Button>
          ))}
        </Space>
      ) : null}
    </Card>
  );
}
