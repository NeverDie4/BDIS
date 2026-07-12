<template>
  <view v-if="visible" class="assistant-root">
    <view
      v-if="!panelOpen"
      class="assistant-float"
      :style="floatStyle"
      @touchstart="handleTouchStart"
      @touchmove.stop.prevent="handleTouchMove"
      @touchend="handleTouchEnd"
      @tap="handleFloatTap"
    >
      <image
        v-if="!iconLoadFailed"
        class="assistant-float-icon"
        src="/static/images/assistant-float.png"
        mode="aspectFit"
        @error="handleIconError"
      />
      <view v-else class="assistant-float-fallback">AI</view>
    </view>

    <view v-if="panelOpen" class="assistant-mask" @tap="close">
      <view class="assistant-panel" @tap.stop>
        <view class="assistant-header">
          <view class="assistant-heading">
            <image
              class="assistant-avatar"
              src="/static/images/assistant-float.png"
              mode="aspectFit"
            />
            <text class="assistant-title">生物医药数字信息系统AI助手</text>
          </view>
          <button class="assistant-close" aria-label="关闭" @tap="close">×</button>
        </view>

        <scroll-view class="assistant-messages" scroll-y :scroll-top="scrollTop">
          <view
            v-for="(message, index) in messages"
            :key="`${message.role}-${index}`"
            class="message-row"
            :class="`message-${message.role}`"
          >
            <view class="message-bubble">
              <rich-text
                v-if="message.role === 'assistant'"
                class="assistant-markdown"
                :nodes="formatAssistantMarkdown(message.content)"
              />
              <text v-else class="message-plain">{{ message.content }}</text>
            </view>
          </view>
          <view v-if="loading" class="message-row message-assistant">
            <view class="message-bubble loading-bubble">正在思考...</view>
          </view>
          <view v-else-if="historyLoading" class="message-row message-assistant">
            <view class="message-bubble loading-bubble">正在加载会话...</view>
          </view>
        </scroll-view>

        <scroll-view class="assistant-quick-list" scroll-x :show-scrollbar="false">
          <view class="assistant-quick-inner">
            <button
              v-for="question in quickQuestions"
              :key="question"
              class="assistant-quick"
              :disabled="loading || historyLoading"
              @tap="sendQuickQuestion(question)"
            >
              {{ question }}
            </button>
          </view>
        </scroll-view>

        <view class="assistant-input-bar">
          <input
            v-model.trim="inputText"
            class="assistant-input"
            :disabled="loading || historyLoading"
            :adjust-position="true"
            cursor-spacing="16"
            confirm-type="send"
            placeholder="输入你的问题"
            @confirm="handleSend"
          />
          <button
            class="assistant-send"
            :disabled="loading || historyLoading || !inputText"
            @tap="handleSend"
          >
            发送
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import {
  chatWithAssistant,
  explainBatch as requestBatchExplanation,
  explainImage as requestImageExplanation,
  getAssistantMessages,
  getAssistantSessions
} from '../api/assistantApi'
import { isLoggedIn } from '../utils/auth'
import { getStorage, setStorage } from '../utils/storage'

const FLOAT_SIZE = 42
const EDGE_GAP = 16
const TOP_GAP = 64
const TAB_BAR_BOTTOM_GAP = 80
const DEFAULT_BOTTOM_GAP = 85
const SESSION_KEY = 'assistantSessionId'
const FLOAT_LEFT_KEY = 'assistantFloatLeft'
const FLOAT_TOP_KEY = 'assistantFloatTop'
const POSITION_SYNC_EVENT = 'assistant-float-position-change'
const ROUTE_SYNC_EVENT = 'app-tabbar-route-change'
const TAB_ROUTES = new Set(['/pages/index/index', '/pages/task/list', '/pages/mine/index'])

const visible = ref(false)
const panelOpen = ref(false)
const loading = ref(false)
const historyLoading = ref(false)
const historyLoaded = ref(false)
const iconLoadFailed = ref(false)
const inputText = ref('')
const sessionId = ref('')
const scrollTop = ref(0)
const floatLeft = ref(0)
const floatTop = ref(0)
const screenWidth = ref(375)
const screenHeight = ref(667)
const dragState = {
  startX: 0,
  startY: 0,
  originLeft: 0,
  originTop: 0,
  moved: false
}
let suppressTap = false
let historyLoadPromise = null

const welcomeMessages = [
  {
    role: 'assistant',
    content: '你好，我是生物医药数字信息系统AI助手。'
  }
]
const messages = ref([...welcomeMessages])

const quickQuestions = [
  '这个系统怎么上传图片？',
  '批次采集流程是什么？',
  '为什么需要人工复核？',
  '批次质量等级怎么看？',
  '拍照采集有什么规范？'
]

const floatStyle = computed(() => ({
  left: `${floatLeft.value}px`,
  top: `${floatTop.value}px`
}))

onMounted(() => {
  visible.value = isLoggedIn()
  if (!visible.value) {
    return
  }

  initSession()
  initFloatPosition()
  uni.$on(POSITION_SYNC_EVENT, syncFloatPosition)
  uni.$on(ROUTE_SYNC_EVENT, ensureSafePosition)
})

onUnmounted(() => {
  uni.$off(POSITION_SYNC_EVENT, syncFloatPosition)
  uni.$off(ROUTE_SYNC_EVENT, ensureSafePosition)
})

function initSession() {
  sessionId.value = getStorage(SESSION_KEY, '')
  if (!sessionId.value) {
    sessionId.value = `mobile-assistant-${Date.now()}`
    setStorage(SESSION_KEY, sessionId.value)
  }
}

function initFloatPosition() {
  const systemInfo = uni.getSystemInfoSync()
  screenWidth.value = Number(systemInfo.windowWidth || screenWidth.value)
  screenHeight.value = Number(systemInfo.windowHeight || screenHeight.value)
  const defaultLeft = screenWidth.value - FLOAT_SIZE - EDGE_GAP
  const defaultTop = screenHeight.value - FLOAT_SIZE - currentBottomGap()
  const cachedLeft = Number(getStorage(FLOAT_LEFT_KEY, defaultLeft))
  const cachedTop = Number(getStorage(FLOAT_TOP_KEY, defaultTop))
  if (isSafeCachedPosition(cachedLeft, cachedTop)) {
    floatLeft.value = cachedLeft
    floatTop.value = cachedTop
    return
  }

  floatLeft.value = defaultLeft
  floatTop.value = defaultTop
  setStorage(FLOAT_LEFT_KEY, defaultLeft)
  setStorage(FLOAT_TOP_KEY, defaultTop)
}

function isSafeCachedPosition(left, top) {
  return (
    Number.isFinite(left) &&
    Number.isFinite(top) &&
    left >= EDGE_GAP &&
    left <= maxLeft() &&
    top >= TOP_GAP &&
    top <= maxTop()
  )
}

function handleTouchStart(event) {
  const touch = event.touches?.[0]
  if (!touch) {
    return
  }

  dragState.startX = touch.clientX
  dragState.startY = touch.clientY
  dragState.originLeft = floatLeft.value
  dragState.originTop = floatTop.value
  dragState.moved = false
}

function handleTouchMove(event) {
  const touch = event.touches?.[0]
  if (!touch) {
    return
  }

  const offsetX = touch.clientX - dragState.startX
  const offsetY = touch.clientY - dragState.startY
  if (Math.abs(offsetX) > 5 || Math.abs(offsetY) > 5) {
    dragState.moved = true
  }
  floatLeft.value = clamp(dragState.originLeft + offsetX, EDGE_GAP, maxLeft())
  floatTop.value = clamp(dragState.originTop + offsetY, TOP_GAP, maxTop())
}

function handleTouchEnd() {
  if (!dragState.moved) {
    return
  }

  suppressTap = true
  persistFloatPosition(floatLeft.value, floatTop.value)
  uni.$emit(POSITION_SYNC_EVENT, {
    left: floatLeft.value,
    top: floatTop.value
  })
}

function syncFloatPosition(position = {}) {
  const left = Number(position.left)
  const top = Number(position.top)
  if (!isSafeCachedPosition(left, top)) {
    initFloatPosition()
    return
  }

  floatLeft.value = left
  floatTop.value = top
}

function persistFloatPosition(left, top) {
  setStorage(FLOAT_LEFT_KEY, left)
  setStorage(FLOAT_TOP_KEY, top)
}

function ensureSafePosition() {
  if (isSafeCachedPosition(floatLeft.value, floatTop.value)) {
    return
  }
  initFloatPosition()
}

function handleFloatTap() {
  if (suppressTap) {
    suppressTap = false
    return
  }
  open()
}

function handleIconError() {
  iconLoadFailed.value = true
}

async function open() {
  if (!isLoggedIn()) {
    visible.value = false
    return
  }
  panelOpen.value = true
  await loadSessionMessages()
  scrollToBottom()
}

async function loadSessionMessages() {
  if (historyLoaded.value || !sessionId.value) {
    return
  }
  if (historyLoadPromise) {
    return historyLoadPromise
  }

  historyLoading.value = true
  historyLoadPromise = getAssistantSessions({ source: 'mobile', pageNum: 1, pageSize: 200 })
    .then((sessionPage) => {
      const sessionExists = sessionPage?.records?.some(
        (session) => session?.sessionId === sessionId.value
      )
      return sessionExists ? getAssistantMessages(sessionId.value) : []
    })
    .then((response) => {
      const history = Array.isArray(response)
        ? response.filter(
            (message) =>
              (message?.role === 'user' || message?.role === 'assistant') && message?.content
          )
        : []
      if (history.length > 0) {
        messages.value = history
      }
      historyLoaded.value = true
    })
    .catch((error) => {
      if (isUnauthorizedError(error) || !isLoggedIn()) {
        handleUnauthorized()
      }
    })
    .finally(() => {
      historyLoading.value = false
      historyLoadPromise = null
      scrollToBottom()
    })

  return historyLoadPromise
}

function close() {
  panelOpen.value = false
}

async function handleSend() {
  const message = inputText.value.trim()
  if (!message || loading.value || historyLoading.value) {
    return
  }

  inputText.value = ''
  await sendAssistantRequest(
    () =>
      chatWithAssistant({
        ...baseRequestData(),
        message
      }),
    message
  )
}

function sendQuickQuestion(question) {
  if (loading.value || historyLoading.value) {
    return
  }
  inputText.value = question
  handleSend()
}

async function explainBatch(batchId, data = {}) {
  if (!batchId || loading.value) {
    return
  }
  await open()
  await sendAssistantRequest(
    () => requestBatchExplanation(batchId, { ...baseRequestData(), ...data }),
    '请解释当前批次的识别结果和质量情况。'
  )
}

async function explainImage(imageId, data = {}) {
  if (!imageId || loading.value) {
    return
  }
  await open()
  await sendAssistantRequest(
    () => requestImageExplanation(imageId, { ...baseRequestData(), ...data }),
    '请解释当前图片的识别结果。'
  )
}

async function sendAssistantRequest(requester, userMessage) {
  if (!isLoggedIn()) {
    handleUnauthorized()
    return
  }

  messages.value.push({ role: 'user', content: userMessage })
  loading.value = true
  scrollToBottom()

  try {
    const response = await requester()
    messages.value.push({
      role: 'assistant',
      content: resolveAnswer(response)
    })
  } catch (error) {
    if (isUnauthorizedError(error) || !isLoggedIn()) {
      handleUnauthorized()
    } else {
      messages.value.push({
        role: 'assistant',
        content: '请求失败，请稍后重试。'
      })
    }
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function baseRequestData() {
  return {
    sessionId: sessionId.value,
    source: 'mobile',
    useRag: true,
    topK: 5
  }
}

function resolveAnswer(response) {
  return (
    response?.answer ||
    response?.content ||
    response?.message ||
    response?.data?.answer ||
    '生物医药数字信息系统AI助手暂时没有返回内容。'
  )
}

function formatAssistantMarkdown(text) {
  const lines = String(text ?? '').replace(/\r\n?/g, '\n').split('\n')
  const html = []
  let listType = ''
  let inCodeBlock = false
  let codeLanguage = ''
  let codeLines = []

  const closeList = () => {
    if (!listType) {
      return
    }
    html.push(`</${listType}>`)
    listType = ''
  }

  const openList = (type) => {
    if (listType === type) {
      return
    }
    closeList()
    listType = type
    html.push(
      type === 'ul'
        ? '<ul style="margin:6px 0;padding-left:20px;">'
        : '<ol style="margin:6px 0;padding-left:22px;">'
    )
  }

  const flushCodeBlock = () => {
    const languageLabel = codeLanguage
      ? `<div style="margin-bottom:6px;color:#8b7e6b;font-size:12px;">${escapeHtml(codeLanguage)}</div>`
      : ''
    html.push(
      `<pre style="margin:8px 0;padding:10px 12px;overflow-x:auto;border:1px solid #d9cdbd;border-radius:6px;background:#f4ead8;color:#24352d;font-family:Consolas,'Courier New',monospace;font-size:13px;line-height:1.5;white-space:pre-wrap;word-break:break-word;">${languageLabel}<code>${escapeHtml(codeLines.join('\n'))}</code></pre>`
    )
    codeLines = []
    codeLanguage = ''
  }

  for (const line of lines) {
    const fenceMatch = line.match(/^```\s*([\w-]*)\s*$/)
    if (fenceMatch) {
      if (inCodeBlock) {
        flushCodeBlock()
        inCodeBlock = false
      } else {
        closeList()
        inCodeBlock = true
        codeLanguage = fenceMatch[1] || ''
      }
      continue
    }

    if (inCodeBlock) {
      codeLines.push(line)
      continue
    }

    const headingMatch = line.match(/^(#{1,6})\s+(.+)$/)
    if (headingMatch) {
      closeList()
      const level = headingMatch[1].length
      const sizes = [20, 19, 18, 17, 16, 15]
      html.push(
        `<div style="margin:${level <= 2 ? 12 : 9}px 0 6px;color:#0f3d2e;font-size:${sizes[level - 1]}px;font-weight:700;line-height:1.4;">${renderInlineMarkdown(headingMatch[2])}</div>`
      )
      continue
    }

    const unorderedMatch = line.match(/^\s*[-*+]\s+(.+)$/)
    if (unorderedMatch) {
      openList('ul')
      html.push(
        `<li style="margin:4px 0;line-height:1.55;">${renderInlineMarkdown(unorderedMatch[1])}</li>`
      )
      continue
    }

    const orderedMatch = line.match(/^\s*\d+[.)]\s+(.+)$/)
    if (orderedMatch) {
      openList('ol')
      html.push(
        `<li style="margin:4px 0;line-height:1.55;">${renderInlineMarkdown(orderedMatch[1])}</li>`
      )
      continue
    }

    closeList()
    if (!line.trim()) {
      html.push('<div style="height:7px;"></div>')
      continue
    }

    const quoteMatch = line.match(/^>\s?(.*)$/)
    if (quoteMatch) {
      html.push(
        `<div style="margin:7px 0;padding:7px 10px;border-left:3px solid #b7d7c2;background:#f4f0e8;color:#5f5548;line-height:1.55;">${renderInlineMarkdown(quoteMatch[1])}</div>`
      )
      continue
    }

    html.push(
      `<div style="margin:3px 0;color:#1f2933;line-height:1.6;">${renderInlineMarkdown(line)}</div>`
    )
  }

  closeList()
  if (inCodeBlock) {
    flushCodeBlock()
  }
  return html.join('')
}

function renderInlineMarkdown(text) {
  const inlineCode = []
  const placeholders = String(text ?? '').replace(/`([^`]+)`/g, (_, code) => {
    const index = inlineCode.push(code) - 1
    return `\u0000CODE${index}\u0000`
  })

  return escapeHtml(placeholders)
    .replace(/\*\*(.+?)\*\*/g, '<strong style="font-weight:700;color:#0f3d2e;">$1</strong>')
    .replace(/__(.+?)__/g, '<strong style="font-weight:700;color:#0f3d2e;">$1</strong>')
    .replace(/\*([^*\n]+)\*/g, '<em style="font-style:italic;">$1</em>')
    .replace(/\u0000CODE(\d+)\u0000/g, (_, index) =>
      `<code style="padding:1px 4px;border-radius:4px;background:#f4ead8;color:#0b3d2a;font-family:Consolas,'Courier New',monospace;font-size:0.92em;">${escapeHtml(inlineCode[Number(index)] || '')}</code>`
    )
}

function escapeHtml(text) {
  return String(text ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function isUnauthorizedError(error) {
  return error?.code === 401 || error?.code === 'UNAUTHORIZED'
}

function handleUnauthorized() {
  visible.value = false
  panelOpen.value = false
  uni.showToast({
    title: '登录已过期，请重新登录',
    icon: 'none'
  })
}

function scrollToBottom() {
  nextTick(() => {
    scrollTop.value += 100000
  })
}

function maxLeft() {
  return Math.max(EDGE_GAP, screenWidth.value - FLOAT_SIZE - EDGE_GAP)
}

function maxTop() {
  return Math.max(TOP_GAP, screenHeight.value - FLOAT_SIZE - currentBottomGap())
}

function currentBottomGap() {
  const pages = getCurrentPages()
  const route = pages[pages.length - 1]?.route
  return TAB_ROUTES.has(route ? `/${route}` : '') ? TAB_BAR_BOTTOM_GAP : DEFAULT_BOTTOM_GAP
}

function clamp(value, min, max) {
  if (!Number.isFinite(value)) {
    return min
  }
  return Math.min(Math.max(value, min), max)
}

defineExpose({
  open,
  close,
  explainBatch,
  explainImage
})
</script>

<style scoped>
.assistant-root {
  position: relative;
}

.assistant-float {
  position: fixed;
  z-index: 7900;
  display: flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 50%;
  background: transparent;
  box-shadow: 0 4px 12px rgba(15, 81, 50, 0.26);
  touch-action: none;
}

.assistant-float-fallback {
  display: block;
  width: 42px;
  height: 42px;
  border-radius: 50%;
}

.assistant-float-icon {
  display: block;
  width: 42px;
  height: 42px;
  max-width: none;
  flex-shrink: 0;
  object-fit: contain;
}

.assistant-float-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #166534;
  box-shadow: 0 4px 12px rgba(15, 81, 50, 0.26);
  color: #fffaf2;
  font-size: 14px;
  font-weight: 700;
}

.assistant-mask {
  position: fixed;
  z-index: 10000;
  inset: 0;
  background: rgba(15, 61, 46, 0.32);
}

.assistant-panel {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  height: 70vh;
  max-height: 760px;
  min-height: 460px;
  flex-direction: column;
  overflow: hidden;
  border-radius: 16px 16px 0 0;
  background: #fbf7ef;
  box-shadow: 0 -10px 36px rgba(63, 45, 24, 0.16);
}

.assistant-header {
  display: flex;
  flex-shrink: 0;
  height: 62px;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  border-bottom: 1px solid #eadfcd;
  background: #fffaf2;
}

.assistant-heading {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 8px;
}

.assistant-avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 50%;
}

.assistant-title {
  min-width: 0;
  color: #0f3d2e;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.3;
  white-space: nowrap;
}

.assistant-close {
  display: flex;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #7c6f5c;
  font-size: 30px;
  line-height: 40px;
}

.assistant-close::after,
.assistant-quick::after,
.assistant-send::after {
  border: 0;
}

.assistant-messages {
  min-height: 0;
  flex: 1;
  box-sizing: border-box;
  padding: 16px;
}

.message-row {
  display: flex;
  margin-bottom: 14px;
}

.message-user {
  justify-content: flex-end;
}

.message-assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 78%;
  box-sizing: border-box;
  padding: 11px 13px;
  border-radius: 6px;
  font-size: 15px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.assistant-markdown,
.message-plain {
  display: block;
}

.message-assistant .message-bubble {
  border: 1px solid #eadfcd;
  background: #fffaf2;
  color: #1f2933;
}

.message-user .message-bubble {
  background: #0f5132;
  color: #ffffff;
}

.loading-bubble {
  color: #7c6f5c;
}

.assistant-quick-list {
  flex-shrink: 0;
  box-sizing: border-box;
  width: 100%;
  padding: 10px 12px 6px;
  background: #fffaf2;
  white-space: nowrap;
}

.assistant-quick-inner {
  display: inline-flex;
  gap: 8px;
  box-sizing: border-box;
  padding-right: 16px;
}

.assistant-quick {
  height: 34px;
  margin: 0;
  padding: 0 13px;
  border: 1px solid #b7d7c2;
  border-radius: 17px;
  background: #eaf5ee;
  color: #0f5132;
  font-size: 13px;
  line-height: 32px;
}

.assistant-input-bar {
  display: flex;
  flex-shrink: 0;
  gap: 9px;
  box-sizing: border-box;
  padding: 10px 12px calc(12px + env(safe-area-inset-bottom));
  border-top: 1px solid #eadfcd;
  background: #fffaf2;
}

.assistant-input {
  min-width: 0;
  height: 42px;
  flex: 1;
  box-sizing: border-box;
  padding: 0 13px;
  border-radius: 6px;
  border: 1px solid #eadfcd;
  background: #ffffff;
  color: #1f2933;
  font-size: 15px;
}

.assistant-send {
  width: 68px;
  height: 42px;
  margin: 0;
  padding: 0;
  border-radius: 6px;
  background: #166534;
  color: #ffffff;
  font-size: 15px;
  line-height: 42px;
}

.assistant-send[disabled],
.assistant-quick[disabled] {
  border-color: #d7d0c5;
  background: #e8e2d9;
  color: #7c6f5c;
  opacity: 0.72;
}
</style>
