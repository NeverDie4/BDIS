import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const projectRoot = new URL('../../', import.meta.url)

async function readProjectFile(relativePath) {
  return readFile(new URL(relativePath, projectRoot), 'utf8')
}

test('移动端生产构建使用可配置的同源 API 地址', async () => {
  const source = await readProjectFile('mobile/src/config/index.js')

  assert.match(source, /import\.meta\.env\.VITE_API_BASE_URL/)
  assert.doesNotMatch(source, /https?:\/\/localhost(?::\d+)?/)
})

test('移动端镜像构建 H5 并通过 nginx 代理后端 API', async () => {
  const [dockerfile, nginxConfig] = await Promise.all([
    readProjectFile('mobile/Dockerfile'),
    readProjectFile('deploy/nginx/mobile.conf')
  ])

  assert.match(dockerfile, /pnpm --filter @bdis\/mobile build:h5/)
  assert.match(dockerfile, /mobile\/dist\/build\/h5 \/usr\/share\/nginx\/html/)
  assert.match(nginxConfig, /location \/api\//)
  assert.match(nginxConfig, /proxy_pass http:\/\/backend:8080\/api\//)
  assert.match(nginxConfig, /try_files \$uri \$uri\/ \/index\.html/)
})

test('Compose 注册移动端服务、端口和健康检查', async () => {
  const source = await readProjectFile('docker-compose.yml')

  assert.match(source, /\n  mobile:\n/)
  assert.match(source, /dockerfile: mobile\/Dockerfile/)
  assert.match(source, /\$\{MOBILE_PORT:-3001\}:80/)
  assert.match(source, /http:\/\/127\.0\.0\.1\//)
})
