"use client";

import { App, Button, Empty, Input, Select, Space, Spin, Tag } from "antd";
import L, { type LatLng, type Map as LeafletMap } from "leaflet";
import { Layers, LocateFixed, Minus, Plus, RefreshCw, Ruler, Search, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
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
} from "@/lib/map-points";
import { HerbPointFormModal } from "./HerbPointFormModal";
import styles from "./HerbDistributionMap.module.css";

const CHONGQING_CENTER: [number, number] = [29.56301, 106.55156];

const MAP_LAYERS = [
  {
    key: "standard",
    label: "标准地图",
    url: "https://mt{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
    preview: "https://mt0.google.com/vt/lyrs=m&x=418&y=215&z=9",
  },
  {
    key: "satellite",
    label: "卫星地图",
    url: "https://mt{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}",
    preview: "https://mt0.google.com/vt/lyrs=s&x=418&y=215&z=9",
  },
  {
    key: "terrain",
    label: "地形地貌",
    url: "https://mt{s}.google.com/vt/lyrs=p&x={x}&y={y}&z={z}",
    preview: "https://mt0.google.com/vt/lyrs=p&x=418&y=215&z=9",
  },
] as const;

type MapLayerKey = (typeof MAP_LAYERS)[number]["key"];

function createTileLayer(layerKey: MapLayerKey) {
  const layer = MAP_LAYERS.find((item) => item.key === layerKey) ?? MAP_LAYERS[0];
  return L.tileLayer(layer.url, {
    attribution: "&copy; Google",
    maxNativeZoom: 18,
    maxZoom: 22,
    subdomains: ["0", "1", "2", "3"],
  });
}

function getPreviewLayerKey(layerKey: MapLayerKey): MapLayerKey {
  return layerKey === "satellite" ? "standard" : "satellite";
}

function createPreviewTileLayer(layerKey: MapLayerKey) {
  if (layerKey === "standard") {
    return L.tileLayer("https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}{r}.png", {
      attribution: "&copy; CARTO",
      maxNativeZoom: 20,
      maxZoom: 22,
      subdomains: ["a", "b", "c", "d"],
    });
  }
  return createTileLayer("satellite");
}

interface MeasurePoint {
  lat: number;
  lng: number;
}

type FormMode = "create" | "edit";

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
  const [points, setPoints] = useState<MapPoint[]>([]);
  const [keyword, setKeyword] = useState("");
  const [districtFilter, setDistrictFilter] = useState<string>();
  const [sourceFilter, setSourceFilter] = useState<string>();
  const [loading, setLoading] = useState(false);
  const [measuring, setMeasuring] = useState(false);
  const [measurePoints, setMeasurePoints] = useState<MeasurePoint[]>([]);
  const [activeLayer, setActiveLayer] = useState<MapLayerKey>("standard");
  const [mapZoom, setMapZoom] = useState(9);
  const [mapReady, setMapReady] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<FormMode>("create");
  const [editingPoint, setEditingPoint] = useState<Partial<MapPoint>>();
  const [emptyNoticeVisible, setEmptyNoticeVisible] = useState(true);
  const [selectedPointId, setSelectedPointId] = useState<number>();
  const [growthRecords, setGrowthRecords] = useState<GrowthRecord[]>([]);
  const [growthLoading, setGrowthLoading] = useState(false);
  const [growthSubmitting, setGrowthSubmitting] = useState(false);
  const [growthForm, setGrowthForm] = useState<GrowthRecordPayload>({
    collectorName: "",
    growthStage: "",
    sampleWeight: undefined,
    remark: "",
  });
  const mapElementRef = useRef<HTMLDivElement>(null);
  const miniMapElementRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const miniMapRef = useRef<LeafletMap | null>(null);
  const baseLayerRef = useRef<L.TileLayer | null>(null);
  const miniBaseLayerRef = useRef<L.TileLayer | null>(null);
  const previewLayerRef = useRef<MapLayerKey>("satellite");
  const markersLayerRef = useRef<L.LayerGroup | null>(null);
  const measureLayerRef = useRef<L.LayerGroup | null>(null);
  const measuringRef = useRef(false);

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
        if (districtFilter && point.district !== districtFilter) {
          return false;
        }
        if (sourceFilter && point.dataSource !== sourceFilter) {
          return false;
        }
        return true;
      }),
    [districtFilter, points, sourceFilter],
  );

  const selectedPoint = useMemo(
    () => filteredPoints.find((point) => point.id === selectedPointId) ?? filteredPoints[0],
    [filteredPoints, selectedPointId],
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

  const loadPoints = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchMapPoints(keyword ? { keyword } : undefined);
      setPoints(data);
      setSelectedPointId((current) => current ?? data[0]?.id);
      setEmptyNoticeVisible(true);
    } catch (error) {
      message.error(getMapPointRequestErrorMessage(error, "地图点位加载"));
    } finally {
      setLoading(false);
    }
  }, [keyword, message]);

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

  useEffect(() => {
    measuringRef.current = measuring;
  }, [measuring]);

  useEffect(() => {
    previewLayerRef.current = previewLayer;
  }, [previewLayer]);

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
    measureLayerRef.current = L.layerGroup().addTo(map);
    mapRef.current = map;
    setMapZoom(map.getZoom());
    setMapReady(true);

    map.on("click", (event) => {
      if (measuringRef.current) {
        setMeasurePoints((current) => [...current, { lat: event.latlng.lat, lng: event.latlng.lng }]);
        return;
      }
      openCreateForm(event.latlng);
    });
    map.on("dblclick", () => setMeasuring(false));
    map.on("zoomend", () => setMapZoom(map.getZoom()));

    return () => {
      setMapReady(false);
      map.remove();
      mapRef.current = null;
      baseLayerRef.current = null;
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
    loadPoints();
  }, [loadPoints]);

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
      if (point.latitude == null || point.longitude == null) {
        return;
      }
      L.marker([point.latitude, point.longitude], { icon: createHerbIcon(point) })
        .on("click", () => setSelectedPointId(point.id))
        .bindPopup(buildPopupContent(point, openEditForm, confirmDelete), {
          className: "bdis-herb-popup",
          maxWidth: 280,
          minWidth: 240,
        })
        .addTo(layer);
    });
  }, [confirmDelete, filteredPoints, openEditForm]);

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

  async function handleGrowthSubmit() {
    if (!selectedPoint?.id) {
      return;
    }
    if (!growthForm.collectorName?.trim()) {
      message.warning("请填写采集者");
      return;
    }

    setGrowthSubmitting(true);
    try {
      const next = await createGrowthRecord(selectedPoint.id, {
        ...growthForm,
        collectorName: growthForm.collectorName.trim(),
        dataSource: growthForm.dataSource || "map",
      });
      setGrowthRecords((current) => [next, ...current]);
      setPoints((current) =>
        current.map((point) =>
          point.id === selectedPoint.id ? { ...point, lastCollectedAt: next.collectedAt } : point,
        ),
      );
      setGrowthForm({ collectorName: "", growthStage: "", sampleWeight: undefined, remark: "" });
      message.success("采集记录已保存");
    } catch (error) {
      message.error(getMapPointRequestErrorMessage(error, "采集记录保存"));
    } finally {
      setGrowthSubmitting(false);
    }
  }

  function toggleMeasure() {
    setMeasuring((current) => !current);
    setMeasurePoints([]);
  }

  return (
    <div className={styles.shell}>
      <div className={styles.toolbar}>
        <Space.Compact>
          <Input
            allowClear
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onPressEnter={loadPoints}
            placeholder="搜索药材、地点或地址"
            prefix={<Search size={16} />}
          />
          <Button onClick={loadPoints}>查询</Button>
        </Space.Compact>
        <Select
          allowClear
          className={styles.filterSelect}
          placeholder="全部区县"
          options={districtOptions}
          value={districtFilter}
          onChange={setDistrictFilter}
        />
        <Select
          allowClear
          className={styles.filterSelect}
          placeholder="全部来源"
          options={sourceOptions}
          value={sourceFilter}
          onChange={setSourceFilter}
        />
        <Space>
          <Button icon={<RefreshCw size={16} />} onClick={loadPoints}>
            刷新
          </Button>
        </Space>
      </div>

      <div className={styles.contentGrid}>
        <aside className={styles.pointListPanel}>
          <div className={styles.panelHeader}>
            <strong>分布点列表</strong>
            <span>共 {filteredPoints.length} 条</span>
          </div>
          <Button type="primary" block onClick={() => openCreateForm(L.latLng(CHONGQING_CENTER))}>
            新增点位
          </Button>
          <div className={styles.pointList}>
            {filteredPoints.map((point) => (
              <button
                type="button"
                key={point.id}
                className={`${styles.pointItem} ${selectedPoint?.id === point.id ? styles.pointItemActive : ""}`}
                onClick={() => {
                  setSelectedPointId(point.id);
                  mapRef.current?.flyTo([point.latitude, point.longitude], Math.max(mapZoom, 11));
                }}
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
                <Tag color={point.status === 0 ? "default" : "green"}>{point.status === 0 ? "停用" : "启用"}</Tag>
              </button>
            ))}
            {filteredPoints.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无点位" />}
          </div>
        </aside>

      <div className={`${styles.mapWrap} ${activeLayer === "satellite" ? styles.satelliteMap : styles.lightMap}`}>
        {loading && (
          <div className={styles.loadingMask}>
            <Spin />
          </div>
        )}
        <div ref={mapElementRef} className={styles.mapCanvas} />
        <button
          type="button"
          className={`${styles.measureMapButton} ${measuring ? styles.measureMapButtonActive : ""}`}
          aria-label={measuring ? "结束测距" : "测距"}
          onClick={toggleMeasure}
        >
          <Ruler size={16} />
          <span>{measuring ? "结束测距" : "测距"}</span>
        </button>
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

        <aside className={styles.detailPanel}>
          {selectedPoint ? (
            <>
              <div className={styles.detailHeader}>
                <div>
                  <h3>{selectedPoint.locationName || selectedPoint.herbName}</h3>
                  <p>{[selectedPoint.district, selectedPoint.address].filter(Boolean).join(" · ") || "地图点位"}</p>
                </div>
                <Button type="text" icon={<X size={16} />} onClick={() => setSelectedPointId(undefined)} />
              </div>

              <div className={styles.detailSection}>
                <h4>基本信息</h4>
                <div className={styles.infoRows}>
                  <span>药材名称</span>
                  <strong>{selectedPoint.herbName}</strong>
                  <span>经纬度</span>
                  <strong>
                    {selectedPoint.longitude.toFixed(6)}, {selectedPoint.latitude.toFixed(6)}
                  </strong>
                  <span>数据来源</span>
                  <strong>{selectedPoint.dataSource || "暂无"}</strong>
                  <span>最近采集</span>
                  <strong>{formatDateTime(selectedPoint.lastCollectedAt)}</strong>
                </div>
              </div>

              <div className={styles.detailSection}>
                <h4>药材说明</h4>
                <p className={styles.detailText}>
                  {selectedPoint.efficacy || selectedPoint.distributionDesc || selectedPoint.herbDescription || "暂无说明"}
                </p>
              </div>

              <div className={styles.detailSection}>
                <div className={styles.sectionTitleRow}>
                  <h4>采集记录</h4>
                  <Tag>{growthRecords.length} 次</Tag>
                </div>
                <div className={styles.collectionForm}>
                  <Input
                    placeholder="采集者"
                    value={growthForm.collectorName}
                    onChange={(event) => setGrowthForm((current) => ({ ...current, collectorName: event.target.value }))}
                  />
                  <Input
                    placeholder="生长阶段，如花期/结果期"
                    value={growthForm.growthStage}
                    onChange={(event) => setGrowthForm((current) => ({ ...current, growthStage: event.target.value }))}
                  />
                  <Input
                    placeholder="采集重量 g"
                    type="number"
                    value={growthForm.sampleWeight}
                    onChange={(event) =>
                      setGrowthForm((current) => ({
                        ...current,
                        sampleWeight: event.target.value ? Number(event.target.value) : undefined,
                      }))
                    }
                  />
                  <Input
                    placeholder="备注"
                    value={growthForm.remark}
                    onChange={(event) => setGrowthForm((current) => ({ ...current, remark: event.target.value }))}
                  />
                  <Button type="primary" loading={growthSubmitting} onClick={handleGrowthSubmit}>
                    保存采集
                  </Button>
                </div>
                <Spin spinning={growthLoading}>
                  <div className={styles.collectionList}>
                    {growthRecords.map((record) => (
                      <div key={record.id} className={styles.collectionItem}>
                        <div>
                          <strong>{record.collectorName || "未知采集者"}</strong>
                          <span>{formatDateTime(record.collectedAt)}</span>
                        </div>
                        <p>
                          {[record.growthStage, record.sampleWeight ? `${record.sampleWeight} g` : undefined, record.weather]
                            .filter(Boolean)
                            .join(" · ") || "暂无采集详情"}
                        </p>
                      </div>
                    ))}
                    {growthRecords.length === 0 && (
                      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无采集记录" />
                    )}
                  </div>
                </Spin>
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
    </div>
  );
}
