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
  assert.doesNotMatch(source, /className=\{styles\.growthHero\}/);
  assert.doesNotMatch(source, /heroIllustrationUrl/);
});

test("生长数据保持双栏工作台并使用扁平纸张详情", async () => {
  const css = await readSource("app/growth/page.module.css");

  assert.match(
    css,
    /grid-template-columns:\s*minmax\(0,\s*2\.55fr\)\s+minmax\(340px,\s*1fr\)/,
  );
  assert.match(
    css,
    /\.workspaceLayout \.detailPanel\s*\{[^}]*width:\s*auto;[^}]*border-radius:\s*2px;[^}]*box-shadow:\s*none;/s,
  );
  assert.match(
    css,
    /\.detailPanel \.archiveSummary\s*\{[^}]*border-radius:\s*0;[^}]*background:\s*transparent;/s,
  );
  assert.doesNotMatch(
    css,
    /\.detailPanel \.archiveSummary\s*\{[^}]*linear-gradient/s,
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
    /@media \(max-width:\s*1260px\)[\s\S]*?\.workspaceLayout\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)/,
  );
  assert.match(
    css,
    /@media \(max-width:\s*720px\)[\s\S]*?\.workspaceMain \.filterGrid/,
  );
});
