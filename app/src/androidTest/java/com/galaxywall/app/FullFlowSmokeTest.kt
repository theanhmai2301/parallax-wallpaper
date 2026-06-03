package com.galaxywall.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.Direction
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * End-to-end black-box smoke test for the Parallax Wallpaper app.
 *
 * Covers the screens and interactions requested for QA:
 *  - first open: language / survey / onboarding / permission
 *  - home, category, favorite, settings tabs
 *  - gestures: up/down/left/right, fast & slow, fling, tab-switch while scrolling
 *  - network on/off (empty state + retry)
 *  - app background/foreground (online & offline)
 *  - language change at runtime
 *  - set-wallpaper flow (image / parallax / video) with re-entry
 *
 * Run:  ./gradlew connectedDebugAndroidTest
 * Or a single test:
 *   ./gradlew connectedDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=com.galaxywall.app.FullFlowSmokeTest
 *
 * Every step asserts the app process is still alive (crash/ANR sentinel).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FullFlowSmokeTest {

    private val d = UiTestDriver

    @Before
    fun ensureOnline() {
        d.networkOn()
    }

    private fun assertAlive(where: String) {
        assertTrue("App crashed / not running during: $where", d.appAlive())
    }

    /** 00 — fresh install: walk the entire first-open funnel into Home. */
    @Test
    fun t00_firstOpen_to_home() {
        // Reset the funnel via prefs (NOT pm clear - that would kill this very
        // instrumentation process, which runs inside the app).
        d.resetFirstOpen()
        d.launchFromHome()
        assertAlive("launch")
        val reachedHome = d.completeFirstOpen()
        assertAlive("first-open funnel")
        // The funnel should land on MainActivity home.
        assertTrue("First-open funnel did not reach Home; at=${d.currentWindow()}", reachedHome)
        d.goHome()
        assertTrue("Home grid not visible after first open; at=${d.currentWindow()}", d.onHome(8_000))
    }

    /** 10 — home gestures: directions, fast & slow, fling, pull-to-refresh. */
    @Test
    fun t10_home_gestures() {
        d.ensureHome()
        d.swipe(Direction.UP, 1600)   // fast fling up
        d.swipe(Direction.UP, 300)    // slow drag up
        d.swipe(Direction.DOWN, 1600) // fast fling down
        d.swipe(Direction.DOWN, 300)  // slow drag down
        repeat(5) { d.swipe(Direction.UP, 1800) } // repeated fast flings
        d.swipe(Direction.LEFT, 800)  // category chips
        d.swipe(Direction.RIGHT, 800)
        assertAlive("home gestures")
    }

    /** 20 — tab switching slow, fast, and while scrolling. */
    @Test
    fun t20_tab_switching() {
        d.ensureHome()
        val tabs = listOf("navCategory", "navFavorite", "navSetting", "navHome")
        // slow
        for (t in tabs) { d.tap(t, 2_000); d.device.waitForIdle(800); assertAlive("slow tab $t") }
        // fast
        repeat(3) { for (t in tabs) { d.tap(t, 1_000) } }
        assertAlive("fast tab switching")
        // switch while scrolling
        d.goHome(); d.swipe(Direction.UP, 1600)
        d.tap("navCategory", 1_000); d.swipe(Direction.UP, 1600)
        d.tap("navFavorite", 1_000)
        assertAlive("scroll-while-switching")
    }

    /** 30 — settings: dark mode, parallax toggle, sensitivity. */
    @Test
    fun t30_settings_toggles() {
        d.ensureHome()
        assertTrue("Settings tab not reachable; at=${d.currentWindow()}", d.tap("navSetting", 3_000))
        d.tap("switchDarkMode", 2_000); d.device.waitForIdle(800)
        d.tap("switchDarkMode", 2_000); d.device.waitForIdle(800)
        d.tap("switchParallax", 1_500)
        d.tap("switchParallax", 1_500)
        assertAlive("settings toggles")
    }

    /** 35 — change language at runtime from settings. */
    @Test
    fun t35_language_change() {
        d.ensureHome()
        d.tap("navSetting", 3_000)
        if (d.tap("rowLanguage", 2_000)) {
            if (d.exists("recyclerViewLanguage", 3_000)) {
                d.tapFirstInList("recyclerViewLanguage")
                if (!d.tap("imgConfirm", 2_000)) d.back()
            }
        }
        assertAlive("language change")
        d.goHome()
        assertTrue("Home not visible after language change", d.onHome(6_000))
    }

    /** 40 — network off then on: empty state + retry recovery. */
    @Test
    fun t40_network_off_on() {
        d.ensureHome()
        d.networkOff()
        d.swipe(Direction.DOWN, 600)        // pull to refresh while offline
        d.device.waitForIdle(2_000)
        // offline navigation should not crash
        d.tap("navCategory", 1_500)
        d.tap("navFavorite", 1_500)
        d.goHome()
        d.tap("emptyRetry", 1_500)          // retry while still offline (if shown)
        assertAlive("offline browsing")
        // recover
        d.networkOn()
        d.device.waitForIdle(3_000)
        d.tap("emptyRetry", 1_500)
        d.swipe(Direction.DOWN, 600)
        d.device.waitForIdle(3_000)
        assertAlive("network recovery")
    }

    /** 50 — background/foreground the app, online and offline. */
    @Test
    fun t50_app_switching() {
        d.ensureHome()
        d.home(); d.device.waitForIdle(1_500)
        d.launchFromHome(); assertAlive("resume online")
        d.home(); d.networkOff(); d.device.waitForIdle(1_500)
        d.launchFromHome(); assertAlive("resume offline")
        d.networkOn()
    }

    /** 60 — set-wallpaper flow with re-entry (home -> item -> preview -> result -> set). */
    @Test
    fun t60_set_wallpaper_flow_reentry() {
        d.ensureHome()
        repeat(3) { iteration ->
            d.goHome()
            // open first wallpaper item
            if (!d.tapFirstInList("recycler")) return@repeat
            d.device.waitForIdle(1_500)
            // preview: swipe pager fast & slow, then Next
            d.swipe(Direction.LEFT, 1600)
            d.swipe(Direction.LEFT, 300)
            d.tap("btnNext", 2_000)
            // optional edit screen: nudge depth slider then Next
            if (d.exists("sliderDepth", 1_500)) {
                d.find("sliderDepth")?.let { s ->
                    val b = s.visibleBounds
                    d.device.click((b.left + b.width() * 0.7).toInt(), b.centerY())
                }
                d.tap("btnNext", 2_000)
            }
            // result: set background
            if (d.tap("btnSetBackground", 2_500)) {
                d.tap("btnSkip", 1_200)               // dismiss ad gate if shown
                d.tapText("Set wallpaper", 1_500)      // system live-wallpaper picker
                d.tapText("Set on both", 1_500)
                d.tapText("Set", 1_200)
                d.tap("btnHome", 1_500)                // success screen
            }
            assertAlive("set-wallpaper iteration $iteration")
            // return to home for the next iteration
            repeat(3) { d.back() }
        }
        d.goHome()
        assertAlive("after set-wallpaper re-entry")
    }
}
