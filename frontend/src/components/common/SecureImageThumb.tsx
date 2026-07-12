"use client";

import { Image, Skeleton } from "antd";
import { useEffect, useState } from "react";
import { request } from "@/lib/request";

export function SecureImageThumb({ src, alt }: { src: string; alt: string }) {
  const [objectUrl, setObjectUrl] = useState<string>();

  useEffect(() => {
    let active = true;
    let nextUrl: string | undefined;
    const requestUrl = src.startsWith("/api/") ? src.slice(4) : src;
    request
      .get<Blob>(requestUrl, { responseType: "blob" })
      .then((response) => {
        nextUrl = URL.createObjectURL(response.data);
        if (active) setObjectUrl(nextUrl);
      })
      .catch(() => {
        if (active) setObjectUrl(undefined);
      });
    return () => {
      active = false;
      if (nextUrl) URL.revokeObjectURL(nextUrl);
    };
  }, [src]);

  return objectUrl ? (
    <Image alt={alt} height={86} preview src={objectUrl} width={86} />
  ) : (
    <Skeleton.Image active style={{ height: 86, width: 86 }} />
  );
}
