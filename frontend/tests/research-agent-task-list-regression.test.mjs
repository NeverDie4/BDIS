import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const pageSource = readFileSync(
  new URL("../src/app/assistant/research-agent/ResearchAgentContent.tsx", import.meta.url),
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
