import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile, readdir } from "node:fs/promises";
import test from "node:test";

const migrations = new URL(
  "../src/main/resources/db/migration/",
  import.meta.url,
);
const devBaseline = new URL(
  "../src/test/resources/flyway-dev-migration-baseline.txt",
  import.meta.url,
);

async function readMigration(name) {
  return readFile(new URL(name, migrations), "utf8");
}

function sha256(content) {
  return createHash("sha256").update(content).digest("hex");
}

test("dev 基线迁移保持原始字节 SHA-256", async () => {
  const entries = (await readFile(devBaseline, "utf8"))
    .trim()
    .split(/\r?\n/)
    .map((line) => line.trim().split(/\s+/));

  for (const [name, expectedHash] of entries) {
    assert.match(expectedHash, /^[a-f0-9]{64}$/);
    assert.equal(
      sha256(await readFile(new URL(name, migrations))),
      expectedHash,
      `checksum mismatch for ${name}`,
    );
  }
});

test("AI 表结构调整通过独立前向迁移完成", async () => {
  const forward = await readMigration(
    "V20260714_004__forward_fix_ai_table_schema.sql",
  );

  for (const token of [
    "herb_ai_chat_session",
    "herb_ai_chat_message",
    "herb_ai_knowledge_doc",
    "herb_ai_knowledge_chunk",
    "is_deleted",
    "user_id",
    "uk_ai_chat_session_user_id",
    "fk_ai_chat_message_session",
  ]) {
    assert.match(forward, new RegExp(token));
  }
  assert.match(forward, /information_schema\.columns/i);
  assert.match(forward, /information_schema\.statistics/i);
});

test("文件访问级别兼容修复位于独立前向迁移", async () => {
  const forward = await readMigration(
    "V20260714_007__forward_fix_file_access_level.sql",
  );

  assert.match(forward, /sys_file_resource/i);
  assert.match(forward, /access_level/i);
  assert.match(forward, /information_schema\.columns/i);
  assert.match(forward, /information_schema\.statistics/i);
  assert.match(forward, /PREPARE stmt/i);
});

test("审核员生长记录范围迁移不覆盖既有数据范围", async () => {
  const migration = await readMigration(
    "V20260714_006__grant_growth_reviewer_data_scope.sql",
  );

  assert.match(migration, /'department'/);
  assert.doesNotMatch(migration, /'all'/);
  assert.doesNotMatch(migration, /ON DUPLICATE KEY UPDATE/i);
});

test("迁移版本唯一且 PR 迁移晚于 dev 基线", async () => {
  const files = (await readdir(migrations)).filter((name) =>
    /^V\d+_\d+__.+\.sql$/.test(name),
  );
  const versions = files.map((name) => name.match(/^V(\d+_\d+)__/)?.[1]);
  assert.equal(
    new Set(versions).size,
    versions.length,
    "Flyway migration versions must be unique",
  );

  const expected = [
    "V20260714_002__add_growth_trace_events.sql",
    "V20260714_003__link_growth_record_to_collection_batch.sql",
    "V20260714_004__forward_fix_ai_table_schema.sql",
    "V20260714_005__add_growth_trace_qrcode_fields.sql",
    "V20260714_006__grant_growth_reviewer_data_scope.sql",
    "V20260714_007__forward_fix_file_access_level.sql",
    "V20260714_008__restore_growth_reviewer_data_scope.sql",
  ];

  for (const name of expected)
    assert.ok(files.includes(name), `missing migration ${name}`);
});

test("Agent 持久化底座通过独立前向迁移建立并发约束", async () => {
  const migration = await readMigration(
    "V20260716_007__add_assistant_agent_task_tables.sql",
  );

  for (const table of [
    "assistant_agent_task",
    "assistant_agent_step",
    "assistant_agent_finding",
    "assistant_agent_action",
  ]) {
    assert.match(migration, new RegExp("CREATE TABLE `" + table + "`"));
  }
  assert.match(migration, /uk_assistant_agent_task_no/);
  assert.match(migration, /uk_agent_task_active_key/);
  assert.match(migration, /uk_agent_step_single_running/);
  assert.match(migration, /active_task_key[\s\S]*GENERATED ALWAYS/i);
  assert.match(migration, /running_guard[\s\S]*GENERATED ALWAYS/i);
  assert.match(migration, /LONGTEXT/);
  assert.doesNotMatch(migration, /INSERT\s+INTO/i);
  assert.doesNotMatch(migration, /^\s*DROP\s+TABLE/im);
});

test("Agent 只读工具调用日志通过独立前向迁移建立", async () => {
  const migration = await readMigration(
    "V20260716_008__add_assistant_agent_tool_call_log.sql",
  );

  assert.match(
    migration,
    /CREATE TABLE `assistant_agent_tool_call_log`/,
  );
  for (const index of [
    "idx_agent_tool_task",
    "idx_agent_tool_step",
    "idx_agent_tool_user_time",
    "idx_agent_tool_name",
  ]) {
    assert.match(migration, new RegExp(index));
  }
  assert.match(migration, /output_json` LONGTEXT/i);
  assert.doesNotMatch(migration, /INSERT\s+INTO/i);
  assert.doesNotMatch(migration, /^\s*DROP\s+TABLE/im);
});

test("药材分类字典类型由独立前向迁移初始化", async () => {
  const migration = await readMigration(
    "V20260714_017__seed_herb_category_dictionary.sql",
  );

  assert.match(migration, /INSERT\s+(?:IGNORE\s+)?INTO\s+`dict_type`/i);
  assert.match(migration, /'herb_category'/);
  assert.match(migration, /中药材分类/);
  assert.match(migration, /INSERT\s+(?:IGNORE\s+)?INTO\s+`dict_item`/i);
  assert.match(migration, /HERB_CAT_QINGRE/);
  assert.match(migration, /HERB_CAT_BUYI/);
  assert.match(migration, /HERB_CAT_HUOXUE/);
  assert.match(
    migration,
    /SELECT\s+DISTINCT\s+category_code[\s\S]*FROM\s+herb_species/i,
  );
  assert.match(
    migration,
    /UPDATE\s+herb_species[\s\S]*SET\s+s\.category_id\s*=\s*item\.id/i,
  );
  assert.match(
    migration,
    /ON\s+DUPLICATE\s+KEY\s+UPDATE[\s\S]*is_deleted\s*=\s*0/i,
  );
  assert.doesNotMatch(migration, /ALTER\s+TABLE|DROP\s+COLUMN/i);
});

test("药材分类字典迁移不得复制到新的重复版本", async () => {
  const files = await readdir(migrations);

  assert.equal(
    files.includes("V20260715_002__complete_herb_category_dictionary_seed.sql"),
    false,
  );
});

test("Agent diagnosis finding idempotency uses a forward migration", async () => {
  const migration = await readMigration(
    "V20260716_009__add_agent_finding_idempotency_key.sql",
  );

  assert.match(migration, /ALTER TABLE `assistant_agent_finding`/);
  assert.match(migration, /finding_guard[\s\S]*GENERATED ALWAYS/i);
  assert.match(migration, /uk_agent_finding_guard/);
  assert.doesNotMatch(migration, /INSERT\s+INTO/i);
  assert.doesNotMatch(migration, /^\s*DROP\s+TABLE/im);
});

test("Agent follow-up collection plan uses a forward migration", async () => {
  const migration = await readMigration(
    "V20260716_010__add_agent_collection_plan.sql",
  );

  assert.match(migration, /CREATE TABLE `assistant_agent_collection_plan`/);
  assert.match(migration, /uk_agent_collection_plan_no/);
  assert.match(migration, /uk_agent_collection_round/);
  assert.match(migration, /uk_agent_active_collection_plan_action/);
  assert.match(migration, /required_metrics_json` LONGTEXT/i);
  assert.doesNotMatch(migration, /INSERT\s+INTO/i);
  assert.doesNotMatch(migration, /^\s*DROP\s+TABLE/im);
});

test("Agent terminal confirmation actions close historical waiting steps", async () => {
  const migration = await readMigration(
    "V20260716_014__close_agent_confirmation_wait_steps.sql",
  );

  assert.match(migration, /UPDATE `assistant_agent_step` AS waiting_step/i);
  assert.match(migration, /waiting_step\.step_type = 'WAIT_FOR_CONFIRMATION'/i);
  assert.match(migration, /action_row\.status IN \('SUCCEEDED', 'REJECTED', 'CANCELLED', 'FAILED'\)/i);
  assert.match(migration, /generation_step\.step_no \+ 1/i);
  assert.doesNotMatch(migration, /^\s*DROP\s+TABLE/im);
});

test("Agent removes unsupported sample weight requirements with a forward migration", async () => {
  const migration = await readMigration(
    "V20260716_015__remove_agent_sample_weight_requirement.sql",
  );

  assert.match(migration, /required_metrics_json[\s\S]*sampleWeight/i);
  assert.match(migration, /condition_json[\s\S]*sampleWeight/i);
  assert.match(migration, /current_snapshot_json[\s\S]*缺少采样重量/i);
  assert.match(
    migration,
    /DELETE FROM `assistant_agent_collection_requirement`[\s\S]*requirement_code = 'sampleWeight'/i,
  );
  assert.doesNotMatch(migration, /^\s*DROP\s+TABLE/im);
});
