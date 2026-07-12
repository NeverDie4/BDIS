<template>
  <view class="page detail-page image-upload-page">
    <view class="herb-card batch-summary-card">
      <view class="section-header">
        <text class="section-title">上传到批次</text>
        <text class="status-tag" :class="`status-${batchInfo.batchStatus || 'unknown'}`">
          {{ formatStatus(batchInfo.batchStatus, BATCH_STATUS_MAP) }}
        </text>
      </view>

      <view v-if="batchError" class="warn-box">{{ batchError }}</view>
      <view v-if="batchInfo.batchStatus && !canUpload" class="warn-box">当前批次状态不允许继续上传图片</view>

      <view class="info-row">
        <text class="label">批次名称</text>
        <text class="value">{{ displayText(batchInfo.batchName) }}</text>
      </view>
      <view class="info-block">
        <text class="info-block-label">批次编码</text>
        <text class="info-block-value code-text">{{ displayText(batchInfo.batchCode) }}</text>
      </view>
      <view class="info-row">
        <text class="label">药材</text>
        <text class="value">{{ displayText(batchInfo.speciesName) }}</text>
      </view>
      <view class="info-row">
        <text class="label">产地</text>
        <text class="value">{{ displayText(batchInfo.originPlace) }}</text>
      </view>
      <view class="info-row">
        <text class="label">当前图片数</text>
        <text class="value">{{ displayNumber(batchInfo.imageCount) }}</text>
      </view>
    </view>

    <view class="herb-card image-picker-card">
      <text class="section-title">图片选择</text>
      <view class="image-picker">
        <image v-if="imagePath" class="preview-image" mode="aspectFill" :src="imagePath" @click="previewImage" />
        <view v-else class="preview-placeholder">请拍照或从相册选择图片</view>
      </view>
      <view class="button-row">
        <button class="primary-btn picker-btn" @click="chooseImage('camera')">拍照</button>
        <button class="secondary-btn picker-btn" @click="chooseImage('album')">从相册选择</button>
      </view>
    </view>

    <view class="herb-card collect-form-card">
      <text class="section-title">采集信息</text>

      <view class="form-item">
        <text class="form-label">图片角色 <text class="required">*</text></text>
        <picker :range="imageRoleOptions" range-key="label" :value="imageRoleIndex" @change="onImageRoleChange">
          <view class="picker-value">{{ imageRoleLabel }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">生长阶段</text>
        <picker :range="growthStageOptions" range-key="label" :value="growthStageIndex" @change="onGrowthStageChange">
          <view class="picker-value">{{ growthStageLabel }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">健康状态</text>
        <picker :range="healthStatusOptions" range-key="label" :value="healthStatusIndex" @change="onHealthStatusChange">
          <view class="picker-value">{{ healthStatusLabel }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">采集地点</text>
        <input v-model.trim="form.collectPlace" class="form-input" placeholder="请输入采集地点" />
      </view>

      <view class="form-item">
        <text class="form-label">采集时间 <text class="required">*</text></text>
        <input v-model.trim="form.collectTime" class="form-input" placeholder="yyyy-MM-dd HH:mm:ss" />
      </view>

      <view class="switch-row">
        <text class="form-label">设为主图</text>
        <switch :checked="form.isPrimary" color="#166534" @change="onPrimaryChange" />
      </view>

      <view class="switch-row">
        <text class="form-label">上传后自动识别</text>
        <switch :checked="form.autoIdentify" color="#166534" @change="onAutoIdentifyChange" />
      </view>
      <text class="form-tip">
        建议先上传并绑定批次，再到批次详情中点击“识别”或“识别未完成图片”。开启自动识别时，上传会等待完整识别流程，耗时可能较长。
      </text>

      <view class="form-item">
        <text class="form-label">备注</text>
        <textarea v-model.trim="form.remark" class="form-textarea" maxlength="500" placeholder="请输入备注" />
      </view>

      <button class="primary-btn upload-btn" :loading="uploading" @click="handleUpload">上传图片</button>
    </view>

    <view v-if="uploadError" class="herb-card error-card">
      <text class="result-title">上传失败</text>
      <text class="error-message">{{ uploadError }}</text>
      <view class="button-row">
        <button class="secondary-btn picker-btn" @click="chooseImage('album')">重新选择</button>
        <button class="primary-btn picker-btn" :loading="uploading" @click="retryUpload">重新上传</button>
      </view>
    </view>

    <view v-if="uploadResult" class="herb-card result-card">
      <text class="result-title">上传成功</text>
      <view class="info-block">
        <text class="info-block-label">图片编码</text>
        <text class="info-block-value code-text">{{ displayText(uploadResult.imageCode) }}</text>
      </view>
      <view class="info-row">
        <text class="label">图片角色</text>
        <text class="value">{{ formatStatus(uploadResult.imageRole || form.imageRole, IMAGE_ROLE_MAP) }}</text>
      </view>
      <view class="info-row">
        <text class="label">是否主图</text>
        <text class="value">{{ uploadResult.isPrimary || form.isPrimary ? '是' : '否' }}</text>
      </view>
      <view class="info-row">
        <text class="label">自动识别</text>
        <text class="value">{{ formatAutoIdentifyResult(uploadResult) }}</text>
      </view>
      <view class="info-row">
        <text class="label">识别结果</text>
        <text class="value">{{ uploadResult.finalSpeciesName || '图片已上传，暂未生成识别结果' }}</text>
      </view>
      <view class="info-row">
        <text class="label">置信度</text>
        <text class="value">{{ formatPercent(uploadResult.finalConfidence) }}</text>
      </view>
      <view class="info-row">
        <text class="label">复核状态</text>
        <text class="value">{{ displayText(uploadResult.reviewStatus) }}</text>
      </view>
      <text v-if="uploadResult.message" class="result-message">{{ uploadResult.message }}</text>
      <text v-else-if="uploadResult.autoIdentifySuccess === false" class="result-message">
        图片已上传并绑定批次，但自动识别失败，可稍后在批次详情中手动触发识别。
      </text>

      <view class="button-row">
        <button class="secondary-btn picker-btn" @click="continueUpload">继续上传</button>
        <button class="secondary-btn picker-btn" @click="goBatchDetail">返回批次详情</button>
        <button v-if="uploadResult.imageId" class="primary-btn picker-btn" @click="goResult">查看识别结果</button>
      </view>
    </view>
    <AssistantFloat />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getBatchDetail, uploadBatchImage } from '../../api/mobileBatchApi'
import {
  BATCH_STATUS_MAP,
  GROWTH_STAGE_OPTIONS,
  HEALTH_STATUS_OPTIONS,
  IMAGE_ROLE_MAP,
  IMAGE_ROLE_OPTIONS
} from '../../utils/constants'
import { formatPercent, formatStatus, getNowDateTime } from '../../utils/format'
import { getAuthUser } from '../../utils/auth'

const batchId = ref('')
const batchInfo = ref({})
const batchError = ref('')
const imagePath = ref('')
const uploading = ref(false)
const uploadResult = ref(null)
const uploadError = ref('')

const imageRoleOptions = IMAGE_ROLE_OPTIONS
const growthStageOptions = GROWTH_STAGE_OPTIONS
const healthStatusOptions = HEALTH_STATUS_OPTIONS

const form = reactive({
  imageRole: 'other',
  imageType: 'other',
  growthStage: 'unknown',
  healthStatus: 'unknown',
  collectPlace: '',
  collectTime: getNowDateTime(),
  isPrimary: false,
  autoIdentify: true,
  remark: ''
})

const canUpload = computed(() => ['draft', 'collecting'].includes(batchInfo.value.batchStatus))
const imageRoleIndex = computed(() => Math.max(0, imageRoleOptions.findIndex((item) => item.value === form.imageRole)))
const growthStageIndex = computed(() => Math.max(0, growthStageOptions.findIndex((item) => item.value === form.growthStage)))
const healthStatusIndex = computed(() => Math.max(0, healthStatusOptions.findIndex((item) => item.value === form.healthStatus)))
const imageRoleLabel = computed(() => findOptionLabel(imageRoleOptions, form.imageRole))
const growthStageLabel = computed(() => findOptionLabel(growthStageOptions, form.growthStage))
const healthStatusLabel = computed(() => findOptionLabel(healthStatusOptions, form.healthStatus))

onLoad((options) => {
  batchId.value = options.batchId || options.id || ''

  if (!batchId.value) {
    batchError.value = '批次 ID 不存在'
    showToast('批次 ID 不存在')
    return
  }

  loadBatchInfo()
})

async function loadBatchInfo() {
  batchError.value = ''

  try {
    const data = await getBatchDetail(batchId.value)
    batchInfo.value = normalizeBatchDetail(data)
    applyBatchDefaults(batchInfo.value)
  } catch (error) {
    console.error('批次信息加载失败', error)
    batchError.value = '批次信息加载失败，请确认批次是否存在'
    showToast('批次信息加载失败，请确认批次是否存在')
  }
}

function normalizeBatchDetail(data) {
  const source = data || {}
  return source.batch || source.detail || source
}

function applyBatchDefaults(batch) {
  form.collectPlace = batch.originPlace || form.collectPlace
}

function chooseImage(sourceType) {
  uni.chooseImage({
    count: 1,
    sourceType: [sourceType],
    sizeType: ['compressed'],
    success: (res) => {
      imagePath.value = res.tempFilePaths[0] || ''
      uploadResult.value = null
      uploadError.value = ''
    },
    fail: (error) => {
      if (error && error.errMsg && error.errMsg.includes('cancel')) {
        return
      }
      console.error('图片选择失败', error)
      showToast('图片选择失败')
    }
  })
}

function previewImage() {
  if (!imagePath.value) {
    return
  }

  uni.previewImage({
    urls: [imagePath.value]
  })
}

function onImageRoleChange(e) {
  const option = imageRoleOptions[Number(e.detail.value)]
  if (!option) {
    return
  }

  form.imageRole = option.value
  form.imageType = option.value
}

function onGrowthStageChange(e) {
  const option = growthStageOptions[Number(e.detail.value)]
  if (option) {
    form.growthStage = option.value
  }
}

function onHealthStatusChange(e) {
  const option = healthStatusOptions[Number(e.detail.value)]
  if (option) {
    form.healthStatus = option.value
  }
}

function onPrimaryChange(e) {
  form.isPrimary = Boolean(e.detail.value)
}

function onAutoIdentifyChange(e) {
  form.autoIdentify = Boolean(e.detail.value)
}

function validateForm() {
  if (!batchId.value) {
    showToast('批次 ID 不存在')
    return false
  }

  if (!canUpload.value) {
    showToast('当前批次状态不允许继续上传图片')
    return false
  }

  if (!imagePath.value) {
    showToast('请先选择图片')
    return false
  }

  if (!form.imageRole) {
    showToast('请选择图片角色')
    return false
  }

  if (!form.collectTime) {
    showToast('请输入采集时间')
    return false
  }

  return true
}

async function handleUpload() {
  if (uploading.value || !validateForm()) {
    return
  }

  uploading.value = true
  uploadError.value = ''
  const authUser = getAuthUser()

  try {
    const formData = {
      collectorId: authUser?.userId || '',
      collectorName: authUser?.realName || authUser?.username || '',
      collectPlace: form.collectPlace,
      collectTime: form.collectTime,
      imageType: form.imageType || form.imageRole,
      growthStage: form.growthStage,
      healthStatus: form.healthStatus,
      imageRole: form.imageRole,
      isPrimary: form.isPrimary ? 1 : 0,
      sortOrder: '',
      autoIdentify: form.autoIdentify ? 'true' : 'false',
      remark: form.remark
    }
    const uploadTimeout = form.autoIdentify ? 180000 : 60000
    const result = await uploadBatchImage(batchId.value, imagePath.value, formData, {
      timeout: uploadTimeout
    })
    uploadResult.value = result || {}
    uploadError.value = ''

    uni.showToast({
      title: '图片上传成功',
      icon: 'success'
    })

    if (uploadResult.value.autoIdentifySuccess === false && uploadResult.value.message) {
      setTimeout(() => {
        showToast(uploadResult.value.message)
      }, 600)
    }
  } catch (error) {
    console.error('图片上传失败', error)
    if (isTimeoutError(error)) {
      uploadError.value = '上传或自动识别处理超时。图片可能仍在后端处理中，建议关闭自动识别后重试，或稍后返回批次详情查看。已保留当前选择的图片和采集信息。'
      showToast('上传处理超时')
    } else {
      uploadError.value = '图片上传失败，请检查网络或后端服务后重试。已保留当前选择的图片和采集信息。'
      showToast('图片上传失败，请检查网络或后端服务')
    }
  } finally {
    uploading.value = false
  }
}

function isTimeoutError(error) {
  const message = error?.errMsg || error?.message || ''
  return message.includes('timeout')
}

function continueUpload() {
  imagePath.value = ''
  uploadResult.value = null
  uploadError.value = ''
  form.isPrimary = false
  form.remark = ''
  form.collectTime = getNowDateTime()
}

function retryUpload() {
  handleUpload()
}

function goBatchDetail() {
  const pages = getCurrentPages()
  if (pages.length <= 1 && batchId.value) {
    uni.redirectTo({
      url: `/pages/batch/detail?batchId=${batchId.value}`
    })
    return
  }

  uni.navigateBack()
}

function goResult() {
  if (!uploadResult.value || !uploadResult.value.imageId) {
    return
  }

  uni.navigateTo({
    url: `/pages/image/result?imageId=${uploadResult.value.imageId}&batchId=${batchId.value}`
  })
}

function formatAutoIdentifyResult(result) {
  if (!form.autoIdentify) {
    return '未开启'
  }

  if (result.autoIdentifySuccess === true) {
    return '成功'
  }

  if (result.autoIdentifySuccess === false) {
    return '失败'
  }

  return '处理中'
}

function findOptionLabel(options, value) {
  const option = options.find((item) => item.value === value)
  return option ? option.label : '-'
}

function displayText(value) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function displayNumber(value) {
  return value === undefined || value === null || value === '' ? 0 : value
}

function showToast(title) {
  uni.showToast({
    title,
    icon: 'none'
  })
}
</script>

<style scoped>
.detail-page {
  padding-top: 32rpx;
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}

.section-title,
.result-title {
  display: block;
  color: #1f2933;
  font-size: 32rpx;
  font-weight: 700;
}

.result-title {
  margin-bottom: 18rpx;
}

.status-tag {
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #eaf5ee;
  color: #166534;
  font-size: 23rpx;
  line-height: 1;
}

.status-draft,
.status-collecting {
  background: #edf7ed;
  color: #1f8a3b;
}

.status-submitted,
.status-identifying,
.status-reviewing {
  background: #fff7e6;
  color: #c46a00;
}

.status-confirmed,
.status-archived {
  background: #eaf5ee;
  color: #166534;
}

.status-cancelled {
  background: #f4eadf;
  color: #64748b;
}

.warn-box {
  margin-bottom: 16rpx;
  padding: 16rpx 18rpx;
  border-radius: 12rpx;
  background: #fff7e6;
  color: #c46a00;
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
  width: 150rpx;
  color: #64748b;
}

.value {
  min-width: 0;
  flex: 1;
  color: #1f2933;
  text-align: right;
  word-break: break-all;
}

.image-picker {
  margin-top: 22rpx;
}

.preview-image,
.preview-placeholder {
  width: 100%;
  height: 420rpx;
  border-radius: 16rpx;
  background: #f4eadf;
}

.preview-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 28rpx;
}

.button-row {
  display: flex;
  gap: 18rpx;
  margin-top: 20rpx;
}

.picker-btn {
  flex: 1;
  margin-top: 0;
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

.picker-value,
.form-input,
.form-textarea {
  box-sizing: border-box;
  width: 100%;
  margin-top: 12rpx;
  padding: 20rpx 22rpx;
  border-radius: 12rpx;
  background: #fffaf2;
  color: #1f2933;
  font-size: 28rpx;
}

.picker-value,
.form-input {
  min-height: 88rpx;
}

.form-textarea {
  min-height: 170rpx;
  line-height: 1.5;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 26rpx;
}

.upload-btn {
  margin-top: 30rpx;
}

.result-card {
  margin-bottom: 28rpx;
}

.error-card {
  border: 1rpx solid #fecdd3;
}

.error-message {
  display: block;
  color: #be123c;
  font-size: 26rpx;
  line-height: 1.5;
}

.result-message {
  display: block;
  margin-top: 18rpx;
  color: #5f5548;
  font-size: 26rpx;
  line-height: 1.5;
}

.section-title,
.result-title {
  color: #0f3d2e;
}

.status-tag,
.status-draft,
.status-collecting,
.status-confirmed {
  background: #e8f7ed;
  color: #15803d;
}

.status-submitted,
.status-identifying,
.status-reviewing {
  background: #fff4df;
  color: #d97706;
}

.status-archived {
  background: #f4ead8;
  color: #8a5a2b;
}

.warn-box {
  border: 1rpx solid #f3d39a;
  background: #fff4df;
  color: #d97706;
}

.label,
.form-label {
  color: #7c6f5c;
}

.value {
  color: #1f2933;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.info-block {
  margin-bottom: 22rpx;
}

.info-block-label,
.info-block-value {
  display: block;
  text-align: left;
}

.info-block-label {
  margin-bottom: 8rpx;
  color: #7c6f5c;
  font-size: 26rpx;
}

.info-block-value {
  color: #1f2933;
  font-size: 28rpx;
  line-height: 1.6;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.code-text {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 25rpx;
  line-height: 1.45;
  overflow-wrap: normal;
  word-break: break-all;
}

.preview-image,
.preview-placeholder {
  border: 1rpx solid #eadfcd;
  background: #fffaf2;
}

.preview-placeholder {
  color: #8b7e6b;
}

.picker-value,
.form-input,
.form-textarea {
  border: 1rpx solid #eadfcd;
  background: #fffaf2;
  color: #1f2933;
}

.picker-value:focus,
.form-input:focus,
.form-textarea:focus {
  border-color: #0f5132;
}

.result-message {
  color: #5f5548;
}

.image-upload-page {
  padding-bottom: calc(190rpx + env(safe-area-inset-bottom));
}

.herb-card {
  box-sizing: border-box;
  margin-bottom: 24rpx;
  padding: 28rpx;
  border: 1rpx solid #eadfcd;
  border-radius: 24rpx;
  background: #fffaf2;
  background: rgba(255, 250, 242, 0.96);
  box-shadow: 0 10rpx 28rpx rgba(63, 45, 24, 0.06);
}

.section-title {
  display: flex;
  align-items: center;
  color: #0f3d2e;
}

.section-title::before {
  width: 8rpx;
  height: 32rpx;
  margin-right: 14rpx;
  border-radius: 999rpx;
  background: #166534;
  content: '';
}

.section-header .section-title {
  margin-bottom: 0;
}

.batch-summary-card .info-row,
.result-card .info-row {
  align-items: center;
  margin-bottom: 16rpx;
  padding: 0;
}

.batch-summary-card .label,
.result-card .label {
  width: auto;
  color: #7c6f5c;
}

.picker-btn,
.upload-btn {
  height: 78rpx;
  border-radius: 18rpx;
  font-size: 28rpx;
  line-height: 78rpx;
}
</style>
