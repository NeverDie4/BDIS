"use client";

import Image from "next/image";

import styles from "./HerbEssenceStrip.module.css";

interface HerbEssenceItem {
  id: string;
  name: string;
  description: string;
  origin: string;
  imageSrc: string;
  imageAlt: string;
}

const herbEssenceItems: HerbEssenceItem[] = [
  {
    id: "renshen",
    name: "人参",
    description: "大补元气，复脉固脱",
    origin: "吉林 · 长白山",
    imageSrc: "/images/herbs/showcase/renshen.png",
    imageAlt: "人参药材图",
  },
  {
    id: "huangqi",
    name: "黄芪",
    description: "补气升阳，固表止汗",
    origin: "甘肃 · 岷县",
    imageSrc: "/images/herbs/showcase/huangqi.png",
    imageAlt: "黄芪药材图",
  },
  {
    id: "gouqi",
    name: "枸杞",
    description: "滋补肝肾，益精明目",
    origin: "宁夏 · 中宁",
    imageSrc: "/images/herbs/showcase/gouqi.png",
    imageAlt: "枸杞药材图",
  },
  {
    id: "danshen",
    name: "丹参",
    description: "活血祛瘀，通经止痛",
    origin: "山东 · 临沂",
    imageSrc: "/images/herbs/showcase/danshen.png",
    imageAlt: "丹参药材图",
  },
  {
    id: "shihu",
    name: "石斛",
    description: "益胃生津，滋阴清热",
    origin: "浙江 · 雁荡山",
    imageSrc: "/images/herbs/showcase/shihu.png",
    imageAlt: "石斛药材图",
  },
  {
    id: "danggui",
    name: "当归",
    description: "补血活血，调经止痛",
    origin: "甘肃 · 岷县",
    imageSrc: "/images/herbs/showcase/danggui.png",
    imageAlt: "当归药材图",
  },
  {
    id: "lingzhi",
    name: "灵芝",
    description: "扶正固本，安神益智",
    origin: "浙江 · 龙泉",
    imageSrc: "/images/herbs/showcase/lingzhi.png",
    imageAlt: "灵芝药材图",
  },
  {
    id: "bohe",
    name: "薄荷",
    description: "疏散风热，清利头目",
    origin: "江苏 · 苏州",
    imageSrc: "/images/herbs/showcase/bohe.png",
    imageAlt: "薄荷药材图",
  },
];

function renderHerbItem(item: HerbEssenceItem, keyPrefix: string) {
  return (
    <article className={styles.item} key={`${keyPrefix}-${item.id}`}>
      <div className={styles.imageFrame}>
        {item.imageSrc ? (
          <Image alt={item.imageAlt} className={styles.image} height={96} src={item.imageSrc} width={148} />
        ) : (
          <div aria-label={`${item.name}图片占位`} className={styles.imagePlaceholder} role="img">
            <span>{item.name.slice(0, 1)}</span>
          </div>
        )}
      </div>
      <h3>{item.name}</h3>
      <p className={styles.description}>{item.description}</p>
      <p className={styles.origin}>{item.origin}</p>
    </article>
  );
}

export function HerbEssenceStrip() {
  return (
    <section aria-label="本草精华药材陈列" className={styles.strip}>
      <div className={styles.viewport}>
        <div className={styles.track}>
          <div className={styles.segment}>{herbEssenceItems.map((item) => renderHerbItem(item, "primary"))}</div>
          <div aria-hidden="true" className={`${styles.segment} ${styles.duplicate}`}>
            {herbEssenceItems.map((item) => renderHerbItem(item, "duplicate"))}
          </div>
        </div>
      </div>
    </section>
  );
}
