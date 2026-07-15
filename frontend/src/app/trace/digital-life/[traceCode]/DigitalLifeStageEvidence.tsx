"use client";

import { Image } from "antd";
import { ImageOff } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  resolveDigitalLifeResourceUrl,
  type PublicDigitalLifeImageApi,
  type PublicDigitalLifeStageApi,
} from "@/lib/digital-life";
import styles from "./page.module.css";

const IMAGE_TYPE_LABELS: Record<string, string> = {
  leaf: "叶片",
  stem: "茎部",
  root: "根部",
  flower: "花",
  fruit: "果实",
  whole_plant: "整株",
  medicinal_part: "药用部位",
  environment: "生长环境",
  other: "其他",
};

type NumericMetric = {
  key:
    | "plantHeight"
    | "stemDiameter"
    | "temperature"
    | "humidity"
    | "soilMoisture"
    | "soilPh"
    | "light";
  label: string;
  unit: string;
};

const CORE_METRICS: NumericMetric[] = [
  { key: "plantHeight", label: "株高", unit: "cm" },
  { key: "temperature", label: "温度", unit: "℃" },
  { key: "humidity", label: "湿度", unit: "%" },
];

const OTHER_METRICS: NumericMetric[] = [
  { key: "stemDiameter", label: "茎粗", unit: "mm" },
  { key: "soilMoisture", label: "土壤湿度", unit: "%" },
  { key: "soilPh", label: "土壤 pH", unit: "" },
  { key: "light", label: "光照", unit: "lx" },
];

type Props = {
  stages: PublicDigitalLifeStageApi[];
  currentStageIndex: number;
  speciesName?: string;
};

function formatDateTime(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString("zh-CN", { hour12: false });
}

function imageTypeName(image: PublicDigitalLifeImageApi) {
  if (image.imageTypeName && image.imageTypeName !== image.imageType) return image.imageTypeName;
  return IMAGE_TYPE_LABELS[image.imageType || "other"] || "其他";
}

function precision(value: number) {
  return String(value).split(".")[1]?.length || 0;
}

function useAnimatedNumber(value?: number | null) {
  const previousValueRef = useRef<number | null | undefined>(value);
  const [displayValue, setDisplayValue] = useState<number | null>(value ?? null);

  useEffect(() => {
    const from = previousValueRef.current;
    previousValueRef.current = value;
    if (from == null || value == null) {
      setDisplayValue(value ?? null);
      return;
    }
    const duration = 750;
    const startedAt = performance.now();
    let frameId = 0;
    const animate = (now: number) => {
      const ratio = Math.min(1, (now - startedAt) / duration);
      const eased = 1 - Math.pow(1 - ratio, 3);
      setDisplayValue(from + (value - from) * eased);
      if (ratio < 1) frameId = requestAnimationFrame(animate);
    };
    frameId = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(frameId);
  }, [value]);

  return displayValue;
}

function AnimatedMetric({
  metric,
  value,
  previousValue,
  prominent = false,
}: {
  metric: NumericMetric;
  value?: number | null;
  previousValue?: number | null;
  prominent?: boolean;
}) {
  const animatedValue = useAnimatedNumber(value);
  const digits = value == null ? 0 : precision(value);
  const difference = previousValue == null || value == null ? null : value - previousValue;
  const differencePrecision =
    previousValue == null || value == null
      ? 0
      : Math.max(precision(value), precision(previousValue));
  return (
    <div className={prominent ? styles.coreMetric : styles.secondaryMetric}>
      <span>{metric.label}</span>
      <strong>
        {animatedValue == null ? "-" : animatedValue.toFixed(digits)}
        {animatedValue != null && metric.unit ? <small>{metric.unit}</small> : null}
      </strong>
      {difference != null ? (
        <em>
          {difference > 0 ? "+" : ""}
          {difference.toFixed(differencePrecision)}
          {metric.unit}
        </em>
      ) : null}
    </div>
  );
}

export default function DigitalLifeStageEvidence({
  stages,
  currentStageIndex,
  speciesName,
}: Props) {
  const currentStage = stages[currentStageIndex];
  const previousStage = currentStageIndex > 0 ? stages[currentStageIndex - 1] : undefined;
  const images = useMemo(
    () =>
      [...(currentStage?.images || [])].sort(
        (left, right) => Number(Boolean(right.primaryImage)) - Number(Boolean(left.primaryImage)),
      ),
    [currentStage?.images],
  );
  const [imageSelection, setSelectedImageIndex] = useState({
    stageIndex: currentStageIndex,
    imageIndex: 0,
  });
  const selectedImageIndex =
    imageSelection.stageIndex === currentStageIndex
      ? Math.min(imageSelection.imageIndex, Math.max(0, images.length - 1))
      : 0;
  const selectedImage = images[selectedImageIndex];

  useEffect(() => {
    const preloadUrls = [
      ...(currentStage?.images || []),
      ...(stages[currentStageIndex + 1]?.images || []),
    ].map((image) => resolveDigitalLifeResourceUrl(image.imageUrl));
    preloadUrls.forEach((url) => {
      if (!url) return;
      const image = new window.Image();
      image.src = url;
    });
  }, [currentStage?.images, currentStageIndex, stages]);

  return (
    <section className={styles.stageEvidence}>
      {selectedImage ? (
        <div key={`${currentStageIndex}-${selectedImageIndex}`} className={styles.imageStage}>
          <figure>
            <Image
              src={resolveDigitalLifeResourceUrl(selectedImage.imageUrl)}
              alt={`${speciesName || "药材"}${imageTypeName(selectedImage)}`}
              preview
            />
            <figcaption>
              <span>
                <strong>{imageTypeName(selectedImage)}</strong> ·{" "}
                {formatDateTime(selectedImage.uploadTime)}
              </span>
              <span>
                {selectedImage.uploaderName
                  ? `上传人：${selectedImage.uploaderName}`
                  : "上传人未记录"}{" "}
                · {selectedImageIndex + 1} / {images.length}
              </span>
            </figcaption>
          </figure>
          {images.length > 1 ? (
            <div className={styles.imageThumbnails} aria-label="阶段图片缩略图">
              {images.map((image, index) => (
                <button
                  key={`${image.imageUrl}-${index}`}
                  type="button"
                  className={index === selectedImageIndex ? styles.thumbnailActive : ""}
                  onClick={() =>
                    setSelectedImageIndex({ stageIndex: currentStageIndex, imageIndex: index })
                  }
                  title={`查看${imageTypeName(image)}`}
                >
                  <Image
                    src={resolveDigitalLifeResourceUrl(image.imageUrl)}
                    alt={imageTypeName(image)}
                    preview={false}
                  />
                </button>
              ))}
            </div>
          ) : null}
        </div>
      ) : (
        <div className={styles.imageEmpty}>
          <ImageOff size={30} />
          <strong>当前阶段暂无现场影像</strong>
        </div>
      )}

      <div className={styles.coreMetricGrid}>
        {CORE_METRICS.map((metric) => (
          <AnimatedMetric
            key={metric.key}
            metric={metric}
            value={currentStage?.metrics?.[metric.key]}
            previousValue={previousStage?.metrics?.[metric.key]}
            prominent
          />
        ))}
      </div>
      <div className={styles.secondaryMetricGrid}>
        {OTHER_METRICS.map((metric) => (
          <AnimatedMetric
            key={metric.key}
            metric={metric}
            value={currentStage?.metrics?.[metric.key]}
            previousValue={previousStage?.metrics?.[metric.key]}
          />
        ))}
        <div className={styles.textMetric}>
          <span>叶色</span>
          <strong>{currentStage?.metrics?.leafColor || "-"}</strong>
        </div>
        <div className={styles.textMetric}>
          <span>开花情况</span>
          <strong>{currentStage?.metrics?.floweringStatus || "-"}</strong>
        </div>
      </div>
    </section>
  );
}
