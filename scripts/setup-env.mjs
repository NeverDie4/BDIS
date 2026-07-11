import { copyFileSync, existsSync } from "node:fs";
import { displayPath, projectPath } from "./utils.mjs";

const templates = [
  {
    source: projectPath(".env.example"),
    target: projectPath(".env"),
    note: "Root environment file for Docker Compose and backend local runs.",
  },
  {
    source: projectPath("frontend", ".env.example"),
    target: projectPath("frontend", ".env.local"),
    note: "Frontend local environment file for Next.js.",
  },
];

console.log("BDIS environment setup");

for (const item of templates) {
  if (!existsSync(item.source)) {
    console.error(`[missing] ${displayPath(item.source)}`);
    process.exitCode = 1;
    continue;
  }

  if (existsSync(item.target)) {
    console.log(`[skip] ${displayPath(item.target)} already exists`);
    continue;
  }

  copyFileSync(item.source, item.target);
  console.log(
    `[done] created ${displayPath(item.target)} from ${displayPath(item.source)}`,
  );
  console.log(`       ${item.note}`);
}

if (process.exitCode) {
  process.exit();
}

console.log("\nNext steps:");
console.log(
  "1. Edit .env and frontend/.env.local for your local database, Redis, and API URL.",
);
console.log("2. Do not commit .env or frontend/.env.local.");
console.log("3. Run pnpm check, then pnpm validate.");
