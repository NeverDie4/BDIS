import Image from "next/image";
import type { CSSProperties, ReactNode } from "react";
import styles from "./ModuleHeroBanner.module.css";

const DEFAULT_MODULE_HERO_BACKGROUND = "/images/herbs/herb-title-banner.png";

export interface ModuleHeroBannerProps {
  title: ReactNode;
  description?: ReactNode;
  eyebrow?: ReactNode;
  sealText?: ReactNode;
  backgroundImage?: string;
  backgroundPosition?: string;
  decorationImage?: string;
  decorationAlt?: string;
  decorationPosition?: "right" | "center-right";
  actions?: ReactNode;
  extra?: ReactNode;
  variant?: "default" | "compact";
  className?: string;
}

function isLocalAssetPath(value: string) {
  return value.startsWith("/") && !value.startsWith("//");
}

export function ModuleHeroBanner({
  title,
  description,
  eyebrow,
  sealText,
  backgroundImage,
  backgroundPosition = "center",
  decorationImage,
  decorationAlt,
  decorationPosition = "right",
  actions,
  extra,
  variant = "default",
  className,
}: ModuleHeroBannerProps) {
  const resolvedBackgroundImage = backgroundImage ?? DEFAULT_MODULE_HERO_BACKGROUND;
  const safeBackgroundImage = isLocalAssetPath(resolvedBackgroundImage) ? resolvedBackgroundImage : undefined;
  const safeDecorationImage = decorationImage && isLocalAssetPath(decorationImage) ? decorationImage : undefined;
  const backgroundStyle: CSSProperties | undefined = safeBackgroundImage
    ? {
        backgroundImage: `linear-gradient(90deg, rgba(255, 250, 242, 0.7) 0%, rgba(255, 250, 242, 0.42) 34%, rgba(255, 250, 242, 0.12) 58%, rgba(255, 250, 242, 0) 76%), url("${safeBackgroundImage}")`,
        backgroundPosition,
        backgroundRepeat: "no-repeat",
        backgroundSize: "auto, cover",
      }
    : undefined;

  return (
    <section className={`${styles.moduleHero} ${styles[variant]} ${className ?? ""}`}>
      <div aria-hidden="true" className={styles.backgroundLayer} style={backgroundStyle} />
      {safeDecorationImage ? (
        <div
          aria-hidden={!decorationAlt}
          className={`${styles.decorationLayer} ${styles[decorationPosition]}`}
        >
          <div className={styles.decorationFrame}>
            <Image
              fill
              alt={decorationAlt ?? ""}
              className={styles.decorationImage}
              sizes="(max-width: 640px) 0px, 34vw"
              src={safeDecorationImage}
            />
          </div>
        </div>
      ) : null}

      <div className={styles.content}>
        {eyebrow || sealText ? (
          <div className={styles.eyebrowRow}>
            {eyebrow ? <span className={styles.eyebrow}>{eyebrow}</span> : null}
            {sealText ? <span className={styles.seal}>{sealText}</span> : null}
          </div>
        ) : null}
        <h1 className={styles.title}>{title}</h1>
        {description ? <p className={styles.description}>{description}</p> : null}
        {extra ? <div className={styles.extra}>{extra}</div> : null}
      </div>

      {actions ? <div className={styles.actionArea}>{actions}</div> : null}
    </section>
  );
}
