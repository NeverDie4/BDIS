#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080/api}"
AI_BASE="${AI_BASE:-http://localhost:8001}"
IMAGE_PATH="${IMAGE_PATH:-D:/BDIS/ai_service/test.jpg}"
SPECIES_ID="${SPECIES_ID:-1}"

echo "1. Check FastAPI /extract-feature"
curl -s -X POST "$AI_BASE/extract-feature" -F "file=@$IMAGE_PATH"

echo
echo "2. Batch extract atlas features"
curl -s -X POST "$API_BASE/herb/atlas/feature/batch-extract" \
  -H "Content-Type: application/json" \
  -d '{"speciesId":null,"forceRefresh":false}'

echo
echo "3. Upload user image"
UPLOAD_JSON=$(curl -s -X POST "$API_BASE/herb/image/upload" \
  -F "file=@$IMAGE_PATH" \
  -F "speciesId=$SPECIES_ID" \
  -F "uploadSource=script" \
  -F "collectorId=1")
echo "$UPLOAD_JSON"
IMAGE_ID=$(python -c "import json,sys; print(json.load(sys.stdin)['data']['id'])" <<< "$UPLOAD_JSON")

echo
echo "4. Extract image feature"
curl -s -X POST "$API_BASE/herb/image/$IMAGE_ID/feature/extract"

echo
echo "5. Match local atlas"
curl -s -X POST "$API_BASE/herb/image/$IMAGE_ID/match" \
  -H "Content-Type: application/json" \
  -d '{"topK":5,"speciesId":null,"forceRefresh":true}'

echo
echo "6. Identify"
IDENTIFY_JSON=$(curl -s -X POST "$API_BASE/herb/image/$IMAGE_ID/identify" \
  -H "Content-Type: application/json" \
  -d '{"forceRefresh":true,"topK":5,"speciesId":null}')
echo "$IDENTIFY_JSON"
IDENTIFICATION_ID=$(python -c "import json,sys; print(json.load(sys.stdin)['data']['id'])" <<< "$IDENTIFY_JSON")

echo
echo "7. Review"
curl -s -X PUT "$API_BASE/herb/identification/$IDENTIFICATION_ID/review" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d '{"finalSpeciesId":1,"finalSpeciesName":"党参","reviewStatus":"confirmed","reviewerId":1001,"reviewerName":"管理员","reviewComment":"脚本人工确认"}'

echo
echo "8. Latest identification"
curl -s -X GET "$API_BASE/herb/image/$IMAGE_ID/identification/latest"
