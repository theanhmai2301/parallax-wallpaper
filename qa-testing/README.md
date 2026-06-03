# QA Testing - com.galaxywall.app

Hai lớp test cho app wallpaper/parallax:

1. **Smoke test tự động (adb)** - `run_smoke.ps1` lái app qua adb trên thiết bị/emulator bất kỳ,
   đi hết các màn hình, bắn gesture, bật/tắt mạng, đổi ngôn ngữ, chạy flow set wallpaper,
   chụp screenshot từng bước và quét logcat tìm crash/ANR. Không cần build lại app.
2. **Bộ test bền vững (UI Automator)** - `app/src/androidTest/.../FullFlowSmokeTest.kt` chạy bằng
   Gradle, lặp lại được trong CI.

## 1. Smoke test (PowerShell + adb)

```powershell
cd qa-testing
# liệt kê thiết bị
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices

# chạy trên 1 thiết bị (clear data -> test full first-open -> home -> ... -> set wallpaper)
powershell -ExecutionPolicy Bypass -File .\run_smoke.ps1 -Serial <serial> -Label <ten>

# bỏ qua first-open, bắt đầu ở home:
powershell -ExecutionPolicy Bypass -File .\run_smoke.ps1 -Serial <serial> -Label <ten> -KeepData

# tổng hợp báo cáo tất cả lần chạy:
powershell -ExecutionPolicy Bypass -File .\make_report.ps1
```

Kết quả nằm trong `results\<Label>_<timestamp>\`:
- `NNN_*.png` - screenshot từng checkpoint
- `run.log` - nhật ký từng bước + activity hiện tại
- `CRASHES.txt` - chỉ tồn tại nếu phát hiện crash/ANR
- `results\REPORT.md` - báo cáo tổng hợp (sau khi chạy make_report.ps1)

### Các phase được test
- **PHASE 0** first-open: language -> 3 survey variants -> onboarding -> permission (pre-grant) -> home
- **PHASE 1** home gestures: lướt lên/xuống/trái/phải, nhanh & chậm, fling, pull-to-refresh
- **PHASE 2** chuyển tab chậm/nhanh, vừa lướt vừa chuyển tab
- **PHASE 3** settings: dark mode, parallax toggle, đổi ngôn ngữ
- **PHASE 4** mạng off/on: empty state + retry recovery
- **PHASE 5** chuyển app (background/foreground) khi có mạng & mất mạng
- **PHASE 6** set wallpaper: home -> item -> preview -> edit -> result -> set background, lặp 3 lần (re-entry)

## 2. Bộ test UI Automator (Gradle)

```powershell
# chạy toàn bộ trên thiết bị/emulator đang kết nối
.\gradlew connectedDebugAndroidTest

# chỉ 1 class
.\gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.galaxywall.app.FullFlowSmokeTest"
```

Báo cáo HTML: `app/build/reports/androidTests/connected/`.

## Emulator giả lập kích thước Xiaomi / Samsung

Android SDK không có ROM MIUI/One UI thật. Hai AVD sau chạy Android gốc nhưng giả lập
độ phân giải/mật độ của Xiaomi & Samsung để bắt lỗi layout:

| AVD | Resolution | Density |
|-----|-----------|---------|
| `Xiaomi_Redmi_Test`  | 1080x2400 | 440 dpi |
| `Samsung_Galaxy_Test`| 1080x2340 | 420 dpi |

```powershell
$emu = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
& $emu -list-avds
Start-Process $emu -ArgumentList '-avd','Xiaomi_Redmi_Test','-no-snapshot'
```

> Lưu ý RAM: máy ~16GB RAM chỉ nên chạy **1 emulator tại một thời điểm** + thiết bị thật.
> Test các emulator tuần tự, không đồng thời.

## Giới hạn đã biết
- Parallax/video set wallpaper mở **system live-wallpaper picker** (ngoài app) - script
  cố bấm nút "Set" nhưng việc xác nhận cuối có thể khác theo OEM/locale; hãy kiểm tra
  screenshot `*_set_dialog` / `*_after_set`.
- Cảm giác "mượt/giật" của parallax cần **mắt người** đánh giá - script chỉ bắt crash/ANR
  và lưu ảnh, không chấm điểm UX.
