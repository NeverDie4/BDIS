import { spawnSync } from 'node:child_process';
import { existsSync, rmSync } from 'node:fs';
import { delimiter, dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export const rootDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');

export function commandName(name) {
  return process.platform === 'win32' ? `${name}.cmd` : name;
}

function quoteWindowsCommandArg(value) {
  const text = String(value);
  if (!/[\s&()<>^|"]/u.test(text)) {
    return text;
  }

  return `"${text.replaceAll('"', '\\"')}"`;
}

function spawnCommand(command, args, options) {
  if (process.platform !== 'win32' || !/\.(?:cmd|bat)$/i.test(command)) {
    return spawnSync(command, args, options);
  }

  const commandLine = [command, ...args.map(quoteWindowsCommandArg)].join(' ');
  return spawnSync('cmd.exe', ['/d', '/s', '/c', commandLine], options);
}

export function projectPath(...parts) {
  return resolve(rootDir, ...parts);
}

export function displayPath(path) {
  return relative(rootDir, path).replaceAll('\\', '/');
}

export function runStep(label, command, args, options = {}) {
  console.log(`\n> ${label}`);
  console.log(`  ${[command, ...args].join(' ')}`);

  const result = spawnCommand(command, args, {
    cwd: rootDir,
    env: process.env,
    stdio: 'inherit',
    shell: false,
    ...options,
  });

  if (result.error) {
    console.error(`\n[failed] ${label}`);
    console.error(`Reason: ${result.error.message}`);
    process.exit(1);
  }

  if (result.status !== 0) {
    console.error(`\n[failed] ${label}`);
    process.exit(result.status ?? 1);
  }

  console.log(`[ok] ${label}`);
}

export function capture(command, args, options = {}) {
  const result = spawnCommand(command, args, {
    cwd: rootDir,
    env: process.env,
    encoding: 'utf8',
    shell: false,
    ...options,
  });

  if (result.error || result.status !== 0) {
    return {
      ok: false,
      message: result.error?.message || result.stderr || result.stdout || 'command failed',
    };
  }

  return {
    ok: true,
    message: `${result.stdout || result.stderr}`.trim(),
  };
}

export function removeGeneratedPath(path) {
  if (!existsSync(path)) {
    console.log(`[skip] ${displayPath(path)} not found`);
    return;
  }

  rmSync(path, { recursive: true, force: true });
  console.log(`[done] removed ${displayPath(path)}`);
}

export function detectJavaMajor(output) {
  const legacy = output.match(/version "1\.(\d+)\./);
  if (legacy) {
    return Number(legacy[1]);
  }

  const modern = output.match(/version "(\d+)(?:[._]\d+)?/);
  if (modern) {
    return Number(modern[1]);
  }

  return null;
}

export function findJava21Home() {
  const javaBinary = process.platform === 'win32' ? 'java.exe' : 'java';
  const candidates = [];

  if (process.env.JAVA_HOME) {
    candidates.push(process.env.JAVA_HOME);
  }

  if (process.platform !== 'win32') {
    candidates.push('/usr/lib/jvm/java-21-openjdk');
    candidates.push('/usr/lib/jvm/java-21');
    candidates.push('/usr/lib/jvm/jdk-21');
  }

  const seen = new Set();
  for (const candidate of candidates) {
    if (!candidate || seen.has(candidate)) {
      continue;
    }
    seen.add(candidate);

    const javaPath = join(candidate, 'bin', javaBinary);
    if (!existsSync(javaPath)) {
      continue;
    }

    const result = spawnSync(javaPath, ['-version'], { encoding: 'utf8' });
    const output = `${result.stdout || ''}${result.stderr || ''}`;
    if (result.status === 0 && detectJavaMajor(output) === 21) {
      return { home: candidate, output };
    }
  }

  const current = spawnSync('java', ['-version'], { encoding: 'utf8' });
  const currentOutput = `${current.stdout || ''}${current.stderr || ''}`;
  if (current.status === 0 && detectJavaMajor(currentOutput) === 21) {
    return { home: null, output: currentOutput };
  }

  return null;
}

export function java21Env() {
  const java = findJava21Home();
  if (!java?.home) {
    return process.env;
  }

  return {
    ...process.env,
    JAVA_HOME: java.home,
    PATH: `${join(java.home, 'bin')}${delimiter}${process.env.PATH || ''}`,
  };
}
