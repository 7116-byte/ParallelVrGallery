[CmdletBinding()]
param([switch]$FullArchive,[switch]$StrictLint)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
try {
    if ($FullArchive) {
        & (Join-Path $root 'scripts/Verify-Archives.ps1')
    }
    $model = Join-Path $root '.local/models/depth_anything_v2.tflite'
    if ((Get-FileHash -LiteralPath $model).Hash -ne 'B407F34F61750F31441E6F858A4BC48D8572F9EE5399FFD015CEE5FA1767083F') { throw 'Stable model mismatch' }
    $git = Join-Path $root '.local/toolchain/git/cmd/git.exe'
    & $git -C $root fsck --full
    if ($LASTEXITCODE -ne 0) { throw 'Git integrity check failed' }
    # Tests belong to this ordinary-edition source tree; never count Pro's archived reports.
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root 'Build.ps1') -Verify
    if ($LASTEXITCODE -ne 0) { throw 'Build / test verification failed' }
    $reports = @(Get-ChildItem -LiteralPath (Join-Path $root 'app/build/test-results/testDebugUnitTest') -Filter '*.xml' -File)
    if (!$reports.Count) { throw 'No JUnit test reports were produced' }
    $testCount=0; $failures=0; $errors=0; $skipped=0
    foreach ($report in $reports) {
        [xml]$xml = Get-Content -LiteralPath $report.FullName -Raw
        $testCount += [int]$xml.testsuite.tests; $failures += [int]$xml.testsuite.failures
        $errors += [int]$xml.testsuite.errors; $skipped += [int]$xml.testsuite.skipped
    }
    if ($testCount -eq 0 -or $failures -or $errors) { throw 'JUnit reports contain no tests or contain failures/errors' }
    & (Join-Path $root 'scripts/Check-Lint.ps1') -Strict:$StrictLint
    $lint = Get-Content (Join-Path $root '.local/verification/latest-lint.json') -Raw | ConvertFrom-Json
    $build = Get-Content (Join-Path $root '.local/verification/latest-build.json') -Raw | ConvertFrom-Json
    $status = if ($lint.errors) { 'PASS_WITH_KNOWN_LINT_ERRORS' } else { 'PASS' }
    [ordered]@{status=$status; checkedAt=[DateTimeOffset]::Now.ToString('o'); build=$build; lint=$lint; unitTests=@{tests=$testCount; failures=$failures; errors=$errors; skipped=$skipped}; archiveChecked=[bool]$FullArchive} | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $root '.local/verification/latest-acceptance.json') -Encoding UTF8
    Write-Output "$status : offline clean build, package/version/certificate, model, Git integrity. Unit tests: $testCount, failures $failures, errors $errors. Lint: $($lint.errors) errors, $($lint.warnings) warnings."
    exit 0
} catch {
    [ordered]@{status='FAILED'; checkedAt=[DateTimeOffset]::Now.ToString('o'); error=$_.Exception.Message} | ConvertTo-Json | Set-Content (Join-Path $root '.local/verification/latest-acceptance.json') -Encoding UTF8
    Write-Output ("VERIFY FAILED: " + $_.Exception.Message)
    exit 1
}
