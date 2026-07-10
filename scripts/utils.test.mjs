import test from 'node:test';
import assert from 'node:assert/strict';

import { capture, commandName } from './utils.mjs';

test('capture runs pnpm command wrappers on Windows', () => {
  const result = capture(commandName('pnpm'), ['--version']);

  assert.equal(result.ok, true, result.message);
  assert.match(result.message, /^\d+\.\d+\.\d+$/);
});
