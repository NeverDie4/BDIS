import { existsSync, readFileSync } from "node:fs";
import { commandName, java21Env, projectPath, runStep } from "./utils.mjs";

const validGoals = new Set(["info", "migrate", "validate", "repair", "clean"]);
const usage = `Usage: node scripts/run-flyway.mjs <info|migrate|validate|repair|clean> [--local]`;
const [goal, ...rawArgs] = process.argv.slice(2);

if (!goal || !validGoals.has(goal)) {
  console.error(usage);
  process.exit(1);
}

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
  if (!existsSync(envPath)) {
    return {};
  }

  const values = {};
  const content = readFileSync(envPath, "utf8");
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }

    const normalized = trimmed.startsWith("export ")
      ? trimmed.slice("export ".length).trim()
      : trimmed;
    const equalsIndex = normalized.indexOf("=");
    if (equalsIndex <= 0) {
      continue;
    }

    const key = normalized.slice(0, equalsIndex).trim();
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(key)) {
      continue;
    }

    values[key] = parseDotEnvValue(normalized.slice(equalsIndex + 1));
  }

  return values;
}

function envValue(env, name, fallback) {
  const value = env[name];
  return value === undefined || value === "" ? fallback : value;
}

function jdbcHost(host) {
  return host.includes(":") && !host.startsWith("[") ? `[${host}]` : host;
}

function buildJdbcUrl({ host, port, database }) {
  const params = new URLSearchParams({
    useUnicode: "true",
    characterEncoding: "utf8",
    serverTimezone: "Asia/Shanghai",
    useSSL: "false",
    allowPublicKeyRetrieval: "true",
  });

  return `jdbc:mysql://${jdbcHost(host)}:${port}/${database}?${params.toString()}`;
}

function ensureLocalClean({ host, database }, args) {
  const requestedLocal = args.includes("--local");
  const localHosts = new Set(["localhost", "127.0.0.1", "::1", "[::1]"]);

  if (!requestedLocal) {
    console.error(
      "[failed] flyway clean must be called through pnpm db:clean:local.",
    );
    process.exit(1);
  }

  if (!localHosts.has(host)) {
    console.error(
      `[failed] refusing to clean non-local database host: ${host}`,
    );
    process.exit(1);
  }

  if (!database.endsWith("_dev")) {
    console.error(
      `[failed] refusing to clean database that does not end with _dev: ${database}`,
    );
    process.exit(1);
  }
}

const dotenv = loadDotEnv();
const mergedEnv = { ...dotenv, ...process.env };

const config = {
  host: envValue(mergedEnv, "MYSQL_HOST", "localhost"),
  port: envValue(mergedEnv, "MYSQL_PORT", "3306"),
  database: envValue(mergedEnv, "MYSQL_DATABASE", "biomed_dev"),
  user: envValue(mergedEnv, "MYSQL_USER", "bdis"),
  password: envValue(mergedEnv, "MYSQL_PASSWORD", "change-me"),
};

if (goal === "clean") {
  ensureLocalClean(config, rawArgs);
}

const mavenArgs = ["-f", "flyway/pom.xml"];
if (goal === "clean") {
  mavenArgs.push("-Dflyway.cleanDisabled=false");
}
mavenArgs.push(`flyway:${goal}`);

const flywayEnv = {
  ...mergedEnv,
  ...java21Env(),
  FLYWAY_URL: buildJdbcUrl(config),
  FLYWAY_USER: config.user,
  FLYWAY_PASSWORD: config.password,
};

console.log(
  `Flyway target: ${config.user}@${config.host}:${config.port}/${config.database}`,
);
if (goal === "repair") {
  console.log(
    "Warning: flyway repair changes Flyway metadata. Use it only after the cause is confirmed.",
  );
}
if (goal === "clean") {
  console.log("Warning: cleaning local development database objects.");
}

runStep(`flyway ${goal}`, commandName("mvn"), mavenArgs, { env: flywayEnv });
