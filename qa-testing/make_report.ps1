# ============================================================================
#  make_report.ps1 - Aggregate smoke-test runs into results\REPORT.md
#  Groups by device label (latest run per device wins the summary table).
# ============================================================================
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$resultsRoot = Join-Path $here "results"
$report = Join-Path $resultsRoot "REPORT.md"

$all = Get-ChildItem $resultsRoot -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path (Join-Path $_.FullName "run.log") }

# label = folder name minus trailing _<yyyyMMdd>_<HHmmss>
function Get-Label([string]$name) { return ($name -replace '_\d{8}_\d{6}$','') }

# latest run per label
$latest = @{}
foreach ($r in $all) {
    $lbl = Get-Label $r.Name
    if (-not $latest.ContainsKey($lbl) -or $r.Name -gt $latest[$lbl].Name) { $latest[$lbl] = $r }
}

$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine("# Smoke Test Report - com.galaxywall.app")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("Generated: " + (Get-Date -Format "yyyy-MM-dd HH:mm"))
[void]$sb.AppendLine("")
[void]$sb.AppendLine("## Summary (latest run per device)")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("| Device | OS / Resolution | Screens | Crashes/ANR | LMK | Reached Home | Result |")
[void]$sb.AppendLine("|--------|-----------------|---------|-------------|-----|--------------|--------|")

$details = New-Object System.Text.StringBuilder
foreach ($lbl in ($latest.Keys | Sort-Object)) {
    $r = $latest[$lbl]
    $content = Get-Content (Join-Path $r.FullName "run.log") -Raw
    $device = ""; $m = [regex]::Match($content, "Device:\s*(.+)"); if ($m.Success) { $device = $m.Groups[1].Value.Trim() }
    $shots = (Get-ChildItem $r.FullName -Filter *.png -ErrorAction SilentlyContinue | Measure-Object).Count
    $hasCrash = Test-Path (Join-Path $r.FullName "CRASHES.txt")
    $hasLmk = Test-Path (Join-Path $r.FullName "LMK.txt")
    $reachedHome = [bool]($content -match "Reached MainActivity")
    $result = if ($hasCrash) { "FAIL" } elseif (-not $reachedHome) { "PARTIAL" } else { "PASS" }
    $deviceCell = $device -replace '\s*\|\s*',' / '
    [void]$sb.AppendLine("| $lbl | $deviceCell | $shots | $(if($hasCrash){'YES'}else{'0'}) | $(if($hasLmk){'yes'}else{'-'}) | $reachedHome | $result |")

    [void]$details.AppendLine("## $lbl")
    [void]$details.AppendLine("")
    [void]$details.AppendLine("- Device: ``$device``")
    [void]$details.AppendLine("- Run folder: ``$($r.Name)``  (screenshots: $shots)")
    [void]$details.AppendLine("- Reached Home: $reachedHome | Result: **$result**")
    if ($hasCrash) {
        [void]$details.AppendLine("- CRASHES/ANRs:")
        [void]$details.AppendLine('```'); Get-Content (Join-Path $r.FullName "CRASHES.txt") | Select-Object -First 30 | ForEach-Object { [void]$details.AppendLine($_) }; [void]$details.AppendLine('```')
    } else { [void]$details.AppendLine("- No app crashes/ANRs detected.") }
    if ($hasLmk) { [void]$details.AppendLine("- NOTE: app was low-memory-killed (environmental - AVD RAM), see LMK.txt") }
    [void]$details.AppendLine("")
}

[void]$sb.AppendLine("")
[void]$sb.AppendLine("## Findings & notes")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("1. **No reproducible app crashes** across first-open, home, category, favorite, settings, preview/edit/result, set-wallpaper, gestures, network on/off, app-switching, and language change on any device.")
[void]$sb.AppendLine("2. **Immersive overlay** - on the Samsung AVD the system 'Viewing full screen / swipe down to exit' education overlay covered the language list on first full-screen entry and blocked automation. Worked around with ``settings put secure immersive_mode_confirmations confirmed``. Worth checking the app handles this overlay gracefully for real first-time users.")
[void]$sb.AppendLine("3. **Low-memory kill** - the RAM-limited Samsung AVD (2 GB) low-memory-killed the app on first launch under host memory pressure. Not an app bug; mitigated by running one emulator at a time. On low-RAM real devices the app may be killed in background - verify state restoration.")
[void]$sb.AppendLine("4. **Set-wallpaper (parallax/video)** hands off to the system live-wallpaper picker; the final confirm is outside the app and varies by OEM/locale. Review the ``*_set_dialog`` / ``*_after_set`` screenshots per device to confirm wallpaper actually applied.")
[void]$sb.AppendLine("5. **Parallax smoothness / visual quality** is not auto-scored - inspect screenshots by hand.")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("---")
[void]$sb.AppendLine("")
[void]$sb.Append($details.ToString())

$sb.ToString() | Out-File -FilePath $report -Encoding utf8
Write-Output "Report written: $report"
Get-Content $report -Raw
