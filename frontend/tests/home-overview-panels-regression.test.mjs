import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const root = new URL("../src/", import.meta.url);

async function readSource(relativePath) {
  return readFile(new URL(relativePath, root), "utf8");
}

test("道地药材精选使用三列紧凑卡片并处理异常占位", async () => {
  const panel = await readSource("components/home/FeaturedHerbsPanel.tsx");
  const styles = await readSource("components/home/FeaturedHerbsPanel.module.css");
  const grid = await readSource("components/home/HomeOverviewGrid.tsx");

  assert.match(grid, /species\.slice\(0,\s*3\)/);
  assert.match(panel, /function getPlaceholderCharacter/);
  assert.doesNotMatch(panel, /herbName\.slice\(0,\s*1\)/);
  assert.match(panel, /categoryName\s*\|\|\s*herb\.category\s*\|\|\s*"暂未分类"/);
  assert.match(panel, /药用部位待完善/);
  assert.match(panel, /药材档案信息待完善/);
  assert.match(panel, /查看档案/);
  assert.match(panel, /暂无推荐药材/);
  assert.match(panel, /药材档案完善后将在此展示/);

  assert.match(styles, /grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/);
  assert.match(styles, /gap:\s*16px/);
  assert.match(styles, /height:\s*320px/);
  assert.match(styles, /padding:\s*(18px|20px)/);
  assert.match(styles, /-webkit-line-clamp:\s*2/);
  assert.match(styles, /grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/);
  assert.match(styles, /grid-template-columns:\s*1fr/);
});

test("生长观测动态使用紧凑摘要、真实字段和中文状态", async () => {
  const panel = await readSource("components/home/GrowthObservationPanel.tsx");
  const styles = await readSource("components/home/HomeOverviewGrid.module.css");

  for (const [raw, label] of [
    ["draft", "草稿"],
    ["submitted", "待审核"],
    ["approved", "已通过"],
    ["rejected", "已驳回"],
  ]) {
    assert.match(panel, new RegExp(`${raw}:[\\s\\S]*?${label}`));
  }

  assert.match(panel, /观测记录/);
  assert.doesNotMatch(panel, /采集总数/);
  assert.doesNotMatch(panel, /<Tag>\{record\.reviewStatus/);
  assert.match(panel, /基地未关联/);
  assert.match(panel, /采集员未知/);
  assert.match(panel, /阶段未填写/);
  assert.match(panel, /collectedAt/);
  assert.match(panel, /\.sort\(/);
  assert.match(panel, /slice\(0,\s*[3-5]\)/);
  assert.match(panel, /href="\/growth"/);
  assert.match(panel, /暂无生长观测记录/);
  assert.match(panel, /移动端提交的生长数据将在此同步展示/);

  assert.match(styles, /grid-template-columns:\s*44px\s+minmax\(0,\s*1fr\)\s+auto/);
  assert.match(styles, /min-height:\s*(82px|84px|88px|90px|92px|96px)/);
  assert.match(styles, /\.growthStatusApproved[\s\S]*?background:/);
  assert.match(styles, /\.growthStatusSubmitted[\s\S]*?background:/);
  assert.match(styles, /\.growthStatusRejected[\s\S]*?background:/);
});
