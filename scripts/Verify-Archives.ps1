$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$manifest = Join-Path $root '.local/restoration/migration-files.csv'
$summary = Get-Content (Join-Path $root '.local/restoration/migration-summary.json') -Raw | ConvertFrom-Json
if ((Get-FileHash -LiteralPath $manifest).Hash -ne $summary.csvSHA256) { throw 'Migration manifest SHA256 mismatch' }
$rows = @(Import-Csv $manifest)
if ($rows.Count -ne $summary.files) { throw 'Migration manifest file count mismatch' }
$failures = [Collections.Generic.List[string]]::new()
$n = 0
foreach ($r in $rows) {
    $p = [IO.Path]::GetFullPath((Join-Path $root $r.Destination))
    if (!$p.StartsWith((Join-Path $root '.local/archives/').Replace('/','\'),[StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid archive manifest path' }
    if (!(Test-Path -LiteralPath $p -PathType Leaf)) { $failures.Add("Missing: $($r.Destination)") }
    elseif ((Get-Item -LiteralPath $p).Length -ne [long]$r.Bytes -or (Get-FileHash -LiteralPath $p).Hash -ne $r.DestinationSHA256) { $failures.Add("Mismatch: $($r.Destination)") }
    $n++; if ($n % 10000 -eq 0) { Write-Output "Archive: $n / $($rows.Count)" }
}
[ordered]@{files=$n; failures=@($failures.ToArray()); passed=($failures.Count -eq 0); checkedAt=[DateTimeOffset]::Now.ToString('o')} | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $root '.local/restoration/last-archive-verify.json') -Encoding UTF8
if ($failures.Count) { throw ($failures -join "`n") }
Write-Output "PASS: $n archive files match migration SHA256; original backup drives are not needed."
