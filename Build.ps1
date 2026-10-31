[CmdletBinding()]
param(
    [switch]$Online,
    [switch]$Verify,
    [ValidatePattern('^[A-Za-z][A-Za-z0-9:]*$')]
    [string[]]$Tasks = @('assembleDebug')
)
$ErrorActionPreference = 'Stop'
if ($Verify) { $Tasks = @('clean','testDebugUnitTest','assembleDebug') }
$root = $PSScriptRoot
$names = @('JAVA_HOME','ANDROID_HOME','ANDROID_SDK_ROOT','GRADLE_USER_HOME','ANDROID_USER_HOME','ANDROID_EMULATOR_HOME','GRADLE_RO_DEP_CACHE','GRADLE_OPTS','JAVA_OPTS','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS','TEMP','TMP','PATH')
$saved = @{}
foreach ($n in $names) { $saved[$n] = [Environment]::GetEnvironmentVariable($n, 'Process') }
$exitCode = 1
$run = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$evidence = Join-Path $root ".local/verification/$run"
[IO.Directory]::CreateDirectory($evidence) | Out-Null
try {
    . (Join-Path $root 'scripts/Use-Environment.ps1')
    foreach ($p in @((Join-Path $env:JAVA_HOME 'bin/java.exe'),(Join-Path $root '.local/signing/debug.keystore'),(Join-Path $env:ANDROID_HOME 'build-tools/34.0.0/aapt.exe'))) {
        if (!(Test-Path -LiteralPath $p)) { throw "Required private asset missing: $p" }
    }
    # local.properties is machine-specific and ignored by Git.
    Set-Content -LiteralPath (Join-Path $root 'local.properties') -Value ('sdk.dir=' + $env:ANDROID_HOME.Replace('\','/')) -Encoding ASCII
    $gradleArgs = @($Tasks) + @('--no-daemon','--console=plain','--max-workers=2','-Pkotlin.compiler.execution.strategy=in-process','-Dorg.gradle.java.installations.auto-download=false',('-Dorg.gradle.java.installations.paths=' + $env:JAVA_HOME))
    if (!$Online) { $gradleArgs += '--offline' }
    $quotedArgs = ($gradleArgs | ForEach-Object { '"' + $_ + '"' }) -join ' '
    Push-Location $root
    try {
        $command = '""{0}" {1} 2>&1"' -f (Join-Path $root 'gradlew.bat'),$quotedArgs
        & $env:ComSpec /d /s /c $command | Tee-Object -FilePath (Join-Path $evidence 'build.log')
        $gradleExit = $LASTEXITCODE
        if ($gradleExit -ne 0) { throw "Gradle failed ($gradleExit). See $evidence/build.log" }
    } finally { Pop-Location }
    $apk = Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk'
    if (!(Test-Path $apk)) { throw 'No debug APK was produced. Tasks must include assembleDebug.' }
    $tools = Join-Path $env:ANDROID_HOME 'build-tools/35.0.0'
    $badging = & (Join-Path $tools 'aapt.exe') dump badging $apk
    if ($LASTEXITCODE -ne 0) { throw 'aapt failed' }
    $badging | Set-Content (Join-Path $evidence 'apk-badging.txt') -Encoding UTF8
    $cert = & (Join-Path $tools 'apksigner.bat') verify --verbose --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }
    $cert | Set-Content (Join-Path $evidence 'apk-signature.txt') -Encoding UTF8
    $expectedCert = 'c51c08290903e65d2e99fe35ceffd44b637f04cc5bbdd5ea9ecb8847d4ea7359'
    if (($cert -join "`n") -notmatch "certificate SHA-256 digest: $expectedCert") { throw 'Original signing certificate mismatch' }
    $packageLine = @($badging | Where-Object { $_ -like 'package:*' })[0]
    if ($packageLine -notmatch "name='com.local.parallelvrgallery' versionCode='(\d+)' versionName='([^']+)'") { throw 'Package identity mismatch' }
    $versionCode = [int]$Matches[1]; $versionName = $Matches[2]
    $gradleText = Get-Content (Join-Path $root 'app/build.gradle.kts') -Raw
    if ($gradleText -notmatch "versionCode\s*=\s*$versionCode\b" -or $gradleText -notmatch ('versionName\s*=\s*"' + [regex]::Escape($versionName) + '"')) { throw 'APK version differs from source' }
    $artifacts = Join-Path $root '.local/artifacts'
    [IO.Directory]::CreateDirectory($artifacts) | Out-Null
    Copy-Item -LiteralPath $apk -Destination (Join-Path $artifacts 'app-debug.apk') -Force
    $result = [ordered]@{ passed=$true; checkedAt=[DateTimeOffset]::Now.ToString('o'); offline=(!$Online); tasks=$Tasks; package='com.local.parallelvrgallery'; versionName=$versionName; versionCode=$versionCode; certificateSHA256=$expectedCert; apkSHA256=(Get-FileHash $apk).Hash; apk='app/build/outputs/apk/debug/app-debug.apk'; evidence=$evidence.Substring($root.Length+1) }
    $result | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $evidence 'result.json') -Encoding UTF8
    $result | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $root '.local/verification/latest-build.json') -Encoding UTF8
    Write-Output ($result | ConvertTo-Json -Compress)
    $exitCode = 0
} catch {
    $failure = [ordered]@{passed=$false; checkedAt=[DateTimeOffset]::Now.ToString('o'); error=$_.Exception.Message; evidence=$evidence.Substring($root.Length+1)}
    $failure | ConvertTo-Json | Set-Content (Join-Path $evidence 'result.json') -Encoding UTF8
    $failure | ConvertTo-Json | Set-Content (Join-Path $root '.local/verification/latest-build.json') -Encoding UTF8
    Write-Output ("BUILD FAILED: " + $_.Exception.Message)
} finally {
    foreach ($n in $names) { [Environment]::SetEnvironmentVariable($n, $saved[$n], 'Process') }
}
exit $exitCode
