param(
  [ValidateSet('Base','FollowUp')]
  [string]$Phase = 'Base',
  [string]$Database = $(if ($env:MYSQL_DATABASE) { $env:MYSQL_DATABASE } else { 'biomed_dev' }),
  [string]$HostName = $(if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { '127.0.0.1' }),
  [int]$Port = $(if ($env:MYSQL_PORT) { [int]$env:MYSQL_PORT } else { 3306 }),
  [string]$User = $(if ($env:MYSQL_USER) { $env:MYSQL_USER } else { 'bdis' }),
  [string]$Password = $env:MYSQL_PASSWORD
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$storageRoot = Join-Path $root 'backend\storage'
$demoStorage = Join-Path $storageRoot 'demo-agent'
$resolvedStorage = [IO.Path]::GetFullPath($demoStorage)
if (-not $resolvedStorage.StartsWith([IO.Path]::GetFullPath($storageRoot), [StringComparison]::OrdinalIgnoreCase)) {
  throw '演示文件目标路径超出 backend/storage，已停止。'
}
New-Item -ItemType Directory -Path $resolvedStorage -Force | Out-Null

$copies = @{
  'backend\import\herb_atlas\huanglian_coptis_chinensis\whole_growth_fresh\huanglian_whole_growth_fresh_01.png' = 'whole-stage-01.png';
  'backend\import\herb_atlas\huanglian_coptis_chinensis\leaf_growth_fresh\huanglian_leaf_growth_fresh_01.png' = 'leaf-stage-02.png';
  'backend\import\herb_atlas\huanglian_coptis_chinensis\whole_growth_fresh\huanglian_whole_growth_fresh_02.jpeg' = 'whole-stage-03.jpeg';
  'backend\import\herb_atlas\huanglian_coptis_chinensis\whole_growth_fresh\huanglian_whole_growth_fresh_03.jpeg' = 'followup-whole.jpeg';
  'backend\import\herb_atlas\huanglian_coptis_chinensis\leaf_growth_fresh\huanglian_leaf_growth_fresh_02.jpeg' = 'followup-leaf.jpeg';
  'backend\import\herb_atlas\huanglian_coptis_chinensis\rhizome_mature_dried\huanglian_rhizome_dried_01.png' = 'followup-root.png'
}
foreach ($item in $copies.GetEnumerator()) {
  $source = Join-Path $root $item.Key
  if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw "演示图片不存在：$source" }
  Copy-Item -LiteralPath $source -Destination (Join-Path $resolvedStorage $item.Value) -Force
}

$sqlFile = if ($Phase -eq 'Base') {
  Join-Path $PSScriptRoot 'dev-research-agent-demo.sql'
} else {
  Join-Path $PSScriptRoot 'dev-research-agent-followup.sql'
}
if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
  throw '未找到 mysql 客户端，请安装或将其加入 PATH。'
}
$previousPassword = $env:MYSQL_PWD
try {
  if ($Password) { $env:MYSQL_PWD = $Password }
  Get-Content -LiteralPath $sqlFile -Raw -Encoding UTF8 |
    & mysql --default-character-set=utf8mb4 --host=$HostName --port=$Port --user=$User $Database
  if ($LASTEXITCODE -ne 0) { throw "MySQL 执行失败，退出码：$LASTEXITCODE" }
} finally {
  $env:MYSQL_PWD = $previousPassword
}
Write-Host "科研 Agent 演示数据阶段 $Phase 已恢复。所有样本均标记为 demo_seed / 非现场实测。"
