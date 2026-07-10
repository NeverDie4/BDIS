param(
    [string]$ApiBase = "http://localhost:8080/api",
    [string]$FeatureBase = "http://localhost:8002",
    [string]$ImagePath = $env:IMAGE_PATH,
    [int]$SpeciesId = 1,
    [string]$AccessToken = $env:BDIS_ACCESS_TOKEN
)

$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if ([string]::IsNullOrWhiteSpace($ImagePath) -or -not (Test-Path -LiteralPath $ImagePath -PathType Leaf)) {
    throw "Set -ImagePath or IMAGE_PATH to an existing image file."
}

if ([string]::IsNullOrWhiteSpace($AccessToken)) {
    $AccessToken = $env:ACCESS_TOKEN
}

if ([string]::IsNullOrWhiteSpace($AccessToken)) {
    throw "Set -AccessToken, BDIS_ACCESS_TOKEN, or ACCESS_TOKEN to a valid JWT."
}

$Headers = @{ Authorization = "Bearer $AccessToken" }

Write-Host "1. Check FastAPI /extract-feature"
curl.exe --fail-with-body -sS -X POST "$FeatureBase/extract-feature" -F "file=@$ImagePath"

Write-Host "`n2. Batch extract atlas features"
Invoke-RestMethod -Method POST `
  -Uri "$ApiBase/herb/atlas/feature/batch-extract" `
  -Headers $Headers `
  -ContentType "application/json" `
  -Body '{"speciesId":null,"forceRefresh":false}' | ConvertTo-Json -Depth 4

Write-Host "`n3. Upload user image"
$upload = curl.exe --fail-with-body -sS -X POST "$ApiBase/herb/image/upload" `
  -H "Authorization: Bearer $AccessToken" `
  -F "file=@$ImagePath" `
  -F "speciesId=$SpeciesId" `
  -F "uploadSource=script" `
  -F "collectorId=1" | ConvertFrom-Json
$imageId = $upload.data.id
Write-Host "imageId=$imageId"

Write-Host "`n4. Extract image feature"
Invoke-RestMethod -Method POST -Uri "$ApiBase/herb/image/$imageId/feature/extract" -Headers $Headers |
  ConvertTo-Json -Depth 4

Write-Host "`n5. Match local atlas"
Invoke-RestMethod -Method POST `
  -Uri "$ApiBase/herb/image/$imageId/match" `
  -Headers $Headers `
  -ContentType "application/json" `
  -Body '{"topK":5,"speciesId":null,"forceRefresh":true}' | ConvertTo-Json -Depth 5

Write-Host "`n6. Identify"
$identify = Invoke-RestMethod -Method POST `
  -Uri "$ApiBase/herb/image/$imageId/identify" `
  -Headers $Headers `
  -ContentType "application/json" `
  -Body '{"forceRefresh":true,"topK":5,"speciesId":null}'
$identify | ConvertTo-Json -Depth 6

Write-Host "`n7. Review"
Invoke-RestMethod -Method PUT `
  -Uri "$ApiBase/herb/identification/$($identify.data.id)/review" `
  -Headers $Headers `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"finalSpeciesId":1,"finalSpeciesName":"党参","reviewStatus":"confirmed","reviewerId":1001,"reviewerName":"管理员","reviewComment":"脚本人工确认"}' |
  ConvertTo-Json -Depth 5

Write-Host "`n8. Latest identification"
Invoke-RestMethod -Method GET -Uri "$ApiBase/herb/image/$imageId/identification/latest" -Headers $Headers |
  ConvertTo-Json -Depth 6
