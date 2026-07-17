const configuredApiBaseUrl = String(
  import.meta.env.VITE_API_BASE_URL || "",
).replace(/\/+$/, "");

const config = {
  baseUrl: configuredApiBaseUrl,
  mobilePrefix: "/api/mobile/herb",
  timeout: 30000,
};

export default config;
