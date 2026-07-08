import { existsSync } from 'node:fs';
import { commandName, java21Env, projectPath, runStep } from './utils.mjs';

const pnpm = commandName('pnpm');
const mvn = commandName('mvn');

const requiredComposeEnv = ['MYSQL_ROOT_PASSWORD', 'MYSQL_PASSWORD', 'REDIS_PASSWORD'];
const hasRootEnv = existsSync(projectPath('.env'));
const missingComposeEnv = requiredComposeEnv.filter((name) => !process.env[name]);

console.log('BDIS validation');

if (!hasRootEnv && missingComposeEnv.length > 0) {
  console.error('[failed] .env is missing and required Docker Compose variables are not set.');
  console.error('Run pnpm setup, then edit .env before running pnpm validate.');
  process.exit(1);
}

runStep('frontend lint', pnpm, ['--dir', 'frontend', 'lint']);
runStep('frontend build', pnpm, ['--dir', 'frontend', 'build']);
runStep('backend tests', mvn, ['-f', 'backend/pom.xml', 'test'], { env: java21Env() });
runStep('docker compose config', 'docker', ['compose', 'config', '--quiet']);

console.log('\nValidation passed.');
