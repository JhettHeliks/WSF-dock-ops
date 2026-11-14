param(
    [string]$AvdName = "Medium_Phone_API_37.0",
    [switch]$EmulatorOnly,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot

function Resolve-AndroidSdk {
    if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
        return $env:ANDROID_HOME
    }
    if ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) {
        return $env:ANDROID_SDK_ROOT
    }
    $localProps = Join-Path $ProjectRoot "local.properties"
    if (Test-Path $localProps) {
        $sdkLine = Get-Content $localProps | Where-Object { $_ -match "^sdk\.dir=" } | Select-Object -First 1
        if ($sdkLine) {
            $raw = $sdkLine.Substring("sdk.dir=".Length).Trim()
            $unescaped = $raw -replace "\\\\", "\" -replace "\\:", ":"
            if (Test-Path $unescaped) {
                return $unescaped
            }
        }
    }
    $defaultSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $defaultSdk) {
        return $defaultSdk
    }
    throw "Android SDK not found. Set ANDROID_HOME or sdk.dir in local.properties."
}

$SdkPath = Resolve-AndroidSdk
$Adb = Join-Path $SdkPath "platform-tools\adb.exe"
$Emulator = Join-Path $SdkPath "emulator\emulator.exe"
$Gradlew = Join-Path $ProjectRoot "gradlew.bat"
$ApkPath = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
$Component = "com.aistudio.wsfdockops.mrvkpx/com.example.MainActivity"

if (-not (Test-Path $Adb)) { throw "adb.exe not found at $Adb" }
if (-not (Test-Path $Emulator)) { throw "emulator.exe not found at $Emulator" }

if (-not $EmulatorOnly -and -not $SkipBuild) {
    Write-Host "Building debug APK..." -ForegroundColor Cyan
    Push-Location $ProjectRoot
    try {
        & $Gradlew assembleDebug
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

$devicesOutput = & $Adb devices
$hasDevice = $devicesOutput | Where-Object { $_ -match "^(emulator-\d+|\S+)\s+(device|offline)$" }
if (-not $hasDevice) {
    Write-Host "Starting Android emulator ($AvdName)..." -ForegroundColor Cyan
    Start-Process -FilePath $Emulator -ArgumentList "-avd", $AvdName
} else {
    Write-Host "Android device/emulator already running." -ForegroundColor Green
}

if ($EmulatorOnly) {
    exit 0
}

Write-Host "Waiting for emulator to finish booting..." -ForegroundColor Cyan
& $Adb wait-for-device
while ($true) {
    $rawBoot = & $Adb shell getprop sys.boot_completed 2>$null
    $booted = ($rawBoot | Out-String).Trim()
    if ($booted -eq "1") {
        break
    }
    Start-Sleep -Seconds 2
}

if (-not (Test-Path $ApkPath)) {
    throw "Debug APK not found at $ApkPath"
}

Write-Host "Installing app-debug.apk..." -ForegroundColor Cyan
& $Adb install -r $ApkPath
if ($LASTEXITCODE -ne 0) {
    throw "adb install failed with exit code $LASTEXITCODE"
}

Write-Host "Launching $Component..." -ForegroundColor Cyan
& $Adb shell am start -n $Component
if ($LASTEXITCODE -ne 0) {
    throw "Failed to launch MainActivity (exit code $LASTEXITCODE)"
}

Write-Host "WSF DockOps is running on the emulator." -ForegroundColor Green
