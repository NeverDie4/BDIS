param(
  [string]$BaseUrl = "http://localhost:8080/api",
  [string]$CollectorUsername = "collector",
  [string]$CollectorPassword = "password",
  [string]$ReviewerUsername = "reviewer",
  [string]$ReviewerPassword = "password",
  [string]$AdminUsername = "admin",
  [string]$AdminPassword = "password"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Write-Step {
  param([string]$Text)
  Write-Host ""
  Write-Host "== $Text ==" -ForegroundColor Cyan
}

function Login {
  param([string]$Username, [string]$Password)
  $body = @{ username = $Username; password = $Password } | ConvertTo-Json
  $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/sessions" -ContentType "application/json; charset=utf-8" -Body $body
  return @{ Authorization = "Bearer $($response.data.accessToken)" }
}

function Invoke-Bdis {
  param(
    [string]$Method,
    [string]$Path,
    [hashtable]$Headers,
    [object]$Body = $null
  )
  $uri = "$BaseUrl$Path"
  if ($null -eq $Body) {
    return Invoke-RestMethod -Method $Method -Uri $uri -Headers $Headers
  }
  $json = $Body | ConvertTo-Json -Depth 8
  return Invoke-RestMethod -Method $Method -Uri $uri -Headers $Headers -ContentType "application/json; charset=utf-8" -Body $json
}

Write-Step "登录采集员、审核员、管理员"
$collectorHeaders = Login -Username $CollectorUsername -Password $CollectorPassword
$reviewerHeaders = Login -Username $ReviewerUsername -Password $ReviewerPassword
$adminHeaders = Login -Username $AdminUsername -Password $AdminPassword
Write-Host "登录完成"

Write-Step "查询可用药材"
$herbs = Invoke-Bdis -Method Get -Path "/herb/species/list" -Headers $collectorHeaders
if (-not $herbs.data -or $herbs.data.Count -eq 0) {
  throw "没有可用药材，请先初始化 herb_species 数据。"
}
$speciesId = $herbs.data[0].id
Write-Host "使用药材 ID: $speciesId"

Write-Step "创建生长采集记录"
$createBody = @{
  speciesId = $speciesId
  growthStage = "seedling"
  soilType = "loam"
  temperature = 22.5
  humidity = 66
  weather = "sunny"
  sampleWeight = 12.3
  remark = "PowerShell 审核链路测试记录"
}
$created = Invoke-Bdis -Method Post -Path "/growth-records" -Headers $collectorHeaders -Body $createBody
$recordId = $created.data.id
Write-Host "创建记录 ID: $recordId，状态: $($created.data.reviewStatus)"

Write-Step "提交审核 draft -> submitted"
$submitted = Invoke-Bdis -Method Put -Path "/growth-records/$recordId/submit" -Headers $collectorHeaders
Write-Host "状态: $($submitted.data.reviewStatus)"

Write-Step "查询审核历史和溯源事件"
$history = Invoke-Bdis -Method Get -Path "/growth-records/$recordId/audit-history" -Headers $collectorHeaders
$trace = Invoke-Bdis -Method Get -Path "/growth-records/$recordId/trace-events" -Headers $collectorHeaders
Write-Host "审核历史条数: $($history.data.Count)"
Write-Host "溯源事件条数: $($trace.data.Count)"

Write-Step "审核驳回 submitted -> rejected"
$rejected = Invoke-Bdis -Method Put -Path "/growth-records/$recordId/reject" -Headers $reviewerHeaders -Body @{ comment = "图片不清晰，请补充采集地点说明" }
Write-Host "状态: $($rejected.data.reviewStatus)"

Write-Step "修改驳回记录"
$updateBody = $createBody.Clone()
$updateBody.remark = "已补充采集地点说明，重新提交审核"
$updated = Invoke-Bdis -Method Put -Path "/growth-records/$recordId" -Headers $collectorHeaders -Body $updateBody
Write-Host "修改完成，状态: $($updated.data.reviewStatus)"

Write-Step "重新提交 rejected -> submitted"
$resubmitted = Invoke-Bdis -Method Put -Path "/growth-records/$recordId/submit" -Headers $collectorHeaders
Write-Host "状态: $($resubmitted.data.reviewStatus)"

Write-Step "审核通过 submitted -> approved"
$approved = Invoke-Bdis -Method Put -Path "/growth-records/$recordId/approve" -Headers $reviewerHeaders -Body @{ comment = "数据完整，审核通过" }
Write-Host "状态: $($approved.data.reviewStatus)"

Write-Step "管理员归档 approved -> archived"
$archived = Invoke-Bdis -Method Put -Path "/growth-records/$recordId/archive" -Headers $adminHeaders -Body @{ comment = "审核完成，归档保存" }
Write-Host "状态: $($archived.data.reviewStatus)"

Write-Step "再次查询审核历史和溯源事件"
$finalHistory = Invoke-Bdis -Method Get -Path "/growth-records/$recordId/audit-history" -Headers $adminHeaders
$finalTrace = Invoke-Bdis -Method Get -Path "/growth-records/$recordId/trace-events" -Headers $adminHeaders
Write-Host "最终审核历史条数: $($finalHistory.data.Count)"
Write-Host "最终溯源事件条数: $($finalTrace.data.Count)"

Write-Step "尝试修改 archived 记录，确认失败"
try {
  $updateBody.remark = "归档后不应允许修改"
  Invoke-Bdis -Method Put -Path "/growth-records/$recordId" -Headers $collectorHeaders -Body $updateBody | Out-Null
  throw "归档记录修改竟然成功，测试失败。"
} catch {
  Write-Host "归档后修改已被拒绝: $($_.Exception.Message)"
}

Write-Step "审核链路脚本执行完成"
Write-Host "记录 ID: $recordId"
