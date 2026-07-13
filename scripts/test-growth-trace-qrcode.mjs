import { createClient, parseArgs, requirePositiveId, step } from "./lib/bdis-api-client.mjs";

const args = parseArgs();
const baseUrl = args["base-url"] || process.env.BDIS_API_BASE_URL || "http://localhost:8080/api";
const client = createClient(baseUrl);
const username = args["admin-username"] || process.env.BDIS_ADMIN_USERNAME || "admin";
const password = args["admin-password"] || process.env.BDIS_ADMIN_PASSWORD || "password";

step("登录管理员并选择生长记录");
const token = await client.login(username, password);
let recordIdValue = args["record-id"] || process.env.BDIS_TEST_GROWTH_RECORD_ID;
if (!recordIdValue) {
  const page = await client.request("/growth-records?page=1&size=1", { token });
  recordIdValue = page?.records?.[0]?.id;
}
const recordId = requirePositiveId(recordIdValue, "recordId");

step("生成溯源码和二维码");
const traceInfo = await client.request(`/growth-records/${recordId}/trace-code/generate`, {
  method: "POST",
  token,
});
const traceCode = traceInfo?.traceCode;
if (!traceCode) throw new Error("生成溯源码后未返回 traceCode");
const qrInfo = await client.request(`/growth-records/${recordId}/trace-qrcode/generate`, {
  method: "POST",
  token,
});
if (!qrInfo?.qrCodeUrl) throw new Error("生成二维码后未返回 qrCodeUrl");
await client.request(`/growth-records/${recordId}/trace-qrcode`, { token });

step("开启公开溯源并验证匿名档案和二维码资源");
const enabled = await client.request(`/growth-records/${recordId}/trace/public-enable`, {
  method: "PUT",
  token,
});
if (!Boolean(enabled?.publicVisible)) throw new Error("公开溯源未成功开启");
const publicArchive = await client.request(`/trace/growth/${traceCode}`);
if (publicArchive?.traceCode !== traceCode) throw new Error("公开档案返回的 traceCode 不一致");
const publicQr = await client.request(`/trace/growth/${traceCode}/qrcode`, { raw: true });
if (!publicQr.ok || !publicQr.headers.get("content-type")?.includes("image/png")) {
  throw new Error(`匿名二维码资源不可用：HTTP ${publicQr.status}`);
}

step("关闭公开溯源并校验访问与事件");
const disabled = await client.request(`/growth-records/${recordId}/trace/public-disable`, {
  method: "PUT",
  token,
});
if (Boolean(disabled?.publicVisible)) throw new Error("公开溯源未成功关闭");
const denied = await client.request(`/trace/growth/${traceCode}`, { raw: true });
if (denied.ok) throw new Error("关闭公开溯源后匿名档案仍可访问");

const events = await client.request(`/growth-records/${recordId}/trace-events`, { token });
const eventTypes = new Set((events || []).map((event) => event.eventType));
for (const type of [
  "trace_code_generated",
  "trace_qrcode_generated",
  "public_trace_enabled",
  "public_trace_disabled",
]) {
  if (!eventTypes.has(type)) throw new Error(`缺少溯源事件：${type}`);
}

process.stdout.write(`\n二维码链路验证通过：recordId=${recordId}，traceCode=${traceCode}\n`);
