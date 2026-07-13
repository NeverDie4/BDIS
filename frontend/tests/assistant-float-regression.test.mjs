import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const componentSource = readFileSync(
  new URL("../src/components/assistant-float.tsx", import.meta.url),
  "utf8",
);
const cssSource = readFileSync(
  new URL("../src/components/assistant-float.module.css", import.meta.url),
  "utf8",
);

test("AI 悬浮球支持自由拖动并持久化统一位置", () => {
  assert.match(componentSource, /FLOAT_POSITION_KEY/);
  assert.match(componentSource, /onPointerDown=\{handleFloatPointerDown\}/);
  assert.match(componentSource, /onPointerMove=\{handleFloatPointerMove\}/);
  assert.match(componentSource, /onPointerUp=\{handleFloatPointerUp\}/);
  assert.match(componentSource, /setPointerCapture/);
  assert.match(componentSource, /clampFloatPosition/);
  assert.match(componentSource, /localStorage\.setItem\(FLOAT_POSITION_KEY/);
  assert.match(componentSource, /addEventListener\("storage"/);
  assert.match(componentSource, /FLOAT_POSITION_EVENT/);
});

test("AI 悬浮球拖动时使用抓取光标且不触发页面滚动", () => {
  assert.match(cssSource, /\.floatButton\s*\{[^}]*touch-action:\s*none;/s);
  assert.match(cssSource, /\.floatButtonDragging\s*\{[^}]*cursor:\s*grabbing;/s);
});

test("AI 悬浮球松手后左右吸边并保存边侧与高度", () => {
  assert.match(componentSource, /const FLOAT_POSITION_KEY = "web_assistant_float_position"/);
  assert.match(componentSource, /type FloatAnchor = \{[\s\S]*side: "left" \| "right";[\s\S]*top: number;/);
  assert.match(componentSource, /function snapFloatToSide/);
  assert.match(componentSource, /position\.x \+ width \/ 2 < window\.innerWidth \/ 2/);
  assert.match(componentSource, /persistFloatPosition\(snappedPosition\.anchor\)/);
  assert.match(componentSource, /FLOAT_HORIZONTAL_GAP = 24/);
  assert.match(componentSource, /FLOAT_NAV_HEIGHT \+ FLOAT_NAV_GAP/);
});

test("AI 悬浮球吸边时有动画而拖动时关闭动画", () => {
  assert.match(
    cssSource,
    /\.floatButton\s*\{[^}]*transition:[^}]*left 0\.22s ease,[^}]*top 0\.18s ease,[^}]*transform 0\.18s ease;/s,
  );
  assert.match(cssSource, /\.floatButtonDragging\s*\{[^}]*transition:\s*none;/s);
});
