import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const pageSource = readFileSync(
  new URL("../src/app/assistant/research-agent/ResearchAgentContent.tsx", import.meta.url),
  "utf8",
);
const requestSource = readFileSync(
  new URL("../src/lib/request.ts", import.meta.url),
  "utf8",
);

test("科研 Agent 创建后刷新任务列表且列表错误不伪装为空状态", () => {
  assert.match(
    pageSource,
    /invalidateQueries\(\{ queryKey: \["research-agent-list"\] \}\)/,
  );
  assert.match(pageSource, /loading=\{recentTasks\.isLoading \|\| recentTasks\.isFetching\}/);
  assert.match(pageSource, /error=\{recentTasks\.isError/);
  assert.match(pageSource, /onRetry=\{\(\) => void recentTasks\.refetch\(\)\}/);
  assert.match(pageSource, /无法读取科研 Agent 任务/);
});

test("用户界面隐藏技术超时异常并将 Agent 创建超时视为后台处理中", () => {
  assert.match(requestSource, /export function isRequestTimeoutError/);
  assert.match(requestSource, /请求仍在处理中，请稍后查看结果/);
  assert.match(requestSource, /sanitizeTimeoutError\(error\);/);
  assert.match(
    requestSource,
    /if \(isRequestTimeoutError\(error\)\) \{\s*return REQUEST_TIMEOUT_MESSAGE;/,
  );
  assert.match(
    pageSource,
    /onError: async \(error\) => \{\s*if \(isRequestTimeoutError\(error\)\)/,
  );
  assert.match(pageSource, /科研 Agent 任务正在后台创建，请稍后在任务列表查看/);
});
