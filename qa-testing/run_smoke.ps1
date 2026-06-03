# ============================================================================
#  run_smoke.ps1 - Automated smoke/exploratory test for com.galaxywall.app
#  Usage: powershell -ExecutionPolicy Bypass -File run_smoke.ps1 -Serial <serial> -Label <name>
#  Drives first-open -> home -> tabs -> gestures -> network -> set-wallpaper,
#  taking screenshots and scanning logcat for crashes/ANRs at every checkpoint.
# ============================================================================
param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [Parameter(Mandatory=$true)][string]$Label,
    [switch]$KeepData   # if set, do NOT pm clear (skip first-open, start at home)
)

$ErrorActionPreference = "Continue"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here "lib.ps1")

$stamp  = Get-Date -Format "yyyyMMdd_HHmmss"
$OutDir = Join-Path $here "results\$Label`_$stamp"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$Global:ShotIndex = 0

Log-Step $OutDir "===== SMOKE TEST START: $Label ($Serial) ====="
$info = (& $Global:ADB -s $Serial shell getprop ro.product.model).Trim() + " | Android " + (& $Global:ADB -s $Serial shell getprop ro.build.version.release).Trim() + " | " + ((& $Global:ADB -s $Serial shell wm size) -replace '.*:\s*','').Trim()
Log-Step $OutDir "Device: $info"

Net-On $Serial
Clear-Logcat $Serial
# Suppress the "Viewing full screen / swipe down to exit" immersive education
# overlay, which otherwise covers app content on first full-screen entry and
# blocks UI Automator dumps (seen on the Samsung AVD).
& $Global:ADB -s $Serial shell settings put secure immersive_mode_confirmations confirmed 2>$null | Out-Null

# ---------------------------------------------------------------------------
# PHASE 0: First-open flow (fresh install state)
# ---------------------------------------------------------------------------
if (-not $KeepData) {
    Log-Step $OutDir "PHASE 0: First-open flow (clearing app data)"
    Stop-App $Serial
    Clear-AppData $Serial
    & $Global:ADB -s $Serial shell pm grant $Global:PKG android.permission.POST_NOTIFICATIONS 2>$null | Out-Null
    Launch-App $Serial
    Start-Sleep -Seconds 4
    Checkpoint $Serial $OutDir "00_splash" | Out-Null

    # --- Language screen(s) ---
    # Wait for the language list to actually render (slow emulators), then loop:
    # select an item + confirm, until we leave LanguageFO1/FO2. Up to 2 lang screens.
    for ($lg = 0; $lg -lt 2; $lg++) {
        # wait for list to render (up to ~18s)
        $rendered = $false
        for ($w0 = 0; $w0 -lt 12; $w0++) {
            $xml = Get-UiXml $Serial $OutDir
            if ($xml -match "language_item|recyclerViewLanguage") { $rendered = $true; break }
            Start-Sleep -Milliseconds 1500
        }
        # dismiss the immersive "Got it" overlay if it still slipped through
        Tap-Text $Serial $OutDir "Got it" 1 | Out-Null
        if (-not $rendered) { break }   # not a language screen
        Checkpoint $Serial $OutDir "01_language$lg" | Out-Null
        $advanced = $false
        for ($try = 0; $try -lt 6; $try++) {
            $xml = Get-UiXml $Serial $OutDir
            $li = Find-ById $xml "language_item"
            if ($li.found) { Tap-XY $Serial $li.x $li.y; Start-Sleep -Milliseconds 500 }
            Tap-Id $Serial $OutDir "imgConfirm" 1 | Out-Null
            Start-Sleep -Seconds 2
            $act = Current-Activity $Serial
            if ($act -notmatch "LanguageFO") { $advanced = $true; break }
        }
        if ($advanced) { Log-Step $OutDir "Language #$lg confirmed" }
        else { Log-Step $OutDir "Could not advance past language screen #$lg" "WARN" }
        Checkpoint $Serial $OutDir "02_after_language$lg" | Out-Null
        # if we've moved to survey/onboarding/main, stop the language loop
        $act = Current-Activity $Serial
        if ($act -notmatch "LanguageFO") { break }
    }

    # --- Survey screen(s): there are up to 3 chained variants (Survey, Dup, 3) ---
    # Each requires selecting >= 2 topics. Tap the actual survey_item bounds.
    for ($sv = 0; $sv -lt 3; $sv++) {
        $xml = Get-UiXml $Serial $OutDir
        if ($xml -notmatch "survey_item|topic_image|recycler_view") { break }
        Log-Step $OutDir "Survey variant #$sv detected; selecting topics"
        $items = Find-AllById $xml "survey_item"
        if ($items.Count -eq 0) { $items = Find-AllById $xml "topic_image" }
        $pick = [Math]::Min(3, $items.Count)
        for ($k = 0; $k -lt $pick; $k++) { Tap-XY $Serial $items[$k].x $items[$k].y; Start-Sleep -Milliseconds 250 }
        if ($pick -eq 0) {
            # fallback: spread taps
            $sz = ((& $Global:ADB -s $Serial shell wm size) -replace '.*:\s*','').Trim() -split 'x'
            $w=[int]$sz[0]; $h=[int]$sz[1]
            Tap-XY $Serial ([int]($w*0.25)) ([int]($h*0.40)); Tap-XY $Serial ([int]($w*0.75)) ([int]($h*0.40)); Tap-XY $Serial ([int]($w*0.25)) ([int]($h*0.58))
        }
        Checkpoint $Serial $OutDir "03_survey${sv}_selected" | Out-Null
        if (-not (Tap-Id $Serial $OutDir "tv_next" 2)) { Swipe $Serial "left" 250 }
        Start-Sleep -Seconds 2
        Checkpoint $Serial $OutDir "04_after_survey$sv" | Out-Null
    }

    # --- Onboarding pager (only when genuinely on onboarding) ---
    for ($i = 0; $i -lt 5; $i++) {
        $xml = Get-UiXml $Serial $OutDir
        if ($xml -match "img_onboarding|viewPager2|tv_des_onboard") {
            if (-not (Tap-Id $Serial $OutDir "tv_next" 1)) { Swipe $Serial "left" 250 }
            Start-Sleep -Milliseconds 900
        } else { break }
    }
    Checkpoint $Serial $OutDir "05_after_onboarding" | Out-Null

    # --- Permission screen (Android 13+) ---
    $xml = Get-UiXml $Serial $OutDir
    if ($xml -match "switchNotif|button3|permTitle") {
        Log-Step $OutDir "Permission screen; pre-granting POST_NOTIFICATIONS then continuing"
        # Pre-grant so the system dialog never blocks the funnel (locale-proof).
        & $Global:ADB -s $Serial shell pm grant $Global:PKG android.permission.POST_NOTIFICATIONS 2>$null | Out-Null
        Tap-Id $Serial $OutDir "button3" | Out-Null
        Start-Sleep -Seconds 1
        # If a system grant dialog still shows, click its allow button by id (locale-proof).
        $sx = Get-UiXml $Serial $OutDir
        $m = [regex]::Match($sx, 'resource-id="com\.android\.permissioncontroller:id/permission_allow_button"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
        if ($m.Success) {
            $cx=[int](([int]$m.Groups[1].Value+[int]$m.Groups[3].Value)/2); $cy=[int](([int]$m.Groups[2].Value+[int]$m.Groups[4].Value)/2)
            Tap-XY $Serial $cx $cy
        }
        Start-Sleep -Seconds 1
        Checkpoint $Serial $OutDir "06_after_permission" | Out-Null
    }
}
else {
    Log-Step $OutDir "PHASE 0 skipped (-KeepData): launching at home"
    Stop-App $Serial; Launch-App $Serial
}

# wait until we land on MainActivity (home)
if (Wait-ForMain $Serial 20) {
    Log-Step $OutDir "Reached MainActivity (home)"
} else {
    Log-Step $OutDir "DID NOT reach MainActivity after first-open; current=$(Current-Activity $Serial)" "WARN"
    # last-ditch: tap any visible next/confirm then re-check
    Tap-Id $Serial $OutDir "tv_next" 1 | Out-Null
    Tap-Id $Serial $OutDir "imgConfirm" 1 | Out-Null
    Wait-ForMain $Serial 8 | Out-Null
}
Start-Sleep -Seconds 2
Checkpoint $Serial $OutDir "10_home" | Out-Null

# ---------------------------------------------------------------------------
# PHASE 1: Home gestures (fast & slow, all directions)
# ---------------------------------------------------------------------------
Log-Step $OutDir "PHASE 1: Home gestures"
Swipe $Serial "up" 120;   Checkpoint $Serial $OutDir "11_home_swipe_up_fast" | Out-Null
Swipe $Serial "up" 800;   Checkpoint $Serial $OutDir "12_home_swipe_up_slow" | Out-Null
Swipe $Serial "down" 120; Checkpoint $Serial $OutDir "13_home_swipe_down_fast" | Out-Null
Swipe $Serial "down" 800; Checkpoint $Serial $OutDir "14_home_swipe_down_slow" | Out-Null
# fling many times fast
for ($i=0;$i -lt 5;$i++){ Swipe $Serial "up" 80 }
Checkpoint $Serial $OutDir "15_home_fling" | Out-Null
# category chips horizontal swipe (left/right)
Swipe $Serial "left" 200;  Checkpoint $Serial $OutDir "16_home_swipe_left" | Out-Null
Swipe $Serial "right" 200; Checkpoint $Serial $OutDir "17_home_swipe_right" | Out-Null
# swipe-refresh (pull down from top)
$sz = ((& $Global:ADB -s $Serial shell wm size) -replace '.*:\s*','').Trim() -split 'x'
$w=[int]$sz[0]; $h=[int]$sz[1]
& $Global:ADB -s $Serial shell input swipe ([int]($w/2)) ([int]($h*0.2)) ([int]($w/2)) ([int]($h*0.7)) 400 | Out-Null
Start-Sleep -Seconds 2
Checkpoint $Serial $OutDir "18_home_pull_refresh" | Out-Null

# ---------------------------------------------------------------------------
# PHASE 2: Tab switching (fast & slow) + scroll-while-switching
# ---------------------------------------------------------------------------
Log-Step $OutDir "PHASE 2: Tab switching"
$tabs = @("navCategory","navFavorite","navSetting","navHome")
# slow switch
foreach ($t in $tabs) { Tap-Id $Serial $OutDir $t 1 | Out-Null; Start-Sleep -Seconds 1; Checkpoint $Serial $OutDir "20_tab_$t" | Out-Null }
# fast switch (rapid)
foreach ($r in 1..2) { foreach ($t in $tabs) { Tap-Id $Serial $OutDir $t 1 | Out-Null; Start-Sleep -Milliseconds 250 } }
Checkpoint $Serial $OutDir "21_tabs_fast" | Out-Null
# switch tab WHILE scrolling
Tap-Id $Serial $OutDir "navHome" 1 | Out-Null
Swipe $Serial "up" 100
Tap-Id $Serial $OutDir "navCategory" 1 | Out-Null
Swipe $Serial "up" 100
Tap-Id $Serial $OutDir "navFavorite" 1 | Out-Null
Checkpoint $Serial $OutDir "22_scroll_while_switching" | Out-Null

# ---------------------------------------------------------------------------
# PHASE 3: Settings (dark mode, parallax toggle, sensitivity, language)
# ---------------------------------------------------------------------------
Log-Step $OutDir "PHASE 3: Settings"
Tap-Id $Serial $OutDir "navSetting" 1 | Out-Null
Start-Sleep -Seconds 1
Checkpoint $Serial $OutDir "30_settings" | Out-Null
Tap-Id $Serial $OutDir "switchDarkMode" 1 | Out-Null; Start-Sleep -Milliseconds 800
Checkpoint $Serial $OutDir "31_dark_on" | Out-Null
Tap-Id $Serial $OutDir "switchDarkMode" 1 | Out-Null; Start-Sleep -Milliseconds 800
Checkpoint $Serial $OutDir "32_dark_off" | Out-Null
Tap-Id $Serial $OutDir "switchParallax" 1 | Out-Null; Start-Sleep -Milliseconds 500
Checkpoint $Serial $OutDir "33_parallax_toggle" | Out-Null
Tap-Id $Serial $OutDir "switchParallax" 1 | Out-Null; Start-Sleep -Milliseconds 500

# --- Language change from settings ---
if (Tap-Id $Serial $OutDir "rowLanguage" 1) {
    Start-Sleep -Seconds 1
    Checkpoint $Serial $OutDir "34_language_list" | Out-Null
    # pick second language item then confirm
    $xml = Get-UiXml $Serial $OutDir
    $li = Find-ById $xml "language_item"
    if ($li.found) { Tap-XY $Serial $li.x $li.y; Start-Sleep -Milliseconds 500 }
    if (-not (Tap-Id $Serial $OutDir "imgConfirm" 1)) { Press-Back $Serial }
    Start-Sleep -Seconds 1
    Checkpoint $Serial $OutDir "35_after_language_change" | Out-Null
}

# ---------------------------------------------------------------------------
# PHASE 4: Network off / on  (empty state + retry)
# ---------------------------------------------------------------------------
Log-Step $OutDir "PHASE 4: Network off/on"
Tap-Id $Serial $OutDir "navHome" 1 | Out-Null
Start-Sleep -Seconds 1
Net-Off $Serial
Log-Step $OutDir "Network OFF"
Start-Sleep -Seconds 2
# force reload while offline (pull refresh)
& $Global:ADB -s $Serial shell input swipe ([int]($w/2)) ([int]($h*0.2)) ([int]($w/2)) ([int]($h*0.7)) 400 | Out-Null
Start-Sleep -Seconds 3
Checkpoint $Serial $OutDir "40_home_offline" | Out-Null
# try navigating tabs while offline
Tap-Id $Serial $OutDir "navCategory" 1 | Out-Null; Start-Sleep -Milliseconds 800
Checkpoint $Serial $OutDir "41_category_offline" | Out-Null
Tap-Id $Serial $OutDir "navFavorite" 1 | Out-Null; Start-Sleep -Milliseconds 800
Checkpoint $Serial $OutDir "42_favorite_offline" | Out-Null
# offline: open an item if any cached
Tap-Id $Serial $OutDir "navHome" 1 | Out-Null
# retry button if present
if (Tap-Id $Serial $OutDir "emptyRetry" 1) { Log-Step $OutDir "Tapped retry (still offline)" }
Checkpoint $Serial $OutDir "43_offline_retry" | Out-Null
# back online
Net-On $Serial
Log-Step $OutDir "Network ON"
Start-Sleep -Seconds 4
if (Tap-Id $Serial $OutDir "emptyRetry" 1) { Log-Step $OutDir "Tapped retry (online)" }
& $Global:ADB -s $Serial shell input swipe ([int]($w/2)) ([int]($h*0.2)) ([int]($w/2)) ([int]($h*0.7)) 400 | Out-Null
Start-Sleep -Seconds 4
Checkpoint $Serial $OutDir "44_home_back_online" | Out-Null

# ---------------------------------------------------------------------------
# PHASE 5: App switching (background/foreground), online & offline
# ---------------------------------------------------------------------------
Log-Step $OutDir "PHASE 5: App switching"
Press-Home $Serial; Start-Sleep -Seconds 2
Launch-App $Serial; Start-Sleep -Seconds 2
Checkpoint $Serial $OutDir "50_resume_online" | Out-Null
# go background, kill network, resume
Press-Home $Serial; Net-Off $Serial; Start-Sleep -Seconds 2
Launch-App $Serial; Start-Sleep -Seconds 2
Checkpoint $Serial $OutDir "51_resume_offline" | Out-Null
Net-On $Serial; Start-Sleep -Seconds 3

# ---------------------------------------------------------------------------
# PHASE 6: Set-wallpaper flow + re-entry permutations
#   Drives: home -> item -> preview -> next -> (edit) -> result -> set background
#   Repeats several times to cover "set then set again" re-entry.
# ---------------------------------------------------------------------------
Log-Step $OutDir "PHASE 6: Set-wallpaper flows"
function Open-FirstItem {
    param([string]$Serial,[string]$OutDir)
    $xml = Get-UiXml $Serial $OutDir
    $card = Find-ById $xml "card"
    if (-not $card.found) { $card = Find-ById $xml "thumb" }
    if ($card.found) { Tap-XY $Serial $card.x $card.y; Start-Sleep -Seconds 2; return $true }
    # fallback: tap grid area
    Tap-XY $Serial ([int]($w*0.3)) ([int]($h*0.4)); Start-Sleep -Seconds 2; return $true
}
function Run-SetFlow {
    param([string]$Serial,[string]$OutDir,[string]$Tag)
    Tap-Id $Serial $OutDir "navHome" 1 | Out-Null; Start-Sleep -Milliseconds 800
    Open-FirstItem $Serial $OutDir | Out-Null
    Checkpoint $Serial $OutDir "60_${Tag}_preview" | Out-Null
    # preview -> swipe pager fast/slow then next
    Swipe $Serial "left" 120; Swipe $Serial "left" 600
    Tap-Id $Serial $OutDir "btnNext" 1 | Out-Null; Start-Sleep -Seconds 1
    Checkpoint $Serial $OutDir "61_${Tag}_edit_or_result" | Out-Null
    # if edit screen, adjust slider then next
    $xml = Get-UiXml $Serial $OutDir
    if ($xml -match "sliderDepth|editPreview") {
        $s = Find-ById $xml "sliderDepth"
        if ($s.found) { Tap-XY $Serial ([int]($w*0.7)) $s.y }
        Tap-Id $Serial $OutDir "btnNext" 1 | Out-Null; Start-Sleep -Seconds 1
        Checkpoint $Serial $OutDir "62_${Tag}_result" | Out-Null
    }
    # result -> set background
    if (Tap-Id $Serial $OutDir "btnSetBackground" 1) {
        Start-Sleep -Seconds 2
        Checkpoint $Serial $OutDir "63_${Tag}_set_dialog" | Out-Null
        # ad gate?
        if (Tap-Id $Serial $OutDir "btnSkip" 1) { Start-Sleep -Seconds 1 }
        # system live-wallpaper picker may appear: try common "Set wallpaper" buttons
        Tap-Text $Serial $OutDir "Set wallpaper" 1 | Out-Null
        Tap-Text $Serial $OutDir "Set on both" 1 | Out-Null
        Tap-Text $Serial $OutDir "Set" 1 | Out-Null
        Start-Sleep -Seconds 2
        Checkpoint $Serial $OutDir "64_${Tag}_after_set" | Out-Null
        # success screen -> home
        Tap-Id $Serial $OutDir "btnHome" 1 | Out-Null
    } else {
        Log-Step $OutDir "btnSetBackground not found for $Tag" "WARN"
    }
    # ensure back at home
    for ($i=0;$i -lt 3;$i++){ Press-Back $Serial }
    Tap-Id $Serial $OutDir "navHome" 1 | Out-Null
}

# Run the flow repeatedly to cover set-then-set re-entry (image/parallax/video
# depend on which grid items exist; categories may filter type).
Run-SetFlow $Serial $OutDir "set1"
Run-SetFlow $Serial $OutDir "set2"
# switch category (may change wallpaper type) then set again
Tap-Id $Serial $OutDir "navHome" 1 | Out-Null
Swipe $Serial "left" 200  # category chips
$xml = Get-UiXml $Serial $OutDir
$chip = Find-ById $xml "categoryChips"
if ($chip.found) { Tap-XY $Serial $chip.x $chip.y; Start-Sleep -Seconds 2 }
Run-SetFlow $Serial $OutDir "set3"

# ---------------------------------------------------------------------------
# WRAP UP
# ---------------------------------------------------------------------------
Tap-Id $Serial $OutDir "navHome" 1 | Out-Null
Checkpoint $Serial $OutDir "90_final_home" | Out-Null

$crashFile = Join-Path $OutDir "CRASHES.txt"
$shots = (Get-ChildItem $OutDir -Filter *.png | Measure-Object).Count
Log-Step $OutDir "===== SMOKE TEST DONE: $Label | screenshots=$shots ====="
if (Test-Path $crashFile) {
    Log-Step $OutDir "RESULT: CRASHES/ANRs FOUND - see CRASHES.txt" "ERROR"
} else {
    Log-Step $OutDir "RESULT: no crashes/ANRs detected" "INFO"
}
Write-Output "DONE:${Label}:$OutDir"
