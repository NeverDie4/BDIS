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