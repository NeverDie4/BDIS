import { spawnSync } from "node:child_process";
import {
  copyFileSync,
  existsSync,
  mkdirSync,
  readFileSync,
  statSync,
} from "node:fs";
import { dirname } from "node:path";
import { displayPath, projectPath } from "./utils.mjs";

const LOCAL_HOSTS = new Set(["localhost", "127.0.0.1", "::1", "[::1]"]);
const args = new Set(process.argv.slice(2));
const requireAssets = args.has("--require-assets");
const assetsOnly = args.has("--assets-only");

const usage = `用法：pnpm demo:defense:seed [--require-assets] [--assets-only]

--require-assets  缺少任一答辩素材时失败，适合最终验收。
--assets-only     不重写核心数据，仅复制并绑定后续补齐的六个素材。`;

if (args.has("--help")) {
  process.stdout.write(`${usage}\n`);
  process.exit(0);
}

const coreCopies = [
  [
    "backend/import/herb_atlas/huanglian_coptis_chinensis/whole_growth_fresh/huanglian_whole_growth_fresh_01.png",
    "backend/storage/demo-agent/whole-stage-01.png",
    "demo-agent/whole-stage-01.png",
  ],
  [
    "backend/import/herb_atlas/huanglian_coptis_chinensis/leaf_growth_fresh/huanglian_leaf_growth_fresh_01.png",
    "backend/storage/demo-agent/leaf-stage-02.png",
    "demo-agent/leaf-stage-02.png",
  ],
  [
    "backend/import/herb_atlas/huanglian_coptis_chinensis/whole_growth_fresh/huanglian_whole_growth_fresh_02.jpeg",
    "backend/storage/demo-agent/whole-stage-03.jpeg",
    "demo-agent/whole-stage-03.jpeg",
  ],
  [
    "backend/import/herb_atlas/huanglian_coptis_chinensis/whole_growth_fresh/huanglian_whole_growth_fresh_03.jpeg",
    "backend/storage/demo-agent/followup-whole.jpeg",
    "demo-agent/followup-whole.jpeg",
  ],
  [
    "backend/import/herb_atlas/huanglian_coptis_chinensis/leaf_growth_fresh/huanglian_leaf_growth_fresh_02.jpeg",
    "backend/storage/demo-agent/followup-leaf.jpeg",
    "demo-agent/followup-leaf.jpeg",
  ],
  [
    "backend/import/herb_atlas/huanglian_coptis_chinensis/rhizome_mature_dried/huanglian_rhizome_dried_01.png",
    "backend/storage/demo-agent/followup-root.png",
    "demo-agent/followup-root.png",
  ],
  [
    "backend/import/herb_atlas/huanglian_coptis_chinensis/whole_growth_fresh/huanglian_whole_growth_fresh_01.png",
    "backend/storage/defense-demo/map-point-01.png",
    "defense-demo/map-point-01.png",
  ],
  [
    "backend/import/herb_atlas/huanglian_coptis_chinensis/whole_growth_fresh/huanglian_whole_growth_fresh_02.jpeg",
    "backend/storage/defense-demo/map-point-02.jpeg",
    "defense-demo/map-point-02.jpeg",
  ],
  [
    "backend/import/herb_atlas/huanglian_coptis_chinensis/whole_growth_fresh/huanglian_whole_growth_fresh_03.jpeg",
    "backend/storage/defense-demo/map-point-03.jpeg",
    "defense-demo/map-point-03.jpeg",
  ],
  [
    "backend/import/herb_atlas/huanglian_coptis_chinensis/leaf_growth_fresh/huanglian_leaf_growth_fresh_03.jpeg",
    "backend/storage/defense-demo/map-point-04.jpeg",
    "defense-demo/map-point-04.jpeg",
  ],
];

const requiredAssets = [
  {
    file: "huanglian-observation-form.pdf",
    fileNo: "DEMO_HL_ASSET_OBSERVATION_FORM",
    purpose: "黄连生长观测记录表",
  },
  {
    file: "huanglian-identification-demo.mp4",
    fileNo: "DEMO_HL_ASSET_IDENTIFICATION_VIDEO",
    purpose: "Chrome 可直接播放的黄连形态识别演示视频",
  },
  {
    file: "huanglian-experiment-report.pdf",
    fileNo: "DEMO_HL_ASSET_EXPERIMENT_REPORT",
    purpose: "黄连三阶段观察实验报告",
  },
  {
    file: "huanglian-stage-comparison.png",
    fileNo: "DEMO_HL_ASSET_STAGE_COMPARISON",
    purpose: "黄连三阶段影像对比图",
  },
  {
    file: "huanglian-research-summary.pdf",
    fileNo: "DEMO_HL_ASSET_RESEARCH_SUMMARY",
    purpose: "黄连数字化研究过程摘要",
  },
  {
    file: "huanglian-digital-archive-summary.pdf",
    fileNo: "DEMO_HL_ASSET_ARCHIVE_SUMMARY",
    purpose: "黄连全生命周期数字档案摘要",
  },
];

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
  const path = projectPath(".env");
  if (!existsSync(path)) return {};
  const values = {};
  for (const line of readFileSync(path, "utf8").split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const normalized = trimmed.startsWith("export ")
      ? trimmed.slice("export ".length).trim()
      : trimmed;
    const equalsIndex = normalized.indexOf("=");
    if (equalsIndex <= 0) continue;
    const key = normalized.slice(0, equalsIndex).trim();
    if (!/^[A-Za-z_][A-Za-z0-9_]*$/u.test(key)) continue;
    values[key] = parseDotEnvValue(normalized.slice(equalsIndex + 1));
  }
  return values;
}

function envValue(env, name, fallback) {
  return env[name] === undefined || env[name] === "" ? fallback : env[name];
}

function run(command, commandArgs, options = {}) {
  const result = spawnSync(command, commandArgs, {
    cwd: projectPath(),
    encoding: "utf8",
    shell: false,
    ...options,
  });
  if (result.error || result.status !== 0) {
    const detail = (
      result.stderr ||
      result.stdout ||
      result.error?.message ||
      ""
    ).trim();
    throw new Error(`${command} 执行失败${detail ? `：${detail}` : ""}`);
  }
  return (result.stdout || "").trim();
}

function mysql(config, sql, label) {
  process.stdout.write(`> ${label}\n`);
  const output = run(
    "mysql",
    [
      "--protocol=TCP",
      `--host=${config.host}`,
      `--port=${config.port}`,
      `--user=${config.user}`,
      `--database=${config.database}`,
      "--default-character-set=utf8mb4",
      "--batch",
      "--skip-column-names",
    ],
    {
      env: { ...process.env, MYSQL_PWD: config.password },
      input: sql,
    },
  );
  if (output) process.stdout.write(`${output}\n`);
}

function requireLocalDevDatabase(config) {
  if (!LOCAL_HOSTS.has(config.host.toLowerCase())) {
    throw new Error(`拒绝向非本地数据库写入演示数据：${config.host}`);
  }
  if (!config.database.endsWith("_dev")) {
    throw new Error(`拒绝向非 _dev 数据库写入演示数据：${config.database}`);
  }
}

function copyLocal(source, destination) {
  if (!existsSync(source)) {
    throw new Error(`演示源图片不存在：${displayPath(source)}`);
  }
  mkdirSync(dirname(destination), { recursive: true });
  copyFileSync(source, destination);
  process.stdout.write(`[copy] ${displayPath(destination)}\n`);
}

function runningBackendContainer() {
  const result = spawnSync(
    "docker",
    ["compose", "ps", "--status", "running", "-q", "backend"],
    { cwd: projectPath(), encoding: "utf8", shell: false },
  );
  return result.status === 0 && Boolean(result.stdout.trim());
}

function copyToDockerStorage(source, storageRelativePath) {
  const containerTarget = `/app/storage/${storageRelativePath}`;
  run("docker", [
    "compose",
    "exec",
    "-T",
    "backend",
    "mkdir",
    "-p",
    dirname(containerTarget),
  ]);
  run("docker", ["compose", "cp", source, `backend:${containerTarget}`]);
}

function copyFiles(files, useDocker) {
  for (const [sourceRelative, destinationRelative, storageRelative] of files) {
    const source = projectPath(sourceRelative);
    const destination = projectPath(destinationRelative);
    copyLocal(source, destination);
    if (useDocker) copyToDockerStorage(source, storageRelative);
  }
}

function assetCopies() {
  return requiredAssets.map(({ file }) => [
    `backend/import/defense_demo/${file}`,
    `backend/storage/defense-demo/${file}`,
    `defense-demo/${file}`,
  ]);
}

function missingAssets() {
  return requiredAssets.filter(
    ({ file }) =>
      !existsSync(projectPath("backend", "import", "defense_demo", file)),
  );
}

function assetSizeSql() {
  const statements = requiredAssets.map(({ file, fileNo }) => {
    const size = statSync(
      projectPath("backend", "import", "defense_demo", file),
    ).size;
    return `UPDATE sys_file_resource SET file_size=${size}, updated_at=NOW() WHERE file_no='${fileNo}' AND is_deleted=0;`;
  });
  statements.push(
    "UPDATE edu_course_resource r JOIN sys_file_resource f ON f.id=r.file_id SET r.file_size=f.file_size, r.updated_at=NOW() WHERE r.remark='BDIS_DEFENSE_DEMO:S03' AND r.is_deleted=0;",
    "UPDATE eval_attachment a JOIN sys_file_resource f ON f.id=a.file_id SET a.file_size=f.file_size, a.updated_at=NOW() WHERE a.remark IN ('BDIS_DEFENSE_DEMO:S15','BDIS_DEFENSE_DEMO:S16') AND a.is_deleted=0;",
  );
  return statements.join("\n");
}

function readSql(name) {
  return readFileSync(projectPath("scripts", name), "utf8");
}

function verifyCore(config) {
  const sql = `SELECT IF(
    (SELECT COUNT(*) FROM sys_user WHERE username IN ('agent_teacher_demo','collector_agent_demo','agent_reviewer_demo','student_hl_demo','researcher_hl_demo','trainer_hl_demo') AND is_deleted=0)=6
    AND (SELECT COUNT(*) FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:M%' AND is_deleted=0)=4
    AND (SELECT COUNT(*) FROM herb_growth_review_record WHERE remark LIKE 'BDIS_DEFENSE_DEMO:P%')=6
    AND (SELECT COUNT(*) FROM edu_experiment_step WHERE course_id=(SELECT id FROM edu_course WHERE course_no='DEMO-HL-COURSE-01'))=3
    AND (SELECT COUNT(*) FROM rel_project_member WHERE project_id=(SELECT id FROM research_project WHERE project_no='DEMO-HL-RESEARCH-01'))=3
    AND (SELECT COUNT(*) FROM edu_training_plan_item WHERE plan_id=(SELECT id FROM edu_training_plan WHERE plan_no='DEMO-HL-TRAIN-01') AND is_deleted=0)=4
    AND (SELECT COUNT(*) FROM edu_training_record WHERE attendance_no LIKE 'DEMO-HL-ATT-%')=3
    AND (SELECT COUNT(*) FROM edu_training_feedback WHERE remark='BDIS_DEFENSE_DEMO:S11')=2
    AND (SELECT COUNT(*) FROM eval_score_record WHERE task_id=(SELECT id FROM eval_task WHERE task_no='DEMO-HL-EVAL-01'))=4
    AND EXISTS (SELECT 1 FROM eval_result r JOIN eval_task t ON t.id=r.task_id WHERE t.task_no='DEMO-HL-EVAL-01' AND r.total_score=91.10 AND r.result_level='excellent')
    AND (SELECT COUNT(*) FROM eval_application WHERE application_no LIKE 'DEMO-HL-DECL-%')=2
    AND (SELECT COUNT(*) FROM perf_record WHERE performance_no LIKE 'DEMO-HL-PERF-%')=2,
    1, 0
  );`;
  const output = run(
    "mysql",
    [
      "--protocol=TCP",
      `--host=${config.host}`,
      `--port=${config.port}`,
      `--user=${config.user}`,
      `--database=${config.database}`,
      "--default-character-set=utf8mb4",
      "--batch",
      "--skip-column-names",
    ],
    { env: { ...process.env, MYSQL_PWD: config.password }, input: sql },
  );
  if (output.trim() !== "1") throw new Error("核心演示数据验收未通过");
  process.stdout.write("[ok] 核心演示数据数量与关键状态校验通过\n");
}

function verifyAssets(config) {
  const sql = `SELECT IF(
    (SELECT COUNT(*) FROM sys_file_resource WHERE file_no LIKE 'DEMO_HL_ASSET_%' AND is_deleted=0)=6
    AND NOT EXISTS (SELECT 1 FROM sys_file_resource WHERE file_no LIKE 'DEMO_HL_ASSET_%' AND is_deleted=0 AND (file_size IS NULL OR file_size<=0))
    AND (SELECT COUNT(*) FROM edu_course_resource WHERE course_id=(SELECT id FROM edu_course WHERE course_no='DEMO-HL-COURSE-01') AND is_deleted=0 AND file_size>0)=2
    AND EXISTS (SELECT 1 FROM edu_experiment_record WHERE record_no='DEMO-HL-EXP-01' AND report_file_id IS NOT NULL)
    AND (SELECT COUNT(*) FROM sys_file_business WHERE biz_type='research_project' AND biz_id=(SELECT id FROM research_project WHERE project_no='DEMO-HL-RESEARCH-01') AND remark='BDIS_DEFENSE_DEMO:S07')=2
    AND (SELECT COUNT(*) FROM eval_attachment WHERE application_id=(SELECT id FROM eval_application WHERE application_no='DEMO-HL-DECL-PENDING') AND is_deleted=0 AND file_size>0)=3
    AND (SELECT COUNT(*) FROM eval_attachment WHERE application_id=(SELECT id FROM eval_application WHERE application_no='DEMO-HL-DECL-APPROVED') AND is_deleted=0 AND file_size>0)=3
    AND (SELECT COUNT(*) FROM sys_file_business WHERE biz_type='perf_record' AND biz_id=(SELECT id FROM perf_record WHERE performance_no='DEMO-HL-PERF-PENDING') AND remark='BDIS_DEFENSE_DEMO:S18')=2
    AND (SELECT COUNT(*) FROM sys_file_business WHERE biz_type='perf_record' AND biz_id=(SELECT id FROM perf_record WHERE performance_no='DEMO-HL-PERF-APPROVED') AND remark='BDIS_DEFENSE_DEMO:S19')=2,
    1, 0
  );`;
  const output = run(
    "mysql",
    [
      "--protocol=TCP",
      `--host=${config.host}`,
      `--port=${config.port}`,
      `--user=${config.user}`,
      `--database=${config.database}`,
      "--default-character-set=utf8mb4",
      "--batch",
      "--skip-column-names",
    ],
    { env: { ...process.env, MYSQL_PWD: config.password }, input: sql },
  );
  if (output.trim() !== "1") throw new Error("答辩素材绑定验收未通过");
  process.stdout.write("[ok] 六个答辩素材与业务绑定校验通过\n");
}

function main() {
  const env = { ...loadDotEnv(), ...process.env };
  const config = {
    host: envValue(env, "MYSQL_HOST", "localhost"),
    port: envValue(env, "MYSQL_PORT", "3306"),
    database: envValue(env, "MYSQL_DATABASE", "biomed_dev"),
    user: envValue(env, "MYSQL_USER", "bdis"),
    password: envValue(env, "MYSQL_PASSWORD", "change-me"),
  };
  requireLocalDevDatabase(config);
  process.stdout.write(
    `BDIS 答辩演示数据目标：${config.user}@${config.host}:${config.port}/${config.database}\n`,
  );

  const useDockerStorage = runningBackendContainer();
  if (!assetsOnly) {
    process.stdout.write("\n> 复制仓库现有黄连图片\n");
    copyFiles(coreCopies, useDockerStorage);
    mysql(
      config,
      readSql("dev-research-agent-demo.sql"),
      "写入科研 Agent 基础演示数据（幂等）",
    );
    mysql(
      config,
      readSql("dev-defense-demo-data.sql"),
      "写入答辩核心演示数据（幂等）",
    );
    verifyCore(config);
  } else {
    verifyCore(config);
  }

  const missing = missingAssets();
  if (missing.length > 0) {
    process.stdout.write(
      "\n[assets pending] 以下文件尚未放入 backend/import/defense_demo/，本次不写入失效附件记录：\n",
    );
    for (const item of missing) {
      process.stdout.write(`- ${item.file}：${item.purpose}\n`);
    }
    process.stdout.write(
      "\n文件补齐后运行 pnpm demo:defense:seed -- --assets-only 即可完成绑定。\n",
    );
    if (requireAssets) process.exitCode = 2;
    return;
  }

  process.stdout.write("\n> 复制答辩附件素材\n");
  copyFiles(assetCopies(), useDockerStorage);
  mysql(
    config,
    readSql("dev-defense-demo-assets.sql"),
    "写入课程、课题、申报和业绩附件绑定（幂等）",
  );
  mysql(config, assetSizeSql(), "同步答辩素材真实文件大小");
  verifyAssets(config);
  process.stdout.write("\n[ok] BDIS 答辩演示数据和附件已就绪。\n");
}

try {
  main();
} catch (error) {
  const detail = error instanceof Error ? error.message : String(error);
  process.stderr.write(`\n[failed] 答辩演示数据准备失败：${detail}\n`);
  process.exitCode = 1;
}
