"use client";

import L, { type CircleMarker, type Map as LeafletMap } from "leaflet";
import { useEffect, useMemo, useRef, useState } from "react";
import { CHONGQING_CENTER, createTileLayer } from "@/components/map/leaflet-config";
import type { PublicDigitalLifeStageApi } from "@/lib/digital-life";
import styles from "./page.module.css";

type StageMarker = {
  index: number;
  marker: CircleMarker;
};

type Props = {
  stages: PublicDigitalLifeStageApi[];
  currentStageIndex: number;
  onStageSelect: (index: number) => void;
};

const AUDIT_STATUS_META: Record<string, { label: string; className: string }> = {
  draft: { label: "草稿", className: styles.mapPopupStatusDraft },
  草稿: { label: "草稿", className: styles.mapPopupStatusDraft },
  submitted: { label: "待审核", className: styles.mapPopupStatusSubmitted },
  待审核: { label: "待审核", className: styles.mapPopupStatusSubmitted },
  approved: { label: "已通过", className: styles.mapPopupStatusApproved },
  已通过: { label: "已通过", className: styles.mapPopupStatusApproved },
  rejected: { label: "已驳回", className: styles.mapPopupStatusRejected },
  已驳回: { label: "已驳回", className: styles.mapPopupStatusRejected },
};

function escapeHtml(value?: string) {
  return (value || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function formatDateTime(value?: string) {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? ""
    : date.toLocaleString("zh-CN", {
        year: "numeric",
        month: "numeric",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      });
}

function displayText(value?: string) {
  const normalized = value?.trim();
  return normalized && !/^\d+(?:\.\d+)?$/.test(normalized) ? normalized : "";
}

function displayGrowthStage(value?: string) {
  const normalized = displayText(value);
  if (!normalized || normalized.length > 16) return "";
  if (/^(?:test|testing|测试|未知|无|待完善|null|undefined)$/i.test(normalized)) return "";
  return normalized;
}

function createPopupContent(stage: PublicDigitalLifeStageApi) {
  const growthStage = displayGrowthStage(stage.growthStage);
  const stageTitle = growthStage
    ? `第 ${stage.sequence} 阶段 · ${growthStage}`
    : `第 ${stage.sequence} 阶段`;
  const collectedAt = formatDateTime(stage.collectedAt);
  const baseName = displayText(stage.baseName);
  const locationName = displayText(stage.locationName);
  const collectorName = stage.collectorName?.trim() || "采集员未知";
  const imageCount = stage.images?.length;
  const auditStatus = AUDIT_STATUS_META[stage.auditStatus || ""];

  return `<article class="${styles.mapPopupBody}">
    <header class="${styles.mapPopupHeader}">
      <div class="${styles.mapPopupTitleRow}">
        <span class="${styles.mapPopupSequence}">${String(stage.sequence).padStart(2, "0")}</span>
        <strong>${escapeHtml(stageTitle)}</strong>
      </div>
      ${
        collectedAt || auditStatus
          ? `<div class="${styles.mapPopupMeta}">
              ${collectedAt ? `<time>${escapeHtml(collectedAt)}</time>` : "<span></span>"}
              ${auditStatus ? `<span class="${styles.mapPopupStatus} ${auditStatus.className}">${auditStatus.label}</span>` : ""}
            </div>`
          : ""
      }
    </header>
    <div class="${styles.mapPopupPlace}">
      ${baseName ? `<strong>${escapeHtml(baseName)}</strong>` : ""}
      ${locationName ? `<span class="${baseName ? "" : styles.mapPopupPlacePrimary}">${escapeHtml(locationName)}</span>` : ""}
      ${!baseName && !locationName ? "<span>暂无地点信息</span>" : ""}
    </div>
    <footer class="${styles.mapPopupFooter}">
      <span>${escapeHtml(collectorName)}</span>
      ${imageCount ? `<span>现场影像 ${imageCount} 张</span>` : ""}
    </footer>
  </article>`;
}

function getCoordinates(stage?: PublicDigitalLifeStageApi): [number, number] | undefined {
  if (stage?.latitude == null || stage.longitude == null) return undefined;
  const latitude = Number(stage.latitude);
  const longitude = Number(stage.longitude);
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return undefined;
  if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return undefined;
  return [latitude, longitude];
}

function hasCompleteData(stage: PublicDigitalLifeStageApi) {
  if (stage.dataStatus) return stage.dataStatus === "complete";
  return Boolean(stage.metrics && stage.images?.length);
}

export default function DigitalLifeStageMap({ stages, currentStageIndex, onStageSelect }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const markerLayerRef = useRef<L.LayerGroup | null>(null);
  const markersRef = useRef<StageMarker[]>([]);
  const [mapReady, setMapReady] = useState(false);
  const currentStage = stages[currentStageIndex];
  const currentCoordinates = useMemo(() => getCoordinates(currentStage), [currentStage]);
  const locatedStages = useMemo(
    () =>
      stages.flatMap((stage, index) => {
        const coordinates = getCoordinates(stage);
        return coordinates ? [{ stage, index, coordinates }] : [];
      }),
    [stages],
  );
  const allAtSameLocation =
    locatedStages.length > 1 &&
    new Set(locatedStages.map(({ coordinates }) => coordinates.join(","))).size === 1;

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;
    const map = L.map(containerRef.current, {
      center: CHONGQING_CENTER,
      zoom: 9,
      zoomControl: true,
    });
    createTileLayer("standard").addTo(map);
    markerLayerRef.current = L.layerGroup().addTo(map);
    mapRef.current = map;
    setMapReady(true);
    window.setTimeout(() => map.invalidateSize({ animate: false }), 0);

    return () => {
      map.remove();
      mapRef.current = null;
      markerLayerRef.current = null;
      markersRef.current = [];
    };
  }, []);

  useEffect(() => {
    const map = mapRef.current;
    const layer = markerLayerRef.current;
    if (!mapReady || !map || !layer) return;
    layer.clearLayers();
    markersRef.current = locatedStages.map(({ stage, index, coordinates }) => {
      const complete = hasCompleteData(stage);
      const marker = L.circleMarker(coordinates, {
        radius: 7,
        color: complete ? "#4f7f5c" : "#8a928b",
        fillColor: complete ? "#b9d5bd" : "#c8ccc8",
        fillOpacity: 0.92,
        weight: 2,
      }).addTo(layer);
      marker.bindPopup(createPopupContent(stage), {
        className: `${styles.mapPopup} digital-life-stage-popup`,
        closeButton: false,
        maxWidth: 260,
        offset: L.point(0, -10),
        keepInView: true,
        autoPanPaddingTopLeft: L.point(54, 22),
        autoPanPaddingBottomRight: L.point(22, 22),
      });
      marker.on("click", () => onStageSelect(index));
      return { index, marker };
    });

    if (locatedStages.length > 1 && !allAtSameLocation) {
      map.fitBounds(L.latLngBounds(locatedStages.map(({ coordinates }) => coordinates)), {
        animate: false,
        maxZoom: 13,
        padding: [34, 34],
      });
    } else if (locatedStages[0]) {
      map.setView(locatedStages[0].coordinates, 12, { animate: false });
    }
  }, [allAtSameLocation, locatedStages, mapReady, onStageSelect]);

  useEffect(() => {
    const map = mapRef.current;
    if (!mapReady || !map) return;
    markersRef.current.forEach(({ index, marker }) => {
      const active = index === currentStageIndex;
      const complete = hasCompleteData(stages[index]);
      marker.setRadius(active ? 10 : 7);
      marker.setStyle({
        color: active ? "#0f5132" : complete ? "#4f7f5c" : "#8a928b",
        fillColor: active ? "#0f5132" : complete ? "#b9d5bd" : "#c8ccc8",
        fillOpacity: active ? 1 : 0.92,
        weight: active ? 4 : 2,
      });
      marker.getElement()?.classList.toggle(styles.activeMapMarker, active);
      if (active) marker.bringToFront();
    });

    const activeMarker = markersRef.current.find(
      ({ index }) => index === currentStageIndex,
    )?.marker;
    if (currentCoordinates && activeMarker) {
      map.flyTo(currentCoordinates, Math.max(map.getZoom(), 12), { animate: true, duration: 0.65 });
      activeMarker.openPopup();
    } else {
      map.closePopup();
    }
  }, [currentCoordinates, currentStageIndex, mapReady, stages]);

  return (
    <div className={styles.mapViewport}>
      <div ref={containerRef} className={styles.mapCanvas} aria-label="数字生命阶段位置地图" />
      {!locatedStages.length ? (
        <div className={styles.mapNotice}>本档案暂无精确坐标，未生成地图标记。</div>
      ) : !currentCoordinates ? (
        <div className={styles.mapNotice}>当前阶段暂无精确坐标，已保留地点文本信息。</div>
      ) : allAtSameLocation ? (
        <div className={styles.mapHint}>本档案所有观测均来自同一基地。</div>
      ) : null}
    </div>
  );
}
