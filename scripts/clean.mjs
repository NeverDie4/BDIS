import { projectPath, removeGeneratedPath } from "./utils.mjs";

const targets = [
  projectPath("frontend", ".next"),
  projectPath("frontend", "out"),
  projectPath("frontend", "coverage"),
  projectPath("mobile", "dist"),
  projectPath("mobile", "unpackage"),
  projectPath("backend", "target"),
  projectPath("flyway", "target"),
  projectPath("ai_service", "__pycache__"),
  projectPath("ai_service", ".pytest_cache"),
];

console.log("BDIS generated file cleanup");

for (const target of targets) {
  removeGeneratedPath(target);
}

console.log("\nCleanup finished.");
