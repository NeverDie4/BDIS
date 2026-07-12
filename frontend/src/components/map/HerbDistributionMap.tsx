"use client";

import { App, Button, Empty, Input, Space, Spin, Tag } from "antd";
import L, { type LatLng, type Map as LeafletMap } from "leaflet";
import { LocateFixed, Minus, Plus, RefreshCw, Ruler, Search, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  createMapPoint,
  deleteMapPoint,
  fetchMapPoints,
  getMapPointRequestErrorMessage,
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
  const mapElementRef = useRef<HTMLDivElement>(null);
  const miniMapElementRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const miniMapRef = useRef<LeafletMap | null>(null);
  const baseLayerRef = useRef<L.TileLayer | null>(null);
  const miniBaseLayerRef = useRef<L.TileLayer | null>(null);
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

  const activeLayerConfig = useMemo(
    () => MAP_LAYERS.find((layer) => layer.key === activeLayer) ?? MAP_LAYERS[0],
    [activeLayer],
  );

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
      dataSource: "map-demo",
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
      // tap: false, // Leaflet 1.9+ 中 tap 选项已被移除，在移动设备上默认启用点击延迟处理
      touchZoom: false,
      zoom: Math.max(map.getZoom() - 4, 3),
      zoomControl: false,
    });

    miniBaseLayerRef.current = createTileLayer(activeLayer).addTo(miniMap);
    miniMapRef.current = miniMap;

    return () => {
      miniMap.remove();
      miniMapRef.current = null;
      miniBaseLayerRef.current = null;
    };
  }, [activeLayer, mapReady]);

  useEffect(() => {
    const miniMap = miniMapRef.current;
    if (!miniMap) {
      return;
    }

    if (miniBaseLayerRef.current) {
      miniMap.removeLayer(miniBaseLayerRef.current);
    }
    miniBaseLayerRef.current = createTileLayer(activeLayer).addTo(miniMap);
  }, [activeLayer]);

  useEffect(() => {
    const map = mapRef.current;
    const miniMap = miniMapRef.current;
    if (!mapReady || !map || !miniMap) {
      return;
    }

    const syncMiniMap = () => {
      miniMap.setView(map.getCenter(), Math.max(map.getZoom() - 4, 3), { animate: false });
    };

    syncMiniMap();
    map.on("moveend zoomend", syncMiniMap);

    return () => {
      map.off("moveend zoomend", syncMiniMap);
    };
  }, [mapReady]);

  useEffect(() => {
    const task = window.setTimeout(() => void loadPoints(), 0);
    return () => window.clearTimeout(task);
  }, [loadPoints]);

  useEffect(() => {
    const layer = markersLayerRef.current;
    if (!layer) {
      return;
    }

    layer.clearLayers();
    points.forEach((point) => {
      if (point.latitude == null || point.longitude == null) {
        return;
      }
      L.marker([point.latitude, point.longitude], { icon: createHerbIcon(point) })
        .bindPopup(buildPopupContent(point, openEditForm, confirmDelete), {
          className: "bdis-herb-popup",
          maxWidth: 280,
          minWidth: 240,
        })
        .addTo(layer);
    });
  }, [confirmDelete, openEditForm, points]);

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
        setEmptyNoticeVisible(false);
        message.success("地图点位已新增");
      }
      setFormOpen(false);
    } catch (error) {
      message.error(getMapPointRequestErrorMessage(error, "地图点位保存"));
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
        <Space>
          <Button icon={<RefreshCw size={16} />} onClick={loadPoints}>
            刷新
          </Button>
          <Button
            type={measuring ? "primary" : "default"}
            icon={<Ruler size={16} />}
            onClick={toggleMeasure}
          >
            {measuring ? "结束测距" : "测距"}
          </Button>
        </Space>
      </div>

      <div className={styles.mapWrap}>
        {loading && (
          <div className={styles.loadingMask}>
            <Spin />
          </div>
        )}
        <div ref={mapElementRef} className={styles.mapCanvas} />
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
            <span className={styles.layerBadge}>图层</span>
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
