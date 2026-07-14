import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const root = new URL("../src/", import.meta.url);

async function readSource(relativePath) {
  return readFile(new URL(relativePath, root), "utf8");
}

function section(source, start, end) {
  const startIndex = source.indexOf(start);
  const endIndex = source.indexOf(end, startIndex + start.length);
  assert.notEqual(startIndex, -1, `missing section start: ${start}`);
  assert.notEqual(endIndex, -1, `missing section end: ${end}`);
  return source.slice(startIndex, endIndex);
}

test("药材分类不展示或提交状态字段", async () => {
  const source = await readSource("components/herbs/HerbResourceClient.tsx");
  const categorySubmit = section(
    source,
    '} else if (activeTab === "categories") {',
    "} else {",
  );
  const categoryColumns = section(source, "const categoryColumns", "const baseColumns");
  const formSource = source.slice(source.indexOf("function renderFormFields()"));
  const categoryForm = section(
    formSource,
    'if (activeTab === "categories") {',
    "return <>",
  );

  assert.doesNotMatch(categorySubmit, /status\s*:/);
  assert.doesNotMatch(categoryColumns, /T\.status|dataIndex:\s*"status"/);
  assert.doesNotMatch(categoryForm, /name="status"|T\.status/);
});

test("药材分类 API 不暴露状态查询或写入参数", async () => {
  const source = await readSource("lib/herbs.ts");
  const payload = section(source, "export type DictItemPayload", "export type HerbBasePayload");
  const fetcher = section(source, "export function fetchHerbCategories", "export function createHerbCategory");

  assert.doesNotMatch(payload, /status\??:/);
  assert.match(fetcher, /fetchHerbCategories\(\)/);
  assert.doesNotMatch(fetcher, /status|\{\s*status\s*\}/);
});

test("药材资源批量删除必须汇报部分成功并刷新当前页签", async () => {
  const source = await readSource("components/herbs/HerbResourceClient.tsx");

  assert.match(source, /Promise\.allSettled/);
  assert.match(source, /const successCount = results\.filter/);
  assert.match(source, /const failedCount = results\.length - successCount/);
  assert.match(source, /await reloadCurrentTab\(\)/);
  assert.doesNotMatch(source, /await Promise\.all\(/);
});

test("药材、分类和基地管理按钮按真实权限显示", async () => {
  const clientSource = await readSource("components/herbs/HerbResourceClient.tsx");
  const toolbarSource = await readSource("components/herbs/HerbActionToolbar.tsx");
  const tableSource = await readSource("components/herbs/HerbTable.tsx");

  for (const permission of [
    "herb:species:create",
    "herb:species:update",
    "herb:species:delete",
    "dictionary:manage",
    "map:base:manage",
  ]) {
    assert.match(clientSource, new RegExp(permission));
  }
  assert.match(toolbarSource, /canCreate/);
  assert.match(toolbarSource, /canEdit/);
  assert.match(toolbarSource, /canDelete/);
  assert.match(tableSource, /canEdit/);
});
