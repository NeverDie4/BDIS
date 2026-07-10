"use client";

import { Tooltip } from "antd";
import { Minus, Plus, RotateCcw } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { abstractChongqingRegions, type AbstractRegion } from "./abstractChongqingMapData";
import styles from "./AbstractChongqingMap.module.css";

const legendItems = [
  { label: "资源丰富", color: "#5f8f55" },
  { label: "资源较多", color: "#8fac78" },
  { label: "资源一般", color: "#c8d6ad" },
  { label: "资源不足", color: "#e7ecda" },
];

const MIN_ZOOM = 0.8;
const MAX_ZOOM = 2.25;
const ZOOM_STEP = 0.15;

export function getRegionColor(count: number) {
  if (count >= 45) return "#5f8f55";
  if (count >= 30) return "#8fac78";
  if (count >= 15) return "#c8d6ad";
  return "#e7ecda";
}

type AbstractChongqingMapProps = {
  regions?: AbstractRegion[];
};

export function AbstractChongqingMap({ regions = abstractChongqingRegions }: AbstractChongqingMapProps) {
  const viewportRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef({ active: false, x: 0, y: 0, startX: 0, startY: 0 });
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });

  const getDefaultZoom = useCallback(() => {
    const width = viewportRef.current?.clientWidth ?? 640;
    return width < 460 ? 1.3 : width < 600 ? 1.12 : 1;
  }, []);

  const resetView = useCallback(() => {
    setZoom(getDefaultZoom());
    setPan({ x: 0, y: 0 });
  }, [getDefaultZoom]);

  useEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport) return;

    const resizeObserver = new ResizeObserver(() => {
      setZoom((current) => (current === 1 ? getDefaultZoom() : current));
    });
    resizeObserver.observe(viewport);
    resetView();
    return () => resizeObserver.disconnect();
  }, [getDefaultZoom, resetView]);

  const updateZoom = (delta: number) => {
    setZoom((current) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, Number((current + delta).toFixed(2)))));
  };

  const handleWheel = (event: React.WheelEvent<HTMLDivElement>) => {
    event.preventDefault();
    updateZoom(event.deltaY > 0 ? -ZOOM_STEP : ZOOM_STEP);
  };

  const handlePointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    dragRef.current = {
      active: true,
      x: pan.x,
      y: pan.y,
      startX: event.clientX,
      startY: event.clientY,
    };
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!dragRef.current.active) return;
    setPan({
      x: dragRef.current.x + event.clientX - dragRef.current.startX,
      y: dragRef.current.y + event.clientY - dragRef.current.startY,
    });
  };

  const handlePointerUp = (event: React.PointerEvent<HTMLDivElement>) => {
    dragRef.current.active = false;
    event.currentTarget.releasePointerCapture(event.pointerId);
  };

  return (
    <div className={styles.mapPanel}>
      <div className={styles.mapToolbar}>
        <span className={styles.mapHint}>滚轮缩放 · 拖拽查看</span>
        <div className={styles.zoomControls} aria-label="地图缩放控制">
          <button aria-label="缩小地图" className={styles.zoomButton} type="button" onClick={() => updateZoom(-ZOOM_STEP)}>
            <Minus size={14} strokeWidth={2.2} />
          </button>
          <span className={styles.zoomValue}>{Math.round(zoom * 100)}%</span>
          <button aria-label="放大地图" className={styles.zoomButton} type="button" onClick={() => updateZoom(ZOOM_STEP)}>
            <Plus size={14} strokeWidth={2.2} />
          </button>
          <button aria-label="重置地图视图" className={styles.resetButton} type="button" onClick={resetView}>
            <RotateCcw size={13} strokeWidth={2.2} />
          </button>
        </div>
      </div>

      <div
        ref={viewportRef}
        aria-label="重庆市中药材资源矩形热力图"
        className={styles.mapViewport}
        role="img"
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
        onWheel={handleWheel}
      >
        <div
          className={styles.mapScene}
          style={{ transform: `translate(calc(-50% + ${pan.x}px), calc(-50% + ${pan.y}px)) scale(${zoom})` }}
        >
          <div aria-label="药材资源分布图例" className={styles.legend}>
            {legendItems.map((item) => (
              <span className={styles.legendItem} key={item.label}>
                <span className={styles.legendSwatch} style={{ backgroundColor: item.color }} />
                {item.label}
              </span>
            ))}
          </div>

          <div className={styles.mapGrid}>
            {regions.map((region) => (
              <Tooltip key={region.name} title={`${region.name}：${region.count} 处资源点`}>
                <button
                  aria-label={`${region.name}：${region.count} 处资源点`}
                  className={styles.regionCell}
                  style={{
                    backgroundColor: getRegionColor(region.count),
                    gridColumn: `${region.col} / span ${region.colSpan ?? 1}`,
                    gridRow: region.row,
                  }}
                  type="button"
                >
                  <span className={styles.regionName}>{region.shortName}</span>
                  <span className={styles.regionCount}>{region.count}</span>
                </button>
              </Tooltip>
            ))}
          </div>
        </div>
      </div>

      <p className={styles.mapNote}>重庆市中药材资源分布（示意）</p>
    </div>
  );
}
