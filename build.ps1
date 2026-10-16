param([switch]$SkipTests,[string]$ToolRoot)
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
if(-not $ToolRoot){$ToolRoot=Join-Path $PSScriptRoot '.tools'}
$jdk=(Get-ChildItem (Join-Path $ToolRoot 'jdk') -Directory | Select-Object -First 1).FullName
$bt=(Get-ChildItem (Join-Path $ToolRoot 'sdk\build-tools') -Directory | Select-Object -First 1).FullName
$android=Join-Path $ToolRoot 'sdk\platforms\android-36\android.jar'
$env:JAVA_HOME=$jdk
function Run([string]$exe,[string[]]$arguments) { & $exe @arguments; if($LASTEXITCODE -ne 0){throw "Build command failed: $exe"} }
New-Item -ItemType Directory -Force build\classes,build\generated,build\dex,build\tests,dist | Out-Null
Run "$bt\aapt2.exe" @('compile','--dir','app\src\main\res','-o','build\resources.zip')
Run "$bt\aapt2.exe" @('link','-o','build\base.apk','-I',$android,'--manifest','app\src\main\AndroidManifest.xml','--java','build\generated','--version-code','3','--version-name','1.2','--auto-add-overlay','-R','build\resources.zip')
$sources=@(Get-ChildItem app\src\main\java,build\generated -Recurse -Filter *.java | ForEach-Object FullName)
Run "$jdk\bin\javac.exe" (@('--release','8','-encoding','UTF-8','-cp',$android,'-d','build\classes')+$sources)
if(-not $SkipTests) {
    Run "$jdk\bin\javac.exe" @('--release','8','-encoding','UTF-8','-cp',"build\classes;$android",'-d','build\tests','tests\MathTests.java')
    Run "$jdk\bin\java.exe" @('-cp',"build\tests;build\classes;$android",'app.meridian.MathTests')
}
Run "$jdk\bin\jar.exe" @('cf','build\classes.jar','-C','build\classes','.')
Run "$bt\d8.bat" @('--release','--min-api','28','--lib',$android,'--output','build\dex','build\classes.jar')
Copy-Item build\base.apk build\unsigned.apk -Force
Run "$jdk\bin\jar.exe" @('uf','build\unsigned.apk','-C','build\dex','classes.dex')
Run "$bt\zipalign.exe" @('-f','-p','4','build\unsigned.apk','build\aligned.apk')
Copy-Item build\aligned.apk dist\Meridian-1.2-unsigned.apk -Force
Write-Output 'Unsigned APK built. Signing credentials are managed separately and never included here.'
