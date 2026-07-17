"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { ArrowRight, CalendarDays, Layers3, MapPin, Search } from "lucide-react";
import { SiteLayout } from "@/components/layout/SiteLayout";
import {
  getPublicDigitalLifeGallery,
  type PublicDigitalLifeSummaryApi,
} from "@/lib/digital-life";
import styles from "./page.module.css";

function firstDisplayCharacter(value?: string) {
  const normalized = value?.trim() ?? "";
  const chineseCharacter = normalized.match(/[\u3400-\u9fff]/)?.[0];
  return chineseCharacter ?? "药";
}

function displayText(value: string | undefined, fallback: string) {
  return value?.trim() || fallback;
}

function formatDate(value?: string) {
  if (!value) return "时间待完善";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "时间待完善";
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

function formatPeriod(archive: PublicDigitalLifeSummaryApi) {
  if (!archive.startTime && !archive.endTime) return "观测周期待完善";
  const start = formatDate(archive.startTime);
  const end = formatDate(archive.endTime ?? archive.startTime);
  return start === end ? start : `${start} - ${end}`;
}

export default function DigitalLifeGalleryPage() {
  const [archives, setArchives] = useState<PublicDigitalLifeSummaryApi[]>([]);
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);

  const loadArchives = useCallback(async (searchKeyword?: string) => {
    setLoading(true);
    setLoadFailed(false);
    try {
      setArchives(await getPublicDigitalLifeGallery(searchKeyword));
    } catch {
      setArchives([]);
      setLoadFailed(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadArchives();
  }, [loadArchives]);

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void loadArchives(keyword);
  };

  return (
    <SiteLayout>
      <div className={styles.page}>
        <section className={styles.hero}>
          <div className={styles.heroContent}>
            <span className={styles.eyebrow}>PUBLIC DIGITAL HERBARIUM</span>
            <h1>数字生命展馆</h1>
            <p>连续观测档案公开目录</p>
          </div>
          <div className={styles.archiveCount} aria-label={`公开档案 ${archives.length} 份`}>
            <strong>{loading ? "--" : archives.length}</strong>
            <span>公开档案</span>
          </div>
        </section>

        <section className={styles.gallerySection}>
          <header className={styles.sectionHeader}>
            <div>
              <span>ARCHIVE COLLECTION</span>
              <h2>馆藏档案</h2>
            </div>
            <form className={styles.searchForm} onSubmit={handleSearch}>
              <Search size={17} aria-hidden="true" />
              <input
                aria-label="搜索公开数字生命档案"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="搜索药材、基地或任务"
              />
              <button type="submit">搜索</button>
            </form>
          </header>

          {loading ? (
            <div className={styles.statePanel} role="status">
              <span className={styles.loader} />
              <strong>正在整理公开档案</strong>
            </div>
          ) : loadFailed ? (
            <div className={styles.statePanel}>
              <strong>档案目录暂时无法加载</strong>
              <button type="button" onClick={() => void loadArchives(keyword)}>
                重新加载
              </button>
            </div>
          ) : archives.length === 0 ? (
            <div className={styles.statePanel}>
              <strong>暂无公开的数字生命档案</strong>
              <span>完成审核并公开的连续观测任务将在此展示</span>
            </div>
          ) : (
            <div className={styles.archiveGrid}>
              {archives.map((archive) => (
                <Link
                  className={styles.archiveCard}
                  href={`/trace/digital-life/${encodeURIComponent(archive.traceCode)}`}
                  key={archive.traceCode}
                >
                  <div className={styles.cardTop}>
                    <span className={styles.herbMark}>
                      {firstDisplayCharacter(archive.speciesName)}
                    </span>
                    <span className={styles.publicTag}>公开档案</span>
                  </div>
                  <div className={styles.cardBody}>
                    <p className={styles.taskCode}>
                      {displayText(archive.taskCode, archive.traceCode)}
                    </p>
                    <h3>{displayText(archive.speciesName, "药材档案")}</h3>
                    <strong>{displayText(archive.taskName, "连续观测任务")}</strong>
                    <p className={styles.description}>
                      {displayText(archive.description, "连续观测档案信息待完善")}
                    </p>
                  </div>
                  <dl className={styles.cardFacts}>
                    <div>
                      <MapPin size={15} aria-hidden="true" />
                      <dt>观测基地</dt>
                      <dd>{displayText(archive.baseName, "基地信息待完善")}</dd>
                    </div>
                    <div>
                      <CalendarDays size={15} aria-hidden="true" />
                      <dt>观测周期</dt>
                      <dd>{formatPeriod(archive)}</dd>
                    </div>
                  </dl>
                  <footer>
                    <span>
                      <Layers3 size={16} aria-hidden="true" />
                      {archive.stageCount ?? 0} 个公开阶段
                    </span>
                    <span className={styles.openArchive}>
                      查看完整档案 <ArrowRight size={16} aria-hidden="true" />
                    </span>
                  </footer>
                </Link>
              ))}
            </div>
          )}
        </section>
      </div>
    </SiteLayout>
  );
}
