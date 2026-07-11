"use client";

import { Typography } from "antd";
import styles from "./ChongqingMapPanel.module.css";

export type ChongqingMapMarker = {
  id: string | number;
  name: string;
  district: string;
  herbName?: string;
  x: number;
  y: number;
  status?: "normal" | "pending" | "warning";
};

type ChongqingMapPanelProps = {
  title?: string;
  description?: string;
  markers: ChongqingMapMarker[];
  selectedId?: string | number;
  onMarkerClick?: (marker: ChongqingMapMarker) => void;
  className?: string;
};

export function ChongqingMapPanel({
  title = "重庆中药材分布示意",
  description = "静态 SVG 占位地图，后续可替换为 Leaflet 图层。",
  markers,
  selectedId,
  onMarkerClick,
  className,
}: ChongqingMapPanelProps) {
  return (
    <section className={`${styles.panel} ${className ?? ""}`}>
      <div className={styles.copy}>
        <Typography.Title level={2}>{title}</Typography.Title>
        <Typography.Paragraph>{description}</Typography.Paragraph>
      </div>

      <div className={styles.mapArea}>
        <svg className={styles.mapShape} viewBox="0 0 720 420" role="img" aria-label="重庆中药材分布静态地图">
          <path d="M82 260 C124 166 214 128 318 150 C380 88 490 96 548 154 C640 158 678 230 624 306 C560 396 438 350 354 374 C244 404 150 366 82 260 Z" />
          <path d="M172 276 C236 238 270 256 326 204 C384 150 470 166 540 126" />
          <path d="M138 316 C226 290 302 310 382 262 C468 210 548 228 616 190" />
        </svg>

        {markers.map((marker) => (
          <button
            aria-label={`查看${marker.district}${marker.name}`}
            className={`${styles.marker} ${styles[marker.status ?? "normal"]} ${
              selectedId === marker.id ? styles.active : ""
            }`}
            key={marker.id}
            style={{ left: `${marker.x}%`, top: `${marker.y}%` }}
            type="button"
            onClick={() => onMarkerClick?.(marker)}
          >
            <span className={styles.dot} />
            <span className={styles.markerLabel}>{marker.district}</span>
          </button>
        ))}
      </div>
    </section>
  );
}
