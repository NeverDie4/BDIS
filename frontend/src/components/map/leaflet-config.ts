import L from "leaflet";

export const CHONGQING_CENTER: [number, number] = [29.56301, 106.55156];

export const MAP_LAYERS = [
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

export type MapLayerKey = (typeof MAP_LAYERS)[number]["key"];

export function createTileLayer(layerKey: MapLayerKey) {
  const layer = MAP_LAYERS.find((item) => item.key === layerKey) ?? MAP_LAYERS[0];
  return L.tileLayer(layer.url, {
    attribution: "&copy; Google",
    maxNativeZoom: 18,
    maxZoom: 22,
    subdomains: ["0", "1", "2", "3"],
  });
}

export function getPreviewLayerKey(layerKey: MapLayerKey): MapLayerKey {
  return layerKey === "satellite" ? "standard" : "satellite";
}

export function createPreviewTileLayer(layerKey: MapLayerKey) {
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
