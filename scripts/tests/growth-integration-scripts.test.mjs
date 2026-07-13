import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";

const root = path.resolve(import.meta.dirname, "..", "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("growth integration checks use cross-platform Node entry points", () => {
  const packageJson = JSON.parse(read("package.json"));

  assert.equal(
    packageJson.scripts["test:growth:audit"],
    "node scripts/test-growth-audit-chain.mjs",
  );
  assert.equal(
    packageJson.scripts["test:growth:trace"],
    "node scripts/test-growth-trace-qrcode.mjs",
  );
  assert.equal(fs.existsSync(path.join(root, "scripts/test-growth-audit-chain.ps1")), false);
  assert.equal(fs.existsSync(path.join(root, "scripts/test-growth-trace-qrcode.ps1")), false);
});

test("audit chain creates and updates records through an explicit owned batch", () => {
  const source = read("scripts/test-growth-audit-chain.mjs");

  assert.match(source, /--batch-id/);
  assert.match(source, /\/herb\/batch\/\$\{batchId\}\/growth-record/);
  assert.doesNotMatch(source, /method:\s*["']POST["'][\s\S]{0,120}path:\s*["']\/growth-records["']/);
});

test("trace check verifies the controlled anonymous QR resource", () => {
  const source = read("scripts/test-growth-trace-qrcode.mjs");

  assert.match(source, /\/trace\/growth\/\$\{traceCode\}\/qrcode/);
  assert.match(source, /trace_code_generated/);
  assert.match(source, /public_trace_disabled/);
});
