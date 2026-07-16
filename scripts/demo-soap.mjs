import { spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import {
  createClient,
  parseArgs,
  requirePositiveId,
  step,
} from "./lib/bdis-api-client.mjs";
import { projectPath } from "./utils.mjs";

const LOCAL_HOSTS = new Set(["localhost", "127.0.0.1", "::1"]);
const EXPECTED_EXTERNAL_NO = "CAMPUS-DEMO-GROWTH-001";
const usage = `用法：pnpm demo:soap [--skip-seed] [--base-url <本地 API 地址>]

前置条件：本地 MySQL、Redis 和 BDIS 后端已启动；默认种子步骤需要 mysql/MariaDB 客户端；提供 BDIS_DEMO_TOKEN，或同时提供 BDIS_DEMO_USERNAME 与 BDIS_DEMO_PASSWORD。`;

function parseDotEnvValue(value) {
  const trimmed = value.trim();
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1);
  }
  const hashIndex = trimmed.indexOf(" #");
  return hashIndex === -1 ? trimmed : trimmed.slice(0, hashIndex).trimEnd();
}

function loadDotEnv() {
  const envPath = projectPath(".env");
  if (!existsSync(envPath)) return {};
  const values = {};
  for (const line of readFileSync(envPath, "utf8").split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const normalized = trimmed.startsWith("export ")
      ? trimmed.slice("export ".length).trim()
      : trimmed;
    const equalsIndex = normalized.indexOf("=");
    if (equalsIndex <= 0) continue;
    const key = normalized.slice(0, equalsIndex).trim();
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(key)) continue;
    values[key] = parseDotEnvValue(normalized.slice(equalsIndex + 1));
  }
  return values;
}

function envValue(env, name, fallback) {
  const value = env[name];
  return value === undefined || value === "" ? fallback : value;
}

function isLocalHost(host) {
  return LOCAL_HOSTS.has(host.replace(/^\[|\]$/g, "").toLowerCase());
}

function requireLocalHttpUrl(value, label) {
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new Error(`${label} 必须是有效的 HTTP 地址`);
  }
  if (url.protocol !== "http:" || !isLocalHost(url.hostname)) {
    throw new Error(`${label} 必须指向本机 HTTP 地址，拒绝连接非本地环境`);
  }
  return url;
}

function requireLocalDevDatabase(config) {
  if (!isLocalHost(config.host)) {
    throw new Error(`拒绝向非本地数据库执行演示种子脚本：${config.host}`);
  }
  if (!config.database.endsWith("_dev")) {
    throw new Error(`拒绝向非 _dev 数据库执行演示种子脚本：${config.database}`);
  }
}

function wsdlUrl(apiUrl) {
  return `${apiUrl.origin}${apiUrl.pathname.replace(/\/$/, "")}/services/campus-growth?wsdl`;
}

async function verifyMockService(url) {
  let response;
  try {
    response = await fetch(url, { signal: AbortSignal.timeout(5000) });
  } catch (error) {
    throw new Error(`无法访问本地 SOAP WSDL（${url}）：${error.message}`);
  }
  const body = await response.text();
  if (!response.ok || !body.includes("CampusGrowthDataService")) {
    throw new Error(`本地 SOAP WSDL 校验失败：HTTP ${response.status}`);
  }
}

function seedDemoData(config) {
  requireLocalDevDatabase(config);
  const result = spawnSync(
    "mysql",
    [
      "--protocol=TCP",
      `--host=${config.host}`,
      `--port=${config.port}`,
      `--user=${config.user}`,
      `--database=${config.database}`,
      "--default-character-set=utf8mb4",
    ],
    {
      cwd: projectPath(),
      encoding: "utf8",
      env: { ...process.env, MYSQL_PWD: config.password },
      input: readFileSync(projectPath("scripts", "dev-soap-seed.sql"), "utf8"),
      shell: false,
    },
  );
  if (result.error?.code === "ENOENT") {
    throw new Error(
      "未找到 mysql 客户端；安装 MySQL/MariaDB 客户端，或使用 --skip-seed 后手工执行 scripts/dev-soap-seed.sql",
    );
  }
  if (result.error || result.status !== 0) {
    throw new Error(
      `演示种子脚本执行失败：${(result.stderr || result.error?.message || "未知错误").trim()}`,
    );
  }
}

function parsePayload(payload) {
  if (!payload) throw new Error("SOAP 交换记录缺少解析结果");
  try {
    return JSON.parse(payload);
  } catch {
    throw new Error("SOAP 交换记录的解析结果不是有效 JSON");
  }
}

function assertSuccessfulExchange(exchange) {
  if (exchange?.exchangeStatus !== "SUCCESS") {
    throw new Error(
      `SOAP 任务未成功：${exchange?.errorMessage || exchange?.exchangeStatus || "未知状态"}`,
    );
  }
  if (!exchange?.requestXml || !exchange?.responseXml) {
    throw new Error("SOAP 交换记录缺少请求或响应报文");
  }
  if (
    parsePayload(exchange.parsedPayload).externalNo !== EXPECTED_EXTERNAL_NO
  ) {
    throw new Error("SOAP 导入外部编号不符合演示契约");
  }
}

async function obtainToken(client, args, env) {
  const token = args.token || env.BDIS_DEMO_TOKEN;
  if (token) return token;
  const username = args.username || env.BDIS_DEMO_USERNAME;
  const password = args.password || env.BDIS_DEMO_PASSWORD;
  if (!username || !password) {
    throw new Error(
      "请提供 BDIS_DEMO_TOKEN，或同时提供 BDIS_DEMO_USERNAME 与 BDIS_DEMO_PASSWORD",
    );
  }
  return client.login(username, password);
}

async function main() {
  const args = parseArgs();
  if (args.help) {
    process.stdout.write(`${usage}\n`);
    return;
  }

  const env = { ...loadDotEnv(), ...process.env };
  const apiUrl = requireLocalHttpUrl(
    args["base-url"] || env.BDIS_API_BASE_URL || "http://localhost:8080/api",
    "BDIS_API_BASE_URL",
  );
  if (envValue(env, "BDIS_SOAP_MODE", "").toLowerCase() !== "mock") {
    throw new Error(
      "必须显式设置 BDIS_SOAP_MODE=mock，演示脚本不会连接真实校内系统",
    );
  }
  const expectedEndpoint = `${apiUrl.origin}${apiUrl.pathname.replace(/\/$/, "")}/services/campus-growth`;
  const campusEndpoint = requireLocalHttpUrl(
    envValue(env, "BDIS_SOAP_CAMPUS_ENDPOINT", expectedEndpoint),
    "BDIS_SOAP_CAMPUS_ENDPOINT",
  );
  if (
    `${campusEndpoint.origin}${campusEndpoint.pathname}` !== expectedEndpoint
  ) {
    throw new Error(
      "BDIS_SOAP_CAMPUS_ENDPOINT 必须指向当前本地 BDIS 的 campus-growth mock 服务",
    );
  }

  const wsdl = wsdlUrl(apiUrl);
  step("校验本地 mock SOAP 服务");
  await verifyMockService(wsdl);
  process.stdout.write(`WSDL：${wsdl}\n`);

  if (!args["skip-seed"]) {
    step("写入本地演示前置数据（幂等）");
    seedDemoData({
      host: envValue(env, "MYSQL_HOST", "localhost"),
      port: envValue(env, "MYSQL_PORT", "3306"),
      database: envValue(env, "MYSQL_DATABASE", "biomed_dev"),
      user: envValue(env, "MYSQL_USER", "bdis"),
      password: envValue(env, "MYSQL_PASSWORD", "change-me"),
    });
    process.stdout.write("演示药材、基地和地图点位已就绪。\n");
  }

  const client = createClient(apiUrl.toString());
  step("获取具备 SOAP 权限的本地访问令牌");
  const token = await obtainToken(client, args, env);
  step("创建并执行 GROWTH_RECORD SOAP 同步任务");
  const exchange = await client.request("/soap-exchange-jobs", {
    method: "POST",
    token,
    body: {
      resourceType: "GROWTH_RECORD",
      mock: true,
      direction: "INBOUND",
      remark: "本地一键 SOAP 演示",
    },
  });
  assertSuccessfulExchange(exchange);
  const taskId = requirePositiveId(exchange.taskId, "taskId");
  const exchangeId = requirePositiveId(exchange.id, "exchangeId");
  step("读取交换留痕并确认导入结果");
  const detail = await client.request(`/soap-exchange-jobs/${taskId}`, {
    token,
  });
  assertSuccessfulExchange(detail);
  process.stdout.write(
    `\nSOAP 演示成功：taskId=${taskId}，exchangeId=${exchangeId}，externalNo=${EXPECTED_EXTERNAL_NO}。\n` +
      "可在地图点位详情的生长记录中查看来源为 SOAP 的导入数据。\n",
  );
}

main().catch((error) => {
  process.stderr.write(`\n[failed] SOAP 演示：${error.message}\n`);
  process.exitCode = 1;
});
