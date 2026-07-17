import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const root = new URL("../src/", import.meta.url);

async function readSource(relativePath) {
  return readFile(new URL(relativePath, root), "utf8");
}

test("生长数据复用统一本草标题栏", async () => {
  const source = await readSource("app/growth/page.tsx");

  assert.match(source, /import \{ ModuleHeroBanner \}/);
  assert.match(source, /<ModuleHeroBanner/);
  assert.match(source, /eyebrow="GROWTH DATA ARCHIVE"/);
  assert.match(source, /sealText="生长"/);
  assert.doesNotMatch(source, /<ModuleHeroBanner\s+variant="compact"/);
  assert.doesNotMatch(source, /className=\{styles\.growthHero\}/);
  assert.doesNotMatch(source, /heroIllustrationUrl/);
});

test("growth detail keeps the archive card visual and the stable scroll chain", async () => {
  const css = await readSource("app/growth/page.module.css");

  assert.match(
    css,
    /\.workspaceLayout \.detailPanel\s*\{[^}]*height:\s*calc\(100vh - 92px\);[^}]*flex-direction:\s*column;/s,
  );
  assert.match(
    css,
    /\.detailPanel > :global\(\.ant-spin-nested-loading\)\s*\{[^}]*flex:\s*1;[^}]*min-height:\s*0;[^}]*overflow:\s*hidden;/s,
  );
  assert.match(
    css,
    /\.detailPanel \.drawerContent\s*\{[^}]*height:\s*100%;[^}]*overflow-y:\s*auto;/s,
  );
  assert.match(css, /\.workspaceLayout \.detailPanel\s*\{[^}]*border-radius:\s*20px;/s);
  assert.doesNotMatch(
    css,
    /Growth workspace visual refinement/,
  );
});

test("生长数据保留现有详情标签交互", async () => {
  const source = await readSource("app/growth/page.tsx");

  assert.match(source, /className=\{styles\.detailTabs\}/);
  assert.match(source, /detailTab === value/);
  assert.match(source, /setDetailTab\(value\)/);
});

test("生长数据在内容不足前切换单栏", async () => {
  const css = await readSource("app/growth/page.module.css");

  assert.match(
    css,
    /@media \(max-width:\s*1280px\)[\s\S]*?\.workspaceLayout\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)/,
  );
  assert.match(
    css,
    /@media \(max-width:\s*900px\)[\s\S]*?\.workspaceMain \.filterGrid/,
  );
});

test("herb workspace provides desktop table scrolling and mobile document scrolling", async () => {
  const css = await readSource("components/herbs/herbs.module.css");

  assert.match(css, /\.tableArea\s*\{[^}]*overflow:\s*auto;/s);
  assert.match(
    css,
    /@media \(max-width:\s*899px\)[\s\S]*?\.herbPage\s*\{[^}]*height:\s*auto;[^}]*overflow:\s*visible;/s,
  );
  assert.match(
    css,
    /@media \(max-width:\s*899px\)[\s\S]*?\.detailScroll\s*\{[^}]*height:\s*auto;[^}]*overflow-y:\s*visible;/s,
  );
});
