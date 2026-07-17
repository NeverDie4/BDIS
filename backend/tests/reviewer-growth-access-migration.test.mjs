import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const migrationUrl = new URL(
  "../src/main/resources/db/migration/V20260717_002__configure_reviewer_growth_access.sql",
  import.meta.url,
);

test("reviewers can view and audit all growth tasks", async () => {
  const sql = await readFile(migrationUrl, "utf8");

  assert.match(sql, /role_code\s*=\s*'REVIEWER'/i);
  assert.match(sql, /resource_type[\s\S]*'herb_growth_record'/i);
  assert.match(sql, /scope_type[\s\S]*'all'/i);
  assert.match(sql, /ON DUPLICATE KEY UPDATE/i);
  assert.match(sql, /permission_code[\s\S]*'growth:record:view'/i);
  assert.match(sql, /permission_code[\s\S]*'growth:record:audit'/i);
});
