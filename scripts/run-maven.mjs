import { commandName, java21Env, runStep } from './utils.mjs';

const args = process.argv.slice(2);

if (args.length === 0) {
  console.error('Usage: node scripts/run-maven.mjs <maven-args>');
  process.exit(1);
}

runStep('maven', commandName('mvn'), args, { env: java21Env() });
