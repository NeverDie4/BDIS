import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import test from "node:test";

const pageSource = readFileSync(new URL("../src/app/growth/page.tsx", import.meta.url), "utf8");
const dataSource = readFileSync(new URL("../src/lib/growth-records.ts", import.meta.url), "utf8");
const cssSource = readFileSync(
  new URL("../src/app/growth/page.module.css", import.meta.url),
  "utf8",
);
const publicTracePageSource = readFileSync(
  new URL("../src/app/trace/growth/[traceCode]/page.tsx", import.meta.url),
  "utf8",
);
const publicTraceCssSource = readFileSync(
  new URL("../src/app/trace/growth/[traceCode]/page.module.css", import.meta.url),
  "utf8",
);
const publicRoutesSource = readFileSync(
  new URL("../src/config/routes/public.ts", import.meta.url),
  "utf8",
);
const reviewerScopeMigrationUrl = new URL(
  "../../backend/src/main/resources/db/migration/V20260714_006__grant_growth_reviewer_data_scope.sql",
  import.meta.url,
);

test("生长趋势使用 ECharts Canvas 而不是 React SVG", () => {
  assert.match(pageSource, /CanvasRenderer/);
  assert.match(pageSource, /LineChart/);
  assert.match(pageSource, /init\(containerRef\.current\)/);
  assert.doesNotMatch(pageSource, /<svg\b/i);
});

test("没有有效点位时仍渲染 ECharts 坐标轴和空状态", () => {
  assert.match(pageSource, /emptyDescription/);
  assert.match(pageSource, /graphic:/);
  assert.match(pageSource, /data\.length\s*\?\s*420\s*:\s*320/);
  assert.match(pageSource, /name:\s*"采集时间"/);
  assert.match(pageSource, /nameLocation:\s*"middle"/);
  assert.doesNotMatch(pageSource, /\blegend:\s*\{/);
  assert.doesNotMatch(pageSource, /LegendComponent/);
});

test("默认指标按后端支持顺序选择首个有效指标", () => {
  assert.match(
    pageSource,
    /DEFAULT_METRIC_ORDER\s*=\s*\[\s*"plantHeight",\s*"temperature",\s*"humidity",\s*"soilPh",\s*"soilMoisture",\s*"light",\s*"sampleWeight",?\s*\]/,
  );
  assert.match(pageSource, /findFirstValidMetric/);
  assert.match(pageSource, /autoMetricNotice/);
});

test("页面包含任务概览与当前指标有效观测点统计", () => {
  assert.match(pageSource, /taskOverview/);
  assert.match(pageSource, /当前指标有效观测点/);
});

test("图表数据构建区分四种空状态且只接受真实数值", () => {
  assert.match(dataSource, /GrowthChartEmptyReason/);
  assert.match(dataSource, /"no-records"/);
  assert.match(dataSource, /"no-status-records"/);
  assert.match(dataSource, /"no-metric-values"/);
  assert.match(dataSource, /"metric-missing"/);
  assert.match(dataSource, /Number\.isFinite/);
  assert.match(dataSource, /hasOwnProperty/);
  assert.doesNotMatch(dataSource, /record\[metricKey\]\s*\?\?\s*record\.value/);
});

test("空点位摘要展示筛选诊断和建议操作", () => {
  assert.match(pageSource, /匹配记录数/);
  assert.match(pageSource, /空状态原因/);
  assert.match(pageSource, /查看全部状态/);
  assert.match(pageSource, /切换指标/);
  assert.match(pageSource, /重置筛选/);
});

test("指标诊断使用下拉框而不是固定轮换按钮", () => {
  assert.match(pageSource, /className=\{styles\.metricDiagnosticSelect\}/);
  assert.match(pageSource, /aria-label="切换指标"/);
  assert.doesNotMatch(pageSource, /onClick=\{switchMetric\}>切换指标/);
});

test("采集员使用个人任务接口并隐藏审核工作区", () => {
  assert.match(dataSource, /fetchMyGrowthTasks/);
  assert.match(dataSource, /\/herb\/collection-task\/my/);
  assert.match(pageSource, /isCollectorOnly/);
  assert.match(pageSource, /showReviewWorkspace/);
  assert.match(pageSource, /!showReviewWorkspace \? \(/);
  assert.match(pageSource, /showReviewWorkspace\s*\?\s*\(/);
});

test("页面 Hero 使用紧凑的生长数据管理标题和可替换插画占位", () => {
  assert.match(pageSource, />生长数据</);
  assert.match(pageSource, /管理移动端采集的中药材生长记录，汇聚现场图片、审核状态与溯源轨迹/);
  assert.match(pageSource, /const heroIllustrationUrl = ""/);
  assert.doesNotMatch(pageSource, /我的生长观测|GROWTH OBSERVATION|MY GROWTH/);
  assert.match(cssSource, /\.growthHero\s*\{[^}]*min-height:\s*148px;/s);
  assert.match(cssSource, /\.growthHero::after/);
});

test("任务概览去除重复药材基地并使用紧凑统计网格", () => {
  assert.doesNotMatch(pageSource, /styles\.overviewContext/);
  assert.doesNotMatch(pageSource, /styles\.taskTags/);
  assert.match(pageSource, /styles\.taskMetaLine/);
  assert.match(pageSource, /styles\.currentMetricTag/);
  assert.match(
    cssSource,
    /\.overviewGrid\s*\{[^}]*grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\);/s,
  );
  assert.match(cssSource, /\.overviewGrid\s*>\s*div\s*\{[^}]*height:\s*116px;/s);
});

test("观测点摘要区区分单点、多点和无数据布局", () => {
  assert.match(pageSource, /POINT SUMMARY/);
  assert.match(pageSource, />观测点摘要</);
  assert.match(pageSource, /styles\.singlePointLayout/);
  assert.match(pageSource, /当前仅有 1 个观测点/);
  assert.match(pageSource, /继续创建新的采集批次并填写生长记录后/);
  assert.match(pageSource, /styles\.pointEmptyState/);
  assert.match(pageSource, />暂无观测点</);
  assert.match(pageSource, /void showAllStatuses\(\)/);
});

test("查看全部状态通过独立处理函数提供反馈", () => {
  assert.match(pageSource, /function showAllStatuses/);
  assert.match(pageSource, /setStatus\("all"\)/);
  assert.match(pageSource, /已切换为全部状态/);
  assert.match(pageSource, /当前已是全部状态/);
});

test("详情现场图片按生长记录 batchId 查询批次图片", () => {
  assert.match(dataSource, /fetchGrowthBatchImages/);
  assert.match(dataSource, /\/herb\/batch\/\$\{batchId\}\/images/);
  assert.match(pageSource, /fetchGrowthBatchImages\(detail\.batchId\)/);
  assert.match(pageSource, /batchImages\.map/);
  assert.doesNotMatch(pageSource, /selectedRecord\.images\?\.length/);
});

test("详情字段使用卡片布局且审核历史与溯源时间线保持独立", () => {
  assert.match(pageSource, /styles\.detailCardGrid/);
  assert.match(pageSource, />审核历史</);
  assert.match(pageSource, />溯源时间线</);
  assert.doesNotMatch(pageSource, /styles\.detailGrid/);
  assert.doesNotMatch(pageSource, /styles\.indicatorGrid/);
});

test("详情抽屉使用本草摘要、指标看板、图片证据和中文状态链路", () => {
  assert.match(pageSource, /styles\.recordSummary/);
  assert.match(pageSource, /styles\.metricNumber/);
  assert.match(pageSource, /styles\.metricUnit/);
  assert.match(pageSource, />现场图片证据</);
  assert.match(pageSource, /以下图片来自当前采集批次，用于佐证本次生长记录/);
  assert.match(pageSource, /styles\.imageEvidenceGrid/);
  assert.match(pageSource, /styles\.auditTimeline/);
  assert.match(pageSource, /styles\.traceTimeline/);
  assert.match(pageSource, /function formatStatusTransition/);
  assert.match(pageSource, /初始状态：/);
  assert.doesNotMatch(pageSource, /\{event\.beforeStatus \|\| "-"\}\s*→/);
});

test("详情信息框文字放大且数值与单位字号统一", () => {
  assert.match(cssSource, /\.metricTile\s*\{[^}]*min-height:\s*78px;/s);
  assert.match(
    cssSource,
    /\.growthMetricGrid\s*>\s*div\s*\{[^}]*min-height:\s*78px;[^}]*background:\s*#fffaf2\s*!important;/s,
  );
  assert.match(cssSource, /\.detailCardGrid dt\s*\{[^}]*font-size:\s*14px;/s);
  assert.match(cssSource, /\.detailCardGrid dd\s*\{[^}]*font-size:\s*17px;/s);
  assert.match(cssSource, /\.metricNumber\s*\{[^}]*font-size:\s*18px;[^}]*font-weight:\s*600;/s);
  assert.match(cssSource, /\.metricUnit\s*\{[^}]*font-size:\s*18px;[^}]*font-weight:\s*600;/s);
  assert.match(cssSource, /\.metricNumber\s*\{[^}]*color:\s*#0f5132;/s);
  assert.match(cssSource, /\.metricUnit\s*\{[^}]*color:\s*#0f5132;/s);
  assert.match(
    cssSource,
    /\.textMetricTile dd\s*\{[^}]*font-size:\s*18px;[^}]*font-weight:\s*600;/s,
  );
  assert.doesNotMatch(cssSource, /\.primaryMetricTile\s*\{[^}]*background:\s*#eef7ef/s);
});

test("记录筛选使用工作台工具条和均衡网格", () => {
  assert.match(pageSource, />记录筛选</);
  assert.match(pageSource, /全部采集人/);
  assert.match(pageSource, /时间范围/);
  assert.match(pageSource, /搜索批次、采集人、阶段或备注/);
  assert.match(pageSource, /className=\{styles\.filterActions\}/);
  assert.match(pageSource, /title=\{selectedTask\?\.taskName\}/);
  assert.match(
    cssSource,
    /\.workspaceMain \.filterGrid,[\s\S]*grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/s,
  );
});

test("growth 页面用户可见文案统一使用观测点", () => {
  assert.doesNotMatch(pageSource, /点位/);
  assert.match(pageSource, /当前筛选条件下暂无趋势观测点/);
  assert.match(pageSource, /橙色观测点表示待审核记录/);
  assert.match(pageSource, /有效观测点数/);
});

test("growth 页面识别后端归档状态且不从趋势数据中过滤归档记录", () => {
  assert.match(pageSource, /archived:\s*\{\s*label:\s*"已归档"/);
  assert.match(pageSource, /const visiblePoints = useMemo\(\(\) => points, \[points\]\)/);
});

test("顶部三区使用统一间距、轻量指标标签和同款 KPI 卡", () => {
  assert.match(cssSource, /\.heroBanner\s*\{[^}]*height:\s*228px;/s);
  assert.match(cssSource, /\.heroCopy h1\s*\{[^}]*font-size:\s*44px;/s);
  assert.match(
    cssSource,
    /\.heroMetric\s*\{[^}]*height:\s*116px;[^}]*border:\s*1px solid #e5dccd;[^}]*background:\s*#fffaf2;/s,
  );
  assert.match(cssSource, /\.filters\s*\{[^}]*padding:\s*28px 30px;/s);
  assert.match(cssSource, /\.filterGrid\s*\{[^}]*margin-top:\s*24px;/s);
  assert.match(
    cssSource,
    /\.filterGrid :global\(\.ant-select-selector\)\s*\{[^}]*min-height:\s*48px/s,
  );
  assert.match(
    cssSource,
    /\.filterGrid :global\(\.ant-select-selection-item\)[\s\S]*font-size:\s*16px;/s,
  );
  assert.match(
    cssSource,
    /\.filterSummary\s*\{[^}]*margin-top:\s*20px;[^}]*padding:\s*14px 16px;[^}]*border:\s*1px solid #cddfce;[^}]*background:\s*#eef7ef;/s,
  );
  assert.match(pageSource, /styles\.currentMetricTag/);
  assert.doesNotMatch(pageSource, /styles\.currentMetricBadge/);
  assert.match(
    cssSource,
    /\.overviewGrid\s*>\s*div\s*\{[^}]*height:\s*116px;[^}]*border:\s*1px solid #e5dccd;[^}]*background:\s*#fffaf2;/s,
  );
  assert.doesNotMatch(cssSource, /\.overviewGrid \.activeMetricCount\s*\{[^}]*background:/s);
});

test("growth 页面重构为左侧记录工作台和右侧固定详情面板", () => {
  assert.match(pageSource, /const heroIllustrationUrl = ""/);
  assert.match(pageSource, /className=\{styles\.workspaceLayout\}/);
  assert.match(pageSource, /className=\{styles\.recordTable\}/);
  assert.match(pageSource, /className=\{styles\.detailPanel\}/);
  assert.match(pageSource, /className=\{styles\.detailTabs\}/);
  assert.match(pageSource, /展开趋势图/);
  assert.match(pageSource, /生长数据/);
  assert.match(pageSource, /管理移动端采集的中药材生长记录，汇聚现场图片、审核状态与溯源轨迹/);
  assert.doesNotMatch(pageSource, /<DetailDrawer/);
  assert.match(
    cssSource,
    /\.workspaceLayout\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+460px/s,
  );
  assert.match(cssSource, /\.detailPanel\s*\{[^}]*position:\s*sticky/s);
  assert.match(cssSource, /\.growthHero::after/);
});

test("工作台不伪造数据来源且采集员可按真实权限提交审核", () => {
  assert.doesNotMatch(pageSource, /<td>移动端<\/td>/);
  assert.match(pageSource, /const canSubmitSelected\s*=\s*hasPermission\("growth:record:submit"\)/);
});

test("growth 工作台使用宽版双栏、折叠筛选和档案式详情", () => {
  assert.match(pageSource, /const \[advancedFiltersOpen, setAdvancedFiltersOpen\]/);
  assert.match(pageSource, /advancedFiltersOpen \? \(/);
  assert.match(pageSource, /advancedFiltersOpen \? "收起筛选" : "展开筛选"/);
  assert.match(pageSource, /styles\.archiveSummary/);
  assert.match(pageSource, /styles\.archiveMeta/);
  assert.match(pageSource, /className=\{styles\.basicInfoList\}/);
  assert.match(pageSource, /生长趋势分析/);
  assert.match(pageSource, /暂无精确经纬度，已展示采集地点文本信息/);
  assert.match(cssSource, /\.page\s*\{[^}]*width:\s*min\(1560px,\s*calc\(100vw - 64px\)\)/s);
  assert.match(
    cssSource,
    /\.workspaceLayout\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+460px/s,
  );
  assert.match(cssSource, /\.detailPanel\s*\{[^}]*border-radius:\s*20px/s);
  assert.match(cssSource, /\.detailTabs button\s*\{[^}]*min-width:\s*76px;[^}]*font-size:\s*14px/s);
  assert.match(
    cssSource,
    /\.basicInfoList\s*\{[^}]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/s,
  );
});

test("growth 详情使用四个信息域并中文化图片类型", () => {
  assert.match(pageSource, /type DetailTab = "base" \| "metrics" \| "images" \| "trace"/);
  assert.match(pageSource, /\["base", "基础信息"\]/);
  assert.match(pageSource, /\["metrics", "指标数据"\]/);
  assert.match(pageSource, /\["images", "现场图片"\]/);
  assert.match(pageSource, /\["trace", "地图与溯源"\]/);
  assert.doesNotMatch(
    pageSource,
    /\["environment", "环境指标"\]|\["morphology", "形态指标"\]|\["map", "地图定位"\]/,
  );
  assert.match(pageSource, /const IMAGE_TYPE_LABELS/);
  assert.match(pageSource, /whole_plant:\s*"整株"/);
  assert.match(pageSource, /medicinal_part:\s*"药用部位"/);
  assert.match(pageSource, /IMAGE_TYPE_LABELS\[image\.imageType\]/);
  assert.match(pageSource, /className=\{styles\.advancedFilterGrid\}/);
  assert.match(pageSource, /className=\{styles\.trendFacts\}/);
  assert.match(
    cssSource,
    /\.workspaceLayout\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+460px;/s,
  );
});

test("growth 溯源 API 与右侧二维码卡使用后端真实契约", () => {
  assert.match(dataSource, /generateGrowthTraceCode/);
  assert.match(dataSource, /\/trace-code\/generate/);
  assert.match(dataSource, /generateGrowthTraceQrCode/);
  assert.match(dataSource, /\/trace-qrcode\/generate/);
  assert.match(dataSource, /getGrowthTraceQrCode/);
  assert.match(dataSource, /\/trace-qrcode`/);
  assert.match(dataSource, /enableGrowthPublicTrace/);
  assert.match(dataSource, /\/trace\/public-enable/);
  assert.match(dataSource, /disableGrowthPublicTrace/);
  assert.match(dataSource, /\/trace\/public-disable/);
  assert.match(dataSource, /getPublicGrowthTrace/);
  assert.match(dataSource, /downloadGrowthTraceQrCode/);
  assert.match(dataSource, /responseType:\s*"blob"/);
  assert.match(dataSource, /axios\.get<ApiResult<GrowthPublicTraceArchiveApi>>/);
  assert.doesNotMatch(dataSource, /getPublicGrowthTrace[\s\S]{0,180}apiGet/);
  assert.match(pageSource, />溯源二维码</);
  assert.match(pageSource, /performTraceAction/);
  assert.match(pageSource, /await refreshTracePanel/);
  assert.match(pageSource, /公开溯源已开启/);
  assert.match(pageSource, /公开溯源已关闭/);
  assert.match(pageSource, /TRACE_EVENT_LABELS/);
  assert.match(cssSource, /\.traceQrCard/);
  assert.doesNotMatch(pageSource, /fetch\(resolveGrowthResourceUrl\(traceQrCode\.qrCodeUrl\)\)/);
});

test("管理员打开未公开溯源前必须先开启公开查询", () => {
  assert.match(pageSource, /async function openPublicTracePage\(\)/);
  assert.match(pageSource, /okText:\s*"开启并打开"/);
  assert.match(pageSource, /await enableGrowthPublicTrace\(selectedRecord\.id\)/);
  assert.match(pageSource, /await refreshTracePanel\(selectedRecord\.id, qrCode\)/);
  assert.doesNotMatch(pageSource, /okText:\s*"继续打开"/);
});

test("公开生长溯源页提供档案、错误状态、盖章和打印能力", () => {
  assert.match(publicTracePageSource, /getPublicGrowthTrace\(traceCode\)/);
  assert.match(publicTracePageSource, /本草研究院标本馆/);
  assert.match(publicTracePageSource, /中药材数字溯源档案/);
  assert.match(publicTracePageSource, /该溯源档案暂未公开/);
  assert.match(publicTracePageSource, /未找到对应溯源档案/);
  assert.match(publicTracePageSource, /现场图片证据/);
  assert.match(publicTracePageSource, /审核通过/);
  assert.match(publicTracePageSource, /数据可信/);
  assert.match(publicTracePageSource, /window\.print\(\)/);
  assert.doesNotMatch(publicTracePageSource, /SecureImageThumb/);
  assert.match(publicTracePageSource, /resolveGrowthResourceUrl\(archive\.qrCodeUrl\)/);
  assert.match(publicTracePageSource, /resolveGrowthResourceUrl\(image\.imageUrl\)/);
  assert.doesNotMatch(publicTracePageSource, /<SiteLayout/);
  assert.match(publicTraceCssSource, /@media print/);
  assert.match(publicTraceCssSource, /@page\s*\{\s*size:\s*A4/);
  assert.match(publicTraceCssSource, /\.noPrint\s*\{\s*display:\s*none\s*!important/);
});

test("公开生长溯源页面必须注册为免登录路由", () => {
  assert.match(publicRoutesSource, /path:\s*"\/trace\/growth\/\[traceCode\]"[\s\S]*public:\s*true/);
});

test("growth 筛选任务框不越界且审核操作栏位于详情末尾", () => {
  assert.match(
    cssSource,
    /\.workspaceMain \.filterGrid > \.taskFilter\s*\{[^}]*overflow:\s*hidden;[^}]*min-width:\s*0\s*!important;/s,
  );
  assert.match(
    cssSource,
    /\.workspaceMain \.taskFilter :global\(\.ant-select\)\s*\{[^}]*width:\s*100%;[^}]*max-width:\s*100%;/s,
  );
  assert.match(
    cssSource,
    /\.detailPanel \.drawerActions\s*\{[^}]*position:\s*static;[^}]*margin:\s*0;/s,
  );
  assert.doesNotMatch(
    cssSource,
    /\.detailPanel \.drawerActions\s*\{[^}]*margin:\s*0\s+-24px\s+-96px;/s,
  );
});

test("审核员拥有生长记录审核所需的显式数据范围", () => {
  assert.equal(existsSync(reviewerScopeMigrationUrl), true);
  const migrationSource = readFileSync(reviewerScopeMigrationUrl, "utf8");
  assert.match(migrationSource, /role_code\s*=\s*'REVIEWER'/i);
  assert.match(migrationSource, /'herb_growth_record'/i);
  assert.doesNotMatch(migrationSource, /'all'/i);
  assert.doesNotMatch(migrationSource, /ON DUPLICATE KEY UPDATE/i);
  assert.match(migrationSource, /NOT EXISTS/i);
});

test("growth 固定详情使用剩余高度滚动且末端内容不被裁切", () => {
  assert.match(
    cssSource,
    /\.workspaceLayout \.detailPanel\s*\{[^}]*display:\s*flex;[^}]*height:\s*calc\(100vh\s*-\s*92px\);[^}]*flex-direction:\s*column;/s,
  );
  assert.match(
    cssSource,
    /\.detailPanel\s*>\s*:global\(\.ant-spin-nested-loading\)\s*\{[^}]*flex:\s*1;[^}]*min-height:\s*0;[^}]*overflow:\s*hidden;/s,
  );
  assert.match(
    cssSource,
    /\.detailPanel \.drawerContent\s*\{[^}]*height:\s*100%;[^}]*max-height:\s*none;[^}]*overflow-y:\s*auto;[^}]*padding-bottom:\s*32px;/s,
  );
});

test("管理员和教师可创建必须指定采集员的采集任务", () => {
  assert.match(dataSource, /export function createGrowthTask/);
  assert.match(dataSource, /apiPost<GrowthTaskApi>\("\/herb\/collection-task", data\)/);
  assert.match(pageSource, /const canCreateTask = roleCodes\.some\(\(role\) => \["ADMIN", "TEACHER"\]\.includes\(role\)\)/);
  assert.match(pageSource, />\s*创建采集任务\s*</);
  assert.match(pageSource, /name="collectorId"/);
  assert.match(pageSource, /rules=\{\[\{ required: true, message: "请选择采集员" \}\]\}/);
  assert.match(pageSource, /collectorName:/);
  assert.match(pageSource, /await createGrowthTask/);
});
test("草稿采集任务必须通过真实发布接口后才对手机端可见", () => {
  assert.match(dataSource, /export function publishGrowthTask\(taskId: number\)/);
  assert.match(dataSource, /apiPut<GrowthTaskApi>\(`\/herb\/collection-task\/\$\{taskId\}\/publish`\)/);
  assert.match(pageSource, /selectedTask\?\.taskStatus !== "draft"/);
  assert.match(pageSource, />\s*发布任务\s*</);
  assert.match(pageSource, /await publishGrowthTask\(selectedTask\.id\)/);
  assert.match(pageSource, /指定采集员现在可在手机端查看/);
});