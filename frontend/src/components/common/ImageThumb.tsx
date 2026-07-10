"use client";

import { Image } from "antd";
import { PictureOutlined } from "@ant-design/icons";
import styles from "./ImageThumb.module.css";

type ImageThumbProps = {
  src?: string;
  alt: string;
  size?: number;
  shape?: "square" | "rounded" | "circle";
  fallback?: React.ReactNode;
  className?: string;
};

export function ImageThumb({
  src,
  alt,
  size = 64,
  shape = "rounded",
  fallback,
  className,
}: ImageThumbProps) {
  const style = { width: size, height: size };

  if (!src) {
    return (
      <div className={`${styles.thumb} ${styles[shape]} ${className ?? ""}`} style={style} aria-label={alt}>
        {fallback ?? <PictureOutlined />}
      </div>
    );
  }

  return (
    <Image
      alt={alt}
      className={`${styles.image} ${styles[shape]} ${className ?? ""}`}
      height={size}
      preview={false}
      src={src}
      width={size}
    />
  );
}
