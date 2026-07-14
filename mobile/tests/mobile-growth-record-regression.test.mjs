import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const mobileSource = new URL('../src/', import.meta.url)

async function readSource(relativePath) {
  return readFile(new URL(relativePath, mobileSource), 'utf8')
}

test('移动端生长记录 API 复用正式后端批次记录', async () => {
  const source = await readSource('api/mobileGrowthRecordApi.js')

  assert.match(source, /\/api\/herb\/batch\/\$\{batchId\}\/growth-record/)
  assert.match(source, /method:\s*'GET'/)
  assert.match(source, /method:\s*'POST'/)
  assert.match(source, /method:\s*'PUT'/)
  assert.match(source, /\/api\/growth-records\/\$\{recordId\}\/submit/)
  assert.doesNotMatch(source, /localStorage|setStorage|mock/i)
})

test('生长记录表单按 batchId 创建并按 recordId 编辑', async () => {
  const source = await readSource('pages/growth/form.vue')

  for (const field of [
    'growthStage',
    'collectedAt',
    'plantHeight',
    'leafColor',
    'stemDiameter',
    'floweringStatus',
    'growthEvaluation',
    'temperature',
    'humidity',
    'soilMoisture',
    'soilPh',
    'light',
    'remark'
  ]) {
    assert.match(source, new RegExp(field))
  }
  assert.match(source, /createBatchGrowthRecord/)
  assert.match(source, /updateBatchGrowthRecord/)
  assert.match(source, /batchId\.value/)
  assert.match(source, /recordId\.value/)
  assert.match(source, /创建成功/)
  assert.match(source, /修改成功/)
  assert.doesNotMatch(source, /#1677ff|#2563eb|#1d4ed8/i)
})

test('批次详情只保留统一提交审核入口', async () => {
  const source = await readSource('pages/batch/detail.vue')

  assert.match(source, /本次生长记录/)
  assert.match(source, /暂无本次采集的生长数据/)
  assert.match(source, /getBatchGrowthRecord/)
  assert.match(source, /pages\/growth\/form\?batchId=/)
  assert.match(source, /class="secondary-btn action-btn" @click="goUpload">上传图片/)
  assert.match(source, /class="primary-btn action-btn"[^>]*@click="handleSubmitBatch">提交审核/)
  assert.match(source, /submitBatch/)
  assert.doesNotMatch(source, /handleSubmitGrowthRecord|submitGrowthRecord/)
  assert.doesNotMatch(source, /识别未完成图片|identifyMissingImages/)
})

test('生长记录表单已注册且 Web 继续读取任务趋势接口', async () => {
  const [pagesJson, webApi] = await Promise.all([
    readSource('pages.json'),
    readFile(new URL('../../frontend/src/lib/growth-records.ts', import.meta.url), 'utf8')
  ])

  assert.match(pagesJson, /"path":\s*"pages\/growth\/form"/)
  assert.match(webApi, /herb\/collection-task\/\$\{taskId\}\/growth-records\/chart/)
})
