const DEVICE_ID_KEY = "bdis_device_id";

export function getOrCreateDeviceId() {
  if (typeof window === "undefined") {
    return undefined;
  }
  const existing = window.localStorage.getItem(DEVICE_ID_KEY);
  if (existing) {
    return existing;
  }
  const generated =
    typeof crypto.randomUUID === "function"
      ? crypto.randomUUID()
      : `${Date.now()}_${Math.random().toString(36).slice(2)}`;
  window.localStorage.setItem(DEVICE_ID_KEY, generated);
  return generated;
}
