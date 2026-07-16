import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";

const root = path.resolve(import.meta.dirname, "../../..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("research Agent client uses authenticated APIs and adaptive polling", () => {
  const api = read("frontend/src/lib/research-agent.ts");

  assert.match(api, /const base = "\/herb\/assistant\/agent"/);
  assert.doesNotMatch(api, /userId/);
  assert.match(api, /WAITING_FIELD_DATA" \? 30_000 : 5_000/);
  assert.match(api, /\["COMPLETED", "FAILED", "CANCELLED"\]/);
  assert.match(api, /\/actions\/\$\{id\}\/confirm/);
  assert.match(api, /\/tasks\/\$\{id\}\/archive\/prepare/);
});

test("research Agent workbench requires a formal confirmation before business writes", () => {
  const page = read("frontend/src/app/assistant/research-agent/page.tsx");

  assert.match(page, /task\.currentPhase !== "EVIDENCE_ANALYSIS_COMPLETED"/);
  assert.match(page, /resumeLegacyTask\(taskId\)/);
  assert.match(page, /title="高风险动作二次确认"/);
  assert.match(page, /CREATE_FOLLOW_UP_COLLECTION_TASK/);
  assert.match(page, /confirmAgentAction\(selectedAction\.id,[\s\S]*publishAfterCreate|confirmAgentAction\(selectedAction\.id, selectedAction\.actionType === "CREATE_FOLLOW_UP_COLLECTION_TASK"\)/);
  assert.match(page, /是否公开数据/);
  assert.match(page, /是否可撤销/);
});

test("research Agent localizes persisted technical codes for users", () => {
  const page = read("frontend/src/app/assistant/research-agent/page.tsx");

  assert.match(page, /leaf:\s*"叶片"/);
  assert.match(page, /root:\s*"根部"/);
  assert.match(page, /whole_plant:\s*"全株"/);
  assert.match(page, /environment:\s*"现场环境"/);
  assert.match(page, /BATCH:\s*"采集批次"/);
  assert.match(page, /COLLECTION_TASK:\s*"采集任务"/);
  assert.match(page, /localizeTechnicalText/);
  assert.doesNotMatch(page, /\$\{finding\.targetType\}\s+#\$\{finding\.targetId/);
});

test("research Agent hides the stale plan while regeneration is pending", () => {
  const page = read("frontend/src/app/assistant/research-agent/page.tsx");
  const styles = read("frontend/src/app/assistant/research-agent/page.module.css");

  assert.match(page, /planRegenerating/);
  assert.match(page, /正在调用科研模型重新生成方案/);
  assert.match(page, /setPlanRegenerating\(true\)/);
  assert.match(page, /setPlanRegenerating\(false\)/);
  assert.match(page, /styles\.planGenerating/);
  assert.match(styles, /\.planGenerating/);
});

test("assistant float preserves chat while exposing Agent mode", () => {
  const float = read("frontend/src/components/assistant-float.tsx");
  const panel = read("frontend/src/components/assistant-agent-panel.tsx");

  assert.match(float, /智能问答/);
  assert.match(float, /科研 Agent/);
  assert.match(float, /<AssistantAgentPanel/);
  assert.match(panel, /sessionId/);
  assert.match(panel, /\/assistant\/research-agent\?agentTaskId=/);
});

test("demo workflow is explicitly synthetic and supplies the follow-up evidence contract", () => {
  const base = read("scripts/dev-research-agent-demo.sql");
  const followUp = read("scripts/dev-research-agent-followup.sql");
  const restore = read("scripts/restore-research-agent-demo.ps1");

  assert.match(base, /demo_seed/);
  assert.match(base, /not field-measured/);
  assert.match(base, /0\.6200/);
  assert.match(base, /soil_ph|土壤 pH 未记录/);
  assert.match(followUp, /assistant_agent_business_link/);
  assert.match(followUp, /whole_plant/);
  assert.match(followUp, /'leaf'/);
  assert.match(followUp, /'root'/);
  assert.match(followUp, /6\.15/);
  assert.match(restore, /\[ValidateSet\('Base','FollowUp'\)\]/);
  assert.match(restore, /else \{ 'biomed_dev' \}/);
  assert.match(restore, /else \{ 'bdis' \}/);
  assert.match(restore, /非现场实测/);
});
