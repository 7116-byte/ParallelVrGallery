[CmdletBinding()]
param([string]$ApkPath, [string]$EvidenceDirectory)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
if (!$ApkPath) { $ApkPath = Join-Path $root '.local/artifacts/app-debug.apk' }
if (!$EvidenceDirectory) { $EvidenceDirectory = Join-Path $root ('.local/verification/native-alignment-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff')) }
$apk = (Resolve-Path -LiteralPath $ApkPath).Path
[IO.Directory]::CreateDirectory($EvidenceDirectory) | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($apk)
$records = @()
try {
    foreach ($entry in $zip.Entries) {
        if ($entry.FullName -notmatch '^lib/([^/]+)/([^/]+\.so)$') { continue }
        $abi = $Matches[1]; $name = $Matches[2]
        $memory = New-Object IO.MemoryStream
        $stream = $entry.Open()
        try { $stream.CopyTo($memory); $bytes = $memory.ToArray() } finally { $stream.Dispose(); $memory.Dispose() }
        if ($bytes.Length -lt 64 -or $bytes[0] -ne 127 -or $bytes[1] -ne 69 -or $bytes[2] -ne 76 -or $bytes[3] -ne 70) { throw "Invalid ELF: $($entry.FullName)" }
        if ($bytes[5] -ne 1) { throw 'Only little-endian Android ELF files are supported' }
        $is64 = $bytes[4] -eq 2
        if ($is64) {
            $phoff = [BitConverter]::ToUInt64($bytes,32)
            $entsize = [BitConverter]::ToUInt16($bytes,54)
            $count = [BitConverter]::ToUInt16($bytes,56)
        } elseif ($bytes[4] -eq 1) {
            $phoff = [BitConverter]::ToUInt32($bytes,28)
            $entsize = [BitConverter]::ToUInt16($bytes,42)
            $count = [BitConverter]::ToUInt16($bytes,44)
        } else { throw 'Unknown ELF class' }
        $loads = @()
        for ($i = 0; $i -lt $count; $i++) {
            $position = [int]($phoff + $i * $entsize)
            if ($position -lt 0 -or $position + $entsize -gt $bytes.Length) { throw 'ELF program header out of range' }
            if ([BitConverter]::ToUInt32($bytes,$position) -ne 1) { continue }
            if ($is64) {
                $offset = [BitConverter]::ToUInt64($bytes,$position+8)
                $address = [BitConverter]::ToUInt64($bytes,$position+16)
                $alignment = [BitConverter]::ToUInt64($bytes,$position+48)
            } else {
                $offset = [BitConverter]::ToUInt32($bytes,$position+4)
                $address = [BitConverter]::ToUInt32($bytes,$position+8)
                $alignment = [BitConverter]::ToUInt32($bytes,$position+28)
            }
            $loads += [pscustomobject]@{offset=$offset;virtualAddress=$address;alignment=$alignment;aligned16KB=($alignment -ge 16384 -and $offset % 16384 -eq $address % 16384)}
        }
        if (!$loads.Count) { throw 'ELF has no LOAD segments' }
        $sha = [Security.Cryptography.SHA256]::Create()
        try { $hash = ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-','').ToLowerInvariant() } finally { $sha.Dispose() }
        $records += [pscustomobject]@{abi=$abi;library=$name;elfBits=$(if($is64){64}else{32});minimumLoadAlignment=($loads.alignment|Measure-Object -Minimum).Minimum;loadAlignment16KB=(@($loads|Where-Object{!$_.aligned16KB}).Count -eq 0);sha256=$hash;segments=$loads}
    }
} finally { $zip.Dispose() }
if (!$records.Count) { throw 'No native libraries found; this checker expects a native APK' }
$zipalign = Join-Path $root '.local/toolchain/android-sdk/build-tools/35.0.0/zipalign.exe'
& $zipalign -c -P 16 -v 4 $apk | Set-Content (Join-Path $EvidenceDirectory 'zipalign.txt') -Encoding UTF8
$zipExit = $LASTEXITCODE
$failed64 = @($records|Where-Object{ $_.elfBits -eq 64 -and !$_.loadAlignment16KB })
$result = [ordered]@{checkedAt=[DateTimeOffset]::Now.ToString('o');apkSHA256=(Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant();zipAlignment16KB=($zipExit -eq 0);elf64LoadAlignment16KB=($failed64.Count -eq 0);scope='Static ZIP and ELF LOAD alignment only; not runtime/RELRO/allocator validation';libraries=$records}
$result | ConvertTo-Json -Depth 7 | Set-Content (Join-Path $EvidenceDirectory 'result.json') -Encoding UTF8
$records | Select-Object abi,library,elfBits,minimumLoadAlignment,loadAlignment16KB | Format-Table -AutoSize
Write-Output "ZIP alignment: $($result.zipAlignment16KB); 64-bit ELF LOAD alignment: $($result.elf64LoadAlignment16KB). Evidence: $EvidenceDirectory"
if ($zipExit -ne 0 -or $failed64.Count) { exit 2 }
