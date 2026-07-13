param(
  [string]$BaseUrl = "http://localhost:8080/api",
  [string]$AdminUsername = "admin",
  [string]$AdminPassword = "password",
  [long]$RecordId = 0
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

function Invoke-BdisRaw {
  param(
    [string]$Method,
    [string]$Path,
    [hashtable]$Headers = @{}
  )
  return Invoke-WebRequest -Method $Method -Uri "$BaseUrl$Path" -Headers $Headers -SkipHttpErrorCheck
}

Write-Step "登录管理员"
$adminHeaders = Login -Username $AdminUsername -Password $AdminPassword
Write-Host "管理员登录完成"

if ($RecordId -le 0) {
  Write-Step "选择一条已有 growth_record"
  $page = Invoke-Bdis -Method Get -Path "/growth-records?page=1&size=1" -Headers $adminHeaders
  if ($null -eq $page.data.records -or $page.data.records.Count -eq 0) {
    throw "没有可用的 growth_record，请先创建一条生长记录。"
  }
  $RecordId = [long]$page.data.records[0].id
}
Write-Host "使用生长记录 ID: $RecordId"

Write-Step "生成溯源码"
$traceCodeResponse = Invoke-Bdis -Method Post -Path "/growth-records/$RecordId/trace-code/generate" -Headers $adminHeaders
$traceCode = $traceCodeResponse.data.traceCode
if ([string]::IsNullOrWhiteSpace($traceCode)) {
  throw "生成溯源码失败：traceCode 为空。"
}
Write-Host "traceCode: $traceCode"

Write-Step "生成二维码"
$qrResponse = Invoke-Bdis -Method Post -Path "/growth-records/$RecordId/trace-qrcode/generate" -Headers $adminHeaders
if ([string]::IsNullOrWhiteSpace($qrResponse.data.qrCodeUrl)) {
  throw "生成二维码失败：qrCodeUrl 为空。"
}
Write-Host "qrCodeUrl: $($qrResponse.data.qrCodeUrl)"

Write-Step "查询二维码信息"
$qrInfo = Invoke-Bdis -Method Get -Path "/growth-records/$RecordId/trace-qrcode" -Headers $adminHeaders
Write-Host "公开状态: $($qrInfo.data.publicVisible)"

Write-Step "开启公开溯源"
$enabled = Invoke-Bdis -Method Put -Path "/growth-records/$RecordId/trace/public-enable" -Headers $adminHeaders
if ($enabled.data.publicVisible -ne 1) {
  throw "开启公开溯源失败。"
}
Write-Host "公开访问路径: $($enabled.data.traceUrl)"

Write-Step "公开查询应成功"
$publicOk = Invoke-BdisRaw -Method Get -Path "/trace/growth/$traceCode"
if ($publicOk.StatusCode -ne 200) {
  throw "公开查询失败，HTTP 状态码: $($publicOk.StatusCode)"
}
Write-Host "公开查询成功"

Write-Step "关闭公开溯源"
$disabled = Invoke-Bdis -Method Put -Path "/growth-records/$RecordId/trace/public-disable" -Headers $adminHeaders
if ($disabled.data.publicVisible -ne 0) {
  throw "关闭公开溯源失败。"
}
Write-Host "公开溯源已关闭"

Write-Step "关闭后公开查询应不可访问"
$publicDenied = Invoke-BdisRaw -Method Get -Path "/trace/growth/$traceCode"
if ($publicDenied.StatusCode -eq 200) {
  throw "关闭公开溯源后公开接口仍返回 200。"
}
Write-Host "关闭后公开查询已被拒绝，HTTP 状态码: $($publicDenied.StatusCode)"

Write-Step "查询溯源事件"
$events = Invoke-Bdis -Method Get -Path "/growth-records/$RecordId/trace-events" -Headers $adminHeaders
$eventTypes = @($events.data | ForEach-Object { $_.eventType })
$required = @("trace_code_generated", "trace_qrcode_generated", "public_trace_enabled", "public_trace_disabled")
foreach ($eventType in $required) {
  if ($eventTypes -notcontains $eventType) {
    throw "缺少溯源事件: $eventType"
  }
}
Write-Host "溯源事件校验通过"

Write-Step "测试完成"
Write-Host "recordId: $RecordId"
Write-Host "traceCode: $traceCode"
