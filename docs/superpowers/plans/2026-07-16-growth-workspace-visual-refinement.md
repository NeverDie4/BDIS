# 生长数据工作台视觉优化实施计划

> **执行要求：** 必须使用 `executing-plans` 技能逐项实施；每个步骤使用复选框跟踪。

**目标：** 在保留 `/growth` 全部业务逻辑和现有详情标签交互的前提下，统一标题栏并将工作区调整为紧凑、扁平的本草纸张档案风格。

**实现方式：** 页面结构只替换自绘标题区，工作台、表格、趋势图和详情内容结构不重写。视觉调整集中在现有页面样式文件末端，以高明确度选择器覆盖历史重复样式，并用静态回归测试锁定共享标题栏、双栏比例、扁平详情和响应式边界。

**技术栈：** Next.js 15、React、TypeScript、CSS Modules、Ant Design、Node.js 内置测试框架。

---

## 文件结构

- 修改 `frontend/src/app/growth/page.tsx`：复用共享标题栏，保留所有业务组件和状态逻辑。
- 修改 `frontend/src/app/growth/page.module.css`：调整页面、筛选区、记录区、详情区及响应式样式。
- 新建 `frontend/tests/growth-workspace-visual-regression.test.mjs`：锁定本次视觉改造边界。

### 任务一：建立视觉结构回归测试

**文件：**

- 新建：`frontend/tests/growth-workspace-visual-regression.test.mjs`

- [ ] **步骤 1：写入失败测试**

```js
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
  assert.match(css, /grid-template-columns:\s*minmax\(0,\s*2\.55fr\)\s+minmax\(340px,\s*1fr\)/);
  assert.match(css, /\.workspaceLayout \.detailPanel\s*\{[^}]*width:\s*auto;[^}]*border-radius:\s*2px;[^}]*box-shadow:\s*none;/s);
  assert.match(css, /\.detailPanel \.archiveSummary\s*\{[^}]*border-radius:\s*0;[^}]*background:\s*transparent;/s);
  assert.doesNotMatch(css, /\.detailPanel \.archiveSummary\s*\{[^}]*linear-gradient/s);
});

test("生长数据保留现有详情标签交互", async () => {
  const source = await readSource("app/growth/page.tsx");
  assert.match(source, /className=\{styles\.detailTabs\}/);
  assert.match(source, /detailTab === value/);
  assert.match(source, /setDetailTab\(value\)/);
});

test("生长数据在内容不足前切换单栏", async () => {
  const css = await readSource("app/growth/page.module.css");
  assert.match(css, /@media \(max-width:\s*1260px\)[\s\S]*?\.workspaceLayout\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)/);
  assert.match(css, /@media \(max-width:\s*720px\)[\s\S]*?\.workspaceMain \.filterGrid/);
});
```

- [ ] **步骤 2：运行测试并确认失败**

运行：`node --test frontend/tests/growth-workspace-visual-regression.test.mjs`

预期：共享标题栏、双栏比例或响应式断点断言失败。

### 任务二：替换共享标题栏

**文件：**

- 修改：`frontend/src/app/growth/page.tsx:11-56`
- 修改：`frontend/src/app/growth/page.tsx:1040-1056`
- 测试：`frontend/tests/growth-workspace-visual-regression.test.mjs`

- [ ] **步骤 1：引入共享组件并删除旧占位常量**

```tsx
import { ModuleHeroBanner } from "@/components/layout/ModuleHeroBanner";
```

删除：

```tsx
const heroIllustrationUrl = "";
```

- [ ] **步骤 2：替换自绘标题区**

```tsx
<ModuleHeroBanner
  variant="compact"
  eyebrow="本草生长档案"
  title="生长数据"
  description="管理移动端采集的中药材生长记录，汇聚现场图片、审核状态与溯源轨迹。"
/>
```

- [ ] **步骤 3：运行共享标题栏测试**

运行：`node --test frontend/tests/growth-workspace-visual-regression.test.mjs`

预期：标题栏测试通过，样式相关测试仍失败。

### 任务三：调整工作区比例和扁平纸张样式

**文件：**

- 修改：`frontend/src/app/growth/page.module.css:3037` 之后追加视觉覆盖层
- 测试：`frontend/tests/growth-workspace-visual-regression.test.mjs`

- [ ] **步骤 1：追加页面和主工作区样式**

```css
/* Growth workspace visual refinement */
.page {
  width: min(1780px, calc(100vw - 24px));
  gap: 12px;
}

.workspaceLayout {
  grid-template-columns: minmax(0, 2.55fr) minmax(340px, 1fr);
  gap: 10px;
}

.workspaceMain {
  gap: 10px;
}

.workspaceMain .filters,
.recordWorkspace,
.trendDisclosure {
  border: 1px solid #ddd1bd;
  border-radius: 2px;
  background: rgba(255, 253, 247, 0.96);
  box-shadow: none;
}
```

- [ ] **步骤 2：追加筛选和详情扁平化样式**

```css
.workspaceMain .filters {
  padding: 14px 16px;
}

.workspaceMain .filterGrid,
.workspaceMain .collectorFilterGrid {
  gap: 10px 12px;
  margin-top: 10px;
}

.filterActions {
  margin-top: 10px;
  padding-top: 10px;
}

.workspaceLayout .detailPanel {
  width: auto;
  min-width: 0;
  border: 1px solid #d8cbb6;
  border-radius: 2px;
  background: #fffdf7;
  box-shadow: none;
}

.detailPanel .archiveSummary {
  padding: 4px 0 16px;
  border-bottom: 1px solid #e3d7c4;
  border-radius: 0;
  background: transparent;
}

.detailPanel .detailCardGrid > div,
.detailPanel .growthMetricGrid > div,
.detailPanel .metricCardGrid > div {
  border-radius: 2px;
  background: transparent !important;
  box-shadow: none;
}

.traceQrCard,
.traceQrImage,
.traceQrMeta > div {
  border-radius: 2px;
  background: #fffdf7;
  box-shadow: none;
}
```

- [ ] **步骤 3：追加响应式边界**

```css
@media (max-width: 1260px) {
  .workspaceLayout {
    grid-template-columns: minmax(0, 1fr);
  }

  .workspaceLayout .detailPanel {
    position: relative;
    top: auto;
    width: 100%;
    height: auto;
    max-height: none;
  }
}

@media (max-width: 720px) {
  .page {
    width: min(100%, calc(100vw - 16px));
  }

  .workspaceMain .filterGrid,
  .workspaceMain .collectorFilterGrid,
  .advancedFilterGrid {
    grid-template-columns: minmax(0, 1fr);
  }

  .workspaceMain .taskFilter,
  .workspaceMain .dateFilter,
  .workspaceMain .keywordFilter {
    grid-column: auto;
  }
}
```

- [ ] **步骤 4：运行视觉结构测试**

运行：`node --test frontend/tests/growth-workspace-visual-regression.test.mjs`

预期：4 项测试全部通过。

### 任务四：完整验证和提交

**文件：**

- 验证：`frontend/src/app/growth/page.tsx`
- 验证：`frontend/src/app/growth/page.module.css`
- 验证：`frontend/tests/growth-workspace-visual-regression.test.mjs`

- [ ] **步骤 1：运行全部静态回归测试**

运行：`node --test 'frontend/tests/*.test.mjs'`

预期：全部通过，无失败和跳过。

- [ ] **步骤 2：运行前端单元测试、代码检查和构建**

运行：`npm.cmd test --prefix frontend`

运行：`npm.cmd run lint --prefix frontend`

运行：`npm.cmd run build --prefix frontend`

预期：测试和构建成功；代码检查无新增错误。

- [ ] **步骤 3：浏览器检查**

检查 `/growth` 在 1920、1180 和 390 像素宽度下：无横向溢出；标题背景正确；桌面端为左右双栏；中小屏为单栏；详情标签切换、关闭和重新选择记录正常；右侧详情可独立滚动至末端操作区。

- [ ] **步骤 4：核对差异并提交**

运行：`git diff --check`

运行：`git diff --name-only HEAD`

预期：页面代码、页面样式和视觉回归测试之外没有业务文件改动。

提交命令：

```bash
git add frontend/src/app/growth/page.tsx frontend/src/app/growth/page.module.css frontend/tests/growth-workspace-visual-regression.test.mjs
git commit -m "feat: 优化生长数据工作台视觉"
```

