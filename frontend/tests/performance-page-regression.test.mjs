import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const root = new URL("../src/", import.meta.url);

test("业绩页面为查询参数客户端组件提供 Suspense 边界", async () => {
  const [pageSource, clientSource] = await Promise.all([
    readFile(new URL("app/performance/page.tsx", root), "utf8"),
    readFile(new URL("components/performance/PerformancePageClient.tsx", root), "utf8"),
  ]);

  assert.match(clientSource, /useSearchParams/);
  assert.match(pageSource, /import\s*\{\s*Suspense\s*\}\s*from\s*["']react["']/);
  assert.match(
    pageSource,
    /<Suspense\s+fallback=\{null\}>\s*<PerformancePageClient\s*\/>\s*<\/Suspense>/s,
  );
});
