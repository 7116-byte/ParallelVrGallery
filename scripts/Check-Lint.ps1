[CmdletBinding()]
param([switch]$Strict,[ValidateSet('Debug','Release')][string]$Variant='Debug')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$names = @('JAVA_HOME','ANDROID_HOME','ANDROID_SDK_ROOT','GRADLE_USER_HOME','ANDROID_USER_HOME','ANDROID_EMULATOR_HOME','GRADLE_RO_DEP_CACHE','GRADLE_OPTS','JAVA_OPTS','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS','TEMP','TMP','PATH')
$saved = @{}
foreach ($n in $names) { $saved[$n] = [Environment]::GetEnvironmentVariable($n, 'Process') }
$evidence = Join-Path $root ('.local/verification/lint-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
[IO.Directory]::CreateDirectory($evidence) | Out-Null
try {
    . (Join-Path $PSScriptRoot 'Use-Environment.ps1')
    Set-Content -LiteralPath (Join-Path $root 'local.properties') -Value ('sdk.dir=' + $env:ANDROID_HOME.Replace('\','/')) -Encoding ASCII
    Push-Location $root
    try {
        $command = '""{0}" lint{1} --offline --no-daemon --console=plain --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process 2>&1"' -f (Join-Path $root 'gradlew.bat'),$Variant
        & $env:ComSpec /d /s /c $command | Tee-Object -FilePath (Join-Path $evidence 'lint.log')
        $gradleExit = $LASTEXITCODE
    } finally { Pop-Location }
    $variantLower=$Variant.ToLowerInvariant()
    $xmlPath = Join-Path $root "app/build/reports/lint-results-$variantLower.xml"
    $logText = Get-Content (Join-Path $evidence 'lint.log') -Raw
    if ($gradleExit -ne 0 -and ($logText -notmatch ":app:lint$Variant FAILED" -or $logText -notmatch 'Lint found')) { throw 'Lint execution failed before a usable report was produced' }
    [xml]$report = Get-Content $xmlPath -Raw
    foreach ($extension in @('xml','html','txt')) {
        $p = Join-Path $root "app/build/reports/lint-results-$variantLower.$extension"
        if (Test-Path $p) { Copy-Item $p $evidence }
    }
    $issues = @($report.issues.issue)
    $errors = @($issues | Where-Object { $_.severity -in @('Error','Fatal') })
    $known = @( (Get-Content (Join-Path $root 'verification/lint-known-errors.json') -Raw | ConvertFrom-Json).errors )
    $newErrors = @()
    foreach ($i in $errors) {
        $file = ([string]$i.location.file).Replace('\','/')
        $prefix = $root.Replace('\','/') + '/'
        if ($file.StartsWith($prefix,[StringComparison]::OrdinalIgnoreCase)) { $file = $file.Substring($prefix.Length) }
        $match = @($known | Where-Object { $_.id -eq [string]$i.id -and $_.file -eq $file -and $_.code -eq ([string]$i.errorLine1).Trim() })
        if ($match.Count) { $known = @($known | Where-Object { $_ -ne $match[0] }) }
        else { $newErrors += "${file}:$($i.location.line) $($i.id)" }
    }
    $status = if ($errors.Count -eq 0) { 'PASS' } elseif ($newErrors.Count -eq 0) { 'KNOWN_BASELINE_ERRORS' } else { 'NEW_ERRORS' }
    $result = [ordered]@{status=$status; rawGradleExit=$gradleExit; errors=$errors.Count; warnings=@($issues | Where-Object severity -eq 'Warning').Count; information=@($issues | Where-Object severity -eq 'Information').Count; newErrors=$newErrors; strict=[bool]$Strict; evidence=$evidence.Substring($root.Length+1); checkedAt=[DateTimeOffset]::Now.ToString('o')}
    $result | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $evidence 'result.json') -Encoding UTF8
    $result | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $root '.local/verification/latest-lint.json') -Encoding UTF8
    Write-Output ($result | ConvertTo-Json -Compress)
    if ($newErrors.Count -or ($Strict -and $errors.Count)) { throw 'Lint errors require attention; see the raw report' }
} catch {
    if (!(Test-Path (Join-Path $evidence 'result.json'))) {
        $failure = [ordered]@{status='EXECUTION_FAILED'; checkedAt=[DateTimeOffset]::Now.ToString('o'); error=$_.Exception.Message; evidence=$evidence.Substring($root.Length+1)}
        $failure | ConvertTo-Json | Set-Content (Join-Path $evidence 'result.json') -Encoding UTF8
        $failure | ConvertTo-Json | Set-Content (Join-Path $root '.local/verification/latest-lint.json') -Encoding UTF8
    }
    throw
} finally {
    foreach ($n in $names) { [Environment]::SetEnvironmentVariable($n, $saved[$n], 'Process') }
}
