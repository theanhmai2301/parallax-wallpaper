# ============================================================================
#  lib.ps1 - Helpers to drive com.galaxywall.app via adb / UI Automator
#  Used by run_smoke.ps1. Dot-source it: . .\lib.ps1
# ============================================================================

$Global:PKG = "com.galaxywall.app"
$Global:ADB = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

function Adb {
    param([string]$Serial, [Parameter(ValueFromRemainingArguments=$true)]$Args)
    & $Global:ADB -s $Serial @Args
}

# ---- logging -----------------------------------------------------------------
function Log-Step {
    param([string]$OutDir, [string]$Msg, [string]$Level = "INFO")
    $ts = (Get-Date).ToString("HH:mm:ss")
    $line = "[$ts][$Level] $Msg"
    Write-Host $line
    Add-Content -Path (Join-Path $OutDir "run.log") -Value $line -Encoding utf8
}

# ---- screenshots -------------------------------------------------------------
$Global:ShotIndex = 0
function Shot {
    param([string]$Serial, [string]$OutDir, [string]$Name)
    $Global:ShotIndex++
    $idx = "{0:D3}" -f $Global:ShotIndex
    $safe = ($Name -replace '[^\w\-]', '_')
    $path = Join-Path $OutDir "$idx`_$safe.png"
    # screencap to device then pull (reliable across PowerShell binary-stdout quirks)
    & $Global:ADB -s $Serial shell screencap -p /sdcard/_shot.png 2>$null | Out-Null
    & $Global:ADB -s $Serial pull /sdcard/_shot.png $path 2>$null | Out-Null
    return $path
}

# ---- UI Automator dump + element find ---------------------------------------
function Get-UiXml {
    param([string]$Serial, [string]$OutDir)
    & $Global:ADB -s $Serial shell uiautomator dump /sdcard/_ui.xml 2>$null | Out-Null
    $local = Join-Path $OutDir "_ui.xml"
    & $Global:ADB -s $Serial pull /sdcard/_ui.xml $local 2>$null | Out-Null
    if (Test-Path $local) { return Get-Content $local -Raw } else { return "" }
}

# Returns @{x;y;found=$true} center of first node whose resource-id ends with :id/<id>
function Find-ById {
    param([string]$Xml, [string]$Id)
    $rid = "$($Global:PKG):id/$Id"
    $pattern = 'resource-id="' + [regex]::Escape($rid) + '"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $m = [regex]::Match($Xml, $pattern)
    if (-not $m.Success) {
        # try bounds-before-resourceid ordering
        $pattern2 = 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*resource-id="' + [regex]::Escape($rid) + '"'
        $m = [regex]::Match($Xml, $pattern2)
    }
    if ($m.Success) {
        $x1=[int]$m.Groups[1].Value; $y1=[int]$m.Groups[2].Value
        $x2=[int]$m.Groups[3].Value; $y2=[int]$m.Groups[4].Value
        return @{ found=$true; x=[int](($x1+$x2)/2); y=[int](($y1+$y2)/2) }
    }
    return @{ found=$false }
}

# Returns array of @{x;y} centers for ALL nodes whose resource-id ends with :id/<id>
function Find-AllById {
    param([string]$Xml, [string]$Id)
    $rid = "$($Global:PKG):id/$Id"
    $results = @()
    $pattern = 'resource-id="' + [regex]::Escape($rid) + '"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    foreach ($m in [regex]::Matches($Xml, $pattern)) {
        $x1=[int]$m.Groups[1].Value; $y1=[int]$m.Groups[2].Value
        $x2=[int]$m.Groups[3].Value; $y2=[int]$m.Groups[4].Value
        $results += @{ x=[int](($x1+$x2)/2); y=[int](($y1+$y2)/2) }
    }
    return $results
}

# Wait until MainActivity is the resumed activity (returns $true if reached)
function Wait-ForMain {
    param([string]$Serial,[int]$TimeoutSec=15)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $act = Current-Activity $Serial
        if ($act -match "\.ui\.MainActivity") { return $true }
        Start-Sleep -Milliseconds 800
    }
    return $false
}

# Find first node whose text matches (case-insensitive contains)
function Find-ByText {
    param([string]$Xml, [string]$Text)
    $pattern = 'text="([^"]*' + [regex]::Escape($Text) + '[^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $m = [regex]::Match($Xml, $pattern, 'IgnoreCase')
    if ($m.Success) {
        $x1=[int]$m.Groups[2].Value; $y1=[int]$m.Groups[3].Value
        $x2=[int]$m.Groups[4].Value; $y2=[int]$m.Groups[5].Value
        return @{ found=$true; x=[int](($x1+$x2)/2); y=[int](($y1+$y2)/2) }
    }
    return @{ found=$false }
}

# ---- input -------------------------------------------------------------------
function Tap-XY { param([string]$Serial,[int]$X,[int]$Y); & $Global:ADB -s $Serial shell input tap $X $Y 2>$null | Out-Null }

function Tap-Id {
    param([string]$Serial,[string]$OutDir,[string]$Id,[int]$Retries=3)
    for ($i=0; $i -lt $Retries; $i++) {
        $xml = Get-UiXml $Serial $OutDir
        $n = Find-ById $xml $Id
        if ($n.found) { Tap-XY $Serial $n.x $n.y; Start-Sleep -Milliseconds 700; return $true }
        Start-Sleep -Milliseconds 600
    }
    return $false
}

function Tap-Text {
    param([string]$Serial,[string]$OutDir,[string]$Text,[int]$Retries=2)
    for ($i=0; $i -lt $Retries; $i++) {
        $xml = Get-UiXml $Serial $OutDir
        $n = Find-ByText $xml $Text
        if ($n.found) { Tap-XY $Serial $n.x $n.y; Start-Sleep -Milliseconds 700; return $true }
        Start-Sleep -Milliseconds 500
    }
    return $false
}

# Swipe: dir = up|down|left|right ; speedMs small=fast, large=slow
function Swipe {
    param([string]$Serial,[string]$Dir,[int]$DurationMs=300)
    $sz = (& $Global:ADB -s $Serial shell wm size) -replace '.*:\s*',''
    $parts = $sz.Trim() -split 'x'
    $w=[int]$parts[0]; $h=[int]$parts[1]
    $cx=[int]($w/2); $cy=[int]($h/2)
    switch ($Dir) {
        'up'    { $x1=$cx;$y1=[int]($h*0.75);$x2=$cx;$y2=[int]($h*0.25) }
        'down'  { $x1=$cx;$y1=[int]($h*0.25);$x2=$cx;$y2=[int]($h*0.75) }
        'left'  { $x1=[int]($w*0.8);$y1=$cy;$x2=[int]($w*0.2);$y2=$cy }
        'right' { $x1=[int]($w*0.2);$y1=$cy;$x2=[int]($w*0.8);$y2=$cy }
    }
    & $Global:ADB -s $Serial shell input swipe $x1 $y1 $x2 $y2 $DurationMs 2>$null | Out-Null
}

function Press-Back { param([string]$Serial); & $Global:ADB -s $Serial shell input keyevent 4 2>$null | Out-Null; Start-Sleep -Milliseconds 600 }
function Press-Home { param([string]$Serial); & $Global:ADB -s $Serial shell input keyevent 3 2>$null | Out-Null; Start-Sleep -Milliseconds 600 }

# ---- app lifecycle -----------------------------------------------------------
function Clear-AppData { param([string]$Serial); & $Global:ADB -s $Serial shell pm clear $Global:PKG 2>$null | Out-Null }
function Launch-App { param([string]$Serial); & $Global:ADB -s $Serial shell monkey -p $Global:PKG -c android.intent.category.LAUNCHER 1 2>$null | Out-Null; Start-Sleep -Seconds 3 }
function Stop-App { param([string]$Serial); & $Global:ADB -s $Serial shell am force-stop $Global:PKG 2>$null | Out-Null }
function Current-Activity {
    param([string]$Serial)
    $o = & $Global:ADB -s $Serial shell dumpsys activity activities 2>$null | Select-String "mResumedActivity|topResumedActivity" | Select-Object -First 1
    return "$o"
}

# ---- network -----------------------------------------------------------------
function Net-Off {
    param([string]$Serial)
    & $Global:ADB -s $Serial shell svc wifi disable 2>$null | Out-Null
    & $Global:ADB -s $Serial shell svc data disable 2>$null | Out-Null
}
function Net-On {
    param([string]$Serial)
    & $Global:ADB -s $Serial shell svc wifi enable 2>$null | Out-Null
    & $Global:ADB -s $Serial shell svc data enable 2>$null | Out-Null
}

# ---- crash / ANR detection ---------------------------------------------------
function Clear-Logcat { param([string]$Serial); & $Global:ADB -s $Serial logcat -c 2>$null | Out-Null }

# Returns GENUINE app crashes/ANRs only (filters out system-process FATALs and
# distinguishes low-memory kills, which are environmental, not app bugs).
function Get-Crashes {
    param([string]$Serial)
    $hits = @()
    # --- crash buffer: Java FATAL or native crash that belongs to OUR package ---
    $crashText = (& $Global:ADB -s $Serial logcat -b crash -d 2>$null) -join "`n"
    if ($crashText) {
        # split into per-crash blocks on "FATAL EXCEPTION"
        foreach ($block in ($crashText -split "(?=FATAL EXCEPTION)")) {
            if ($block -match "Process:\s*$([regex]::Escape($Global:PKG))") {
                foreach ($ln in ($block -split "`n" | Select-Object -First 6)) { if ($ln.Trim()) { $hits += "JAVA-CRASH: " + $ln.Trim() } }
            }
        }
        # native crash signature
        if ($crashText -match ">>> $([regex]::Escape($Global:PKG)) <<<") {
            $hits += "NATIVE-CRASH: " + (($crashText -split "`n" | Select-String "signal|>>> $([regex]::Escape($Global:PKG))" | Select-Object -First 3) -join " | ")
        }
    }
    # --- main buffer: real ANR for our package ---
    $main = & $Global:ADB -s $Serial logcat -d 2>$null
    if ($main) {
        $hits += ($main | Select-String -Pattern "ANR in $([regex]::Escape($Global:PKG))" | ForEach-Object { "ANR: " + $_.ToString().Trim() })
    }
    return $hits | Select-Object -Unique
}

# Returns low-memory-kill lines for our package (environmental, reported as WARN)
function Get-LmkKills {
    param([string]$Serial)
    $main = & $Global:ADB -s $Serial logcat -d 2>$null
    if (-not $main) { return @() }
    return ($main | Select-String -Pattern "lowmemorykiller:.*Kill '$([regex]::Escape($Global:PKG))'|am_kill.*$([regex]::Escape($Global:PKG))" | ForEach-Object { $_.ToString().Trim() } | Select-Object -Unique)
}

# ---- checkpoint: screenshot + crash scan ------------------------------------
function Checkpoint {
    param([string]$Serial,[string]$OutDir,[string]$Name)
    Shot $Serial $OutDir $Name | Out-Null
    $act = Current-Activity $Serial
    Log-Step $OutDir "Checkpoint '$Name' | $act"
    # Low-memory kills are environmental (AVD RAM), logged as WARN not failure.
    $lmk = Get-LmkKills $Serial
    if ($lmk -and $lmk.Count -gt 0) {
        Log-Step $OutDir "LMK (low-memory kill) of app at '$Name' - environmental, not a crash" "WARN"
        Add-Content -Path (Join-Path $OutDir "LMK.txt") -Value "=== $Name ===" -Encoding utf8
        $lmk | ForEach-Object { Add-Content -Path (Join-Path $OutDir "LMK.txt") -Value $_ -Encoding utf8 }
    }
    $c = Get-Crashes $Serial
    if ($c -and $c.Count -gt 0) {
        Log-Step $OutDir "CRASH/ANR detected at '$Name':" "ERROR"
        foreach ($l in ($c | Select-Object -First 8)) { Log-Step $OutDir "    $l" "ERROR" }
        Add-Content -Path (Join-Path $OutDir "CRASHES.txt") -Value "=== $Name ===" -Encoding utf8
        $c | ForEach-Object { Add-Content -Path (Join-Path $OutDir "CRASHES.txt") -Value $_ -Encoding utf8 }
        return $false
    }
    return $true
}
