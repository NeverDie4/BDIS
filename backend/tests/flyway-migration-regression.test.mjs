import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile, readdir } from "node:fs/promises";
import test from "node:test";

const migrations = new URL(
  "../src/main/resources/db/migration/",
  import.meta.url,
);

async function readMigration(name) {
  return readFile(new URL(name, migrations), "utf8");
}

function sha256(content) {
  return createHash("sha256")
    .update(content.replace(/\r\n/g, "\n"))
    .digest("hex");
}

test("已发布的历史迁移保持 dev 原始校验和", async () => {
  const [chat, knowledge, fileAccess] = await Promise.all([
    readMigration("V20260711_001__add_ai_chat_history_tables.sql"),
    readMigration("V20260711_002__add_ai_knowledge_tables.sql"),
    readMigration("V20260711_004__add_file_access_level.sql"),
  ]);

  assert.equal(
    sha256(chat),
    "a7eab907ab7f2985f4fcf683e60ee2cf510eec96811eef881b244168c2588524",
  );
  assert.equal(
    sha256(knowledge),
    "eb5f78f09c82bf85cec91afbe0d6942b169bd986b8740debde4d82443de38a7e",
  );
  assert.equal(
    sha256(fileAccess),
    "84eedde669a9b58d0be90fedbaf5dd58d35997e158e86a34253f80164fe220c8",
  );
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
test("药材分类字典类型由独立前向迁移初始化", async () => {
  const migration = await readMigration(
    "V20260714_017__seed_herb_category_dictionary.sql",
  );

  assert.match(migration, /INSERT\s+IGNORE\s+INTO\s+`dict_type`/i);
  assert.match(migration, /'herb_category'/);
  assert.match(migration, /中药材分类/);
  assert.doesNotMatch(migration, /ALTER\s+TABLE|DROP\s+COLUMN/i);
});
