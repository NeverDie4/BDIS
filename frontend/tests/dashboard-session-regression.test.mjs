import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const source = readFileSync(
  new URL("../src/components/dashboard-shell.tsx", import.meta.url),
  "utf8",
);

test("会话恢复网络错误不会无条件清理 Token", () => {
  assert.match(source, /isSessionInvalidError\(error\)/);
  assert.doesNotMatch(
    source,
    /if \(!isAuthRedirectError\(error\)\) \{\s*clearAuth\(\);\s*router\.replace\("\/login"\);/,
  );
});

test("会话恢复失败提供重试入口", () => {
  assert.match(source, /setSessionLoadError/);
  assert.match(source, /重新加载/);
});
