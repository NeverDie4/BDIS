import { Button, Empty, Image } from "antd";
import { Image as ImageIcon, X } from "lucide-react";
import type { HerbTableRecord } from "./types";
import styles from "./herbs.module.css";

type HerbDetailPanelProps = {
  herb: HerbTableRecord;
  onClose: () => void;
};

type InfoItem = {
  label: string;
  value?: string | number;
  wide?: boolean;
};

function hasValue(value?: string | number) {
  return value != null && String(value).trim() !== "";
}

function valueOrDash(value?: string | number) {
  return hasValue(value) ? value : "-";
}

function getGalleryTitle(herb: HerbTableRecord) {
  if (herb.medicinalPart) {
    return `${herb.medicinalPart}图像`;
  }
  return "药材图片";
}

export function HerbDetailPanel({ herb, onClose }: HerbDetailPanelProps) {
  const category = herb.categoryName || herb.category;
  const galleryItems = herb.coverImageUrl
    ? [{ url: herb.coverImageUrl, title: getGalleryTitle(herb), description: herb.herbName }]
    : [];
  const infoItems: InfoItem[] = [
    { label: "药材编号", value: herb.herbCode },
    { label: "药材名称", value: herb.herbName },
    { label: "拉丁名", value: herb.latinName },
    { label: "别名", value: herb.aliasName },
    { label: "所属分类", value: category },
    { label: "药用部位", value: herb.medicinalPart },
    { label: "分布地区", value: herb.distributionRegionText },
    { label: "功效", value: herb.efficacy, wide: true },
    { label: "描述", value: herb.description, wide: true },
  ];

  return (
    <aside className={styles.detailPanel}>
      <div className={styles.detailScroll}>
        <section className={styles.detailHero}>
          <Button
            type="text"
            aria-label="关闭药材详情"
            className={styles.detailCloseButton}
            icon={<X size={17} />}
            onClick={onClose}
          />
          <div className={styles.detailHeroText}>
            <h2>{valueOrDash(herb.herbName)}</h2>
            {hasValue(herb.latinName) ? <p>{herb.latinName}</p> : null}
            {hasValue(herb.aliasName) ? <span>别名：{herb.aliasName}</span> : null}
          </div>
          <div className={styles.detailHeroMeta}>
            {hasValue(category) ? <span className={styles.detailTag}>{category}</span> : null}
            {hasValue(herb.medicinalPart) ? (
              <span className={styles.detailTag}>{herb.medicinalPart}</span>
            ) : null}
          </div>
          {hasValue(herb.efficacy) ? (
            <p className={styles.detailHeroSummary}>{herb.efficacy}</p>
          ) : null}
        </section>

        <section className={styles.archiveSection}>
          <div className={styles.sectionHeading}>
            <h3>基本档案</h3>
            <p>药材基础信息与药性说明</p>
          </div>
          <div className={styles.archiveGrid}>
            {infoItems.map((item) => (
              <article
                className={`${styles.archiveItem} ${item.wide ? styles.archiveItemWide : ""}`}
                key={item.label}
              >
                <span>{item.label}</span>
                <strong>{valueOrDash(item.value)}</strong>
              </article>
            ))}
          </div>
        </section>

        <section className={styles.archiveSection}>
          <div className={styles.sectionHeading}>
            <h3>图片图鉴</h3>
            <p>展示该药材的相关图谱与样图</p>
          </div>
          {galleryItems.length > 0 ? (
            <div className={`${styles.galleryGrid} ${galleryItems.length === 1 ? styles.galleryGridSingle : ""}`}>
              {galleryItems.map((item) => (
                <article className={styles.galleryCard} key={item.url}>
                  <Image
                    className={styles.galleryImage}
                    src={item.url}
                    alt={item.title}
                    preview={{ mask: "预览图片" }}
                  />
                  <div className={styles.galleryCaption}>
                    <strong>{item.title}</strong>
                    <span>{item.description || "药材图片"}</span>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className={styles.galleryEmpty}>
              <Empty
                image={<ImageIcon size={36} />}
                description="暂无药材图鉴图片"
              />
            </div>
          )}
        </section>
      </div>
    </aside>
  );
}
