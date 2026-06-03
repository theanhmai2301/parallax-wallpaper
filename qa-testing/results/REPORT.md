# Smoke Test Report - com.galaxywall.app

Generated: 2026-06-03 13:51

## Summary (latest run per device)

| Device | OS / Resolution | Screens | Crashes/ANR | LMK | Reached Home | Result |
|--------|-----------------|---------|-------------|-----|--------------|--------|
| itel_P663LN_real | itel P663LN / Android 13 / 720x1612 | 45 | 0 | - | True | PASS |
| Pixel8Pro | sdk_gphone16k_x86_64 / Android 17 / 1344x2992 | 52 | 0 | - | True | PASS |
| Samsung_Galaxy | sdk_gphone16k_x86_64 / Android 17 / 1080x2340 | 51 | 0 | - | True | PASS |
| Xiaomi_Redmi | sdk_gphone16k_x86_64 / Android 17 / 1080x2400 | 51 | 0 | - | True | PASS |

## Findings & notes

1. **No reproducible app crashes** across first-open, home, category, favorite, settings, preview/edit/result, set-wallpaper, gestures, network on/off, app-switching, and language change on any device.
2. **Immersive overlay** - on the Samsung AVD the system 'Viewing full screen / swipe down to exit' education overlay covered the language list on first full-screen entry and blocked automation. Worked around with `settings put secure immersive_mode_confirmations confirmed`. Worth checking the app handles this overlay gracefully for real first-time users.
3. **Low-memory kill** - the RAM-limited Samsung AVD (2 GB) low-memory-killed the app on first launch under host memory pressure. Not an app bug; mitigated by running one emulator at a time. On low-RAM real devices the app may be killed in background - verify state restoration.
4. **Set-wallpaper (parallax/video)** hands off to the system live-wallpaper picker; the final confirm is outside the app and varies by OEM/locale. Review the `*_set_dialog` / `*_after_set` screenshots per device to confirm wallpaper actually applied.
5. **Parallax smoothness / visual quality** is not auto-scored - inspect screenshots by hand.

---

## itel_P663LN_real

- Device: `itel P663LN | Android 13 | 720x1612`
- Run folder: `itel_P663LN_real_20260603_114929`  (screenshots: 45)
- Reached Home: True | Result: **PASS**
- No app crashes/ANRs detected.

## Pixel8Pro

- Device: `sdk_gphone16k_x86_64 | Android 17 | 1344x2992`
- Run folder: `Pixel8Pro_20260603_114750`  (screenshots: 52)
- Reached Home: True | Result: **PASS**
- No app crashes/ANRs detected.

## Samsung_Galaxy

- Device: `sdk_gphone16k_x86_64 | Android 17 | 1080x2340`
- Run folder: `Samsung_Galaxy_20260603_134357`  (screenshots: 51)
- Reached Home: True | Result: **PASS**
- No app crashes/ANRs detected.

## Xiaomi_Redmi

- Device: `sdk_gphone16k_x86_64 | Android 17 | 1080x2400`
- Run folder: `Xiaomi_Redmi_20260603_120051`  (screenshots: 51)
- Reached Home: True | Result: **PASS**
- No app crashes/ANRs detected.


