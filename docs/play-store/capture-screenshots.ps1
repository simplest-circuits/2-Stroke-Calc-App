# Play Store screenshot capture for 2-Stroke Lab (max. 8 screens per locale)
param(
    [string]$Device = "emulator-5554",
    [string]$Package = "com.simplestsoft.twostrokecalc",
    [ValidateSet("all", "en", "de")]
    [string]$Locale = "all",
    [string[]]$Only = @(),
    [switch]$SkipExisting
)

$ErrorActionPreference = "Stop"
$Sdk = "$env:LOCALAPPDATA\Android\Sdk"
$Adb = "$Sdk\platform-tools\adb.exe"
$OutRoot = Join-Path $PSScriptRoot "screenshots"
$UiFile = Join-Path $env:TEMP "twostroke-ui.xml"

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Cmd)
    & $Adb -s $Device @Cmd
}

function Wait-App([int]$Seconds = 2) { Start-Sleep -Seconds $Seconds }

function Get-ScreenSize {
    $raw = (Invoke-Adb shell wm size) -join " "
    if ($raw -match '(\d+)x(\d+)') {
        return [int]$Matches[1], [int]$Matches[2]
    }
    return 1080, 2424
}

$script:ScreenW, $script:ScreenH = Get-ScreenSize
$script:NavY = [int]($ScreenH * 0.938)
$script:TabXs = @(
    [int]($ScreenW * 0.125),
    [int]($ScreenW * 0.375),
    [int]($ScreenW * 0.625),
    [int]($ScreenW * 0.875)
)

function Tap([int]$X, [int]$Y) {
    Invoke-Adb shell input tap $X $Y | Out-Null
    Wait-App 1
}

function Swipe([int]$X1, [int]$Y1, [int]$X2, [int]$Y2, [int]$Ms = 450) {
    Invoke-Adb shell input swipe $X1 $Y1 $X2 $Y2 $Ms | Out-Null
    Wait-App 1
}

function Press-Back { Invoke-Adb shell input keyevent 4 | Out-Null; Wait-App 1 }

function Dump-Ui {
    Invoke-Adb shell uiautomator dump /sdcard/ui.xml | Out-Null
    Invoke-Adb pull /sdcard/ui.xml $UiFile | Out-Null
    return [xml](Get-Content $UiFile -Raw)
}

function Tap-Node($Node) {
    if (-not $Node) { return $false }
    if ($Node.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
        Tap (([int]$Matches[1] + [int]$Matches[3]) / 2) (([int]$Matches[2] + [int]$Matches[4]) / 2)
        return $true
    }
    return $false
}

function Tap-Text([xml]$Ui, [string[]]$Texts, [switch]$Contains) {
    foreach ($text in $Texts) {
        $xpath = if ($Contains) {
            "//node[contains(@text,'$text')]"
        } else {
            "//node[@text='$text']"
        }
        $node = $Ui.SelectSingleNode($xpath)
        if ($node -and (Tap-Node $node)) { return $true }
    }
    return $false
}

function Tap-ContentDesc([xml]$Ui, [string[]]$Descs, [switch]$Contains) {
    foreach ($desc in $Descs) {
        $xpath = if ($Contains) {
            "//node[contains(@content-desc,'$desc')]"
        } else {
            "//node[@content-desc='$desc']"
        }
        $node = $Ui.SelectSingleNode($xpath)
        if ($node -and (Tap-Node $node)) { return $true }
    }
    return $false
}

function Capture-Screen([string]$Path) {
    $dir = Split-Path $Path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $tmp = Join-Path $env:TEMP "twostroke-cap.png"
    for ($attempt = 0; $attempt -lt 3; $attempt++) {
        cmd /c "`"$Adb`" -s $Device exec-out screencap -p > `"$tmp`""
        if ((Test-Path $tmp) -and (Get-Item $tmp).Length -gt 50000) {
            Copy-Item $tmp $Path -Force
            return
        }
        Wait-App 2
    }
    throw "Screenshot capture failed for $Path"
}

function Dismiss-Dialogs {
    $ui = Dump-Ui
    if (Tap-Text $ui @("Wait", "Warten")) { Wait-App 3; return }
    if (Tap-Text $ui @("Close app", "App schließen")) { Wait-App 2; return }
    if (Tap-Text $ui @("Not now", "Nicht jetzt")) { Wait-App 2; return }
    if (Tap-Text $ui @("Skip", "Überspringen")) { Wait-App 1; return }
    if (Tap-Text $ui @("Finish", "Fertig")) { Wait-App 1; return }
    if (Tap-Text $ui @("Next", "Weiter")) { Wait-App 1; return }
    if (Tap-Text $ui @("OK", "Got it", "Verstanden")) { Wait-App 1; return }
}

function Tap-BottomNav([int]$Index) {
    Tap $TabXs[$Index] $NavY
    Wait-App 3
}

function Start-App {
    Invoke-Adb shell am force-stop $Package | Out-Null
    Wait-App 2
    Invoke-Adb shell am start -n "$Package/.MainActivity" | Out-Null
    Wait-App 12
    for ($i = 0; $i -lt 10; $i++) {
        Dismiss-Dialogs
        $ui = Dump-Ui
        if ($ui.SelectSingleNode("//node[contains(@text,'Steuerzeiten') or contains(@text,'Port timing') or contains(@text,'Ignition timing') or contains(@text,'Rechner') or contains(@text,'Calculator')]")) {
            break
        }
        Wait-App 2
    }
}

function Set-AppLanguage([string]$Lang) {
    Tap-BottomNav 3
    $ui = Dump-Ui
    Tap-Text $ui @("Sprache", "Language") | Out-Null
    Wait-App 2
    $ui = Dump-Ui
    if ($Lang -eq "en") {
        Tap-Text $ui @("English", "Englisch") | Out-Null
    } else {
        Tap-Text $ui @("German", "Deutsch") | Out-Null
    }
    Wait-App 3
    Tap-BottomNav 0
    Wait-App 2
}

function Open-Calculator([string[]]$Titles) {
    Tap-BottomNav 0
    for ($attempt = 0; $attempt -lt 5; $attempt++) {
        $ui = Dump-Ui
        foreach ($title in $Titles) {
            if (Tap-Text $ui @($title)) { Wait-App 2; return $true }
        }
        Swipe ([int]($ScreenW / 2)) ([int]($ScreenH * 0.78)) ([int]($ScreenW / 2)) ([int]($ScreenH * 0.36)) 400
    }
    return $false
}

function Open-Tool([string[]]$Titles) {
    Tap-BottomNav 1
    Wait-App 2
    for ($attempt = 0; $attempt -lt 5; $attempt++) {
        $ui = Dump-Ui
        foreach ($title in $Titles) {
            if (Tap-Text $ui @($title)) { Wait-App 3; return $true }
        }
        Swipe ([int]($ScreenW / 2)) ([int]($ScreenH * 0.72)) ([int]($ScreenW / 2)) ([int]($ScreenH * 0.32)) 400
    }
    return $false
}

function Tap-VehicleTab([string[]]$TabLabels) {
    for ($attempt = 0; $attempt -lt 3; $attempt++) {
        $ui = Dump-Ui
        foreach ($label in $TabLabels) {
            if (Tap-Text $ui @($label)) { Wait-App 2; return $true }
        }
        Swipe ([int]($ScreenW * 0.65)) ([int]($ScreenH * 0.14)) ([int]($ScreenW * 0.18)) ([int]($ScreenH * 0.14)) 300
        Wait-App 1
    }
    return $false
}

function Capture-Locale([string]$Lang, [string]$Folder, [switch]$SkipExisting) {
    $outDir = Join-Path $OutRoot $Folder
    if (-not $SkipExisting -and $Only.Count -eq 0 -and (Test-Path $outDir)) {
        Remove-Item $outDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null

    function Save([string]$Name) {
        if ($Only.Count -gt 0 -and ($Only -notcontains $Name)) {
            return
        }
        $path = Join-Path $outDir $Name
        if ($SkipExisting -and (Test-Path $path) -and (Get-Item $path).Length -gt 50000) {
            Write-Host "  skip $Name (exists)"
            return
        }
        Capture-Screen $path
    }

    Write-Host "  Screen: $ScreenW x $ScreenH, nav Y=$NavY"
    $needsFullFlow = $Only.Count -eq 0
    if ($needsFullFlow) {
        Write-Host "  Starting app..."
        Start-App
        Set-AppLanguage $Lang
    } else {
        Start-App
        Set-AppLanguage $Lang
    }

    Write-Host "  01 calculator list"
    Tap-BottomNav 0
    Wait-App 2
    Save "01_calculator_overview_list.png"

    Write-Host "  02 calculator grid"
    $ui = Dump-Ui
    if (-not (Tap-ContentDesc $ui @("Rasteransicht", "Grid view"))) {
        Tap ([int]($ScreenW * 0.89)) ([int]($ScreenH * 0.10))
    }
    Wait-App 2
    Save "02_calculator_overview_grid.png"

    Write-Host "  03 calculator detail"
    Open-Calculator @("Steuerzeiten", "Port timing") | Out-Null
    $ui = Dump-Ui
    Tap-Text $ui @("Berechnen", "Calculate") | Out-Null
    Wait-App 1
    Swipe ([int]($ScreenW / 2)) ([int]($ScreenH * 0.82)) ([int]($ScreenW / 2)) ([int]($ScreenH * 0.45)) 400
    Wait-App 1
    Save "03_calculator_detail.png"
    Press-Back
    Wait-App 2

    Write-Host "  04 tools overview"
    Tap-BottomNav 1
    Wait-App 3
    Save "04_tools_overview.png"

    Write-Host "  05 gps dyno"
    Open-Tool @("Straßen-Dyno", "Street dyno") | Out-Null
    Wait-App 2
    $ui = Dump-Ui
    Tap-Text $ui @("Verstanden, starten", "Understood, start") | Out-Null
    Wait-App 2
    Save "05_gps_dyno.png"
    Press-Back
    Wait-App 2

    Write-Host "  06 vehicles overview"
    Tap-BottomNav 2
    Wait-App 3
    Save "06_vehicles_overview.png"

    Write-Host "  07 fuel log"
    $ui = Dump-Ui
    if (Tap-Text $ui @("Demo Roller", "Demo Scooter") -Contains) {
        Wait-App 3
        $ui = Dump-Ui
        if (-not (Tap-ContentDesc $ui @("Tankbuch", "Fuel log"))) {
            Tap ([int]($ScreenW * 0.78)) ([int]($ScreenH * 0.075))
        }
        Wait-App 3
        Save "07_fuel_log.png"
        Press-Back
        Wait-App 1
        Press-Back
        Wait-App 2
    } else {
        Write-Warning "  Demo vehicle not found - saving vehicles screen as 07"
        Save "07_fuel_log.png"
    }

    Write-Host "  08 settings"
    Tap-BottomNav 3
    Wait-App 3
    Save "08_settings.png"
}

Write-Host "Device: $Device ($ScreenW x $ScreenH)"

if ($Locale -eq "all" -or $Locale -eq "en") {
    Write-Host "=== English (en) ==="
    Capture-Locale "en" "en" -SkipExisting:$SkipExisting
}

if ($Locale -eq "all" -or $Locale -eq "de") {
    Write-Host "=== German (de) ==="
    Capture-Locale "de" "de" -SkipExisting:$SkipExisting
}

Write-Host "Screenshots saved under: $OutRoot"
