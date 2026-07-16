import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const root = new URL("../src/", import.meta.url);

async function readSource(relativePath) {
  return readFile(new URL(relativePath, root), "utf8");
}

test("分布地图复用统一本草标题栏且不保留旧标题组件", async () => {
  const source = await readSource("app/map/page.tsx");

  assert.match(source, /import \{ ModuleHeroBanner \}/);
  assert.match(source, /<ModuleHeroBanner/);
  assert.match(source, /eyebrow="CHONGQING DISTRIBUTION"/);
  assert.match(source, /sealText="分布"/);
  assert.doesNotMatch(source, /PageBanner/);
});

test("分布地图和生长数据使用同一标准标题栏高度", async () => {
  const css = await readSource("components/layout/ModuleHeroBanner.module.css");

  assert.match(css, /\.moduleHero\s*\{[^}]*min-height:\s*164px;/s);
});

test("分布地图使用紧凑三栏纸张布局和扁平详情", async () => {
  const css = await readSource("components/map/HerbDistributionMap.module.css");

  assert.match(
    css,
    /grid-template-columns:\s*minmax\(280px,\s*0\.8fr\)\s+minmax\(560px,\s*1\.55fr\)\s+minmax\(340px,\s*1fr\)/,
  );
  assert.match(css, /\.pointList\s*\{[^}]*gap:\s*0;/s);
  assert.match(css, /\.pointItem\s*\{[^}]*border-bottom:/s);
  assert.match(css, /\.detailHero\s*\{[^}]*background:\s*transparent;/s);
  assert.match(css, /\.coordinateRow\s*\{[^}]*border-radius:\s*0;/s);
  assert.doesNotMatch(css, /\.detailHero\s*\{[^}]*linear-gradient/s);
});

test("分布地图详情保持现有无标签交互结构", async () => {
  const source = await readSource("components/map/HerbDistributionMap.tsx");

  assert.doesNotMatch(source, /<Tabs|items=\{detailTabs\}|activeKey=/);
  assert.match(source, /className=\{styles\.detailHeader\}/);
  assert.match(source, /className=\{styles\.detailHero\}/);
  assert.match(source, /className=\{styles\.coordinateRow\}/);
});

test("分布地图在网格最小宽度之前切换响应式栏数", async () => {
  const css = await readSource("components/map/HerbDistributionMap.module.css");

  assert.match(
    css,
    /@media \(max-width:\s*1230px\)[\s\S]*?\.contentGrid\s*\{[^}]*grid-template-columns:\s*290px\s+minmax\(460px,\s*1fr\)/,
  );
  assert.match(
    css,
    /@media \(max-width:\s*800px\)[\s\S]*?\.contentGrid\s*\{[^}]*grid-template-columns:\s*1fr/,
  );
});
