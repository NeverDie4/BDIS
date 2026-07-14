export const TASK_STATUS_MAP = {
  draft: '草稿',
  published: '已发布',
  in_progress: '进行中',
  completed: '已完成',
  cancelled: '已取消'
}

export const BATCH_STATUS_MAP = {
  draft: '草稿',
  collecting: '采集中',
  submitted: '已提交',
  identifying: '识别中',
  reviewing: '复核中',
  confirmed: '已确认',
  archived: '已归档',
  cancelled: '已取消'
}

export const IMAGE_ROLE_MAP = {
  leaf: '叶片',
  root: '根部',
  stem: '茎',
  flower: '花',
  fruit: '果实',
  whole_plant: '整株',
  medicinal_part: '药用部位',
  environment: '生长环境',
  other: '其他'
}

export const IMAGE_ROLE_OPTIONS = [
  { label: '叶片', value: 'leaf' },
  { label: '根部', value: 'root' },
  { label: '茎', value: 'stem' },
  { label: '花', value: 'flower' },
  { label: '果实', value: 'fruit' },
  { label: '整株', value: 'whole_plant' },
  { label: '药用部位', value: 'medicinal_part' },
  { label: '生长环境', value: 'environment' },
  { label: '其他', value: 'other' }
]

export const GROWTH_STAGE_OPTIONS = [
  { label: '幼苗期', value: 'seedling' },
  { label: '生长期', value: 'growth' },
  { label: '成熟期', value: 'mature' },
  { label: '未知', value: 'unknown' }
]

export const HEALTH_STATUS_OPTIONS = [
  { label: '健康', value: 'healthy' },
  { label: '疑似病害', value: 'disease' },
  { label: '未知', value: 'unknown' }
]

export const RESULT_SOURCE_MAP = {
  local_match: '本地图谱匹配',
  doubao_review: '大模型辅助识别',
  manual_review: '人工复核',
  unknown: '未知'
}

export const REVIEW_STATUS_MAP = {
  pending: '待复核',
  confirmed: '已确认',
  rejected: '已驳回',
  unknown: '未知'
}

export const MATCH_RESULT_MAP = {
  matched: '匹配成功',
  uncertain: '候选不确定',
  low_confidence: '低置信度',
  failed: '识别失败',
  unknown: '未知'
}

export const QUALITY_LEVEL_MAP = {
  excellent: '优秀',
  good: '良好',
  normal: '一般',
  poor: '较差',
  unknown: '未知'
}
