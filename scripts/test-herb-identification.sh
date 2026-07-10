#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080/api}"
FEATURE_BASE="${FEATURE_BASE:-http://localhost:8002}"
IMAGE_PATH="${IMAGE_PATH:-}"
SPECIES_ID="${SPECIES_ID:-1}"
ACCESS_TOKEN="${ACCESS_TOKEN:-${BDIS_ACCESS_TOKEN:-}}"

if [[ -z "$IMAGE_PATH" || ! -f "$IMAGE_PATH" ]]; then
  echo "[failed] Set IMAGE_PATH to an existing image file." >&2
  exit 1
fi

if [[ -z "$ACCESS_TOKEN" ]]; then
  echo "[failed] Set ACCESS_TOKEN or BDIS_ACCESS_TOKEN to a valid JWT." >&2
  exit 1
fi

AUTH_HEADER=(-H "Authorization: Bearer $ACCESS_TOKEN")

read_data_id() {
  node -e '
    let input = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => (input += chunk));
    process.stdin.on("end", () => {
      const response = JSON.parse(input);
      const id = response?.data?.id;
      if (id === undefined || id === null) {
        throw new Error("response does not contain data.id");
      }
      process.stdout.write(String(id));
    });
  '
}

echo "1. Check FastAPI /extract-feature"
curl --fail-with-body --silent --show-error -X POST \
  "$FEATURE_BASE/extract-feature" \
  -F "file=@$IMAGE_PATH"

echo
echo "2. Batch extract atlas features"
curl --fail-with-body --silent --show-error -X POST \
  "$API_BASE/herb/atlas/feature/batch-extract" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"speciesId":null,"forceRefresh":false}'

echo
echo "3. Upload user image"
UPLOAD_JSON=$(curl --fail-with-body --silent --show-error -X POST \
  "$API_BASE/herb/image/upload" \
  "${AUTH_HEADER[@]}" \
  -F "file=@$IMAGE_PATH" \
  -F "speciesId=$SPECIES_ID" \
  -F "uploadSource=script" \
  -F "collectorId=1")
echo "$UPLOAD_JSON"
IMAGE_ID=$(read_data_id <<< "$UPLOAD_JSON")

echo
echo "4. Extract image feature"
curl --fail-with-body --silent --show-error -X POST \
  "$API_BASE/herb/image/$IMAGE_ID/feature/extract" \
  "${AUTH_HEADER[@]}"

echo
echo "5. Match local atlas"
curl --fail-with-body --silent --show-error -X POST \
  "$API_BASE/herb/image/$IMAGE_ID/match" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"topK":5,"speciesId":null,"forceRefresh":true}'

echo
echo "6. Identify"
IDENTIFY_JSON=$(curl --fail-with-body --silent --show-error -X POST \
  "$API_BASE/herb/image/$IMAGE_ID/identify" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"forceRefresh":true,"topK":5,"speciesId":null}')
echo "$IDENTIFY_JSON"
IDENTIFICATION_ID=$(read_data_id <<< "$IDENTIFY_JSON")

echo
echo "7. Review"
curl --fail-with-body --silent --show-error -X PUT \
  "$API_BASE/herb/identification/$IDENTIFICATION_ID/review" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d '{"finalSpeciesId":1,"finalSpeciesName":"党参","reviewStatus":"confirmed","reviewerId":1001,"reviewerName":"管理员","reviewComment":"脚本人工确认"}'

echo
echo "8. Latest identification"
curl --fail-with-body --silent --show-error -X GET \
  "$API_BASE/herb/image/$IMAGE_ID/identification/latest" \
  "${AUTH_HEADER[@]}"
