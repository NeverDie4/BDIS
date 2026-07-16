<template>
  <view class="page growth-form-page">
    <view class="herb-card intro-card">
      <text class="page-title">{{ pageTitle }}</text>
      <text class="page-subtitle">每个采集批次仅保存一条结构化生长记录</text>
    </view>

    <view class="herb-card form-card location-card">
      <text class="section-title">采集位置</text>
      <view v-if="hasLocation" class="location-summary">
        <view>
          <text class="location-label">经度</text>
          <text class="location-value">{{ form.longitude }}</text>
        </view>
        <view>
          <text class="location-label">纬度</text>
          <text class="location-value">{{ form.latitude }}</text>
        </view>
      </view>
      <text v-else class="location-empty">尚未获取当前位置，保存后地图将不会生成阶段标记。</text>
      <text v-if="locationAccuracy" class="location-accuracy">定位精度约 {{ locationAccuracy }} 米</text>
      <button
        v-if="!readOnly"
        class="secondary-btn location-btn"
        :loading="locating"
        :disabled="locating"
        @click="captureLocation"
      >
        {{ hasLocation ? '重新定位' : '获取当前位置' }}
      </button>
      <text class="location-help">使用 WGS84 坐标，仅用于本次生长观测的地图归档。</text>
    </view>

    <view class="herb-card form-card">
      <text class="section-title">基础信息</text>
      <label class="field">
        <text class="field-label required">生长阶段</text>
        <input
          v-model.trim="form.growthStage"
          class="field-input"
          :disabled="readOnly"
          maxlength="50"
          placeholder="例如：苗期、花期、成熟期"
        />
      </label>
      <view class="field">
        <text class="field-label required">采集时间</text>
        <view class="datetime-row">
          <picker mode="date" :disabled="readOnly" :value="collectDate" @change="changeDate">
            <view class="picker-value">{{ collectDate }}</view>
          </picker>
          <picker mode="time" :disabled="readOnly" :value="collectTime" @change="changeTime">
            <view class="picker-value">{{ collectTime }}</view>
          </picker>
        </view>
      </view>
    </view>

    <view class="herb-card form-card">
      <text class="section-title">生长指标</text>
      <view class="field-grid">
        <label v-for="item in growthNumberFields" :key="item.key" class="field compact-field">
          <text class="field-label">{{ item.label }}</text>
          <view class="input-with-unit">
            <input
              v-model.trim="form[item.key]"
              class="field-input"
              :disabled="readOnly"
              type="digit"
              :placeholder="item.placeholder"
            />
            <text class="unit">{{ item.unit }}</text>
          </view>
        </label>
      </view>
      <label class="field">
        <text class="field-label">叶色</text>
        <input v-model.trim="form.leafColor" class="field-input" :disabled="readOnly" maxlength="64" placeholder="记录叶片颜色" />
      </label>
      <label class="field">
        <text class="field-label">开花情况</text>
        <input v-model.trim="form.floweringStatus" class="field-input" :disabled="readOnly" maxlength="100" placeholder="记录花蕾、盛花或落花情况" />
      </label>
      <label class="field">
        <text class="field-label">生长评价</text>
        <textarea v-model.trim="form.growthEvaluation" class="field-textarea" :disabled="readOnly" maxlength="500" placeholder="简要评价本次生长状态" />
      </label>
    </view>

    <view class="herb-card form-card">
      <text class="section-title">环境指标</text>
      <view class="field-grid">
        <label v-for="item in environmentFields" :key="item.key" class="field compact-field">
          <text class="field-label">{{ item.label }}</text>
          <view class="input-with-unit">
            <input
              v-model.trim="form[item.key]"
              class="field-input"
              :disabled="readOnly"
              type="digit"
              :placeholder="item.placeholder"
            />
            <text class="unit">{{ item.unit }}</text>
          </view>
        </label>
      </view>
    </view>

    <view class="herb-card form-card">
      <text class="section-title">备注</text>
      <textarea v-model.trim="form.remark" class="field-textarea remark-input" :disabled="readOnly" maxlength="500" placeholder="补充本次采集现场情况" />
    </view>

    <view v-if="!readOnly" class="bottom-actions">
      <button class="secondary-btn action-btn" :disabled="submitting" @click="goBack">取消</button>
      <button class="primary-btn action-btn" :loading="submitting" :disabled="submitting" @click="submitForm">
        {{ recordId ? '保存修改' : '创建记录' }}
      </button>
    </view>
    <AssistantFloat />
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  createBatchGrowthRecord,
  getBatchGrowthRecord,
  getGrowthRecordDetail,
  updateBatchGrowthRecord
} from '../../api/mobileGrowthRecordApi'

const batchId = ref('')
const recordId = ref('')
const readOnly = ref(false)
const submitting = ref(false)
const locating = ref(false)
const locationAccuracy = ref('')
const collectDate = ref('')
const collectTime = ref('')

const form = reactive({
  growthStage: '',
  collectedAt: '',
  longitude: '',
  latitude: '',
  plantHeight: '',
  leafColor: '',
  stemDiameter: '',
  floweringStatus: '',
  growthEvaluation: '',
  temperature: '',
  humidity: '',
  soilMoisture: '',
  soilPh: '',
  light: '',
  remark: ''
})

const growthNumberFields = [
  { key: 'plantHeight', label: '株高', unit: 'cm', placeholder: '请输入株高' },
  { key: 'stemDiameter', label: '茎粗', unit: 'mm', placeholder: '请输入茎粗' }
]

const environmentFields = [
  { key: 'temperature', label: '温度', unit: '℃', placeholder: '请输入温度' },
  { key: 'humidity', label: '湿度', unit: '%', placeholder: '请输入湿度' },
  { key: 'soilMoisture', label: '土壤湿度', unit: '%', placeholder: '请输入土壤湿度' },
  { key: 'soilPh', label: '土壤 pH', unit: 'pH', placeholder: '请输入 pH' },
  { key: 'light', label: '光照', unit: 'lx', placeholder: '请输入光照' }
]

const pageTitle = computed(() => {
  if (readOnly.value) return '查看生长记录'
  return recordId.value ? '编辑生长记录' : '填写生长记录'
})
const hasLocation = computed(() => form.longitude !== '' && form.latitude !== '')

onLoad(async (options) => {
  batchId.value = options.batchId || ''
  recordId.value = options.recordId || ''
  readOnly.value = options.readOnly === '1'
  setDefaultCollectedAt()

  if (!batchId.value) {
    showToast('批次 ID 不存在')
    return
  }

  if (recordId.value) {
    await loadRecord(recordId.value)
    return
  }

  await detectExistingRecord()
})

function setDefaultCollectedAt() {
  const now = new Date()
  collectDate.value = formatDate(now)
  collectTime.value = `${pad(now.getHours())}:${pad(now.getMinutes())}`
  syncCollectedAt()
}

async function detectExistingRecord() {
  try {
    const existing = await getBatchGrowthRecord(batchId.value)
    if (!existing?.id) return
    recordId.value = String(existing.id)
    fillForm(existing)
    showToast('当前批次已有生长记录，已切换为编辑模式')
  } catch (error) {
    if (!isNotFound(error)) {
      showToast(getErrorMessage(error, '生长记录检查失败'))
    }
  }
}

async function loadRecord(id) {
  try {
    fillForm(await getGrowthRecordDetail(id))
  } catch (error) {
    showToast(getErrorMessage(error, '生长记录加载失败'))
  }
}

function fillForm(record = {}) {
  for (const key of Object.keys(form)) {
    if (key === 'collectedAt') continue
    form[key] = record[key] === undefined || record[key] === null ? '' : String(record[key])
  }
  const value = record.collectedAt || record.collectTime
  if (value) {
    const [datePart, timePart = '00:00'] = String(value).replace(' ', 'T').split('T')
    collectDate.value = datePart
    collectTime.value = timePart.slice(0, 5)
  }
  syncCollectedAt()
}

function changeDate(event) {
  collectDate.value = event.detail.value
  syncCollectedAt()
}

function changeTime(event) {
  collectTime.value = event.detail.value
  syncCollectedAt()
}

function syncCollectedAt() {
  form.collectedAt = `${collectDate.value}T${collectTime.value}:00`
}

function captureLocation() {
  locating.value = true
  uni.getLocation({
    type: 'wgs84',
    isHighAccuracy: true,
    highAccuracyExpireTime: 5000,
    success(result) {
      const longitude = Number(result.longitude)
      const latitude = Number(result.latitude)
      if (!isValidCoordinate(longitude, latitude)) {
        showToast('定位结果无效，请移动到开阔区域后重试')
        return
      }
      form.longitude = longitude.toFixed(7)
      form.latitude = latitude.toFixed(7)
      locationAccuracy.value = Number.isFinite(Number(result.accuracy))
        ? String(Math.round(Number(result.accuracy)))
        : ''
      showToast('定位成功', 'success')
    },
    fail(error) {
      showToast(getErrorMessage(error, '定位失败，请检查定位权限后重试'))
    },
    complete() {
      locating.value = false
    }
  })
}

async function submitForm() {
  if (!validateForm()) return
  submitting.value = true
  try {
    const payload = buildPayload()
    if (recordId.value) {
      await updateBatchGrowthRecord(batchId.value, recordId.value, payload)
      showToast('修改成功', 'success')
    } else {
      const created = await createBatchGrowthRecord(batchId.value, payload)
      recordId.value = String(created.id)
      showToast('创建成功', 'success')
    }
    setTimeout(() => uni.navigateBack(), 450)
  } catch (error) {
    const message = getErrorMessage(error, '保存失败，请稍后重试')
    if (message.includes('已存在生长记录')) {
      await detectExistingRecord()
      showToast('当前批次已存在生长记录，请直接编辑')
    } else {
      showToast(message)
    }
  } finally {
    submitting.value = false
  }
}

function validateForm() {
  if (!batchId.value) return showValidation('批次 ID 不存在')
  if (!form.growthStage) return showValidation('请填写生长阶段')
  if (!form.collectedAt) return showValidation('请选择采集时间')
  if ((form.longitude === '') !== (form.latitude === '')) {
    return showValidation('经纬度必须同时填写')
  }
  if (hasLocation.value && !isValidCoordinate(Number(form.longitude), Number(form.latitude))) {
    return showValidation('经纬度数值不正确')
  }

  const validations = [
    ['plantHeight', 0, null, '株高'],
    ['stemDiameter', 0, null, '茎粗'],
    ['temperature', -80, 80, '温度'],
    ['humidity', 0, 100, '湿度'],
    ['soilMoisture', 0, 100, '土壤湿度'],
    ['soilPh', 0, 14, '土壤 pH'],
    ['light', 0, null, '光照']
  ]
  for (const [key, min, max, label] of validations) {
    const value = form[key]
    if (value === '') continue
    const number = Number(value)
    if (!Number.isFinite(number) || number < min || (max !== null && number > max)) {
      return showValidation(`${label}数值不正确`)
    }
  }
  return true
}

function buildPayload() {
  const payload = {
    growthStage: form.growthStage,
    collectedAt: form.collectedAt,
    leafColor: emptyToUndefined(form.leafColor),
    floweringStatus: emptyToUndefined(form.floweringStatus),
    growthEvaluation: emptyToUndefined(form.growthEvaluation),
    remark: emptyToUndefined(form.remark),
    deviceType: 'mobile',
    dataSource: 'manual'
  }
  for (const key of ['plantHeight', 'stemDiameter', 'temperature', 'humidity', 'soilMoisture', 'soilPh', 'light']) {
    payload[key] = form[key] === '' ? undefined : Number(form[key])
  }
  if (hasLocation.value) {
    payload.longitude = Number(form.longitude)
    payload.latitude = Number(form.latitude)
  }
  return payload
}

function isValidCoordinate(longitude, latitude) {
  return Number.isFinite(longitude) && Number.isFinite(latitude) &&
    longitude >= -180 && longitude <= 180 && latitude >= -90 && latitude <= 90
}

function emptyToUndefined(value) {
  return value === '' ? undefined : value
}

function isNotFound(error) {
  return error?.code === 404 || error?.code === 'NOT_FOUND'
}

function getErrorMessage(error, fallback) {
  return error?.message || error?.msg || fallback
}

function showValidation(message) {
  showToast(message)
  return false
}

function showToast(title, icon = 'none') {
  uni.showToast({ title, icon })
}

function goBack() {
  uni.navigateBack()
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
</script>

<style scoped>
.growth-form-page {
  box-sizing: border-box;
  min-height: 100vh;
  padding: 24rpx 24rpx calc(190rpx + env(safe-area-inset-bottom));
  background: #f7f1e6;
}

.herb-card {
  box-sizing: border-box;
  margin-bottom: 24rpx;
  padding: 28rpx;
  border: 1rpx solid #eadfcd;
  border-radius: 24rpx;
  background: rgba(255, 250, 242, 0.97);
  box-shadow: 0 10rpx 28rpx rgba(63, 45, 24, 0.06);
}

.intro-card {
  padding: 30rpx;
}

.page-title,
.page-subtitle {
  display: block;
}

.page-title {
  color: #0f3d2e;
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1.4;
}

.page-subtitle {
  margin-top: 10rpx;
  color: #7c6f5c;
  font-size: 25rpx;
  line-height: 1.65;
}

.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  color: #0f5132;
  font-size: 31rpx;
  font-weight: 700;
}

.section-title::before {
  width: 8rpx;
  height: 30rpx;
  margin-right: 14rpx;
  border-radius: 999rpx;
  background: #166534;
  content: '';
}

.field {
  display: block;
  margin-bottom: 24rpx;
}

.field:last-child {
  margin-bottom: 0;
}

.field-label {
  display: block;
  margin-bottom: 10rpx;
  color: #5f5548;
  font-size: 26rpx;
  font-weight: 600;
}

.required::after {
  margin-left: 6rpx;
  color: #b94a48;
  content: '*';
}

.field-input,
.picker-value,
.field-textarea {
  box-sizing: border-box;
  width: 100%;
  border: 1rpx solid #ded2bf;
  border-radius: 14rpx;
  background: #fffef9;
  color: #273a2d;
  font-size: 27rpx;
}

.field-input,
.picker-value {
  height: 84rpx;
  padding: 0 22rpx;
  line-height: 84rpx;
}

.field-textarea {
  min-height: 180rpx;
  padding: 20rpx 22rpx;
  line-height: 1.65;
}

.datetime-row,
.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.datetime-row picker {
  min-width: 0;
}

.compact-field {
  min-width: 0;
}

.input-with-unit {
  position: relative;
}

.input-with-unit .field-input {
  padding-right: 68rpx;
}

.unit {
  position: absolute;
  top: 0;
  right: 20rpx;
  height: 84rpx;
  color: #8b7e6b;
  font-size: 23rpx;
  line-height: 84rpx;
}

.remark-input {
  margin-bottom: 0;
}

.location-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.location-summary > view {
  min-width: 0;
  padding: 20rpx;
  border: 1rpx solid #d9e4d8;
  border-radius: 14rpx;
  background: #f3f8f1;
}

.location-label,
.location-value,
.location-empty,
.location-accuracy,
.location-help {
  display: block;
}

.location-label {
  color: #728074;
  font-size: 23rpx;
}

.location-value {
  margin-top: 8rpx;
  color: #174b33;
  font-size: 27rpx;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.location-empty,
.location-help,
.location-accuracy {
  color: #756b5c;
  font-size: 24rpx;
  line-height: 1.6;
}

.location-accuracy {
  margin-top: 14rpx;
  color: #4f6f58;
}

.location-btn {
  height: 78rpx;
  margin: 20rpx 0 0;
  border-radius: 14rpx;
  font-size: 27rpx;
  line-height: 78rpx;
}

.location-help {
  margin-top: 14rpx;
}

.bottom-actions {
  position: fixed;
  z-index: 20;
  right: 0;
  bottom: 0;
  left: 0;
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 18rpx;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #e4d8c6;
  background: rgba(255, 250, 242, 0.96);
}

.action-btn {
  height: 84rpx;
  margin: 0;
  border-radius: 14rpx;
  font-size: 28rpx;
  line-height: 84rpx;
}

.primary-btn {
  border: 0;
  background: #0f5132;
  color: #ffffff;
}

.secondary-btn {
  border: 1rpx solid #9eb5a3;
  background: #f2f7ef;
  color: #0f5132;
}

.field-input[disabled],
.field-textarea[disabled] {
  background: #f2ede3;
  color: #5f665f;
}
</style>
