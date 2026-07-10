param(
    [string]$ApiBase = "http://localhost:8080/api",
    [string]$AiBase = "http://localhost:8001",
    [string]$ImagePath = "D:\BDIS\ai_service\test.jpg",
    [int]$SpeciesId = 1
)

$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "1. Check FastAPI /extract-feature"
curl.exe -s -X POST "$AiBase/extract-feature" -F "file=@$ImagePath"

Write-Host "`n2. Batch extract atlas features"
Invoke-RestMethod -Method POST `
  -Uri "$ApiBase/herb/atlas/feature/batch-extract" `
  -ContentType "application/json" `
  -Body '{"speciesId":null,"forceRefresh":false}' | ConvertTo-Json -Depth 4

Write-Host "`n3. Upload user image"
$upload = curl.exe -s -X POST "$ApiBase/herb/image/upload" `
  -F "file=@$ImagePath" `
  -F "speciesId=$SpeciesId" `
  -F "uploadSource=script" `
  -F "collectorId=1" | ConvertFrom-Json
$imageId = $upload.data.id
Write-Host "imageId=$imageId"

Write-Host "`n4. Extract image feature"
Invoke-RestMethod -Method POST -Uri "$ApiBase/herb/image/$imageId/feature/extract" |
  ConvertTo-Json -Depth 4

Write-Host "`n5. Match local atlas"
Invoke-RestMethod -Method POST `
  -Uri "$ApiBase/herb/image/$imageId/match" `
  -ContentType "application/json" `
  -Body '{"topK":5,"speciesId":null,"forceRefresh":true}' | ConvertTo-Json -Depth 5

Write-Host "`n6. Identify"
$identify = Invoke-RestMethod -Method POST `
  -Uri "$ApiBase/herb/image/$imageId/identify" `
  -ContentType "application/json" `
  -Body '{"forceRefresh":true,"topK":5,"speciesId":null}'
$identify | ConvertTo-Json -Depth 6

Write-Host "`n7. Review"
Invoke-RestMethod -Method PUT `
  -Uri "$ApiBase/herb/identification/$($identify.data.id)/review" `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"finalSpeciesId":1,"finalSpeciesName":"党参","reviewStatus":"confirmed","reviewerId":1001,"reviewerName":"管理员","reviewComment":"脚本人工确认"}' |
  ConvertTo-Json -Depth 5

Write-Host "`n8. Latest identification"
Invoke-RestMethod -Method GET -Uri "$ApiBase/herb/image/$imageId/identification/latest" |
  ConvertTo-Json -Depth 6
