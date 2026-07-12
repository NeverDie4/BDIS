import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const resultPagePath = new URL('../src/pages/image/result.vue', import.meta.url)
const batchDetailPath = new URL('../src/pages/batch/detail.vue', import.meta.url)
const uploadPagePath = new URL('../src/pages/image/upload.vue', import.meta.url)
const uploadRequestPath = new URL(
  '../../backend/src/main/java/com/bdis/modules/mobile/dto/MobileBatchImageUploadRequest.java',
  import.meta.url
)

function extractFunction(source, name) {
  const start = source.indexOf(`function ${name}(`)
  assert.notEqual(start, -1, `未找到函数 ${name}`)

  const bodyStart = source.indexOf('{', start)
  let depth = 0

  for (let index = bodyStart; index < source.length; index += 1) {
    if (source[index] === '{') {
      depth += 1
    } else if (source[index] === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(start, index + 1)
      }
    }
  }

  throw new Error(`函数 ${name} 未正常结束`)
}

test('豆包包装响应中的理由和建议可以被解析', async () => {
  const source = await readFile(resultPagePath, 'utf8')
  const factory = new Function(`
    ${extractFunction(source, 'normalizeResult')}
    ${extractFunction(source, 'parseJson')}
    ${extractFunction(source, 'normalizeDoubao')}
    return normalizeDoubao
  `)
  const normalizeDoubao = factory()
  const rawResult = JSON.stringify({
    code: 200,
    data: {
      results: [
        {
          rank: 1,
          speciesName: '五指毛桃（粗叶榕）',
          confidence: 0.7,
          reason: '叶片形态与五指毛桃特征一致'
        }
      ],
      suggestion: '建议结合根茎和植株被毛复核'
    }
  })

  const result = normalizeDoubao({
    doubaoRecognition: {
      predictedName: '五指毛桃（粗叶榕）',
      confidence: 0.7,
      rawResult
    }
  })

  assert.equal(result.reason, '叶片形态与五指毛桃特征一致')
  assert.equal(result.suggestion, '建议结合根茎和植株被毛复核')
})

test('本地图谱记录不会被误显示为豆包辅助结果', async () => {
  const source = await readFile(resultPagePath, 'utf8')
  const factory = new Function(`
    ${extractFunction(source, 'normalizeResult')}
    ${extractFunction(source, 'parseJson')}
    ${extractFunction(source, 'normalizeDoubao')}
    return normalizeDoubao
  `)
  const normalizeDoubao = factory()

  const result = normalizeDoubao({
    doubaoRecognition: {
      predictedName: '黄连',
      confidence: 0.92,
      recognitionSource: 'local_atlas'
    }
  })

  assert.equal(result, null)
})

test('批次详情不会把私有文件 URL 直接交给 image 标签', async () => {
  const source = await readFile(batchDetailPath, 'utf8')
  const factory = new Function(`
    const imagePreviewUrls = { value: {} }
    const getImagePreviewKey = () => 'image-1'
    const isPrivateFileUrl = (url) => /\\/api\\/files\\//.test(url)
    const resolveImageUrl = (url) => url
    ${extractFunction(source, 'getDisplayImageUrl')}
    return getDisplayImageUrl
  `)
  const getDisplayImageUrl = factory()

  assert.equal(getDisplayImageUrl({ imageUrl: '/api/files/5/content' }), '')
  assert.equal(
    getDisplayImageUrl({ imageUrl: '/api/public-files/5/content' }),
    '/api/public-files/5/content'
  )
})

test('手机上传和后端请求都默认开启自动识别', async () => {
  const [uploadPage, uploadRequest] = await Promise.all([
    readFile(uploadPagePath, 'utf8'),
    readFile(uploadRequestPath, 'utf8')
  ])

  assert.match(uploadPage, /autoIdentify:\s*true/)
  assert.match(uploadRequest, /Boolean autoIdentify\s*=\s*true/)
})

test('批次图片的复核状态和来源使用中文映射', async () => {
  const source = await readFile(batchDetailPath, 'utf8')

  assert.match(
    source,
    /复核状态：\{\{\s*formatStatus\(item\.reviewStatus\s*\|\|\s*'unknown',\s*REVIEW_STATUS_MAP\)\s*\}\}/
  )
  assert.match(
    source,
    /来源：\{\{\s*formatStatus\(item\.resultSource\s*\|\|\s*'unknown',\s*RESULT_SOURCE_MAP\)\s*\}\}/
  )
})
