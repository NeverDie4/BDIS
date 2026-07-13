import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const boundarySource = readFileSync(
  new URL("../src/components/auth/RouteAccessBoundary.tsx", import.meta.url),
  "utf8",
);
const shellBuffer = readFileSync(new URL("../src/components/dashboard-shell.tsx", import.meta.url));

test("会话恢复仅在鉴权失效时清理 Token", () => {
  assert.match(boundarySource, /if \(isAuthRedirectError\(error\)\) \{\s*clearAuth\(\);\s*return;/);
  assert.match(boundarySource, /setInitializationError\(getApiErrorMessage\(error,/);
});

test("会话恢复网络失败提供重试入口", () => {
  assert.match(boundarySource, /setRetryVersion/);
  assert.match(boundarySource, /onClick=\{\(\) => setRetryVersion\(\(value\) => value \+ 1\)\}/);
});

test("DashboardShell 源文件不包含 UTF-8 BOM", () => {
  assert.notDeepEqual([...shellBuffer.subarray(0, 3)], [0xef, 0xbb, 0xbf]);
});
