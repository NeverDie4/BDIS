import assert from "node:assert/strict";
import test from "node:test";
import { escapeCsvCell } from "./csv.ts";

test("escapes quotes without changing ordinary text", () => {
  assert.equal(escapeCsvCell('黄连"基地'), '"黄连""基地"');
});

test("neutralizes spreadsheet formulas in user text", () => {
  assert.equal(escapeCsvCell("=HYPERLINK(\"https://example.com\")"), '"\'=HYPERLINK(""https://example.com"")"');
  assert.equal(escapeCsvCell("  +1+1"), '"\'  +1+1"');
  assert.equal(escapeCsvCell("@SUM(A1:A2)"), '"\'@SUM(A1:A2)"');
});

test("keeps numeric coordinates numeric", () => {
  assert.equal(escapeCsvCell(-106.55156), '"-106.55156"');
});
