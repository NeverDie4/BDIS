import { request } from "@/lib/request";

export async function fetchProtectedFileBlob(url: string) {
  const response = await request.get<Blob>(toApiRequestUrl(url), { responseType: "blob" });
  return response.data;
}

function toApiRequestUrl(value: string) {
  if (/^https?:\/\//i.test(value)) {
    return value;
  }
  return value.startsWith("/api/") ? value.slice("/api".length) : value;
}
