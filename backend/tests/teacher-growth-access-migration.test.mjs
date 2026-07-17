import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const migrationUrl = new URL(
  "../src/main/resources/db/migration/V20260717_001__configure_teacher_and_student_growth_access.sql",
  import.meta.url,
);

test("teacher sees all growth tasks while students lose the growth page permission", async () => {
  const sql = await readFile(migrationUrl, "utf8");

  assert.match(sql, /role_code\s*=\s*'TEACHER'/i);
  assert.match(sql, /resource_type[\s\S]*'herb_growth_record'/i);
  assert.match(sql, /scope_type[\s\S]*'all'/i);
  assert.match(sql, /ON DUPLICATE KEY UPDATE/i);
  assert.match(sql, /DELETE\s+rp\s+FROM\s+rel_role_permission\s+rp/i);
  assert.match(sql, /role_code\s*=\s*'STUDENT'/i);
  assert.match(sql, /permission_code\s*=\s*'growth:record:view'/i);
});
