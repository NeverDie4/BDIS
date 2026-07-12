const DEFAULT_AUTHENTICATED_PATH = "/dashboard";

export function resolveSafeReturnUrl(
  value: string | null | undefined,
  fallback = DEFAULT_AUTHENTICATED_PATH,
) {
  if (!value || !value.startsWith("/") || value.startsWith("//") || value.includes("\\")) {
    return fallback;
  }

  try {
    const base = new URL("https://bdis.local");
    const target = new URL(value, base);
    return target.origin === base.origin
      ? `${target.pathname}${target.search}${target.hash}`
      : fallback;
  } catch {
    return fallback;
  }
}

export function buildLoginUrl(returnUrl?: string) {
  const safeReturnUrl = resolveSafeReturnUrl(returnUrl, DEFAULT_AUTHENTICATED_PATH);
  return `/login?returnUrl=${encodeURIComponent(safeReturnUrl)}`;
}

export function getCurrentRelativeUrl() {
  if (typeof window === "undefined") {
    return DEFAULT_AUTHENTICATED_PATH;
  }
  return `${window.location.pathname}${window.location.search}${window.location.hash}`;
}
