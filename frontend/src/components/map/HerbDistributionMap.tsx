"use client";

import { App, Button, Empty, Form, Input, InputNumber, Modal, Select, Slider, Spin, Tag } from "antd";
import L, { type LatLng, type Map as LeafletMap } from "leaflet";
import { Building2, ChevronDown, ChevronLeft, ChevronRight, ChevronUp, CirclePause, Download, Layers, ListOrdered, LocateFixed, MapPinned, Maximize2, Minimize2, Minus, Navigation, Pencil, Plus, RefreshCw, Route, Ruler, Search, Sparkles, Sprout, Trash2, Warehouse, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import { escapeCsvCell } from "@/lib/csv";
import { fetchDictionaryOptions, type DictionaryOption } from "@/lib/dictionaries";
import {
  createMapPoint,
  createGrowthRecord,
  deleteMapPoint,
  fetchMapPoints,
  fetchGrowthRecords,
  getMapPointRequestErrorMessage,
  type GrowthRecord,
  type GrowthRecordPayload,
  type MapPoint,
  type MapPointPayload,
  updateMapPoint,
  updateMapPointStatus,
} from "@/lib/map-points";
import { useAuthStore } from "@/stores/auth-store";
import { HerbPointFormModal } from "./HerbPointFormModal";
import {
  CHONGQING_CENTER,
  MAP_LAYERS,
  createPreviewTileLayer,
  createTileLayer,
  getPreviewLayerKey,
  type MapLayerKey,
} from "./leaflet-config";
import styles from "./HerbDistributionMap.module.css";

interface MeasurePoint {
  lat: number;
  lng: number;
}

type FormMode = "create" | "edit";
type GrowthCreateFormValues = Omit<GrowthRecordPayload, "collectorName" | "dataSource">;

interface RouteOrigin extends MeasurePoint {
  label: string;
}

interface RoutePlan {
  distanceMeters: number;
  durationSeconds: number;
  geometry: [number, number][];
  roadRoute: boolean;
  steps: RouteNavigationStep[];
}

interface RouteNavigationStep {
  instruction: string;
  location: [number, number];
  routeIndex: number;
}

interface NavigationPosition extends MeasurePoint {
  heading?: number;
  speed?: number;
  accuracy?: number;
}

type SuitabilityLevel = "high" | "medium" | "low" | "insufficient";

interface SuitabilitySnapshot {
  point: MapPoint;
  score?: number;
  level: SuitabilityLevel;
  altitude?: number;
  nearbyPointCount: number;
  recordCount: number;
  recentStage: string;
}

function escapeHtml(value?: string) {
  return (value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function createHerbIcon(point: MapPoint) {
  const image = escapeHtml(point.coverImageUrl);
  const imageMarkup = image
    ? `<img src="${image}" class="bdis-herb-marker-img" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'" />`
    : "";
  const placeholderDisplay = image ? "none" : "flex";

  return L.divIcon({
    className: "bdis-herb-marker",
    html: `
      <div class="bdis-herb-marker-wrap">
        <div class="bdis-herb-marker-avatar">
          ${imageMarkup}
          <div class="bdis-herb-marker-placeholder" style="display:${placeholderDisplay}">药</div>
        </div>
        <div class="bdis-herb-marker-label">${escapeHtml(point.herbName)}</div>
      </div>
    `,
    iconSize: [82, 96],
    iconAnchor: [41, 96],
    popupAnchor: [0, -96],
  });
}

function bearing(p1: MeasurePoint, p2: MeasurePoint) {
  const lat1 = (p1.lat * Math.PI) / 180;
  const lat2 = (p2.lat * Math.PI) / 180;
  const dlng = ((p2.lng - p1.lng) * Math.PI) / 180;
  const y = Math.sin(dlng) * Math.cos(lat2);
  const x =
    Math.cos(lat1) * Math.sin(lat2) -
    Math.sin(lat1) * Math.cos(lat2) * Math.cos(dlng);
  return ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360;
}

function destination(point: MeasurePoint, distanceMeters: number, bearingDeg: number) {
  const radius = 6371000;
  const brng = (bearingDeg * Math.PI) / 180;
  const lat1 = (point.lat * Math.PI) / 180;
  const lng1 = (point.lng * Math.PI) / 180;
  const dR = distanceMeters / radius;
  const lat2 = Math.asin(
    Math.sin(lat1) * Math.cos(dR) + Math.cos(lat1) * Math.sin(dR) * Math.cos(brng),
  );
  const lng2 =
    lng1 +
    Math.atan2(
      Math.sin(brng) * Math.sin(dR) * Math.cos(lat1),
      Math.cos(dR) - Math.sin(lat1) * Math.sin(lat2),
    );
  return { lat: (lat2 * 180) / Math.PI, lng: (lng2 * 180) / Math.PI };
}

function interpolate(p1: MeasurePoint, p2: MeasurePoint, t: number) {
  return {
    lat: p1.lat + t * (p2.lat - p1.lat),
    lng: p1.lng + t * (p2.lng - p1.lng),
  };
}

function getTickGeoSize(zoom: number) {
  return (156543 / Math.pow(2, zoom)) * 4;
}

function getTickInterval(zoom: number) {
  if (zoom <= 8) return 20000;
  if (zoom <= 10) return 10000;
  if (zoom <= 12) return 5000;
  if (zoom <= 13) return 2000;
  if (zoom <= 15) return 500;
  if (zoom <= 17) return 200;
  return 100;
}

function getNorthwardPerpendicular(brng: number) {
  const p1 = (brng + 90) % 360;
  const p2 = (brng + 270) % 360;
  const d1 = Math.min(Math.abs(p1), 360 - Math.abs(p1));
  const d2 = Math.min(Math.abs(p2), 360 - Math.abs(p2));
  return d1 <= d2 ? p1 : p2;
}

function formatDistance(meters: number) {
  return meters >= 1000 ? `${(meters / 1000).toFixed(2)} km` : `${Math.round(meters)} m`;
}

function formatTickDistance(meters: number, useKm: boolean) {
  return useKm ? `${(meters / 1000).toFixed(2)} km` : `${Math.round(meters)} m`;
}

function formatDateTime(value?: string) {
  if (!value) {
    return "暂无";
  }
  return value.replace("T", " ").slice(0, 16);
}

function growthStageTone(stage?: string) {
  const value = stage?.toLowerCase() ?? "";
  if (value.includes("成熟") || value.includes("采收") || value.includes("mature")) return "mature";
  if (value.includes("开花") || value.includes("flower")) return "flowering";
  if (value.includes("幼") || value.includes("苗") || value.includes("seed")) return "seedling";
  return "growing";
}

function getSuitabilityLevel(score: number): SuitabilityLevel {
  if (score >= 75) return "high";
  if (score >= 52) return "medium";
  return "low";
}

function suitabilityColor(level: SuitabilityLevel) {
  if (level === "high") return "#16a34a";
  if (level === "medium") return "#eab308";
  if (level === "insufficient") return "#94a3b8";
  return "#f97316";
}

function stageSuitabilityScore(stage?: string) {
  const tone = growthStageTone(stage);
  if (tone === "mature") return 100;
  if (tone === "flowering") return 86;
  if (tone === "growing") return stage ? 70 : 48;
  return 58;
}

function median(values: number[]) {
  const sorted = [...values].sort((left, right) => left - right);
  const middle = Math.floor(sorted.length / 2);
  return sorted.length % 2 === 0 ? (sorted[middle - 1] + sorted[middle]) / 2 : sorted[middle];
}

function getDistributionTypeLabel(value?: string) {
  if (value === "cultivated") return "人工种植";
  if (value === "wild") return "野生分布";
  if (value === "specimen") return "标本点位";
  return value || "";
}

function formatPointNo(id: number) {
  return `DP${String(id).padStart(6, "0")}`;
}

function createLabelIcon(className: string, text: string) {
  return L.divIcon({
    className,
    html: `<span>${escapeHtml(text)}</span>`,
    iconSize: [0, 0],
    iconAnchor: [0, 0],
  });
}

function createWaypointIcon(index: number, isSatellite: boolean) {
  return L.divIcon({
    className: isSatellite ? "bdis-measure-waypoint bdis-measure-waypoint-sat" : "bdis-measure-waypoint",
    html: `<span>${index}</span>`,
    iconSize: [26, 26],
    iconAnchor: [13, 13],
  });
}

function createRouteStopIcon(index: number, isCurrentStop = false) {
  return L.divIcon({
    className: `bdis-route-stop-marker ${isCurrentStop ? "bdis-route-stop-current" : ""}`,
    html: `<span>${index}</span>`,
    iconSize: [30, 30],
    iconAnchor: [15, 15],
    popupAnchor: [0, -14],
  });
}

function createNavigationIcon(heading?: number) {
  return L.divIcon({
    className: "bdis-navigation-marker",
    html: `<span style="transform: rotate(${Math.round(heading ?? 0)}deg)"></span>`,
    iconSize: [42, 42],
    iconAnchor: [21, 21],
  });
}

function pointDistance(from: MeasurePoint, to: MeasurePoint) {
  return L.latLng(from.lat, from.lng).distanceTo(L.latLng(to.lat, to.lng));
}

function closestRouteIndex(geometry: [number, number][], position: MeasurePoint) {
  let closestIndex = 0;
  let closestDistance = Number.POSITIVE_INFINITY;
  geometry.forEach(([lat, lng], index) => {
    const distance = pointDistance(position, { lat, lng });
    if (distance < closestDistance) {
      closestDistance = distance;
      closestIndex = index;
    }
  });
  return { index: closestIndex, distance: closestDistance };
}

function remainingRouteDistance(geometry: [number, number][], startIndex: number) {
  return geometry.slice(startIndex, -1).reduce(
    (sum, coordinate, index) =>
      sum + pointDistance({ lat: coordinate[0], lng: coordinate[1] }, { lat: geometry[startIndex + index + 1][0], lng: geometry[startIndex + index + 1][1] }),
    0,
  );
}

function optimizeRouteOrder(points: MapPoint[], origin?: RouteOrigin) {
  if (points.length < 2) {
    return points;
  }
  const remaining = [...points];
  const ordered: MapPoint[] = [];
  let current: MeasurePoint;
  if (origin) {
    current = origin;
  } else {
    const first = remaining.shift();
    if (!first) return ordered;
    ordered.push(first);
    current = { lat: first.latitude, lng: first.longitude };
  }
  while (remaining.length > 0) {
    let nearestIndex = 0;
    let nearestDistance = Number.POSITIVE_INFINITY;
    remaining.forEach((point, index) => {
      const distance = pointDistance(current, { lat: point.latitude, lng: point.longitude });
      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearestIndex = index;
      }
    });
    const next = remaining.splice(nearestIndex, 1)[0];
    ordered.push(next);
    current = { lat: next.latitude, lng: next.longitude };
  }
  return ordered;
}

function buildPopupContent(
  point: MapPoint,
  onEdit: (point: MapPoint) => void,
  onDelete: (point: MapPoint) => void,
) {
  const content = document.createElement("div");
  content.className = "bdis-popup-card";
  content.innerHTML = `
    <div class="bdis-popup-header">
      ${
        point.coverImageUrl
          ? `<img src="${escapeHtml(point.coverImageUrl)}" alt="${escapeHtml(point.herbName)}" class="bdis-popup-img" />`
          : `<div class="bdis-popup-img-placeholder">药</div>`
      }
      <div>
        <h3 class="bdis-popup-title">${escapeHtml(point.herbName)}</h3>
        <p class="bdis-popup-subtitle">${escapeHtml(point.locationName || point.district || "地图点位")}</p>
      </div>
    </div>
    <div class="bdis-popup-body">
      <div class="bdis-popup-row"><span>经纬度</span><strong>${point.latitude.toFixed(6)}, ${point.longitude.toFixed(6)}</strong></div>
      ${
        point.address
          ? `<div class="bdis-popup-row"><span>地址</span><strong>${escapeHtml(point.address)}</strong></div>`
          : ""
      }
      ${
        point.baseName
          ? `<div class="bdis-popup-row"><span>基地</span><strong>${escapeHtml(point.baseName)}</strong></div>`
          : ""
      }
      ${
        point.efficacy
          ? `<p class="bdis-popup-desc">${escapeHtml(point.efficacy)}</p>`
          : point.distributionDesc
            ? `<p class="bdis-popup-desc">${escapeHtml(point.distributionDesc)}</p>`
            : ""
      }
    </div>
    <div class="bdis-popup-actions">
      <button type="button" data-action="edit">编辑</button>
      <button type="button" data-action="delete">删除</button>
    </div>
  `;
  content.querySelector('[data-action="edit"]')?.addEventListener("click", () => onEdit(point));
  content.querySelector('[data-action="delete"]')?.addEventListener("click", () => onDelete(point));
  return content;
}

export function HerbDistributionMap() {
  const { message, modal } = App.useApp();
  const currentUser = useAuthStore((state) => state.user);
  const hasPermission = useAuthStore((state) => state.hasPermission);
  const canManagePoints = hasPermission("map:point:update");
  const [growthCreateForm] = Form.useForm<GrowthCreateFormValues>();
  const [points, setPoints] = useState<MapPoint[]>([]);
  const [keyword, setKeyword] = useState("");
  const [herbFilter, setHerbFilter] = useState<string>();
  const [districtFilter, setDistrictFilter] = useState<string>();
  const [baseFilter, setBaseFilter] = useState<string>();
  const [sourceFilter, setSourceFilter] = useState<string>();
  const [loading, setLoading] = useState(false);
  const [statusUpdatingId, setStatusUpdatingId] = useState<number>();
  const [measuring, setMeasuring] = useState(false);
  const [measurePoints, setMeasurePoints] = useState<MeasurePoint[]>([]);
  const [activeLayer, setActiveLayer] = useState<MapLayerKey>("standard");
  const [mapZoom, setMapZoom] = useState(9);
  const [mapReady, setMapReady] = useState(false);
  const [mapFullscreen, setMapFullscreen] = useState(false);
  const [fullscreenTop, setFullscreenTop] = useState(78);
  const [formOpen, setFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<FormMode>("create");
  const [editingPoint, setEditingPoint] = useState<Partial<MapPoint>>();
  const [emptyNoticeVisible, setEmptyNoticeVisible] = useState(true);
  const [selectedPointId, setSelectedPointId] = useState<number>();
  const [growthRecords, setGrowthRecords] = useState<GrowthRecord[]>([]);
  const [growthLoading, setGrowthLoading] = useState(false);
  const [growthSubmitting, setGrowthSubmitting] = useState(false);
  const [growthCreateOpen, setGrowthCreateOpen] = useState(false);
  const [collectionHistoryOpen, setCollectionHistoryOpen] = useState(false);
  const [growthStageOptions, setGrowthStageOptions] = useState<DictionaryOption[]>([]);
  const [soilTypeOptions, setSoilTypeOptions] = useState<DictionaryOption[]>([]);
  const [weatherOptions, setWeatherOptions] = useState<DictionaryOption[]>([]);
  const [suitabilityMode, setSuitabilityMode] = useState(false);
  const [suitabilityCollapsed, setSuitabilityCollapsed] = useState(false);
  const [suitabilityHerb, setSuitabilityHerb] = useState<string>();
  const [suitabilityRadius, setSuitabilityRadius] = useState(18);
  const [suitabilityLoading, setSuitabilityLoading] = useState(false);
  const [suitabilityRecordsByPoint, setSuitabilityRecordsByPoint] = useState<Record<number, GrowthRecord[]>>({});
  const [suitabilityReloadKey, setSuitabilityReloadKey] = useState(0);
  const [routePlanning, setRoutePlanning] = useState(false);
  const [routePointIds, setRoutePointIds] = useState<number[]>([]);
  const [routeOrderedPointIds, setRouteOrderedPointIds] = useState<number[]>([]);
  const [routeOrigin, setRouteOrigin] = useState<RouteOrigin>();
  const [routePlan, setRoutePlan] = useState<RoutePlan>();
  const [routePlanningLoading, setRoutePlanningLoading] = useState(false);
  const [routePlannerCollapsed, setRoutePlannerCollapsed] = useState(false);
  const [routeNavigating, setRouteNavigating] = useState(false);
  const [navigationHudCollapsed, setNavigationHudCollapsed] = useState(false);
  const [navigationPosition, setNavigationPosition] = useState<NavigationPosition>();
  const [navigationStopIndex, setNavigationStopIndex] = useState(0);
  const [navigationRouteIndex, setNavigationRouteIndex] = useState(0);
  const [sidePanelHeight, setSidePanelHeight] = useState<number>();
  const mapElementRef = useRef<HTMLDivElement>(null);
  const miniMapElementRef = useRef<HTMLDivElement>(null);
  const detailPanelRef = useRef<HTMLElement>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const miniMapRef = useRef<LeafletMap | null>(null);
  const baseLayerRef = useRef<L.TileLayer | null>(null);
  const miniBaseLayerRef = useRef<L.TileLayer | null>(null);
  const previewLayerRef = useRef<MapLayerKey>("satellite");
  const markersLayerRef = useRef<L.LayerGroup | null>(null);
  const suitabilityLayerRef = useRef<L.LayerGroup | null>(null);
  const measureLayerRef = useRef<L.LayerGroup | null>(null);
  const routeLayerRef = useRef<L.LayerGroup | null>(null);
  const navigationLayerRef = useRef<L.LayerGroup | null>(null);
  const measuringRef = useRef(false);
  const suitabilityModeRef = useRef(false);
  const routePlanningRef = useRef(false);
  const messageRef = useRef(message);
  const lastRouteRefreshRef = useRef(0);
  const routePlanGeneratorRef = useRef<
    ((origin: RouteOrigin, points: MapPoint[], silent: boolean) => Promise<RoutePlan | undefined>) | undefined
  >(undefined);

  const totalDistance = useMemo(() => {
    if (measurePoints.length < 2) {
      return 0;
    }
    return measurePoints.slice(0, -1).reduce((sum, point, index) => {
      const next = measurePoints[index + 1];
      return sum + L.latLng(point.lat, point.lng).distanceTo(L.latLng(next.lat, next.lng));
    }, 0);
  }, [measurePoints]);

  const districtOptions = useMemo(
    () =>
      Array.from(new Set(points.map((point) => point.district).filter(Boolean))).map((district) => ({
        label: district,
        value: district,
      })),
    [points],
  );

  const herbOptions = useMemo(
    () =>
      Array.from(new Set(points.map((point) => point.herbName).filter(Boolean))).map((herbName) => ({
        label: herbName,
        value: herbName,
      })),
    [points],
  );

  const baseOptions = useMemo(
    () =>
      Array.from(new Set(points.map((point) => point.baseName).filter(Boolean))).map((baseName) => ({
        label: baseName,
        value: baseName,
      })),
    [points],
  );

  const sourceOptions = useMemo(
    () =>
      Array.from(new Set(points.map((point) => point.dataSource).filter(Boolean))).map((source) => ({
        label: source,
        value: source,
      })),
    [points],
  );

  const filteredPoints = useMemo(
    () =>
      points.filter((point) => {
        if (herbFilter && point.herbName !== herbFilter) {
          return false;
        }
        if (districtFilter && point.district !== districtFilter) {
          return false;
        }
        if (baseFilter && point.baseName !== baseFilter) {
          return false;
        }
        if (sourceFilter && point.dataSource !== sourceFilter) {
          return false;
        }
        return true;
      }),
    [baseFilter, districtFilter, herbFilter, points, sourceFilter],
  );

  const suitabilityPoints = useMemo(
    () => filteredPoints.filter((point) => point.herbName === suitabilityHerb && point.status !== 0),
    [filteredPoints, suitabilityHerb],
  );

  const suitabilitySnapshots = useMemo<SuitabilitySnapshot[]>(() => {
    if (!suitabilityMode || suitabilityPoints.length === 0) return [];

    const realAltitudes = suitabilityPoints
      .map((point) => point.altitude)
      .filter((altitude): altitude is number => altitude != null && Number.isFinite(altitude));
    const hasAltitudeReference = realAltitudes.length >= 2;
    const referenceAltitude = hasAltitudeReference ? median(realAltitudes) : undefined;
    const radiusMeters = suitabilityRadius * 1000;
    const densityCounts = suitabilityPoints.map((point) =>
      suitabilityPoints.filter(
        (candidate) =>
          candidate.id !== point.id
          && pointDistance(
            { lat: point.latitude, lng: point.longitude },
            { lat: candidate.latitude, lng: candidate.longitude },
          ) <= radiusMeters,
      ).length,
    );
    const maxDensity = Math.max(...densityCounts, 1);

    return suitabilityPoints.map((point, index) => {
      const records = suitabilityRecordsByPoint[point.id] ?? [];
      const latestRecord = [...records].sort((left, right) =>
        (right.collectedAt ?? "").localeCompare(left.collectedAt ?? ""),
      )[0];
      const hasRealAltitude = point.altitude != null && Number.isFinite(point.altitude);
      const hasGrowthRecord = records.length > 0 && Boolean(latestRecord?.growthStage);
      const sufficient = hasAltitudeReference && hasRealAltitude && suitabilityPoints.length >= 2 && hasGrowthRecord;
      const score = sufficient
        ? Math.round(
            Math.max(0, 1 - Math.abs(point.altitude! - referenceAltitude!) / 650) * 35
              + (densityCounts[index] / maxDensity) * 35
              + stageSuitabilityScore(latestRecord?.growthStage) * 0.3,
          )
        : undefined;
      return {
        point,
        score,
        level: score == null ? "insufficient" : getSuitabilityLevel(score),
        altitude: hasRealAltitude ? Math.round(point.altitude!) : undefined,
        nearbyPointCount: densityCounts[index],
        recordCount: records.length,
        recentStage: latestRecord?.growthStage || "未记录",
      };
    });
  }, [suitabilityMode, suitabilityPoints, suitabilityRadius, suitabilityRecordsByPoint]);

  const suitabilityStatistics = useMemo(() => ({
    high: suitabilitySnapshots.filter((item) => item.level === "high").length,
    medium: suitabilitySnapshots.filter((item) => item.level === "medium").length,
    low: suitabilitySnapshots.filter((item) => item.level === "low").length,
    insufficient: suitabilitySnapshots.filter((item) => item.level === "insufficient").length,
  }), [suitabilitySnapshots]);

  const selectedPoint = useMemo(
    () => filteredPoints.find((point) => point.id === selectedPointId),
    [filteredPoints, selectedPointId],
  );

  const routeSelectedPoints = useMemo(
    () => routePointIds.map((id) => filteredPoints.find((point) => point.id === id)).filter(Boolean) as MapPoint[],
    [filteredPoints, routePointIds],
  );

  const routeOrderedPoints = useMemo(
    () =>
      routeOrderedPointIds
        .map((id) => filteredPoints.find((point) => point.id === id))
        .filter(Boolean) as MapPoint[],
    [filteredPoints, routeOrderedPointIds],
  );

  const navigationStops = routeOrderedPoints.length > 0 ? routeOrderedPoints : routeSelectedPoints;
  const navigationTarget = navigationStops[navigationStopIndex];
  const navigationInstruction = useMemo(
    () => routePlan?.steps.find((step) => step.routeIndex > navigationRouteIndex),
    [navigationRouteIndex, routePlan?.steps],
  );
  const navigationRouteRemainingDistance = useMemo(
    () => {
      if (!routePlan || !navigationPosition) return undefined;
      const closestCoordinate = routePlan.geometry[navigationRouteIndex];
      const approachDistance = closestCoordinate
        ? pointDistance(navigationPosition, { lat: closestCoordinate[0], lng: closestCoordinate[1] })
        : 0;
      return approachDistance + remainingRouteDistance(routePlan.geometry, navigationRouteIndex);
    },
    [navigationPosition, navigationRouteIndex, routePlan],
  );
  const navigationRemainingDistance = useMemo(
    () =>
      navigationPosition && navigationTarget
        ? pointDistance(navigationPosition, { lat: navigationTarget.latitude, lng: navigationTarget.longitude })
        : undefined,
    [navigationPosition, navigationTarget],
  );

  const sortedGrowthRecords = useMemo(
    () =>
      [...growthRecords].sort((left, right) => {
        const leftTime = left.collectedAt ? new Date(left.collectedAt).getTime() : 0;
        const rightTime = right.collectedAt ? new Date(right.collectedAt).getTime() : 0;
        return rightTime - leftTime || right.id - left.id;
      }),
    [growthRecords],
  );

  const mapStatistics = useMemo(
    () => ({
      herbCount: new Set(filteredPoints.map((point) => point.speciesId || point.herbName)).size,
      pointCount: filteredPoints.length,
      baseCount: new Set(filteredPoints.map((point) => point.baseId || point.baseName).filter(Boolean)).size,
      districtCount: new Set(filteredPoints.map((point) => point.district).filter(Boolean)).size,
    }),
    [filteredPoints],
  );

  const activeLayerConfig = useMemo(
    () => MAP_LAYERS.find((layer) => layer.key === activeLayer) ?? MAP_LAYERS[0],
    [activeLayer],
  );
  const previewLayer = useMemo(() => getPreviewLayerKey(activeLayer), [activeLayer]);

  const switchMapLayer = useCallback((layerKey: MapLayerKey) => {
    const map = mapRef.current;
    if (!map) {
      return;
    }

    if (baseLayerRef.current) {
      map.removeLayer(baseLayerRef.current);
    }
    baseLayerRef.current = createTileLayer(layerKey).addTo(map);
    setActiveLayer(layerKey);
  }, []);

  const switchToNextLayer = useCallback(() => {
    const currentIndex = MAP_LAYERS.findIndex((layer) => layer.key === activeLayer);
    const nextLayer = MAP_LAYERS[(currentIndex + 1) % MAP_LAYERS.length];
    switchMapLayer(nextLayer.key);
  }, [activeLayer, switchMapLayer]);

  const zoomIn = useCallback(() => {
    mapRef.current?.zoomIn();
  }, []);

  const zoomOut = useCallback(() => {
    mapRef.current?.zoomOut();
  }, []);

  const locateCurrentPosition = useCallback(() => {
    const map = mapRef.current;
    if (!map || !navigator.geolocation) {
      message.warning("当前浏览器不支持定位");
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const nextCenter: [number, number] = [position.coords.latitude, position.coords.longitude];
        map.flyTo(nextCenter, Math.max(map.getZoom(), 15), { duration: 0.6 });
        L.circleMarker(nextCenter, {
          radius: 8,
          color: "#2563eb",
          fillColor: "#3b82f6",
          fillOpacity: 0.28,
          weight: 2,
        })
          .addTo(map)
          .bindPopup("当前位置")
          .openPopup();
      },
      () => message.error("定位失败，请确认浏览器定位权限已开启"),
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 30000 },
    );
  }, [message]);

  const loadPoints = useCallback(async (searchKeyword: string) => {
    setLoading(true);
    try {
      const normalizedKeyword = searchKeyword.trim();
      const data = await fetchMapPoints({
        keyword: normalizedKeyword || undefined,
        includeDisabled: canManagePoints || undefined,
      });
      setPoints(data);
      setSelectedPointId((current) => current ?? data[0]?.id);
      setEmptyNoticeVisible(true);
    } catch (error) {
      message.error(getMapPointRequestErrorMessage(error, "地图点位加载"));
    } finally {
      setLoading(false);
    }
  }, [canManagePoints, message]);

  const resetMap = useCallback(() => {
    setKeyword("");
    setHerbFilter(undefined);
    setDistrictFilter(undefined);
    setBaseFilter(undefined);
    setSourceFilter(undefined);
    setSelectedPointId(undefined);
    setEmptyNoticeVisible(true);
    setMeasuring(false);
    setMeasurePoints([]);
    setSuitabilityMode(false);
    setSuitabilityCollapsed(false);
    setSuitabilityHerb(undefined);
    setRoutePlanning(false);
    setRoutePointIds([]);
    setRouteOrderedPointIds([]);
    setRouteOrigin(undefined);
    setRoutePlan(undefined);
    setRoutePlannerCollapsed(false);
    setRouteNavigating(false);
    setNavigationHudCollapsed(false);
    setNavigationPosition(undefined);
    setMapZoom(9);
    mapRef.current?.setView(CHONGQING_CENTER, 9, { animate: false });
    if (activeLayer !== "standard") {
      switchMapLayer("standard");
    }

    void loadPoints("");
  }, [activeLayer, loadPoints, switchMapLayer]);

  const toggleRoutePlanning = useCallback(() => {
    setRoutePlanning((current) => !current);
    setMeasuring(false);
    setMeasurePoints([]);
    setSuitabilityMode(false);
    setSuitabilityCollapsed(false);
    setRoutePlan(undefined);
    setRoutePlannerCollapsed(false);
    setRouteNavigating(false);
    setNavigationHudCollapsed(false);
    setNavigationPosition(undefined);
  }, []);

  const toggleSuitabilityMode = useCallback(() => {
    if (suitabilityMode) {
      setSuitabilityMode(false);
      return;
    }
    setMeasuring(false);
    setMeasurePoints([]);
    setRoutePlanning(false);
    setRouteNavigating(false);
    setNavigationHudCollapsed(false);
    setSuitabilityCollapsed(false);
    setSuitabilityHerb((current) =>
      current && filteredPoints.some((point) => point.herbName === current)
        ? current
        : filteredPoints.find((point) => point.status !== 0)?.herbName,
    );
    setSuitabilityReloadKey((current) => current + 1);
    setSuitabilityMode(true);
  }, [filteredPoints, suitabilityMode]);

  const toggleRoutePoint = useCallback((pointId: number) => {
    setRoutePointIds((current) => {
      const next = current.includes(pointId)
        ? current.filter((id) => id !== pointId)
        : [...current, pointId];
      setRouteOrderedPointIds(next);
      return next;
    });
    setRoutePlan(undefined);
  }, []);

  const locateRouteOrigin = useCallback(() => {
    if (!navigator.geolocation) {
      message.warning("当前浏览器不支持定位，路线将从第一个采集点开始");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const origin = {
          lat: position.coords.latitude,
          lng: position.coords.longitude,
          label: "当前位置",
        };
        setRouteOrigin(origin);
        setRoutePlan(undefined);
        mapRef.current?.flyTo([origin.lat, origin.lng], Math.max(mapRef.current.getZoom(), 12));
        message.success("已将当前位置设为路线起点");
      },
      () => message.warning("未能获取当前位置，路线将从第一个采集点开始"),
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 30000 },
    );
  }, [message]);

  const openCreateForm = useCallback((latlng: LatLng) => {
    setFormMode("create");
    setEditingPoint({
      latitude: Number(latlng.lat.toFixed(7)),
      longitude: Number(latlng.lng.toFixed(7)),
      province: "重庆市",
      city: "重庆市",
      distributionType: "cultivated",
      sourceType: "pc",
      dataSource: "map",
    });
    setFormOpen(true);
  }, []);

  const openEditForm = useCallback((point: MapPoint) => {
    setFormMode("edit");
    setEditingPoint(point);
    setFormOpen(true);
  }, []);

  const confirmDelete = useCallback((point: MapPoint) => {
    modal.confirm({
      title: "删除地图点位",
      content: `确定删除“${point.herbName}”这个地图点位吗？`,
      okText: "删除",
      okButtonProps: { danger: true },
      cancelText: "取消",
      async onOk() {
        await deleteMapPoint(point.id);
        setPoints((current) => current.filter((item) => item.id !== point.id));
        message.success("地图点位已删除");
      },
    });
  }, [message, modal]);

  const viewPoint = useCallback((point: MapPoint) => {
    setSelectedPointId(point.id);
    mapRef.current?.flyTo([point.latitude, point.longitude], Math.max(mapRef.current.getZoom(), 11));
  }, []);

  const togglePointStatus = useCallback(async (point: MapPoint) => {
    const nextStatus: 0 | 1 = point.status === 0 ? 1 : 0;
    setStatusUpdatingId(point.id);
    try {
      const next = await updateMapPointStatus(point.id, nextStatus);
      setPoints((current) => current.map((item) => (item.id === next.id ? next : item)));
      message.success(nextStatus === 1 ? "地图点位已启用" : "地图点位已停用");
    } catch (error) {
      message.error(getMapPointRequestErrorMessage(error, nextStatus === 1 ? "地图点位启用" : "地图点位停用"));
    } finally {
      setStatusUpdatingId(undefined);
    }
  }, [message]);

  useEffect(() => {
    measuringRef.current = measuring;
  }, [measuring]);

  useEffect(() => {
    suitabilityModeRef.current = suitabilityMode;
  }, [suitabilityMode]);

  useEffect(() => {
    routePlanningRef.current = routePlanning;
  }, [routePlanning]);

  useEffect(() => {
    messageRef.current = message;
  }, [message]);

  useEffect(() => {
    previewLayerRef.current = previewLayer;
  }, [previewLayer]);

  useEffect(() => {
    if (!mapFullscreen) {
      return;
    }

    const previousOverflow = document.body.style.overflow;
    const syncHeaderHeight = () => {
      const header = document.querySelector("header");
      setFullscreenTop(Math.max(0, Math.round(header?.getBoundingClientRect().bottom ?? 0)));
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMapFullscreen(false);
      }
    };

    syncHeaderHeight();
    document.body.style.overflow = "hidden";
    window.addEventListener("resize", syncHeaderHeight);
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("resize", syncHeaderHeight);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [mapFullscreen]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      mapRef.current?.invalidateSize({ animate: false });
      miniMapRef.current?.invalidateSize({ animate: false });
    }, 80);
    return () => window.clearTimeout(timer);
  }, [mapFullscreen]);

  useEffect(() => {
    const detailPanel = detailPanelRef.current;
    if (!detailPanel || typeof ResizeObserver === "undefined") {
      return;
    }

    const desktopQuery = window.matchMedia("(min-width: 1201px)");
    const syncHeight = () => {
      setSidePanelHeight(desktopQuery.matches ? Math.ceil(detailPanel.getBoundingClientRect().height) : undefined);
    };
    const observer = new ResizeObserver(syncHeight);
    observer.observe(detailPanel);
    desktopQuery.addEventListener("change", syncHeight);
    syncHeight();

    return () => {
      observer.disconnect();
      desktopQuery.removeEventListener("change", syncHeight);
    };
  }, []);

  useEffect(() => {
    const container = mapElementRef.current;
    if (!mapReady || !container || typeof ResizeObserver === "undefined") {
      return;
    }

    const observer = new ResizeObserver(() => {
      mapRef.current?.invalidateSize({ animate: false });
    });
    observer.observe(container);
    return () => observer.disconnect();
  }, [mapReady]);

  useEffect(() => {
    if (!mapElementRef.current || mapRef.current) {
      return;
    }

    const map = L.map(mapElementRef.current, {
      center: CHONGQING_CENTER,
      zoom: 9,
      doubleClickZoom: false,
      zoomControl: false,
    });

    baseLayerRef.current = createTileLayer("standard").addTo(map);
    L.control.scale({ imperial: false, metric: true, position: "bottomright" }).addTo(map);

    markersLayerRef.current = L.layerGroup().addTo(map);
    suitabilityLayerRef.current = L.layerGroup().addTo(map);
    measureLayerRef.current = L.layerGroup().addTo(map);
    routeLayerRef.current = L.layerGroup().addTo(map);
    navigationLayerRef.current = L.layerGroup().addTo(map);
    mapRef.current = map;
    setMapZoom(map.getZoom());
    setMapReady(true);

    map.on("click", (event) => {
      if (measuringRef.current) {
        setMeasurePoints((current) => [...current, { lat: event.latlng.lat, lng: event.latlng.lng }]);
        return;
      }
      if (!suitabilityModeRef.current && !routePlanningRef.current) {
        openCreateForm(event.latlng);
      }
    });
    map.on("dblclick", () => setMeasuring(false));
    map.on("zoomend", () => setMapZoom(map.getZoom()));

    return () => {
      setMapReady(false);
      map.remove();
      mapRef.current = null;
      baseLayerRef.current = null;
      suitabilityLayerRef.current = null;
      routeLayerRef.current = null;
      navigationLayerRef.current = null;
    };
  }, [openCreateForm]);

  useEffect(() => {
    if (!mapReady || !miniMapElementRef.current || miniMapRef.current) {
      return;
    }

    const map = mapRef.current;
    if (!map) {
      return;
    }

    const miniMap = L.map(miniMapElementRef.current, {
      attributionControl: false,
      boxZoom: false,
      center: map.getCenter(),
      doubleClickZoom: false,
      dragging: false,
      keyboard: false,
      scrollWheelZoom: false,
      touchZoom: false,
      zoom: Math.max(map.getZoom() - 4, 3),
      zoomControl: false,
    });

    miniMapRef.current = miniMap;
    miniBaseLayerRef.current = createPreviewTileLayer(previewLayerRef.current).addTo(miniMap);
    window.setTimeout(() => miniMap.invalidateSize(), 0);

    return () => {
      miniMap.remove();
      miniMapRef.current = null;
      miniBaseLayerRef.current = null;
    };
  }, [mapReady, miniMapElementRef]);

  useEffect(() => {
    const miniMap = miniMapRef.current;
    if (!miniMap) {
      return;
    }

    if (miniBaseLayerRef.current) {
      miniMap.removeLayer(miniBaseLayerRef.current);
    }
    miniBaseLayerRef.current = createPreviewTileLayer(previewLayer).addTo(miniMap);
    window.setTimeout(() => miniMap.invalidateSize(), 0);
  }, [previewLayer]);

  useEffect(() => {
    const map = mapRef.current;
    if (!mapReady || !map) {
      return;
    }

    const syncMiniMap = () => {
      const miniMap = miniMapRef.current;
      const container = miniMapElementRef.current;
      if (!miniMap || !container?.isConnected) {
        return;
      }
      miniMap.setView(map.getCenter(), Math.max(map.getZoom() - 4, 3), { animate: false });
    };

    syncMiniMap();
    map.on("moveend zoomend", syncMiniMap);

    return () => {
      map.off("moveend zoomend", syncMiniMap);
    };
  }, [mapReady]);

  useEffect(() => {
    void loadPoints("");
  }, [loadPoints]);

  useEffect(() => {
    if (!suitabilityMode || suitabilityPoints.length === 0) {
      if (suitabilityMode) {
        setSuitabilityRecordsByPoint({});
      }
      return;
    }
    let cancelled = false;
    setSuitabilityLoading(true);
    Promise.allSettled(
      suitabilityPoints.map(async (point) => [point.id, await fetchGrowthRecords(point.id)] as const),
    )
      .then((results) => {
        if (cancelled) return;
        const nextRecords: Record<number, GrowthRecord[]> = {};
        let failedCount = 0;
        results.forEach((result) => {
          if (result.status === "fulfilled") {
            nextRecords[result.value[0]] = result.value[1];
          } else {
            failedCount += 1;
          }
        });
        setSuitabilityRecordsByPoint(nextRecords);
        if (failedCount > 0) {
          messageRef.current.warning(`有 ${failedCount} 个点位的采集记录未加载，分析已基于可用数据完成`);
        }
      })
      .finally(() => {
        if (!cancelled) setSuitabilityLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [suitabilityMode, suitabilityPoints, suitabilityReloadKey]);

  useEffect(() => {
    if (!selectedPoint?.id) {
      setGrowthRecords([]);
      return;
    }

    setGrowthLoading(true);
    fetchGrowthRecords(selectedPoint.id)
      .then(setGrowthRecords)
      .catch((error) => message.error(getMapPointRequestErrorMessage(error, "采集记录加载")))
      .finally(() => setGrowthLoading(false));
  }, [message, selectedPoint?.id]);

  useEffect(() => {
    setCollectionHistoryOpen(false);
    setGrowthCreateOpen(false);
  }, [selectedPoint?.id]);

  useEffect(() => {
    Promise.all([
      fetchDictionaryOptions("growth_stage"),
      fetchDictionaryOptions("soil_type"),
      fetchDictionaryOptions("weather"),
    ]).then(([growthStages, soilTypes, weatherTypes]) => {
      setGrowthStageOptions(growthStages);
      setSoilTypeOptions(soilTypes);
      setWeatherOptions(weatherTypes);
    });
  }, []);

  useEffect(() => {
    if (selectedPointId && !filteredPoints.some((point) => point.id === selectedPointId)) {
      setSelectedPointId(filteredPoints[0]?.id);
    }
  }, [filteredPoints, selectedPointId]);

  useEffect(() => {
    const layer = markersLayerRef.current;
    if (!layer) {
      return;
    }

    layer.clearLayers();
    filteredPoints.forEach((point) => {
      if (point.status === 0 || point.latitude == null || point.longitude == null) {
        return;
      }
      L.marker([point.latitude, point.longitude], { icon: createHerbIcon(point) })
        .on("click", () => {
          if (routePlanningRef.current) {
            toggleRoutePoint(point.id);
            return;
          }
          setSelectedPointId(point.id);
        })
        .bindPopup(buildPopupContent(point, openEditForm, confirmDelete), {
          className: "bdis-herb-popup",
          maxWidth: 280,
          minWidth: 240,
        })
        .addTo(layer);
    });
  }, [confirmDelete, filteredPoints, openEditForm, toggleRoutePoint]);

  useEffect(() => {
    const layer = suitabilityLayerRef.current;
    if (!layer) return;
    layer.clearLayers();
    if (!suitabilityMode) return;

    suitabilitySnapshots.forEach((snapshot) => {
      if (snapshot.level === "insufficient" || snapshot.score == null) return;
      const color = suitabilityColor(snapshot.level);
      const radius = Math.max(4500, suitabilityRadius * 1000 * (0.5 + snapshot.score / 200));
      L.circle([snapshot.point.latitude, snapshot.point.longitude], {
        radius,
        color,
        weight: snapshot.level === "high" ? 2.5 : 1.5,
        opacity: 0.72,
        fillColor: color,
        fillOpacity: snapshot.level === "high" ? 0.24 : snapshot.level === "medium" ? 0.16 : 0.11,
        interactive: false,
      })
        .bindTooltip(
          `${snapshot.point.herbName} · ${snapshot.level === "high" ? "高适生" : snapshot.level === "medium" ? "中适生" : "低适生"} ${snapshot.score} 分`,
          { sticky: true },
        )
        .addTo(layer);
    });
  }, [suitabilityMode, suitabilityRadius, suitabilitySnapshots]);

  useEffect(() => {
    const layer = measureLayerRef.current;
    if (!layer) {
      return;
    }

    layer.clearLayers();
    if (measurePoints.length === 0) {
      return;
    }

    const isSatellite = activeLayer === "satellite";
    const lineColor = isSatellite ? "#ffffff" : "#1f2937";

    measurePoints.forEach((point, index) => {
      L.marker([point.lat, point.lng], { icon: createWaypointIcon(index + 1, isSatellite) }).addTo(layer);
    });

    if (measurePoints.length < 2) {
      return;
    }

    L.polyline(
      measurePoints.map((point) => [point.lat, point.lng]),
      { color: lineColor, weight: 2.5 },
    ).addTo(layer);

    const tickInterval = getTickInterval(mapZoom);
    const tickSize = getTickGeoSize(mapZoom);
    const useKm = totalDistance >= 1000;
    const showNumbers = mapZoom >= 10;
    let cumulativeDist = 0;

    for (let index = 0; index < measurePoints.length - 1; index += 1) {
      const p1 = measurePoints[index];
      const p2 = measurePoints[index + 1];
      const segmentDistance = L.latLng(p1.lat, p1.lng).distanceTo(L.latLng(p2.lat, p2.lng));
      const mid = { lat: (p1.lat + p2.lat) / 2, lng: (p1.lng + p2.lng) / 2 };

      L.marker([mid.lat, mid.lng], {
        icon: createLabelIcon(
          isSatellite ? "bdis-measure-dist-label bdis-measure-dist-label-sat" : "bdis-measure-dist-label",
          formatDistance(segmentDistance),
        ),
        interactive: false,
      }).addTo(layer);

      if (segmentDistance > tickInterval) {
        const brng = bearing(p1, p2);
        const perpBrng = getNorthwardPerpendicular(brng);
        let distance = tickInterval;
        let tickIndex = 0;

        while (distance < segmentDistance - tickInterval / 2) {
          const tickPoint = interpolate(p1, p2, distance / segmentDistance);
          const isMajor = tickIndex % 4 === 0;
          const tickEnd = destination(tickPoint, isMajor ? tickSize * 2 : tickSize, perpBrng);

          L.polyline(
            [
              [tickPoint.lat, tickPoint.lng],
              [tickEnd.lat, tickEnd.lng],
            ],
            { color: lineColor, weight: isMajor ? 2 : 1 },
          ).addTo(layer);

          if (showNumbers && isMajor) {
            const labelPoint = destination(tickPoint, tickSize * 0.6, (perpBrng + 180) % 360);
            L.marker([labelPoint.lat, labelPoint.lng], {
              icon: createLabelIcon(
                isSatellite ? "bdis-measure-tick-label bdis-measure-tick-label-sat" : "bdis-measure-tick-label",
                formatTickDistance(cumulativeDist + distance, useKm),
              ),
              interactive: false,
            }).addTo(layer);
          }

          distance += tickInterval;
          tickIndex += 1;
        }
      }

      cumulativeDist += segmentDistance;
    }
  }, [activeLayer, mapZoom, measurePoints, totalDistance]);

  useEffect(() => {
    const layer = routeLayerRef.current;
    if (!layer) {
      return;
    }
    layer.clearLayers();
    const orderedPoints = routeOrderedPoints.length > 0 ? routeOrderedPoints : routeSelectedPoints;
    if (!routePlanning || orderedPoints.length === 0) {
      return;
    }

    const fallbackGeometry: [number, number][] = [
      ...(routeOrigin ? [[routeOrigin.lat, routeOrigin.lng] as [number, number]] : []),
      ...orderedPoints.map((point) => [point.latitude, point.longitude] as [number, number]),
    ];
    const geometry = routePlan?.geometry.length ? routePlan.geometry : fallbackGeometry;
    if (geometry.length >= 2) {
      L.polyline(geometry, {
        color: routePlan?.roadRoute ? "#0f766e" : "#2563eb",
        weight: routePlan?.roadRoute ? 5 : 3,
        opacity: 0.84,
        dashArray: routePlan?.roadRoute ? undefined : "8 8",
        className: routeNavigating ? "bdis-route-line bdis-route-line-active" : "bdis-route-line",
      }).addTo(layer);
    }
    if (routeOrigin) {
      L.circleMarker([routeOrigin.lat, routeOrigin.lng], {
        radius: 8,
        color: "#ffffff",
        fillColor: "#2563eb",
        fillOpacity: 1,
        weight: 3,
      })
        .bindTooltip("起点：当前位置", { direction: "top" })
        .addTo(layer);
    }
    orderedPoints.forEach((point, index) => {
      L.marker([point.latitude, point.longitude], {
        icon: createRouteStopIcon(index + 1, routeNavigating && index === navigationStopIndex),
        zIndexOffset: 900,
      })
        .bindTooltip(`${index + 1}. ${point.herbName}`, { direction: "top" })
        .addTo(layer);
    });
  }, [navigationStopIndex, routeNavigating, routeOrigin, routeOrderedPoints, routePlan, routePlanning, routeSelectedPoints]);

  useEffect(() => {
    if (!routeNavigating || !navigator.geolocation || !routePlan) {
      return;
    }
    const watchId = navigator.geolocation.watchPosition(
      (position) => {
        const nextPosition: NavigationPosition = {
          lat: position.coords.latitude,
          lng: position.coords.longitude,
          heading: position.coords.heading ?? undefined,
          speed: position.coords.speed ?? undefined,
          accuracy: position.coords.accuracy,
        };
        setNavigationPosition(nextPosition);
        mapRef.current?.setView(
          [nextPosition.lat, nextPosition.lng],
          Math.max(mapRef.current.getZoom(), 16),
          { animate: true },
        );
        const closest = closestRouteIndex(routePlan.geometry, nextPosition);
        setNavigationRouteIndex(closest.index);
        if (closest.distance > 80 && Date.now() - lastRouteRefreshRef.current > 20000) {
          lastRouteRefreshRef.current = Date.now();
          const origin: RouteOrigin = { ...nextPosition, label: "当前位置" };
          const remainingStops = navigationStops.slice(navigationStopIndex);
          setRouteOrigin(origin);
          setNavigationStopIndex(0);
          void routePlanGeneratorRef.current?.(origin, remainingStops, true);
          message.info("检测到偏离路线，正在重新规划剩余采集点");
        }
      },
      () => message.warning("当前位置更新失败，请检查浏览器位置权限"),
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 10000 },
    );
    return () => navigator.geolocation.clearWatch(watchId);
  }, [message, navigationStopIndex, navigationStops, routeNavigating, routePlan]);

  useEffect(() => {
    const layer = navigationLayerRef.current;
    if (!layer) {
      return;
    }
    layer.clearLayers();
    if (!routeNavigating || !navigationPosition) {
      return;
    }
    L.marker([navigationPosition.lat, navigationPosition.lng], {
      icon: createNavigationIcon(navigationPosition.heading),
      zIndexOffset: 1200,
    })
      .bindTooltip("当前位置", { direction: "top" })
      .addTo(layer);
  }, [navigationPosition, routeNavigating]);

  useEffect(() => {
    if (!routeNavigating || navigationRemainingDistance == null || navigationRemainingDistance > 50) {
      return;
    }
    if (navigationStopIndex >= navigationStops.length - 1) {
      message.success("已完成全部采集点导航");
      setRouteNavigating(false);
      setNavigationPosition(undefined);
      return;
    }
    setNavigationStopIndex((current) => current + 1);
  }, [message, navigationRemainingDistance, navigationStopIndex, navigationStops.length, routeNavigating]);

  async function handleSubmit(payload: MapPointPayload) {
    try {
      if (formMode === "edit" && editingPoint?.id) {
        const next = await updateMapPoint(editingPoint.id, payload);
        setPoints((current) => current.map((point) => (point.id === next.id ? next : point)));
        message.success("地图点位已更新");
      } else {
        const next = await createMapPoint(payload);
        setPoints((current) => [next, ...current]);
        setSelectedPointId(next.id);
        setEmptyNoticeVisible(false);
        message.success("地图点位已新增");
      }
      setFormOpen(false);
    } catch (error) {
      message.error(getMapPointRequestErrorMessage(error, "地图点位保存"));
    }
  }

  async function handleGrowthSubmit(values: GrowthCreateFormValues) {
    if (!selectedPoint?.id) {
      return;
    }

    setGrowthSubmitting(true);
    try {
      const next = await createGrowthRecord(selectedPoint.id, {
        ...values,
        collectorName: currentUser?.realName || currentUser?.username || "当前用户",
        dataSource: "map",
      });
      setGrowthRecords((current) => [next, ...current]);
      setPoints((current) =>
        current.map((point) =>
          point.id === selectedPoint.id ? { ...point, lastCollectedAt: next.collectedAt } : point,
        ),
      );
      growthCreateForm.resetFields();
      setGrowthCreateOpen(false);
      message.success("采集记录已保存");
    } catch (error) {
      message.error(getMapPointRequestErrorMessage(error, "采集记录保存"));
    } finally {
      setGrowthSubmitting(false);
    }
  }

  function toggleMeasure() {
    setRoutePlanning(false);
    setMeasuring((current) => !current);
    setMeasurePoints([]);
  }

  async function generateRoutePlan(
    originOverride = routeOrigin,
    pointsOverride = routeSelectedPoints,
    silent = false,
  ): Promise<RoutePlan | undefined> {
    if (pointsOverride.length === 0) {
      message.warning("请先在地图或左侧列表中选择至少一个采集点");
      return undefined;
    }
    const orderedPoints = optimizeRouteOrder(pointsOverride, originOverride);
    setRouteOrderedPointIds(orderedPoints.map((point) => point.id));
    const routeCoordinates = [
      ...(originOverride ? [[originOverride.lat, originOverride.lng] as [number, number]] : []),
      ...orderedPoints.map((point) => [point.latitude, point.longitude] as [number, number]),
    ];
    const fallbackDistance = routeCoordinates.slice(0, -1).reduce(
      (sum, coordinate, index) =>
        sum + pointDistance({ lat: coordinate[0], lng: coordinate[1] }, { lat: routeCoordinates[index + 1][0], lng: routeCoordinates[index + 1][1] }),
      0,
    );

    setRoutePlanningLoading(true);
    const plan = {
      distanceMeters: fallbackDistance,
      durationSeconds: Math.round(fallbackDistance / (35 * 1000 / 3600)),
      geometry: routeCoordinates,
      roadRoute: false,
      steps: [],
    };
    setRoutePlan(plan);
    setRoutePlanningLoading(false);
    if (!silent) {
      message.success(`已生成 ${orderedPoints.length} 个采集点的任务路线估算`);
    }
    return plan;
  }

  routePlanGeneratorRef.current = generateRoutePlan;

  function startRouteNavigation() {
    if (routeSelectedPoints.length === 0) {
      message.warning("请先选择采集点");
      return;
    }
    if (!routePlan?.roadRoute) {
      message.warning("未配置受信任道路导航服务，当前路线仅可用于任务规划和导出");
      return;
    }
    if (!navigator.geolocation) {
      message.error("当前浏览器不支持实时定位，无法启动本地图导航");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const origin = {
          lat: position.coords.latitude,
          lng: position.coords.longitude,
          label: "当前位置",
        };
        setRouteOrigin(origin);
        setNavigationPosition({ ...origin, heading: position.coords.heading ?? undefined, speed: position.coords.speed ?? undefined, accuracy: position.coords.accuracy });
        const plan = await generateRoutePlan(origin, routeSelectedPoints, true);
        if (!plan) return;
        setNavigationStopIndex(0);
        setNavigationRouteIndex(0);
        setNavigationHudCollapsed(false);
        setRouteNavigating(true);
        mapRef.current?.setView([origin.lat, origin.lng], Math.max(mapRef.current.getZoom(), 16), { animate: true });
      },
      () => message.error("无法获取当前位置，请允许浏览器位置权限后重试"),
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 5000 },
    );
  }

  function stopRouteNavigation() {
    setRouteNavigating(false);
    setNavigationPosition(undefined);
    setNavigationRouteIndex(0);
    setNavigationHudCollapsed(false);
  }

  function exportRouteTask() {
    const orderedPoints = routeOrderedPoints.length > 0 ? routeOrderedPoints : routeSelectedPoints;
    if (orderedPoints.length === 0) {
      message.warning("请先选择要导出的采集点");
      return;
    }
    const rows: Array<Array<string | number | undefined>> = [
      ["访问顺序", "药材名称", "点位名称", "区县", "详细地址", "经度", "纬度", "最近采集时间"],
      ...orderedPoints.map((point, index) => [
        index + 1,
        point.herbName,
        point.locationName,
        point.district,
        point.address,
        point.longitude,
        point.latitude,
        formatDateTime(point.lastCollectedAt),
      ]),
    ];
    const csv = `\uFEFF${rows.map((row) => row.map(escapeCsvCell).join(",")).join("\r\n")}`;
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `采集路线任务清单_${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    message.success(`已导出 ${orderedPoints.length} 个采集点的任务清单`);
  }

  function exportMapPoints() {
    if (filteredPoints.length === 0) {
      message.warning("当前没有可导出的地图点位");
      return;
    }

    const rows: Array<Array<string | number | undefined>> = [
      ["点位编号", "药材名称", "点位名称", "区县", "基地", "经度", "纬度", "分布类型", "状态", "数据来源", "最近采集时间", "详细地址"],
      ...filteredPoints.map((point) => [
        point.id,
        point.herbName,
        point.locationName,
        point.district,
        point.baseName,
        point.longitude,
        point.latitude,
        getDistributionTypeLabel(point.distributionType),
        point.status === 0 ? "停用" : "启用",
        point.dataSource,
        formatDateTime(point.lastCollectedAt),
        point.address,
      ]),
    ];
    const csv = `\uFEFF${rows.map((row) => row.map(escapeCsvCell).join(",")).join("\r\n")}`;
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `重庆中药材分布点位_${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    message.success(`已导出 ${filteredPoints.length} 条地图点位`);
  }

  return (
    <div className={styles.shell}>
      <div className={styles.toolbar}>
        <label className={styles.filterField}>
          <span className={styles.filterLabel}>药材品种</span>
          <Select
            allowClear
            className={styles.filterSelect}
            placeholder="全部药材"
            options={herbOptions}
            value={herbFilter}
            onChange={setHerbFilter}
          />
        </label>
        <label className={styles.filterField}>
          <span className={styles.filterLabel}>区县</span>
          <Select
            allowClear
            className={styles.filterSelect}
            placeholder="全部区县"
            options={districtOptions}
            value={districtFilter}
            onChange={setDistrictFilter}
          />
        </label>
        <label className={styles.filterField}>
          <span className={styles.filterLabel}>基地</span>
          <Select
            allowClear
            className={styles.filterSelect}
            placeholder="全部基地"
            options={baseOptions}
            value={baseFilter}
            onChange={setBaseFilter}
          />
        </label>
        <label className={styles.filterField}>
          <span className={styles.filterLabel}>数据来源</span>
          <Select
            allowClear
            className={styles.filterSelect}
            placeholder="全部来源"
            options={sourceOptions}
            value={sourceFilter}
            onChange={setSourceFilter}
          />
        </label>
        <label className={`${styles.filterField} ${styles.keywordField}`}>
          <span className={styles.filterLabel}>关键词搜索</span>
          <Input
            allowClear
            className={styles.keywordInput}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onPressEnter={() => loadPoints(keyword)}
            placeholder="搜索药材、地点或地址"
            suffix={<Search size={16} />}
          />
        </label>
        <div className={styles.toolbarActions}>
          <Button type="primary" onClick={() => loadPoints(keyword)}>
            查询
          </Button>
          <Button onClick={resetMap}>重置</Button>
        </div>
      </div>

      <div className={styles.contentGrid}>
        <aside className={styles.pointListPanel} style={sidePanelHeight ? { height: sidePanelHeight } : undefined}>
          <div className={styles.panelHeader}>
            <strong>分布点列表</strong>
            <span>共 {filteredPoints.length} 条</span>
          </div>
          <div className={styles.pointActions}>
            <Button type="primary" icon={<Plus size={15} />} onClick={() => openCreateForm(L.latLng(CHONGQING_CENTER))}>
              新增点
            </Button>
            <Button disabled={!selectedPoint} icon={<Pencil size={15} />} onClick={() => selectedPoint && openEditForm(selectedPoint)}>
              编辑
            </Button>
            <Button
              disabled={!selectedPoint}
              loading={selectedPoint ? statusUpdatingId === selectedPoint.id : false}
              icon={<CirclePause size={15} />}
              onClick={() => selectedPoint && togglePointStatus(selectedPoint)}
            >
              {selectedPoint?.status === 0 ? "启用" : "停用"}
            </Button>
            <Button danger disabled={!selectedPoint} icon={<Trash2 size={15} />} onClick={() => selectedPoint && confirmDelete(selectedPoint)}>
              删除
            </Button>
          </div>
          <div className={styles.pointList}>
            {filteredPoints.map((point) => (
              <div
                key={point.id}
                className={`${styles.pointItem} ${routePlanning ? styles.pointItemRoutePlanning : ""} ${selectedPoint?.id === point.id ? styles.pointItemActive : ""} ${routePointIds.includes(point.id) ? styles.routePointSelected : ""}`}
              >
                <button
                  type="button"
                  className={styles.pointSummary}
                  onClick={() => routePlanning ? toggleRoutePoint(point.id) : viewPoint(point)}
                >
                  <div
                    className={styles.pointThumb}
                    style={point.coverImageUrl ? { backgroundImage: `url(${point.coverImageUrl})` } : undefined}
                    aria-label={point.herbName}
                  >
                    {!point.coverImageUrl && <span>药</span>}
                  </div>
                  <div className={styles.pointMeta}>
                    <strong>{point.locationName || point.herbName}</strong>
                    <span>{[point.district, point.baseName || point.herbName].filter(Boolean).join(" · ")}</span>
                  </div>
                </button>
                <button
                  type="button"
                  className={`${styles.pointStatus} ${point.status === 0 ? styles.pointStatusDisabled : styles.pointStatusEnabled}`}
                  disabled={statusUpdatingId === point.id}
                  onClick={() => togglePointStatus(point)}
                >
                  {statusUpdatingId === point.id ? "处理中" : point.status === 0 ? "停用" : "启用"}
                </button>
                {routePlanning && (
                  <button
                    type="button"
                    className={`${styles.routePointButton} ${routePointIds.includes(point.id) ? styles.routePointButtonActive : ""}`}
                    onClick={() => toggleRoutePoint(point.id)}
                  >
                    {routePointIds.includes(point.id) ? "已选" : "加入"}
                  </button>
                )}
                <button type="button" className={styles.viewPointButton} onClick={() => viewPoint(point)}>
                  <span>查看</span>
                  <ChevronRight size={15} />
                </button>
              </div>
            ))}
            {filteredPoints.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无点位" />}
          </div>
        </aside>

      <section
        className={`${styles.mapPanel} ${mapFullscreen ? styles.mapPanelFullscreen : ""} ${!mapFullscreen && sidePanelHeight ? styles.mapPanelSynced : ""}`}
        style={{
          "--map-fullscreen-top": `${fullscreenTop}px`,
          ...(!mapFullscreen && sidePanelHeight ? { height: sidePanelHeight, minHeight: 0 } : {}),
        } as CSSProperties}
      >
        <div className={styles.mapPanelHeader}>
          <h2>重庆中药材分布地图</h2>
          <Button icon={<Download size={15} />} onClick={exportMapPoints}>
            导出数据
          </Button>
        </div>
        <div className={styles.mapStatistics}>
          <div className={styles.statCard}>
            <span className={styles.statIcon}><Sprout size={23} /></span>
            <span className={styles.statContent}><small>药材种类</small><strong>{mapStatistics.herbCount}<em>种</em></strong></span>
          </div>
          <div className={styles.statCard}>
            <span className={styles.statIcon}><MapPinned size={23} /></span>
            <span className={styles.statContent}><small>分布点位</small><strong>{mapStatistics.pointCount}<em>个</em></strong></span>
          </div>
          <div className={styles.statCard}>
            <span className={styles.statIcon}><Warehouse size={23} /></span>
            <span className={styles.statContent}><small>涉及基地</small><strong>{mapStatistics.baseCount}<em>个</em></strong></span>
          </div>
          <div className={styles.statCard}>
            <span className={styles.statIcon}><Building2 size={23} /></span>
            <span className={styles.statContent}><small>重点区县</small><strong>{mapStatistics.districtCount}<em>个</em></strong></span>
          </div>
        </div>
        <div className={`${styles.mapWrap} ${activeLayer === "satellite" ? styles.satelliteMap : styles.lightMap}`}>
        {loading && (
          <div className={styles.loadingMask}>
            <Spin />
          </div>
        )}
        <div ref={mapElementRef} className={styles.mapCanvas} />
        <button
          type="button"
          className={`${styles.suitabilityModeButton} ${suitabilityMode ? styles.suitabilityModeButtonActive : ""}`}
          aria-label={suitabilityMode ? "退出药材适生区热力分析" : "进入药材适生区热力分析"}
          aria-pressed={suitabilityMode}
          onClick={toggleSuitabilityMode}
        >
          <Sparkles size={17} />
          <span>{suitabilityMode ? "退出适生分析" : "适生分析"}</span>
        </button>
        {suitabilityMode && !suitabilityCollapsed && (
          <section className={styles.suitabilityPanel} aria-label="药材适生区热力分析">
            <div className={styles.suitabilityPanelHeader}>
              <span><Sparkles size={17} />药材适生区热力分析</span>
              <span className={styles.suitabilityPanelActions}>
                <button type="button" aria-label="收起适生分析" title="收起" onClick={() => setSuitabilityCollapsed(true)}><ChevronLeft size={16} /></button>
                <button type="button" aria-label="退出适生分析" title="退出适生分析" onClick={toggleSuitabilityMode}><X size={16} /></button>
              </span>
            </div>
            <div className={styles.suitabilityPanelBody}>
              <label className={styles.suitabilityField}>
                <span>分析药材</span>
                <Select
                  value={suitabilityHerb}
                  options={herbOptions}
                  placeholder="选择药材"
                  onChange={(value) => {
                    setSuitabilityHerb(value);
                    setSuitabilityReloadKey((current) => current + 1);
                  }}
                />
              </label>
              <div className={styles.suitabilityField}>
                <span>密度分析半径 <b>{suitabilityRadius} km</b></span>
                <Slider min={8} max={35} value={suitabilityRadius} onChange={setSuitabilityRadius} />
              </div>
              <div className={styles.suitabilityMetrics}>
                <span><small>高适生区</small><strong>{suitabilityStatistics.high}</strong></span>
                <span><small>中适生区</small><strong>{suitabilityStatistics.medium}</strong></span>
                <span><small>数据不足</small><strong>{suitabilityStatistics.insufficient}</strong></span>
              </div>
              <div className={styles.suitabilityLegend}>
                <span><i className={styles.suitabilityLegendHigh} />高适生 75-100</span>
                <span><i className={styles.suitabilityLegendMedium} />中适生 52-74</span>
                <span><i className={styles.suitabilityLegendLow} />低适生 0-51</span>
              </div>
              {suitabilityLoading ? (
                <div className={styles.suitabilityLoading}><Spin size="small" />正在汇总采集与生长数据</div>
              ) : suitabilitySnapshots.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前筛选范围内没有可分析点位" />
              ) : (
                <div className={styles.suitabilityResults}>
                  {suitabilitySnapshots
                    .slice()
                    .sort((left, right) => (right.score ?? -1) - (left.score ?? -1))
                    .slice(0, 4)
                    .map((item) => (
                      <button key={item.point.id} type="button" onClick={() => viewPoint(item.point)}>
                        <i style={{ backgroundColor: suitabilityColor(item.level) }} />
                        <span><strong>{item.point.locationName || item.point.district || "采集区域"}</strong><small>{item.altitude == null ? "海拔数据缺失" : `海拔 ${item.altitude} m`} · {item.nearbyPointCount} 个邻近点</small></span>
                        <b>{item.score == null ? "数据不足" : item.score}</b>
                      </button>
                    ))}
                </div>
              )}
            </div>
            <div className={styles.suitabilityPanelFooter}>
              <span>评分：海拔 35% · 采集密度 35% · 生长状态 30%</span>
              <button type="button" onClick={() => setSuitabilityReloadKey((current) => current + 1)} title="刷新分析数据"><RefreshCw size={14} />刷新</button>
            </div>
          </section>
        )}
        {suitabilityMode && suitabilityCollapsed && (
          <button
            type="button"
            className={styles.suitabilityRestoreButton}
            aria-label="展开药材适生区热力分析"
            title="展开适生分析"
            onClick={() => setSuitabilityCollapsed(false)}
          >
            <Sparkles size={16} />
            <span>适生分析</span>
            <ChevronRight size={15} />
          </button>
        )}
        {routePlanning && !routePlannerCollapsed && (
          <section className={styles.routePlannerPanel} aria-label="采集路线规划">
            <div className={styles.routePlannerHeader}>
              <span><Route size={17} />采集路线规划</span>
              <span className={styles.routePlannerHeaderActions}>
                <button type="button" aria-label="收起路线规划" title="收起" onClick={() => setRoutePlannerCollapsed(true)}><ChevronLeft size={16} /></button>
                <button type="button" aria-label="退出路线规划" title="退出路线规划" onClick={toggleRoutePlanning}><X size={16} /></button>
              </span>
            </div>
            <p className={styles.routePlannerHint}>在地图标记或左侧列表中加入一个或多个采集点，生成任务路线估算后可导出采集任务。</p>
            <div className={styles.routeOriginRow}>
              <span>{routeOrigin ? "起点：当前位置" : "起点：第一个采集点"}</span>
              <button type="button" onClick={locateRouteOrigin}><LocateFixed size={14} />定位起点</button>
            </div>
            <div className={styles.routePlannerActions}>
              <Button type="primary" icon={<ListOrdered size={15} />} loading={routePlanningLoading} onClick={() => void generateRoutePlan()}>
                自动规划
              </Button>
              <Button disabled={routeSelectedPoints.length === 0} onClick={() => {
                setRoutePointIds([]);
                setRouteOrderedPointIds([]);
                setRoutePlan(undefined);
              }}>
                清空
              </Button>
            </div>
            <div className={styles.routePlannerMetrics}>
              <span><strong>{routeSelectedPoints.length}</strong> 个采集点</span>
              <span><strong>{routePlan ? formatDistance(routePlan.distanceMeters) : "--"}</strong> {routePlan?.roadRoute ? "道路里程" : "直线估算"}</span>
              <span><strong>{routePlan ? `${Math.max(1, Math.round(routePlan.durationSeconds / 60))} 分钟` : "--"}</strong> 预计时长</span>
            </div>
            <div className={styles.routeStopList}>
              {(routeOrderedPoints.length > 0 ? routeOrderedPoints : routeSelectedPoints).map((point, index) => (
                <div key={point.id} className={styles.routeStopItem}>
                  <span>{index + 1}</span>
                  <strong>{point.herbName}</strong>
                  <small>{point.locationName || point.district || "采集点"}</small>
                </div>
              ))}
              {routeSelectedPoints.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="尚未选择采集点" />}
            </div>
            <div className={styles.routePlannerFooter}>
              <Button
                icon={<Navigation size={15} />}
                disabled={routeSelectedPoints.length === 0 || !routePlan || !routePlan.roadRoute}
                title={routePlan?.roadRoute ? undefined : "道路导航需要配置受信任的路线服务"}
                onClick={routeNavigating ? stopRouteNavigation : startRouteNavigation}
              >
                {routeNavigating ? "结束导航" : "开始导航"}
              </Button>
              <Button icon={<Download size={15} />} disabled={routeSelectedPoints.length === 0} onClick={exportRouteTask}>
                导出任务
              </Button>
            </div>
          </section>
        )}
        {routePlanning && routePlannerCollapsed && (
          <button
            type="button"
            className={styles.routePlannerRestoreButton}
            aria-label="展开采集路线规划"
            title="展开路线规划"
            onClick={() => setRoutePlannerCollapsed(false)}
          >
            <Route size={16} />
            <span>路线</span>
            <ChevronRight size={15} />
          </button>
        )}
        {routeNavigating && navigationTarget && !navigationHudCollapsed && (
          <section className={styles.navigationHud} aria-label="采集路线导航">
            <div className={styles.navigationHudHeader}>
              <span><Navigation size={16} />正在导航</span>
              <span className={styles.navigationHudHeaderActions}>
                <button type="button" aria-label="收起导航卡片" title="收起" onClick={() => setNavigationHudCollapsed(true)}><ChevronDown size={16} /></button>
                <button type="button" aria-label="结束导航" title="结束导航" onClick={stopRouteNavigation}><X size={15} /></button>
              </span>
            </div>
            <strong>下一点：{navigationStopIndex + 1}. {navigationTarget.herbName}</strong>
            <small>{navigationTarget.locationName || navigationTarget.district || "采集点"}</small>
            <p className={styles.navigationInstruction}>{navigationInstruction?.instruction || "沿路线继续前行"}</p>
            <div className={styles.navigationHudMetrics}>
              <span>路线剩余 <b>{navigationRouteRemainingDistance == null ? "定位中" : formatDistance(navigationRouteRemainingDistance)}</b></span>
              <span>进度 <b>{navigationStopIndex + 1}/{navigationStops.length}</b></span>
            </div>
            <small>到达采集点 50 米范围内将自动切换下一点</small>
          </section>
        )}
        {routeNavigating && navigationTarget && navigationHudCollapsed && (
          <button
            type="button"
            className={styles.navigationHudRestoreButton}
            aria-label="展开导航卡片"
            title="展开导航"
            onClick={() => setNavigationHudCollapsed(false)}
          >
            <Navigation size={16} />
            <span>导航至 {navigationTarget.herbName}</span>
            <ChevronUp size={16} />
          </button>
        )}
        <div className={styles.mapTopActions}>
          <button
            type="button"
            className={`${styles.routeMapButton} ${routePlanning ? styles.routeMapButtonActive : ""}`}
            aria-label={routePlanning ? "退出路线规划" : "采集路线规划"}
            aria-pressed={routePlanning}
            onClick={toggleRoutePlanning}
          >
            <Route size={16} />
            <span>路线规划</span>
          </button>
          <button
            type="button"
            className={`${styles.measureMapButton} ${measuring ? styles.measureMapButtonActive : ""}`}
            aria-label={measuring ? "结束测距" : "测距"}
            onClick={toggleMeasure}
          >
            <Ruler size={16} />
            <span>{measuring ? "结束测距" : "测距"}</span>
          </button>
          <button
            type="button"
            className={styles.fullscreenMapButton}
            aria-label={mapFullscreen ? "退出全屏地图" : "全屏显示地图"}
            aria-pressed={mapFullscreen}
            title={mapFullscreen ? "退出全屏" : "全屏"}
            onClick={() => setMapFullscreen((current) => !current)}
          >
            {mapFullscreen ? <Minimize2 size={17} /> : <Maximize2 size={17} />}
          </button>
        </div>
        <div className={styles.mapControls} aria-label="地图控制">
          <button type="button" className={styles.mapControlButton} aria-label="放大地图" onClick={zoomIn}>
            <Plus size={18} />
          </button>
          <button type="button" className={styles.mapControlButton} aria-label="缩小地图" onClick={zoomOut}>
            <Minus size={18} />
          </button>
          <button
            type="button"
            className={styles.mapControlButton}
            aria-label="定位当前位置"
            onClick={locateCurrentPosition}
          >
            <LocateFixed size={18} />
          </button>
        </div>
        <div className={styles.layerSwitcher}>
          <button
            type="button"
            className={styles.layerPreviewButton}
            aria-label="切换地图图层"
            onClick={switchToNextLayer}
          >
            <div ref={miniMapElementRef} className={styles.miniMapCanvas} />
            <span className={styles.layerBadge}>
              <Layers size={14} />
              <span>图层</span>
            </span>
          </button>
          <div className={styles.layerOptions} aria-label="地图样式">
            {MAP_LAYERS.map((layer) => (
              <button
                type="button"
                key={layer.key}
                className={`${styles.layerOption} ${activeLayer === layer.key ? styles.layerOptionActive : ""}`}
                onClick={() => switchMapLayer(layer.key)}
              >
                <span className={styles.layerOptionImage} style={{ backgroundImage: `url(${layer.preview})` }} />
                <span className={styles.layerOptionText}>{layer.label}</span>
              </button>
            ))}
          </div>
        </div>
        {measuring && <div className={styles.measureHint}>点击地图添加测距点，双击地图结束测距</div>}
        {measuring && totalDistance > 0 && (
          <div className={styles.measureTotal}>
            总距离 <strong>{formatDistance(totalDistance)}</strong>
          </div>
        )}
        {!loading && points.length === 0 && emptyNoticeVisible && (
          <div className={styles.emptyLayer}>
            <Button
              type="text"
              size="small"
              className={styles.emptyClose}
              aria-label="关闭暂无点位提示"
              icon={<X size={14} />}
              onClick={() => setEmptyNoticeVisible(false)}
            />
            <Empty description="暂无地图点位，可点击地图新增" />
          </div>
        )}
        </div>
      </section>

        <aside ref={detailPanelRef} className={styles.detailPanel}>
          {selectedPoint ? (
            <>
              <div className={styles.detailHeader}>
                <div>
                  <div className={styles.detailTitleRow}>
                    <h3>{selectedPoint.locationName || selectedPoint.herbName}</h3>
                    <Tag color={selectedPoint.status === 0 ? "default" : "green"}>
                      {selectedPoint.status === 0 ? "停用" : "启用"}
                    </Tag>
                  </div>
                  <p>{[selectedPoint.district, selectedPoint.address].filter(Boolean).join(" · ") || "地图点位"}</p>
                </div>
                <Button type="text" icon={<X size={16} />} onClick={() => setSelectedPointId(undefined)} />
              </div>

              <div className={styles.detailSection}>
                <h4>基本信息</h4>
                <div className={styles.detailHero}>
                  <div
                    className={styles.detailCover}
                    style={selectedPoint.coverImageUrl ? { backgroundImage: `url(${selectedPoint.coverImageUrl})` } : undefined}
                  >
                    {!selectedPoint.coverImageUrl && <span>药</span>}
                  </div>
                  <div className={styles.infoRows}>
                    <span>点位编号</span>
                    <strong>{formatPointNo(selectedPoint.id)}</strong>
                    <span>药材名称</span>
                    <strong>{selectedPoint.herbName}</strong>
                    <span>所在区县</span>
                    <strong>{selectedPoint.district || "暂无"}</strong>
                    <span>基地名称</span>
                    <strong>{selectedPoint.baseName || "暂无"}</strong>
                    <span>数据来源</span>
                    <strong>{selectedPoint.dataSource || "暂无"}</strong>
                  </div>
                </div>
                {(selectedPoint.aliasName || selectedPoint.latinName) && (
                  <div className={styles.herbIdentity}>
                    {selectedPoint.aliasName && <Tag color="green">别名：{selectedPoint.aliasName}</Tag>}
                    {selectedPoint.latinName && <Tag>拉丁名：{selectedPoint.latinName}</Tag>}
                  </div>
                )}
              </div>

              <div className={styles.detailSection}>
                <div className={styles.sectionTitleRow}>
                  <h4>经纬度</h4>
                  <Button size="small" icon={<LocateFixed size={14} />} onClick={() => viewPoint(selectedPoint)}>
                    查看地图
                  </Button>
                </div>
                <div className={styles.coordinateRow}>
                  <span>经度 <strong>{selectedPoint.longitude.toFixed(6)}°E</strong></span>
                  <span>纬度 <strong>{selectedPoint.latitude.toFixed(6)}°N</strong></span>
                </div>
              </div>

              <div className={styles.detailSection}>
                <h4>基地信息</h4>
                <div className={styles.infoRows}>
                  <span>基地名称</span>
                  <strong>{selectedPoint.baseName || "暂无"}</strong>
                  <span>分布类型</span>
                  <strong>{getDistributionTypeLabel(selectedPoint.distributionType) || "暂无"}</strong>
                  {selectedPoint.altitude != null && <><span>海拔</span><strong>{selectedPoint.altitude} m</strong></>}
                  <span>详细地址</span>
                  <strong>{selectedPoint.address || "暂无"}</strong>
                </div>
              </div>

              <div className={styles.detailSection} hidden>
                <div className={styles.sectionTitleRow}>
                  <h4>最近采集记录</h4>
                  <Tag>{growthRecords.length} 次</Tag>
                </div>
                <Spin spinning={growthLoading}>
                  {sortedGrowthRecords[0] ? (
                    <div className={styles.latestCollection}>
                      <div className={styles.latestCollectionGrid}>
                        <span>采集时间</span><strong>{formatDateTime(sortedGrowthRecords[0].collectedAt)}</strong>
                        <span>采集者</span><strong>{sortedGrowthRecords[0].collectorName || "未知采集者"}</strong>
                        <span>生长阶段</span><strong>{sortedGrowthRecords[0].growthStage || "暂无"}</strong>
                        <span>采集重量</span><strong>{sortedGrowthRecords[0].sampleWeight != null ? `${sortedGrowthRecords[0].sampleWeight} g` : "暂无"}</strong>
                        <span>数据来源</span><strong>{sortedGrowthRecords[0].dataSource || "暂无"}</strong>
                        <span>天气</span><strong>{sortedGrowthRecords[0].weather || "暂无"}</strong>
                      </div>
                    </div>
                  ) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无采集记录" />
                  )}
                </Spin>
                <div className={styles.collectionActionRow}>
                  <Button
                    type="primary"
                    disabled={selectedPoint.status === 0}
                    title={selectedPoint.status === 0 ? "停用点位不能新增采集记录" : undefined}
                    onClick={() => setGrowthCreateOpen(true)}
                  >
                    新增采集记录
                  </Button>
                  <Button
                    type="link"
                    className={styles.viewAllButton}
                    disabled={sortedGrowthRecords.length === 0}
                    onClick={() => setCollectionHistoryOpen(true)}
                  >
                    查看全部 <ChevronRight size={14} />
                  </Button>
                </div>
              </div>
            </>
          ) : (
            <Empty description="请选择一个点位" />
          )}
        </aside>
      </div>

      <div className={styles.footer}>
        <Tag color="blue">点位 {points.length}</Tag>
        <Tag color={activeLayer === "satellite" ? "purple" : "green"}>{activeLayerConfig.label}</Tag>
        <Tag>缩放 {mapZoom}</Tag>
      </div>

      <HerbPointFormModal
        open={formOpen}
        mode={formMode}
        initialPoint={editingPoint}
        onCancel={() => setFormOpen(false)}
        onSubmit={handleSubmit}
      />
      <Modal
        open={growthCreateOpen}
        title="新增采集记录"
        footer={null}
        destroyOnHidden
        onCancel={() => setGrowthCreateOpen(false)}
      >
        <Form
          form={growthCreateForm}
          layout="vertical"
          preserve={false}
          onFinish={handleGrowthSubmit}
        >
          <Form.Item label="药材品种">
            <Input disabled value={selectedPoint?.herbName || ""} />
          </Form.Item>
          <Form.Item label="分布点位">
            <Input disabled value={selectedPoint?.locationName || selectedPoint?.district || ""} />
          </Form.Item>
          <Form.Item name="growthStage" label="生长阶段">
            {growthStageOptions.length ? <Select allowClear options={growthStageOptions} /> : <Input />}
          </Form.Item>
          <Form.Item name="soilType" label="土壤类型">
            {soilTypeOptions.length ? <Select allowClear options={soilTypeOptions} /> : <Input />}
          </Form.Item>
          <Form.Item name="weather" label="天气">
            {weatherOptions.length ? <Select allowClear options={weatherOptions} /> : <Input />}
          </Form.Item>
          <Form.Item name="temperature" label="温度">
            <InputNumber style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="humidity" label="湿度">
            <InputNumber min={0} max={100} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="soilPh" label="土壤 pH">
            <InputNumber min={0} max={14} step={0.1} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="sampleWeight" label="样本重量（g）">
            <InputNumber min={0} style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Button block htmlType="submit" type="primary" loading={growthSubmitting}>
            保存草稿
          </Button>
        </Form>
      </Modal>
      <Modal
        open={collectionHistoryOpen}
        title={`${selectedPoint?.herbName || "药材"}全部采集记录`}
        footer={null}
        width={680}
        destroyOnHidden
        onCancel={() => setCollectionHistoryOpen(false)}
      >
        <Spin spinning={growthLoading}>
          <div className={styles.collectionHistoryList}>
            {sortedGrowthRecords.map((record) => (
              <div key={record.id} className={styles.collectionHistoryItem}>
                <div className={styles.collectionHistoryHeader}>
                  <strong>{record.collectorName || "未知采集者"}</strong>
                  <span>{formatDateTime(record.collectedAt)}</span>
                </div>
                <div className={styles.collectionHistoryGrid}>
                  <span>生长阶段</span><strong>{record.growthStage || "暂无"}</strong>
                  <span>采集重量</span><strong>{record.sampleWeight != null ? `${record.sampleWeight} g` : "暂无"}</strong>
                  <span>天气</span><strong>{record.weather || "暂无"}</strong>
                  <span>温度</span><strong>{record.temperature != null ? `${record.temperature} ℃` : "暂无"}</strong>
                  <span>湿度</span><strong>{record.humidity != null ? `${record.humidity}%` : "暂无"}</strong>
                  <span>土壤</span><strong>{record.soilType || "暂无"}</strong>
                </div>
                {record.remark && <p>{record.remark}</p>}
              </div>
            ))}
            {sortedGrowthRecords.length === 0 && <Empty description="暂无采集记录" />}
          </div>
        </Spin>
      </Modal>
    </div>
  );
}
