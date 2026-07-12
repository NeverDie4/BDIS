import os
import uuid
import base64
import shutil
import sys
import json
import time
from datetime import datetime

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.concurrency import run_in_threadpool
from openai import OpenAI

if hasattr(sys.stdout, "reconfigure"):
  sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
  sys.stderr.reconfigure(encoding="utf-8")

app = FastAPI()

MODEL_ID = "doubao-seed-2-0-lite-260428"
UPLOAD_DIR = "uploads"
MAX_FILE_SIZE = 20 * 1024 * 1024
ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "webp"}
os.makedirs(UPLOAD_DIR, exist_ok=True)


def image_to_base64(image_path):
  with open(image_path, "rb") as f:
    return base64.b64encode(f.read()).decode("utf-8")


def call_doubao(image_path):
  api_key = os.getenv("ARK_API_KEY")
  if not api_key:
    raise RuntimeError("ARK_API_KEY is not configured")
  client = OpenAI(
    api_key=api_key,
    base_url="https://ark.cn-beijing.volces.com/api/v3"
  )
  image_base64 = image_to_base64(image_path)

  prompt = """
你是中药材图像识别助手。请根据图片判断中药材品种。

要求：
1. 尽可能快地回答。
2. 如果无法确定，speciesName 返回“不确定”。
3. 只返回 JSON，不要输出多余文字。
4. confidence 范围为 0 到 1。
5. needReview 表示是否需要人工审核。
6. reason 和 suggestion 必须返回且不能为空。
7. reason 必须说明图片中的具体判断依据，例如叶形、叶脉、颜色、纹理或植株形态，控制在 50 字以内。
8. suggestion 必须给出下一步复核建议，例如需要补拍的部位或需要核对的特征，控制在 50 字以内。

返回格式：
{
  "results": [
    {
      "rank": 1,
      "speciesName": "品种名称",
      "confidence": 0.86,
      "reason": "判断依据"
    }
  ],
  "needReview": true,
  "suggestion": "建议"
}
"""

  response = client.chat.completions.create(
    model=MODEL_ID,
    messages=[
      {
        "role": "user",
        "content": [
          {
            "type": "text",
            "text": prompt
          },
          {
            "type": "image_url",
            "image_url": {
              "url": f"data:image/jpeg;base64,{image_base64}"
            }
          }
        ]
      }
    ],
    temperature=0.1
  )

  return response.choices[0].message.content


def log_recognition_result(original_filename, result_json, t0, t1, t2):
  results = result_json.get("results") if isinstance(result_json, dict) else None
  top_result = results[0] if isinstance(results, list) and results else {}
  species_name = top_result.get("speciesName") or "未识别"
  confidence = top_result.get("confidence")
  reason = top_result.get("reason") or "无"
  suggestion = result_json.get("suggestion") or "无"
  need_review = result_json.get("needReview")
  print(
    f"[识别完成] 时间={datetime.now().strftime('%Y-%m-%d %H:%M:%S')} "
    f"文件={original_filename} 保存耗时={t1 - t0:.3f}秒 "
    f"豆包耗时={t2 - t1:.3f}秒 总耗时={t2 - t0:.3f}秒 "
    f"识别结果={species_name} 置信度={confidence if confidence is not None else '无'} "
    f"需要复核={need_review} 理由={reason} 建议={suggestion}",
    flush=True
  )


@app.post("/recognize")
async def recognize(file: UploadFile = File(...)):
  t0 = time.time()

  original_filename = file.filename or "upload.jpg"
  ext = original_filename.split(".")[-1].lower()
  if ext not in ALLOWED_EXTENSIONS:
    raise HTTPException(status_code=400, detail="unsupported image format")

  filename = f"{uuid.uuid4()}.{ext}"
  file_path = os.path.join(UPLOAD_DIR, filename)
  print(
    f"[识别开始] 时间={datetime.now().strftime('%Y-%m-%d %H:%M:%S')} "
    f"文件={original_filename}",
    flush=True
  )

  try:
    with open(file_path, "wb") as buffer:
      shutil.copyfileobj(file.file, buffer)
    if os.path.getsize(file_path) > MAX_FILE_SIZE:
      raise HTTPException(status_code=413, detail="image is too large")
    t1 = time.time()
    result = await run_in_threadpool(call_doubao, file_path)
    t2 = time.time()
    try:
      result_json = json.loads(result)
    except json.JSONDecodeError:
      result_json = {
        "results": [],
        "needReview": True,
        "suggestion": "模型返回格式异常，建议人工审核"
      }
    log_recognition_result(original_filename, result_json, t0, t1, t2)
    return {
      "code": 200,
      "msg": "识别成功",
      "data": result_json,
      "timeCost": {
        "saveFileSeconds": round(t1 - t0, 3),
        "doubaoSeconds": round(t2 - t1, 3),
        "totalSeconds": round(t2 - t0, 3)
      }
    }
  except Exception as error:
    print(
      f"[识别失败] 时间={datetime.now().strftime('%Y-%m-%d %H:%M:%S')} "
      f"文件={original_filename} 总耗时={time.time() - t0:.3f}秒 错误={error}",
      flush=True
    )
    raise
  finally:
    if os.path.exists(file_path):
      os.remove(file_path)


@app.get("/health")
def health():
  return {"status": "UP", "configured": bool(os.getenv("ARK_API_KEY"))}
