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

test("药材资源三个页签均不展示或提交状态属性并重排剩余列", async () => {
  const clientSource = await readSource("components/herbs/HerbResourceClient.tsx");
  const tableSource = await readSource("components/herbs/HerbTable.tsx");
  const filterSource = await readSource("components/herbs/HerbFilterBar.tsx");
  const detailSource = await readSource("components/herbs/HerbDetailPanel.tsx");
  const typeSource = await readSource("components/herbs/types.ts");
  const apiSource = await readSource("lib/herbs.ts");

  const baseColumns = section(clientSource, "const baseColumns", "function renderTable");
  const categoryTable = section(clientSource, "<Table<DictItemApi>", "/>" );
  const baseTable = section(clientSource, "<Table<HerbBaseApi>", "/>" );
  const hero = section(clientSource, "<ModuleHeroBanner", "/>" );
  const formFields = section(clientSource, "function renderFormFields", "const tabName");
  const exports = section(clientSource, "function handleExport", "const categoryColumns");

  assert.doesNotMatch(tableSource, /dataIndex:\s*"status"|statusText|<Tag/);
  assert.doesNotMatch(baseColumns, /T\.status|dataIndex:\s*"status"|<Tag/);
  assert.doesNotMatch(formFields, /name="status"|T\.status|statusOptions/);
  assert.doesNotMatch(exports, /T\.status|statusText|formatStatus/);
  assert.doesNotMatch(filterSource, /name="status"|statusOptions|statusFilter/);
  assert.doesNotMatch(detailSource, /key:\s*"status"|statusText|<Tag/);
  assert.doesNotMatch(typeSource, /status\??:|statusText\??:/);
  assert.doesNotMatch(apiSource, /status\??:|statusText\??:|status:\s*1/);

  const regionColumn = section(
    tableSource,
    'dataIndex: "distributionRegionText"',
    "render: (value?: string)",
  );
  assert.match(regionColumn, /width:\s*"\d+%"/);
  assert.match(tableSource, /tableLayout="fixed"/);
  assert.match(tableSource, /scroll=\{\{ x:\s*1120 \}\}/);
  assert.ok((tableSource.match(/width:\s*"\d+%"/g) ?? []).length >= 8);
  assert.match(categoryTable, /tableLayout="fixed"/);
  assert.match(categoryTable, /scroll=\{\{ x:\s*900 \}\}/);
  assert.match(baseTable, /tableLayout="fixed"/);
  assert.match(baseTable, /scroll=\{\{ x:\s*1200 \}\}/);
  assert.ok((baseColumns.match(/width:\s*"\d+%"/g) ?? []).length >= 9);
  assert.doesNotMatch(hero, /actions=/);
  assert.match(baseColumns, /dataIndex:\s*"address"[\s\S]*?ellipsis:\s*true/);
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

test("药材资源导出防止公式注入且编辑时不伪修改药材编号", async () => {
  const clientSource = await readSource("components/herbs/HerbResourceClient.tsx");
  const apiSource = await readSource("lib/herbs.ts");
  const downloadSource = section(clientSource, "function downloadCsv", "export function HerbResourceClient");
  const updatePayload = section(
    apiSource,
    "export type HerbSpeciesUpdatePayload",
    "export type DictItemPayload",
  );

  assert.match(clientSource, /import \{ escapeCsvCell \} from "@\/lib\/csv"/);
  assert.match(downloadSource, /escapeCsvCell\(row\[header\]\)/);
  assert.doesNotMatch(downloadSource, /replaceAll\('\"'/);
  assert.doesNotMatch(updatePayload, /herbCode\s*:/);
  assert.match(apiSource, /updateHerbSpecies\(id: number, payload: HerbSpeciesUpdatePayload\)/);
  assert.match(clientSource, /name="herbCode"[\s\S]*?<Input disabled=\{modalMode === "edit"\}/);
});

test("药材资源页接收首页链接携带的 keyword 查询参数", async () => {
  const pageSource = await readSource("app/herbs/page.tsx");
  const clientSource = await readSource("components/herbs/HerbResourceClient.tsx");
  const filterSource = await readSource("components/herbs/HerbFilterBar.tsx");

  assert.match(pageSource, /searchParams:\s*Promise<HerbsSearchParams>/);
  assert.match(pageSource, /const params = await searchParams/);
  assert.match(pageSource, /<HerbResourceClient initialKeyword=\{keyword\}/);
  assert.match(clientSource, /initialKeyword\?: string/);
  assert.match(clientSource, /useState<HerbFilterValues>\(\(\) =>\s*initialKeyword/);
  assert.match(clientSource, /<HerbFilterBar[\s\S]*?initialKeyword=\{initialKeyword\}/);
  assert.match(filterSource, /form\.setFieldValue\("keyword", initialKeyword/);
});

test("药材新增编辑支持封面图片并在资源表格显示缩略图", async () => {
  const clientSource = await readSource("components/herbs/HerbResourceClient.tsx");
  const tableSource = await readSource("components/herbs/HerbTable.tsx");
  const typeSource = await readSource("components/herbs/types.ts");
  const apiSource = await readSource("lib/herbs.ts");

  assert.match(apiSource, /coverImageUrl\?: string/);
  assert.match(apiSource, /toBrowserFileUrl/);
  assert.match(typeSource, /coverImageUrl\?: string/);
  assert.match(clientSource, /name="coverImageUrl"/);
  assert.match(clientSource, /<FileUploadField/);
  assert.match(clientSource, /accessLevel="private"/);
  assert.doesNotMatch(clientSource, /accessLevel="public"[\s\S]*?fileUsage="cover"/);
  assert.doesNotMatch(clientSource, /cleanupUnboundOnUnmount=\{false\}/);
  assert.match(tableSource, /dataIndex: "coverImageUrl"/);
  assert.match(tableSource, /className=\{styles\.tableThumb\}/);
});

test("药材详情只保留基本档案和图片图鉴", async () => {
  const detailSource = await readSource("components/herbs/HerbDetailPanel.tsx");
  const cssSource = await readSource("components/herbs/herbs.module.css");

  assert.doesNotMatch(detailSource, /Tabs|Descriptions/);
  assert.doesNotMatch(detailSource, /关联数据|附件资料|relations|attachments/);
  assert.match(detailSource, /styles\.detailHero/);
  assert.match(detailSource, /基本档案/);
  assert.match(detailSource, /图片图鉴/);
  assert.match(detailSource, /herb\.coverImageUrl/);
  assert.match(detailSource, /preview=\{\{ mask: "预览图片" \}\}/);
  assert.match(
    cssSource,
    /\.archiveGrid\s*\{[^}]*grid-template-columns:\s*repeat\(2, minmax\(0, 1fr\)\);/s,
  );
  assert.match(
    cssSource,
    /\.galleryGrid\s*\{[^}]*grid-template-columns:\s*repeat\(auto-fit, minmax\(180px, 1fr\)\);/s,
  );
});
