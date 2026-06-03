package com.galaxywall.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/**
 * Black-box UI Automator helper for driving com.galaxywall.app.
 *
 * The app uses a Navigation graph with custom views, shows ads, and hands off to
 * the system live-wallpaper picker for parallax/video. A black-box driver is the
 * most durable way to exercise it end to end. Every helper is null-safe so a
 * missing ad / dialog never throws — tests assert on screens we DO reach and on
 * the app process staying alive (no crash / ANR).
 */
object UiTestDriver {

    const val PKG = "com.galaxywall.app"
    private const val LAUNCH_TIMEOUT = 8_000L
    private const val UI_TIMEOUT = 5_000L

    val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val ctx: Context
        get() = ApplicationProvider.getApplicationContext()

    fun shell(cmd: String): String =
        device.executeShellCommand(cmd)

    /**
     * Reset the first-open funnel WITHOUT `pm clear`.
     *
     * Instrumentation runs INSIDE the target app process, so `pm clear` /
     * `am force-stop` on [PKG] would kill the test runner itself ("Process
     * crashed"). Instead we clear the SharedPreferences that gate the funnel
     * (language / survey / onboarding) directly via the app Context.
     */
    fun resetFirstOpen() {
        for (name in listOf("first_open")) {
            ctx.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    /** Full data wipe via shell - ONLY safe from a test in a SEPARATE process. */
    fun clearAppData() {
        shell("pm clear $PKG")
    }

    fun launchFromHome() {
        // Suppress the "Viewing full screen" immersive education overlay that
        // otherwise covers app content on first full-screen entry (Samsung AVD).
        shell("settings put secure immersive_mode_confirmations confirmed")
        device.pressHome()
        device.wait(Until.hasObject(By.pkg(device.launcherPackageName).depth(0)), LAUNCH_TIMEOUT)
        val intent = ctx.packageManager.getLaunchIntentForPackage(PKG)!!.apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        ctx.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(PKG).depth(0)), LAUNCH_TIMEOUT)
    }

    /** True while the app process is alive (used as a crash sentinel). */
    fun appAlive(): Boolean =
        shell("pidof $PKG").trim().isNotEmpty()

    /** Current resumed activity/window - for diagnostics in assertion messages. */
    fun currentWindow(): String =
        (shell("dumpsys activity activities").lines().firstOrNull { it.contains("topResumedActivity") }
            ?: shell("dumpsys window").lines().firstOrNull { it.contains("mCurrentFocus") }
            ?: "unknown").trim()

    // ---- element lookup -----------------------------------------------------
    fun id(id: String): BySelector = By.res(PKG, id)

    fun find(id: String, timeout: Long = UI_TIMEOUT): UiObject2? =
        device.wait(Until.findObject(id(id)), timeout)

    fun exists(id: String, timeout: Long = 1_500L): Boolean =
        device.wait(Until.hasObject(id(id)), timeout) == true

    fun tap(id: String, timeout: Long = UI_TIMEOUT): Boolean {
        // Retry on StaleObjectException: the node can be recycled between the
        // find() and the click() while the screen is animating/transitioning.
        repeat(3) {
            val o = find(id, timeout) ?: return false
            try {
                o.click(); device.waitForIdle(1_500); return true
            } catch (_: StaleObjectException) {
                device.waitForIdle(400)
            }
        }
        return false
    }

    fun tapText(text: String, timeout: Long = 2_000L): Boolean {
        repeat(2) {
            val o = device.wait(Until.findObject(By.textContains(text)), timeout) ?: return false
            try { o.click(); device.waitForIdle(1_500); return true }
            catch (_: StaleObjectException) { device.waitForIdle(400) }
        }
        return false
    }

    fun tapFirstInList(listId: String): Boolean {
        try {
            val list = find(listId) ?: return false
            val child = list.children.firstOrNull() ?: run {
                val b = list.visibleBounds
                device.click(b.centerX(), b.centerY())
                return true
            }
            child.click()
            device.waitForIdle(1_500)
            return true
        } catch (_: StaleObjectException) {
            device.waitForIdle(400); return false
        }
    }

    // ---- gestures -----------------------------------------------------------
    /** speed: larger = faster fling (steps fewer). */
    fun swipe(direction: Direction, speed: Int = 1000) {
        val w = device.displayWidth
        val h = device.displayHeight
        val steps = when {
            speed >= 1500 -> 5     // very fast fling
            speed >= 800  -> 12    // fast
            else          -> 40    // slow drag
        }
        when (direction) {
            Direction.UP    -> device.swipe(w / 2, (h * 0.75).toInt(), w / 2, (h * 0.25).toInt(), steps)
            Direction.DOWN  -> device.swipe(w / 2, (h * 0.25).toInt(), w / 2, (h * 0.75).toInt(), steps)
            Direction.LEFT  -> device.swipe((w * 0.8).toInt(), h / 2, (w * 0.2).toInt(), h / 2, steps)
            Direction.RIGHT -> device.swipe((w * 0.2).toInt(), h / 2, (w * 0.8).toInt(), h / 2, steps)
        }
        device.waitForIdle(1_000)
    }

    fun back() { device.pressBack(); device.waitForIdle(1_000) }
    fun home() { device.pressHome(); device.waitForIdle(1_000) }

    // ---- network ------------------------------------------------------------
    fun networkOff() {
        shell("svc wifi disable")
        shell("svc data disable")
    }

    fun networkOn() {
        shell("svc wifi enable")
        shell("svc data enable")
    }

    // ---- first-open walkthrough --------------------------------------------
    private fun grantNotifications() {
        shell("pm grant $PKG android.permission.POST_NOTIFICATIONS")
    }

    /** True once we appear to be on MainActivity (home grid or bottom nav shown). */
    fun onHome(timeout: Long = 800L): Boolean =
        exists("recycler", timeout) || exists("navHome", timeout) || exists("homeRoot", timeout)

    /**
     * Drive the first-open funnel to Home with a state-machine loop: on each
     * tick, detect whichever screen is up (language / survey / onboarding /
     * permission) and advance it. Far more robust than fixed step counts
     * because order/variant/ad timing can vary. Returns true if Home reached.
     */
    fun completeFirstOpen(timeoutMs: Long = 90_000): Boolean {
        grantNotifications()
        val end = android.os.SystemClock.uptimeMillis() + timeoutMs
        while (android.os.SystemClock.uptimeMillis() < end) {
            tapText("Got it", 300)                        // immersive overlay
            // Detect the current screen by ACTIVITY NAME - far more reliable than
            // probing view ids, since some custom views (survey items) don't
            // expose resource-ids through the accessibility tree on all OEMs.
            val win = currentWindow().lowercase()
            when {
                "mainactivity" in win || onHome(400) -> return true
                "language" in win -> {
                    tapFirstInList("recyclerViewLanguage")
                    tap("imgConfirm", 1_500)
                    waitWhileActivity("language", 4_000)
                }
                "survey" in win -> {
                    selectSurveyTopics()
                    tap("tv_next", 2_000)
                    waitWhileActivity("survey", 4_000)   // advance before re-processing
                }
                "onboarding" in win -> {
                    if (!tap("tv_next", 1_000)) swipe(Direction.LEFT, 800)
                    device.waitForIdle(700)
                }
                "permission" in win -> {
                    tap("button3", 1_500); tapText("Allow", 1_000)
                    waitWhileActivity("permission", 4_000)
                }
                else -> { device.waitForIdle(900) }       // splash / ad loading
            }
        }
        return onHome(1_000)
    }

    /** Select >= 2 survey topics, preferring real ids, falling back to taps. */
    private fun selectSurveyTopics() {
        val items = device.findObjects(id("survey_item"))
        if (!items.isNullOrEmpty()) {
            items.take(3).forEach { try { it.click(); device.waitForIdle(250) } catch (_: Exception) {} }
            return
        }
        // Fallback: survey items may be custom views without exposed ids - tap
        // spread positions across the topic grid (mirrors the adb smoke driver).
        val w = device.displayWidth; val h = device.displayHeight
        device.click((w * 0.25).toInt(), (h * 0.42).toInt()); device.waitForIdle(200)
        device.click((w * 0.75).toInt(), (h * 0.42).toInt()); device.waitForIdle(200)
        device.click((w * 0.25).toInt(), (h * 0.60).toInt()); device.waitForIdle(200)
    }

    /** Wait until the resumed activity no longer contains [marker] (lowercased). */
    private fun waitWhileActivity(marker: String, timeoutMs: Long) {
        val end = android.os.SystemClock.uptimeMillis() + timeoutMs
        while (android.os.SystemClock.uptimeMillis() < end) {
            if (marker !in currentWindow().lowercase()) return
            device.waitForIdle(400)
        }
    }

    /**
     * Skip the funnel deterministically by writing the "done" prefs + granting
     * notifications, then launch straight into Home. Used by tests that aren't
     * about first-open itself, so they reliably start on MainActivity.
     */
    fun markFirstOpenDone() {
        grantNotifications()
        ctx.getSharedPreferences("first_open", Context.MODE_PRIVATE).edit()
            .putString("language_code", "en")
            .putBoolean("complete_survey", true)
            .putBoolean("complete_onboarding", true)
            .commit()
    }

    /**
     * Dismiss an interstitial ad / unexpected full-screen overlay that may
     * intercept taps after launch or screen transitions in this ad-driven app.
     */
    fun dismissInterstitial() {
        tapText("Got it", 400)
        if (tap("ad_close", 400)) return
        if (tap("btnClose", 400)) return
        if (tapText("Close", 500)) return
        // AdMob interstitials expose a content-desc "Close" / "Interstitial close button"
        device.wait(Until.findObject(By.desc("Close")), 500)?.click()
        device.pressBack()
        device.waitForIdle(800)
    }

    /** Deterministically land on Home (funnel skipped), tolerating interstitials. */
    fun ensureHome() {
        markFirstOpenDone()
        repeat(3) {
            launchFromHome()
            device.wait(Until.hasObject(id("navHome")), 6_000)
            if (onHome(1_500)) { goHome(); if (onHome(1_500)) return }
            dismissInterstitial()
        }
        goHome()
    }

    /** Ensure we are on the Home tab of MainActivity. */
    fun goHome() {
        tap("navHome", 2_000)
        device.waitForIdle(1_000)
    }
}
