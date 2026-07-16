import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";

const migrationPath = path.resolve(
  import.meta.dirname,
  "../src/main/resources/db/migration/V20260714_008__restore_growth_reviewer_data_scope.sql",
);

test("forward repair restores reviewer scope from role default without granting all", () => {
  const sql = fs.readFileSync(migrationPath, "utf8");

  assert.match(sql, /JOIN\s+`auth_role`/i);
  assert.match(sql, /`scope`\.`scope_type`\s*=\s*`role`\.`data_scope`/i);
  assert.match(sql, /`scope`\.`scope_type`\s*=\s*'all'/i);
  assert.match(sql, /`role`\.`data_scope`\s*<>\s*'all'/i);
  assert.doesNotMatch(sql, /SET\s+`scope`\.`scope_type`\s*=\s*'all'/i);
});
