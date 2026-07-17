import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const apiSource = readFileSync(
  new URL("../src/lib/growth-records.ts", import.meta.url),
  "utf8",
);

test("公开生长溯源资源仅在 API 基址为绝对地址时拼接来源站点", () => {
  assert.ok(apiSource.includes('const normalizedApiBase = baseUrl.replace(/\\/+$/, "");'));
  assert.ok(apiSource.includes("const absoluteApiBase = /^https?:\\/\\//i.test(normalizedApiBase);"));
  assert.ok(apiSource.includes("new URL(normalizedApiBase).origin"));
  assert.ok(
    apiSource.includes(
      "return absoluteApiBase ? `${new URL(normalizedApiBase).origin}${value}` : value;",
    ),
  );
  assert.doesNotMatch(
    apiSource,
    /new URL\(baseUrl,\s*"http:\/\/localhost"\)\.origin/,
  );
});
