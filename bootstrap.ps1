# Downloads only official, pinned portable tools into this directory. No system installation.
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force .tools | Out-Null
function Fetch([string]$url,[string]$name,[string]$hash,[string]$destination) {
    $zip=Join-Path '.tools' $name
    if(-not(Test-Path $zip)){Invoke-WebRequest -Uri $url -OutFile $zip}
    if((Get-FileHash $zip -Algorithm SHA256).Hash -ne $hash){throw "Checksum mismatch for $name"}
    if(-not(Test-Path $destination)){Expand-Archive $zip $destination}
}
Fetch 'https://aka.ms/download-jdk/microsoft-jdk-21.0.12.1-windows-x64.zip' 'jdk.zip' '192441A9D27DA813BADA974BB88B4CF64D37A9589ED37F204374D411CA5CE07F' '.tools\jdk'
Fetch 'https://dl.google.com/android/repository/platform-36_r02.zip' 'platform.zip' '37607369A28C5B640B3A7998868D45898EBCB777565A0E85F9ACF36F29631D2E' '.tools\sdk\platforms'
Fetch 'https://dl.google.com/android/repository/build-tools_r36_windows.zip' 'build-tools.zip' 'AA1095CB14D83E483818A748A2C06FAAEB8E601561B06A356A119A1B2CA280D3' '.tools\sdk\build-tools'
if(-not(Test-Path '.tools\sdk\platform-tools\adb.exe')) {
    Fetch 'https://dl.google.com/android/repository/platform-tools_r37.0.1-win.zip' 'platform-tools.zip' '45F4D63113E895EBDE0C90F194099A4676B6AC653BD28D54314A9E022BBC1A99' '.tools\platform-tools-staging'
    Copy-Item '.tools\platform-tools-staging\platform-tools' '.tools\sdk\platform-tools' -Recurse
}
Write-Output 'Portable toolchain ready. Run .\build.ps1'
