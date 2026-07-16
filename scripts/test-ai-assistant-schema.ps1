$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
$config = @{}

Get-Content -Encoding utf8 -LiteralPath $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $config[$matches[1].Trim()] = $matches[2].Trim().Trim('"').Trim("'")
    }
}

$hostName = if ($config['MYSQL_HOST']) { $config['MYSQL_HOST'] } else { 'localhost' }
$port = if ($config['MYSQL_PORT']) { $config['MYSQL_PORT'] } else { '3306' }
$database = if ($config['MYSQL_DATABASE']) { $config['MYSQL_DATABASE'] } else { 'biomed_dev' }
$user = if ($config['MYSQL_USER']) { $config['MYSQL_USER'] } else { 'bdis' }
$requiredColumns = @(
    'herb_ai_chat_session.is_deleted',
    'herb_ai_chat_message.user_id',
    'herb_ai_chat_message.is_deleted',
    'herb_ai_knowledge_doc.is_deleted',
    'herb_ai_knowledge_chunk.is_deleted'
)

$query = @"
SELECT CONCAT(table_name, '.', column_name)
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND CONCAT(table_name, '.', column_name) IN (
    'herb_ai_chat_session.is_deleted',
    'herb_ai_chat_message.user_id',
    'herb_ai_chat_message.is_deleted',
    'herb_ai_knowledge_doc.is_deleted',
    'herb_ai_knowledge_chunk.is_deleted'
  );
"@

try {
    $env:MYSQL_PWD = $config['MYSQL_PASSWORD']
    $actualColumns = @(
        & mysql --host=$hostName --port=$port --user=$user --database=$database `
            --batch --skip-column-names --execute=$query
    )
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL schema query failed with exit code $LASTEXITCODE"
    }
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

$missingColumns = @($requiredColumns | Where-Object { $_ -notin $actualColumns })
if ($missingColumns.Count -gt 0) {
    throw "AI assistant schema is outdated. Missing columns: $($missingColumns -join ', ')"
}

Write-Output 'AI assistant schema check passed.'
