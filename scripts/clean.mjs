import { projectPath, removeGeneratedPath } from './utils.mjs';

const targets = [
  projectPath('frontend', '.next'),
  projectPath('frontend', 'out'),
  projectPath('frontend', 'coverage'),
  projectPath('backend', 'target'),
];

console.log('BDIS generated file cleanup');

for (const target of targets) {
  removeGeneratedPath(target);
}

console.log('\nCleanup finished.');
