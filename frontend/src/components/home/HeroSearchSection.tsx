"use client";

import { SearchOutlined } from "@ant-design/icons";
import { Button, Input } from "antd";
import { useRouter } from "next/navigation";
import { useState } from "react";
import styles from "./HeroSearchSection.module.css";

const HOT_KEYWORDS = ["黄芪", "当归", "丹参", "三七", "灵芝", "石斛"];

export function HeroSearchSection() {
  const router = useRouter();
  const [keyword, setKeyword] = useState("");

  const runSearch = (value: string) => {
    const nextKeyword = value.trim();
    const searchParams = nextKeyword ? `?keyword=${encodeURIComponent(nextKeyword)}` : "";
    router.push(`/herbs${searchParams}`);
  };

  return (
    <section className={styles.hero} aria-label="首页药材资源搜索">
      <div className={styles.overlay} />
      <div className={styles.paperTexture} />
      <div className={styles.content}>
        <p className={styles.kicker}>HERBARIUM RESEARCH PORTAL</p>
        <div className={styles.titleRow}>
          <h1>生物医药数字信息系统</h1>
          <span className={styles.titleSeal}>本草</span>
        </div>
        <div className={styles.searchBox}>
          <SearchOutlined className={styles.searchIcon} />
          <Input
            aria-label="搜索药材资源"
            className={styles.searchInput}
            placeholder="搜索药材名称、科属、产地、功效或文献..."
            variant="borderless"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onPressEnter={() => runSearch(keyword)}
          />
          <Button className={styles.searchButton} type="primary" onClick={() => runSearch(keyword)}>
            搜索
          </Button>
        </div>
        <div className={styles.hotSearches}>
          <span>热门搜索：</span>
          {HOT_KEYWORDS.map((item) => (
            <button key={item} type="button" onClick={() => runSearch(item)}>
              {item}
            </button>
          ))}
        </div>
      </div>
    </section>
  );
}
