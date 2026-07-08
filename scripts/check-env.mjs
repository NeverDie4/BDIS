import { capture, commandName, detectJavaMajor, findJava21Home, java21Env } from './utils.mjs';

const checks = [
  { name: 'Node.js', command: 'node', args: ['--version'] },
  { name: 'pnpm', command: commandName('pnpm'), args: ['--version'] },
  { name: 'Maven', command: commandName('mvn'), args: ['-version'] },
  { name: 'Docker', command: 'docker', args: ['--version'] },
  { name: 'Docker Compose', command: 'docker', args: ['compose', 'version'] },
];

let failed = false;

console.log('BDIS development environment check');

const java = findJava21Home();
if (!java) {
  failed = true;
  const currentJava = capture('java', ['-version']);
  const currentLine = currentJava.ok ? currentJava.message.split('\n').find(Boolean) : currentJava.message;
  const detected = currentJava.ok ? detectJavaMajor(currentJava.message) : null;
  console.log(`[invalid] Java 21: ${currentLine || 'not found'}`);
  console.log(
    `          Project requires Java 21. Detected ${detected ? `Java ${detected}` : 'no usable Java'}.`,
  );
} else {
  const firstLine = java.output.split('\n').find(Boolean) || 'Java 21 available';
  const source = java.home ? ` using ${java.home}` : ' from PATH';
  console.log(`[ok] Java 21: ${firstLine}${source}`);
}

for (const item of checks) {
  const result = capture(item.command, item.args, { env: java21Env() });
  if (!result.ok) {
    failed = true;
    console.log(`[missing] ${item.name}: ${result.message.split('\n')[0]}`);
    continue;
  }

  const firstLine = result.message.split('\n').find(Boolean) || 'available';
  console.log(`[ok] ${item.name}: ${firstLine}`);
}

if (failed) {
  console.error('\nSome required tools are missing. Install them, then run pnpm check again.');
  process.exit(1);
}

console.log('\nEnvironment check passed.');
