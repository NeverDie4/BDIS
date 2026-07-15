import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import test from "node:test";

const apiUrl = new URL("../src/lib/digital-life.ts", import.meta.url);
const pageUrl = new URL("../src/app/trace/digital-life/[traceCode]/page.tsx", import.meta.url);
const growthTracePageUrl = new URL(
  "../src/app/trace/growth/[traceCode]/page.tsx",
  import.meta.url,
);
const cssUrl = new URL(
  "../src/app/trace/digital-life/[traceCode]/page.module.css",
  import.meta.url,
);
const mapUrl = new URL(
  "../src/app/trace/digital-life/[traceCode]/DigitalLifeStageMap.tsx",
  import.meta.url,
);
const chartUrl = new URL(
  "../src/app/trace/digital-life/[traceCode]/DigitalLifeTrendChart.tsx",
  import.meta.url,
);
const evidenceUrl = new URL(
  "../src/app/trace/digital-life/[traceCode]/DigitalLifeStageEvidence.tsx",
  import.meta.url,
);
const mapConfigUrl = new URL("../src/components/map/leaflet-config.ts", import.meta.url);
const routesSource = readFileSync(
  new URL("../src/config/routes/public.ts", import.meta.url),
  "utf8",
);

test("数字生命档案公开路由无需登录", () => {
  assert.match(routesSource, /path:\s*"\/trace\/digital-life\/\[traceCode\]"/);
  assert.match(routesSource, /\/trace\/digital-life[\s\S]*public:\s*true/);
});

test("公开档案请求不复用携带 JWT 的管理端 request", () => {
  assert.equal(existsSync(apiUrl), true);
  const apiSource = readFileSync(apiUrl, "utf8");
  assert.match(apiSource, /axios\.get<ApiResult<PublicDigitalLifeArchiveApi>>/);
  assert.match(apiSource, /encodeURIComponent\(traceCode\)/);
  assert.doesNotMatch(apiSource, /getStoredToken|Authorization|apiGet\(|request\.get/);
});

test("公开档案页面继承单条溯源页的档案封面、章节卡片和主舞台", () => {
  assert.equal(existsSync(pageUrl), true);
  assert.equal(existsSync(cssUrl), true);
  const pageSource = readFileSync(pageUrl, "utf8");
  const cssSource = readFileSync(cssUrl, "utf8");
  assert.match(pageSource, /currentStageIndex/);
  assert.match(pageSource, /currentStage/);
  assert.match(pageSource, /setCurrentStageIndex/);
  assert.match(pageSource, /DIGITAL LIFE ARCHIVE/);
  assert.match(pageSource, /档案可信状态/);
  assert.match(pageSource, /数字生命回放/);
  assert.match(pageSource, /按时间顺序回放中药材各阶段/);
  assert.match(pageSource, /styles\.trustCard/);
  assert.match(pageSource, /styles\.playbackDeck/);
  assert.match(pageSource, /styles\.stageShell/);
  assert.match(pageSource, /styles\.evidencePanel/);
  assert.match(pageSource, /生长时空轨迹/);
  assert.match(pageSource, /生长趋势/);
  assert.match(pageSource, /AI 阶段解说/);
  assert.match(pageSource, /阶段现场影像与观测指标/);
  assert.match(pageSource, /阶段时间轴/);
  assert.doesNotMatch(pageSource, /暂无可公开的连续观测阶段。暂无可公开的连续观测阶段。/);
  assert.match(cssSource, /max-width:\s*1480px/);
  assert.match(cssSource, /border-radius:\s*24px/);
  assert.match(
    cssSource,
    /grid-template-columns:\s*minmax\(0,\s*1\.55fr\)\s+minmax\(380px,\s*0\.9fr\)/,
  );
  assert.match(cssSource, /\.trustCard/);
  assert.match(cssSource, /\.playbackDeck/);
  assert.match(cssSource, /\.stageShell/);
  assert.match(cssSource, /\.timeline\s*\{[^}]*grid-template-columns:\s*repeat\(var\(--stage-count\),\s*minmax\(190px,\s*1fr\)\)/s);
  assert.match(cssSource, /overflow-x:\s*auto/);
  assert.match(cssSource, /linear-gradient/);
});

test("数字生命回放使用单一阶段入口和单一动画帧计时器", () => {
  const pageSource = readFileSync(pageUrl, "utf8");
  const cssSource = readFileSync(cssUrl, "utf8");
  assert.match(pageSource, /const \[isPlaying, setIsPlaying\]/);
  assert.match(pageSource, /const \[playbackSpeed, setPlaybackSpeed\]/);
  assert.match(pageSource, /const \[progress, setProgress\]/);
  assert.match(pageSource, /const \[hasFinished, setHasFinished\]/);
  assert.match(pageSource, /setActiveStage/);
  assert.match(pageSource, /"autoplay" \| "timeline" \| "previous" \| "next" \| "chart" \| "map"/);
  assert.match(pageSource, /requestAnimationFrame/);
  assert.match(pageSource, /cancelAnimationFrame/);
  assert.match(pageSource, /visibilitychange/);
  assert.match(pageSource, /数字生命回放完成/);
  assert.match(pageSource, /0\.5x/);
  assert.match(pageSource, /2x/);
  assert.match(cssSource, /@keyframes stageFade/);
});

test("阶段地图复用 Leaflet 配置并由统一阶段索引双向联动", () => {
  assert.equal(existsSync(mapUrl), true);
  assert.equal(existsSync(mapConfigUrl), true);
  const pageSource = readFileSync(pageUrl, "utf8");
  const mapSource = readFileSync(mapUrl, "utf8");
  assert.match(pageSource, /dynamic\(\(\) => import\("\.\/DigitalLifeStageMap"\)/);
  assert.match(pageSource, /setActiveStage\(index, "map"\)/);
  assert.match(mapSource, /createTileLayer\("standard"\)/);
  assert.match(mapSource, /const currentCoordinates = useMemo/);
  assert.match(mapSource, /\(\) => getCoordinates\(currentStage\),\s*\[currentStage\]/);
  assert.match(mapSource, /marker\.setRadius/);
  assert.match(mapSource, /map\.flyTo/);
  assert.match(mapSource, /当前阶段暂无精确坐标，已保留地点文本信息/);
  assert.doesNotMatch(mapSource, /CHONGQING_CENTER.*circleMarker/);
});

test("阶段地图弹窗使用本草档案浮签并保持阶段联动", () => {
  const mapSource = readFileSync(mapUrl, "utf8");
  const cssSource = readFileSync(cssUrl, "utf8");
  assert.match(mapSource, /digital-life-stage-popup/);
  assert.match(mapSource, /offset:\s*L\.point\(0,\s*-\d+\)/);
  assert.match(mapSource, /maxWidth:\s*260/);
  assert.doesNotMatch(mapSource, /minWidth:/);
  assert.match(mapSource, /const stageTitle = growthStage\s*\?/);
  assert.match(mapSource, /approved:\s*\{\s*label:\s*"已通过"/);
  assert.match(mapSource, /已通过:\s*\{\s*label:\s*"已通过"/);
  assert.match(mapSource, /stage\.images\?\.length/);
  assert.match(mapSource, /暂无地点信息/);
  assert.doesNotMatch(mapSource, /阶段待完善|状态待确认|基地信息待完善|采集地点待完善/);
  assert.match(mapSource, /onStageSelect\(index\)/);
  assert.match(mapSource, /activeMarker\.openPopup\(\)/);
  assert.match(cssSource, /\.mapViewport\s+:global\(\.digital-life-stage-popup \.leaflet-popup-content-wrapper\)/);
  assert.match(cssSource, /\.mapViewport\s+:global\(\.digital-life-stage-popup \.leaflet-popup-content\)/);
  assert.match(cssSource, /\.mapViewport\s+:global\(\.digital-life-stage-popup \.leaflet-popup-tip\)/);
  assert.match(cssSource, /width:\s*260px/);
  assert.match(cssSource, /background:\s*#fffaf2/);
  assert.match(cssSource, /border-radius:\s*14px/);
  assert.match(cssSource, /max-width:\s*calc\(100vw - 64px\)/);
  assert.match(cssSource, /white-space:\s*nowrap/);
});

test("生长趋势使用 ECharts Canvas 并与统一阶段索引双向联动", () => {
  assert.equal(existsSync(chartUrl), true);
  const pageSource = readFileSync(pageUrl, "utf8");
  const chartSource = readFileSync(chartUrl, "utf8");
  assert.match(pageSource, /setActiveStage\(index, "chart"\)/);
  assert.match(chartSource, /CanvasRenderer/);
  assert.match(chartSource, /LineChart/);
  assert.match(chartSource, /GraphicComponent/);
  assert.match(chartSource, /registerECharts\(\[[^\]]*GraphicComponent/s);
  assert.match(chartSource, /dispatchAction\(\{ type: "highlight"/);
  assert.match(chartSource, /dispatchAction\(\{ type: "showTip"/);
  assert.match(chartSource, /dispatchAction\(\{[\s\S]*type: "dataZoom"/);
  assert.match(chartSource, /value: value == null \? null : value/);
  assert.match(chartSource, /onStageSelect\(stageIndex\)/);
  assert.match(chartSource, /return \(\) => \{\s*chart\.off\("click", handleClick\);\s*\}/);
  assert.match(chartSource, /本阶段暂无该指标/);
  assert.match(chartSource, /graphic:\s*hasValues\s*\?\s*\[\]/);
  assert.doesNotMatch(chartSource, /<svg|createElementNS/);
});

test("阶段证据区同步图片并对真实数值执行轻量动画和客观差值", () => {
  assert.equal(existsSync(evidenceUrl), true);
  const evidenceSource = readFileSync(evidenceUrl, "utf8");
  assert.match(evidenceSource, /currentStageIndex/);
  assert.match(evidenceSource, /primaryImage/);
  assert.match(evidenceSource, /setSelectedImageIndex/);
  assert.match(evidenceSource, /requestAnimationFrame/);
  assert.match(evidenceSource, /cancelAnimationFrame/);
  assert.match(evidenceSource, /previousValue == null \|\| value == null/);
  assert.match(evidenceSource, /leaf:\s*"叶片"/);
  assert.match(evidenceSource, /whole_plant:\s*"整株"/);
  assert.match(evidenceSource, /当前阶段暂无现场影像/);
  assert.doesNotMatch(evidenceSource, /value\s*\?\?\s*0|value\s*\|\|\s*0/);
});

test("公开播放只读取缓存解说并按来源显示标签", () => {
  const pageSource = readFileSync(pageUrl, "utf8");
  const apiSource = readFileSync(apiUrl, "utf8");
  assert.match(pageSource, /currentStage\?\.aiNarration\s*\|\|/);
  assert.match(pageSource, /AI 阶段解说/);
  assert.match(pageSource, /规则摘要/);
  assert.match(apiSource, /narrationSource/);
  assert.match(apiSource, /narrationGeneratedTime/);
  assert.doesNotMatch(pageSource, /narrations\/generate|ArkResponses|ChatClient/);
});

test("公开页只展示后端返回的防篡改校验摘要", () => {
  const pageSource = readFileSync(pageUrl, "utf8");
  const apiSource = readFileSync(apiUrl, "utf8");
  assert.match(apiSource, /getPublicDigitalLifeIntegrity/);
  assert.match(apiSource, /\/integrity`/);
  assert.match(pageSource, /integrityData\.verified/);
  assert.match(pageSource, /防篡改档案校验通过/);
  assert.match(pageSource, /档案完整性校验异常/);
  assert.match(pageSource, /尚未生成哈希证据链/);
  assert.match(pageSource, /查看校验详情/);
  assert.doesNotMatch(pageSource, /crypto\.subtle|createHash|sha256\(/i);
});

test("最终节点和演示模式复用统一播放状态", () => {
  const pageSource = readFileSync(pageUrl, "utf8");
  const cssSource = readFileSync(cssUrl, "utf8");
  assert.match(pageSource, /const \[demoMode, setDemoMode\]/);
  assert.match(pageSource, /setPlaybackSpeed\(1\)/);
  assert.match(pageSource, /playFromBeginning\(\)/);
  assert.match(pageSource, /hasFinished \? \(/);
  assert.match(pageSource, /数字生命回放完成/);
  assert.match(pageSource, /审核人信息未公开/);
  assert.match(pageSource, /window\.print\(\)/);
  assert.match(pageSource, /退出演示/);
  assert.match(cssSource, /\.demoMode/);
  assert.match(cssSource, /@media print/);
});

test("task-level digital life QR code uses the public archive resource", () => {
  const pageSource = readFileSync(pageUrl, "utf8");
  const apiSource = readFileSync(apiUrl, "utf8");
  assert.match(apiSource, /qrCodeUrl\?:\s*string/);
  assert.match(pageSource, /import Image from "next\/image"/);
  assert.match(
    pageSource,
    /resolveDigitalLifeResourceUrl\(archiveData\.qrCodeUrl\)/,
  );
  assert.match(pageSource, /archiveData\.qrCodeUrl\s*\?/);
  assert.match(pageSource, /alt="数字生命档案二维码"/);
});

test("both public archive pages provide a safe back action", () => {
  for (const sourceUrl of [pageUrl, growthTracePageUrl]) {
    const source = readFileSync(sourceUrl, "utf8");
    assert.match(source, /ArrowLeft/);
    assert.match(source, /handleBackNavigation/);
    assert.match(source, /document\.referrer/);
    assert.match(source, /window\.history\.back\(\)/);
    assert.match(source, /window\.location\.assign\("\/"\)/);
    assert.match(source, /aria-label="返回上一页"/);
    assert.match(source, /styles\.backButton/);
  }
});
