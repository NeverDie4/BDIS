import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const root = new URL("../src/", import.meta.url);

async function readSource(relativePath) {
  return readFile(new URL(relativePath, root), "utf8");
}

test("header does not render the museum notification placeholder", async () => {
  const source = await readSource("components/layout/HeaderNav.tsx");
  const css = await readSource("components/layout/HeaderNav.module.css");

  assert.doesNotMatch(source, /BellOutlined|notificationOpen|setNotificationOpen/);
  assert.doesNotMatch(source, /NOTICES|<Drawer|<Badge|<Tooltip|Typography/);
  assert.doesNotMatch(source, /馆内通知|打开通知/);
  assert.doesNotMatch(css, /\.iconButton|\.drawerBody|\.noticeList|\.noticeItem/);
});
