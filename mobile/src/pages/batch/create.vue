<template>
  <view class="page">
    <view class="card">
      <view class="section-header">
        <text class="section-title">所属任务</text>
        <text class="status-tag" :class="`status-${taskInfo.taskStatus || 'unknown'}`">
          {{ formatStatus(taskInfo.taskStatus, TASK_STATUS_MAP) }}
        </text>
      </view>

      <view v-if="taskError" class="task-error">
        <text>{{ taskError }}</text>
      </view>

      <view class="info-row">
        <text class="label">任务名称</text>
        <text class="value">{{ displayText(taskInfo.taskName) }}</text>
      </view>
      <view class="info-row">
        <text class="label">任务编码</text>
        <text class="value">{{ displayText(taskInfo.taskCode) }}</text>
      </view>
      <view class="info-row">
        <text class="label">采集药材</text>
        <text class="value">{{ displayText(taskInfo.speciesName) }}</text>
      </view>
      <view class="info-row">
        <text class="label">基地名称</text>
        <text class="value">{{ displayText(taskInfo.baseName) }}</text>
      </view>
      <view class="info-row">
        <text class="label">采集地点</text>
        <text class="value">{{ displayText(taskInfo.collectPlace) }}</text>
      </view>
    </view>

    <view class="card">
      <text class="section-title">批次信息</text>

      <view class="form-item">
        <text class="form-label">批次名称 <text class="required">*</text></text>
        <input v-model.trim="form.batchName" class="form-input" maxlength="100" placeholder="请输入批次名称" />
      </view>

      <view class="form-item">
        <text class="form-label">产地/采集地点</text>
        <input v-model.trim="form.originPlace" class="form-input" placeholder="请输入产地或采集地点" />
      </view>

      <view class="form-item">
        <text class="form-label">基地名称</text>
        <input v-model.trim="form.baseName" class="form-input" placeholder="请输入基地名称" />
      </view>

      <view class="form-item">
        <text class="form-label">采集开始时间</text>
        <input v-model.trim="form.collectStartTime" class="form-input" placeholder="yyyy-MM-dd HH:mm:ss" />
      </view>

      <view class="form-item">
        <text class="form-label">备注</text>
        <textarea v-model.trim="form.remark" class="form-textarea" maxlength="500" placeholder="请输入备注" />
      </view>
    </view>

    <view class="action-bar">
      <button class="secondary-btn action-btn" @click="handleCancel">取消</button>
      <button class="primary-btn action-btn" :loading="submitting" @click="handleSubmit">创建批次</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createBatchUnderTask, getTaskDetail } from '../../api/mobileTaskApi'
import { TASK_STATUS_MAP } from '../../utils/constants'
import { formatStatus, getNowDateTime } from '../../utils/format'
import { getCurrentCollector } from '../../utils/user'

const taskId = ref('')
const collector = ref(getCurrentCollector())
const taskInfo = ref({})
const taskError = ref('')
const submitting = ref(false)

const form = reactive({
  batchName: '',
  baseId: '',
  baseName: '',
  originPlace: '',
  collectStartTime: getNowDateTime(),
  remark: ''
})

onLoad((options) => {
  taskId.value = options.taskId || options.id || ''

  if (!taskId.value) {
    taskError.value = '任务 ID 不存在'
    uni.showToast({
      title: '任务 ID 不存在',
      icon: 'none'
    })
    return
  }

  loadTaskInfo()
})

async function loadTaskInfo() {
  collector.value = getCurrentCollector()
  taskError.value = ''

  try {
    const data = await getTaskDetail(taskId.value, {
      collectorId: collector.value.collectorId
    })
    const task = normalizeTaskDetail(data)
    taskInfo.value = task
    applyTaskDefaults(task)
  } catch (error) {
    console.error('任务信息加载失败', error)
    taskError.value = '任务信息加载失败，请确认任务是否存在'
    uni.showToast({
      title: '任务信息加载失败，请确认任务是否存在',
      icon: 'none'
    })
  }
}

function normalizeTaskDetail(data) {
  const source = data || {}
  return source.task || source.detail || source
}

function applyTaskDefaults(task) {
  form.baseId = task.baseId || form.baseId
  form.baseName = task.baseName || form.baseName
  form.originPlace = task.collectPlace || form.originPlace

  if (!form.batchName) {
    const speciesName = task.speciesName || '中药材'
    form.batchName = `${speciesName}手机端采集批次`
  }
}

function validateForm() {
  if (!taskId.value) {
    showToast('任务 ID 不存在')
    return false
  }

  if (!form.batchName) {
    showToast('请输入批次名称')
    return false
  }

  if (form.batchName.length > 100) {
    showToast('批次名称不能超过 100 字')
    return false
  }

  if (form.collectStartTime && !/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(form.collectStartTime)) {
    showToast('采集开始时间格式应为 yyyy-MM-dd HH:mm:ss')
    return false
  }

  return true
}

async function handleSubmit() {
  if (submitting.value || !validateForm()) {
    return
  }

  submitting.value = true
  collector.value = getCurrentCollector()

  try {
    const payload = {
      batchName: form.batchName,
      baseId: form.baseId || undefined,
      baseName: form.baseName,
      originPlace: form.originPlace,
      collectStartTime: form.collectStartTime,
      remark: form.remark,
      collectorId: collector.value.collectorId,
      collectorName: collector.value.collectorName
    }
    const result = await createBatchUnderTask(taskId.value, payload)
    const batchId = result && (result.batchId || result.id)

    uni.showToast({
      title: '批次创建成功',
      icon: 'success'
    })

    setTimeout(() => {
      if (batchId) {
        uni.redirectTo({
          url: `/pages/batch/detail?batchId=${batchId}`
        })
      } else {
        uni.navigateBack()
      }
    }, 500)
  } catch (error) {
    console.error('批次创建失败', error)
    showToast('批次创建失败，请检查网络或后端服务')
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  uni.navigateBack()
}

function displayText(value) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function showToast(title) {
  uni.showToast({
    title,
    icon: 'none'
  })
}
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}

.section-title {
  color: #111827;
  font-size: 32rpx;
  font-weight: 700;
}

.status-tag {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #eef5ff;
  color: #1677ff;
  font-size: 23rpx;
  line-height: 1;
}

.status-published {
  background: #edf7ed;
  color: #1f8a3b;
}

.status-in_progress {
  background: #fff7e6;
  color: #c46a00;
}

.status-completed {
  background: #eef5ff;
  color: #1677ff;
}

.status-cancelled {
  background: #f1f5f9;
  color: #64748b;
}

.task-error {
  margin-bottom: 16rpx;
  padding: 16rpx 18rpx;
  border-radius: 12rpx;
  background: #fff1f0;
  color: #cf1322;
  font-size: 25rpx;
}

.info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 22rpx;
  padding: 10rpx 0;
}

.label {
  flex-shrink: 0;
  width: 160rpx;
  color: #64748b;
}

.value {
  min-width: 0;
  flex: 1;
  color: #111827;
  text-align: right;
  word-break: break-all;
}

.form-item {
  margin-top: 26rpx;
}

.form-label {
  display: block;
  color: #334155;
  font-size: 27rpx;
  font-weight: 600;
}

.required {
  color: #cf1322;
}

.form-input,
.form-textarea {
  box-sizing: border-box;
  width: 100%;
  margin-top: 12rpx;
  padding: 20rpx 22rpx;
  border-radius: 12rpx;
  background: #f8fafc;
  color: #111827;
  font-size: 28rpx;
}

.form-input {
  height: 88rpx;
}

.form-textarea {
  min-height: 170rpx;
  line-height: 1.5;
}

.action-bar {
  display: flex;
  gap: 18rpx;
  padding-bottom: 28rpx;
}

.action-btn {
  flex: 1;
}
</style>
