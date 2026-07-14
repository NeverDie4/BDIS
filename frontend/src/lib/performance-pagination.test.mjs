import assert from "node:assert/strict";
import test from "node:test";
import { resolveServerPagination, toPageRequest } from "./performance-pagination.ts";

test("requests the eleventh server page when records exceed one hundred", () => {
  const page = resolveServerPagination(1, 10, 11, 10);

  assert.deepEqual(page, { current: 11, pageSize: 10 });
  assert.deepEqual(toPageRequest(page), { pageNum: 11, pageSize: 10 });
});

test("returns to the first server page after changing page size", () => {
  assert.deepEqual(resolveServerPagination(11, 10, 11, 20), { current: 1, pageSize: 20 });
});
