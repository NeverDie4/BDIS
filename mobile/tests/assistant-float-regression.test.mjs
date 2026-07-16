import assert from 'node:assert/strict'
import { access, readFile } from 'node:fs/promises'
import test from 'node:test'

const mobileSource = new URL('../src/', import.meta.url)

async function readSource(relativePath) {
  return readFile(new URL(relativePath, mobileSource), 'utf8')
}

test('AI 小助手 API 只调用 Spring Boot 后端接口', async () => {
  const source = await readSource('api/assistantApi.js')

  assert.match(source, /const prefix = '\/api\/herb\/assistant'/)
  assert.match(source, /`\$\{prefix\}\/chat`/)
  assert.match(source, /`\$\{prefix\}\/sessions`/)
  assert.match(source, /`\$\{prefix\}\/sessions\/\$\{sessionId\}\/messages`/)
  assert.match(source, /`\$\{prefix\}\/batch\/\$\{batchId\}\/explain`/)
  assert.match(source, /`\$\{prefix\}\/image\/\$\{imageId\}\/explain`/)
  assert.doesNotMatch(source, /api[_-]?key|volces|doubao|openai/i)
})

test('悬浮球支持登录判断、拖动缓存、会话和三类请求', async () => {
  const source = await readSource('components/AssistantFloat.vue')

  for (const token of [
    'isLoggedIn',
    'assistantFloatLeft',
    'assistantFloatTop',
    'assistantSessionId',
    '/static/images/assistant-float.png',
    'iconLoadFailed',
    'handleIconError',
    '@touchstart',
    '@touchmove',
    '@touchend',
    'chatWithAssistant',
    'requestBatchExplanation',
    'requestImageExplanation',
    'defineExpose',
    '这个系统怎么上传图片？',
    '拍照采集有什么规范？'
  ]) {
    assert.match(source, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }

  assert.doesNotMatch(source, /assistant-float-label/)
  assert.doesNotMatch(source, />K</)
  assert.match(source, /<image\s+class="assistant-avatar"\s+src="\/static\/images\/assistant-float\.png"/)
  assert.match(source, /生物医药数字信息系统AI助手/)
  assert.doesNotMatch(source, /<view class="assistant-avatar">AI<\/view>/)
})

test('悬浮球在不同页面使用同一 session 并恢复后端历史消息', async () => {
  const source = await readSource('components/AssistantFloat.vue')

  assert.match(source, /getAssistantMessages/)
  assert.match(source, /getAssistantSessions/)
  assert.match(source, /loadSessionMessages/)
  assert.match(source, /await loadSessionMessages\(\)/)
  assert.match(source, /messages\.value = history/)
})

test('手机端悬浮球松手后左右吸边并跨页面保存边侧与高度', async () => {
  const source = await readSource('components/AssistantFloat.vue')

  assert.match(source, /const FLOAT_POSITION_KEY = 'assistantFloatPosition'/)
  assert.match(source, /function snapToSide\(left, top\)/)
  assert.match(source, /left \+ FLOAT_SIZE \/ 2 < screenWidth\.value \/ 2/)
  assert.match(source, /setStorage\(FLOAT_POSITION_KEY, \{ side, top \}\)/)
  assert.match(source, /:class="\{ 'assistant-float-dragging': dragging \}"/)
  assert.match(
    source,
    /\.assistant-float\s*\{[^}]*transition:[^}]*left 0\.22s ease,[^}]*top 0\.18s ease,[^}]*transform 0\.18s ease;/s
  )
  assert.match(
    source,
    /\.assistant-float-dragging\s*\{[^}]*transition:\s*none;/s
  )
})

test('登录后主要页面均接入悬浮球且登录页不接入', async () => {
  const pages = [
    'pages/index/index.vue',
    'pages/task/list.vue',
    'pages/task/detail.vue',
    'pages/batch/list.vue',
    'pages/batch/create.vue',
    'pages/batch/detail.vue',
    'pages/image/upload.vue',
    'pages/image/result.vue',
    'pages/mine/index.vue'
  ]

  for (const page of pages) {
    assert.match(await readSource(page), /<AssistantFloat\b/)
  }
  assert.doesNotMatch(await readSource('pages/login/index.vue'), /<AssistantFloat\b/)
})

test('批次详情和识别结果页提供 AI 解释入口', async () => {
  const [batchDetail, imageResult] = await Promise.all([
    readSource('pages/batch/detail.vue'),
    readSource('pages/image/result.vue')
  ])

  assert.match(batchDetail, /AI 解释批次/)
  assert.match(batchDetail, /explainBatch/)
  assert.match(imageResult, /AI 解释识别结果/)
  assert.match(imageResult, /explainImage/)
})

test('手机端统一使用本草主题且悬浮球图片资源存在', async () => {
  const themedFiles = [
    'App.vue',
    'pages.json',
    'components/AssistantFloat.vue',
    'pages/index/index.vue',
    'pages/task/list.vue',
    'pages/task/detail.vue',
    'pages/batch/list.vue',
    'pages/batch/create.vue',
    'pages/batch/detail.vue',
    'pages/image/upload.vue',
    'pages/image/result.vue',
    'pages/mine/index.vue'
  ]
  const sources = await Promise.all(themedFiles.map(readSource))
  const combined = sources.join('\n')

  assert.match(combined, /#0f5132/i)
  assert.match(combined, /#f7f1e6/i)
  assert.match(combined, /#fffaf2/i)
  assert.doesNotMatch(combined, /#1677ff|#2563eb|#1d4ed8|#eef5ff|#f5f7fb/i)
  assert.match(await readSource('pages/index/index.vue'), /本草研究院标本馆移动采集端/)
  await access(new URL('static/images/assistant-float.png', mobileSource))
})

test('关键页面使用高优先级绿色按钮和稳定的移动端排版', async () => {
  const [app, home, taskDetail, mine, assistant] = await Promise.all([
    readSource('App.vue'),
    readSource('pages/index/index.vue'),
    readSource('pages/task/detail.vue'),
    readSource('pages/mine/index.vue'),
    readSource('components/AssistantFloat.vue')
  ])

  assert.match(app, /button\.primary-btn/)
  assert.match(app, /linear-gradient\(135deg, #166534 0%, #0f5132 100%\).*important/s)
  assert.match(app, /\.title,[\s\S]*\.sub-title[\s\S]*display:\s*block/)
  assert.match(home, /本草研究院标本馆移动采集端/)
  assert.match(home, /查看采集任务，创建批次并上传现场采集图片。/)
  assert.match(taskDetail, /class="info-block"/)
  assert.match(taskDetail, /class="info-block-value code-text"/)
  assert.doesNotMatch(taskDetail, /class="label block-label">任务说明/)
  assert.match(taskDetail, /text-align:\s*left/)
  for (const token of [
    'user-profile-card',
    'quick-action-card',
    'action-grid',
    'info-card',
    'system-card',
    'logout-card',
    '我的任务',
    '采集批次'
  ]) {
    assert.match(mine, new RegExp(token))
  }
  assert.match(mine, /<AssistantFloat\s*\/>/)
  assert.doesNotMatch(mine, /consult|openAssistant|showHelp|使用帮助|咨询采集流程与规范|查看移动采集流程/)
  assert.doesNotMatch(home, /openAssistant|咨询流程与规范/)
  assert.match(assistant, /const FLOAT_SIZE = 52/)
  assert.match(assistant, /const TAB_BAR_BOTTOM_GAP = 80/)
  assert.match(assistant, /const DEFAULT_BOTTOM_GAP = 85/)
  assert.match(assistant, /isSafeCachedPosition/)
})

test('底部使用本草悬浮胶囊导航并与悬浮球保持安全距离', async () => {
  const [pagesJson, tabBar, app, home, taskList, mine, assistant] = await Promise.all([
    readSource('pages.json'),
    readSource('components/AppTabBar.vue'),
    readSource('App.vue'),
    readSource('pages/index/index.vue'),
    readSource('pages/task/list.vue'),
    readSource('pages/mine/index.vue'),
    readSource('components/AssistantFloat.vue')
  ])

  assert.doesNotMatch(pagesJson, /"custom":\s*true/)
  assert.match(pagesJson, /"borderStyle":\s*"white"/)
  for (const token of ['馆', '任', '我', '/pages/index/index', '/pages/task/list', '/pages/mine/index']) {
    assert.match(tabBar, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
  for (const token of ['app-tabbar-wrap', 'app-tabbar', 'tab-item', 'tab-icon', 'tab-text']) {
    assert.match(tabBar, new RegExp(token))
  }
  assert.match(tabBar, /#fffaf2/i)
  assert.match(tabBar, /#0f5132/i)
  assert.match(tabBar, /#7c6f5c/i)
  assert.match(tabBar, /border-radius:\s*999rpx/)
  assert.match(tabBar, /box-shadow/)
  assert.doesNotMatch(tabBar, /#1677ff|#1890ff|#007aff|\bblue\b/i)
  assert.match(app, /\.uni-tabbar-bottom/)
  for (const page of [home, taskList, mine]) {
    assert.match(page, /<AppTabBar\s*\/>/)
    assert.match(page, /uni\.hideTabBar/)
    assert.match(page, /tab-page/)
  }
  assert.match(tabBar, /height:\s*94rpx/)
  assert.match(tabBar, /height:\s*70rpx/)
  assert.match(tabBar, /width:\s*38rpx/)
  assert.match(assistant, /currentBottomGap/)
  assert.match(assistant, /POSITION_SYNC_EVENT/)
  assert.match(assistant, /uni\.\$emit\(POSITION_SYNC_EVENT/)
  assert.match(assistant, /uni\.\$on\(POSITION_SYNC_EVENT/)
  assert.match(assistant, /uni\.\$off\(POSITION_SYNC_EVENT/)
  assert.doesNotMatch(assistant, /\.assistant-root\s*\{[^}]*z-index/s)
  assert.match(assistant, /\.assistant-mask\s*\{[^}]*z-index:\s*10000/s)
  assert.match(assistant, /\.assistant-input-bar\s*\{[^}]*safe-area-inset-bottom/s)
  assert.match(assistant, /:adjust-position="true"/)
  assert.match(assistant, /cursor-spacing="16"/)
})

test('AI 聊天安全渲染 Markdown 并优化详情长字段和缩略图', async () => {
  const [assistant, taskDetail, batchDetail, imageResult] = await Promise.all([
    readSource('components/AssistantFloat.vue'),
    readSource('pages/task/detail.vue'),
    readSource('pages/batch/detail.vue'),
    readSource('pages/image/result.vue')
  ])

  assert.match(assistant, /<rich-text/)
  assert.match(assistant, /formatAssistantMarkdown/)
  assert.match(assistant, /escapeHtml/)
  assert.match(assistant, /<strong/)
  assert.match(assistant, /<pre/)
  assert.match(assistant, /<ul/)
  assert.match(assistant, /<ol/)
  assert.doesNotMatch(assistant, /formatAssistantText/)
  assert.match(assistant, /show-scrollbar/)
  for (const page of [taskDetail, batchDetail, imageResult]) {
    assert.match(page, /info-block/)
    assert.match(page, /code-text/)
    assert.match(page, /detail-page/)
  }
  assert.match(batchDetail, /width:\s*150rpx/)
  assert.match(batchDetail, /height:\s*150rpx/)
  assert.match(batchDetail, /@error="handleThumbError\(item\)"/)
  assert.match(batchDetail, /height:\s*80rpx/)
})

test('任务与批次业务页使用统一的本草移动卡片分区', async () => {
  const [taskList, batchList, taskDetail, batchDetail, imageResult, assistant] =
    await Promise.all([
      readSource('pages/task/list.vue'),
      readSource('pages/batch/list.vue'),
      readSource('pages/task/detail.vue'),
      readSource('pages/batch/detail.vue'),
      readSource('pages/image/result.vue'),
      readSource('components/AssistantFloat.vue')
    ])

  for (const page of [taskList, batchList]) {
    assert.match(page, /herb-card/)
    assert.match(page, /list-summary-card/)
    assert.match(page, /code-text/)
    assert.match(page, /info-block/)
    assert.match(page, /section-title/)
    assert.match(page, /detail-link/)
  }
  assert.match(batchList, /暂无采集批次/)
  for (const token of ['task-overview-card', 'collection-info-card', 'description-card']) {
    assert.match(taskDetail, new RegExp(token))
  }
  for (const token of [
    'batch-overview-card',
    'collection-info-card',
    'stat-card',
    'evaluation-card',
    'action-card',
    'image-section-card'
  ]) {
    assert.match(batchDetail, new RegExp(token))
  }
  assert.match(imageResult, /image-result-page/)
  assert.match(assistant, /const DEFAULT_BOTTOM_GAP = 85/)
})
