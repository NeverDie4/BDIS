import os
import uuid
import shutil
import time
import torch
import torch.nn as nn
import torch.nn.functional as F
from fastapi import FastAPI, UploadFile, File
from torchvision import models
from torchvision.models import ResNet50_Weights
from PIL import Image

app = FastAPI()

UPLOAD_DIR = "feature_uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

MODEL_VERSION = "resnet50-imagenet-v1"

# 1. 加载 ResNet50 预训练模型
weights = ResNet50_Weights.DEFAULT
base_model = models.resnet50(weights=weights)

# 2. 去掉最后分类层，只保留特征提取部分
feature_model = nn.Sequential(*list(base_model.children())[:-1])
feature_model.eval()

# 3. 使用官方预处理方式
preprocess = weights.transforms()


def extract_feature(image_path):
    image = Image.open(image_path).convert("RGB")
    image_tensor = preprocess(image).unsqueeze(0)

    with torch.no_grad():
        feature = feature_model(image_tensor)

    # shape: [1, 2048, 1, 1] -> [2048]
    feature = feature.squeeze()

    # 归一化，方便后面算余弦相似度
    feature = F.normalize(feature, dim=0)

    return feature.cpu().tolist()


@app.post("/feature")
async def get_feature(file: UploadFile = File(...)):
    original_filename = file.filename or "upload.jpg"
    ext = original_filename.split(".")[-1].lower()

    if ext not in ["jpg", "jpeg", "png", "webp"]:
        ext = "jpg"

    filename = f"{uuid.uuid4()}.{ext}"
    file_path = os.path.join(UPLOAD_DIR, filename)

    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    vector = extract_feature(file_path)

    return {
        "code": 200,
        "msg": "特征提取成功",
        "data": {
            "modelVersion": MODEL_VERSION,
            "dim": len(vector),
            "vector": vector
        }
    }
@app.post("/extract-feature")
async def extract_feature_api(file: UploadFile = File(...)):
    started_at = time.perf_counter()
    try:
        if file is None or not file.filename:
            return {
                "success": False,
                "featureVector": [],
                "dimension": 0,
                "modelName": "resnet50",
                "modelVersion": MODEL_VERSION,
                "message": "file is required",
                "elapsedSeconds": round(time.perf_counter() - started_at, 3),
            }
        result = await get_feature(file)
        data = result["data"]
        elapsed = round(time.perf_counter() - started_at, 3)
        print(
            f"[extract-feature] file={file.filename} model=resnet50 "
            f"dimension={data['dim']} elapsed={elapsed}s"
        )
        return {
            "success": True,
            "featureVector": data["vector"],
            "dimension": data["dim"],
            "modelName": "resnet50",
            "modelVersion": data["modelVersion"],
            "message": "feature extracted successfully",
            "elapsedSeconds": elapsed,
        }
    except Exception as exc:
        elapsed = round(time.perf_counter() - started_at, 3)
        print(f"[extract-feature] file={file.filename} failed elapsed={elapsed}s error={exc}")
        return {
            "success": False,
            "featureVector": [],
            "dimension": 0,
            "modelName": "resnet50",
            "modelVersion": MODEL_VERSION,
            "message": str(exc),
            "elapsedSeconds": elapsed,
        }
