# Dot-source this file for a shell using only this project's private tools.
$PvgRoot = Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME = Join-Path $PvgRoot '.local/toolchain/jdk17'
$env:ANDROID_HOME = Join-Path $PvgRoot '.local/toolchain/android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:GRADLE_USER_HOME = Join-Path $PvgRoot '.local/gradle-home'
$env:ANDROID_USER_HOME = Join-Path $PvgRoot '.local/runtime/android'
$env:ANDROID_EMULATOR_HOME = Join-Path $PvgRoot '.local/runtime/emulator'
$env:GRADLE_RO_DEP_CACHE = $null
$env:GRADLE_OPTS = $null
$env:JAVA_OPTS = $null
$env:JAVA_TOOL_OPTIONS = $null
$env:JDK_JAVA_OPTIONS = $null
$env:_JAVA_OPTIONS = $null
$env:TEMP = Join-Path $PvgRoot '.local/runtime/temp'
$env:TMP = $env:TEMP
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + (Join-Path $PvgRoot '.local/toolchain/git/cmd') + ';' + (Join-Path $PvgRoot '.local/toolchain/github-cli') + ';' + (Join-Path $env:ANDROID_HOME 'platform-tools') + ';' + $env:PATH
foreach ($p in @($env:GRADLE_USER_HOME,$env:ANDROID_USER_HOME,$env:ANDROID_EMULATOR_HOME,$env:TEMP)) {
    [IO.Directory]::CreateDirectory($p) | Out-Null
}
