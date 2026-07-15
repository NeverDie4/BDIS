import assert from "node:assert/strict";
import { createServer } from "node:http";
import test from "node:test";
import { createClient } from "./bdis-api-client.mjs";

test("accepts the project's SUCCESS response code", async (t) => {
  const server = createServer((request, response) => {
    response.writeHead(200, { "Content-Type": "application/json" });
    response.end(
      JSON.stringify({ code: "SUCCESS", message: "操作成功", data: { id: 1 } }),
    );
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());
  const address = server.address();
  const client = createClient(`http://127.0.0.1:${address.port}`);
  assert.deepEqual(await client.request("/demo"), { id: 1 });
});
