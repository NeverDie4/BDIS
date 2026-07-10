"use client";

import { Typography } from "antd";
import { ImageThumb } from "@/components/common/ImageThumb";
import styles from "./HerbImageStrip.module.css";

type HerbStripItem = {
  id: string | number;
  name: string;
  image?: string;
  subtitle?: string;
};

type HerbImageStripProps = {
  title?: string;
  items: HerbStripItem[];
  onItemClick?: (item: HerbStripItem) => void;
  className?: string;
};

export function HerbImageStrip({ title = "本草标本陈列", items, onItemClick, className }: HerbImageStripProps) {
  return (
    <section className={`${styles.strip} ${className ?? ""}`}>
      <div className={styles.header}>
        <Typography.Title level={2}>{title}</Typography.Title>
        <Typography.Text>Herbarium Display</Typography.Text>
      </div>
      <div className={styles.scroller}>
        {items.map((item) => (
          <button
            className={styles.item}
            key={item.id}
            type="button"
            onClick={() => onItemClick?.(item)}
          >
            <ImageThumb alt={`${item.name}标本图`} shape="rounded" size={112} src={item.image} />
            <strong>{item.name}</strong>
            {item.subtitle ? <span>{item.subtitle}</span> : null}
          </button>
        ))}
      </div>
    </section>
  );
}
