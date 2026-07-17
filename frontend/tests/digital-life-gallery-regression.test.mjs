import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import test from "node:test";

const pagePath = new URL("../src/app/digital-life/page.tsx", import.meta.url);
const cssPath = new URL("../src/app/digital-life/page.module.css", import.meta.url);
const apiPath = new URL("../src/lib/digital-life.ts", import.meta.url);
const routesPath = new URL("../src/config/routes/public.ts", import.meta.url);

test("数字生命展馆是公共导航并使用公开摘要接口", () => {
  assert.equal(existsSync(pagePath), true);
  assert.equal(existsSync(cssPath), true);

  const pageSource = readFileSync(pagePath, "utf8");
  const apiSource = readFileSync(apiPath, "utf8");
  const routesSource = readFileSync(routesPath, "utf8");

  assert.match(routesSource, /path:\s*"\/digital-life"/);
  assert.match(routesSource, /navLabel:\s*"数字生命展馆"/);
  assert.match(routesSource, /public:\s*true/);
  assert.match(apiSource, /getPublicDigitalLifeGallery/);
  assert.match(apiSource, /\/trace\/digital-life`/);
  assert.match(pageSource, /getPublicDigitalLifeGallery/);
  assert.match(pageSource, /\/trace\/digital-life\/\$\{encodeURIComponent\(archive\.traceCode\)\}/);
  assert.match(pageSource, /搜索药材、基地或任务/);
  assert.match(pageSource, /暂无公开的数字生命档案/);
  assert.doesNotMatch(pageSource, /\/growth|growth\/records/);
});
