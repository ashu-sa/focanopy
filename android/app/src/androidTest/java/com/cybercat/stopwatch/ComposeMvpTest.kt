package com.cybercat.stopwatch

import androidx.compose.ui.test.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith
import java.time.Instant
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class ComposeMvpTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun clean() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            check(compose.activity.packageName.endsWith(".qa"))
            Repository.prefs.edit().putBoolean("showPet",false).putBoolean("animate",false).putBoolean("focusBlocking",false).putString(Appearance.KEY,"light").apply()
            Repository.discard();Repository.selectMode(false);Repository.replace(emptyList())
            compose.activity.startForegroundService(android.content.Intent(compose.activity,TimerService::class.java).setAction("SYNC"))
        }
        compose.waitUntil(5000) {runCatching {compose.onAllNodesWithContentDescription("Start or resume").fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
    }
    private fun capture(name:String) {
        Thread.sleep(250)
        val bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).takeScreenshot()
        java.io.File(compose.activity.filesDir,"compose-$name.png").outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun startPauseResumeSaveAndRename() {
        capture("focus")
        compose.onNodeWithContentDescription("Start or resume").performClick()
        compose.waitUntil(5000) {Repository.engine.phase=="running"}
        compose.runOnIdle {Repository.engine.accumulated=38*60000L;Repository.engine.anchor=android.os.SystemClock.elapsedRealtime()}
        compose.waitUntil(5000) {compose.onAllNodes(hasText("00:38",substring=true)).fetchSemanticsNodes().isNotEmpty()}
        capture("growing")
        compose.onNodeWithContentDescription("Pause").performClick()
        compose.waitUntil(5000) {Repository.engine.phase=="paused"}
        val elapsed=Repository.engine.elapsed()
        Thread.sleep(150)
        Assert.assertEquals(elapsed,Repository.engine.elapsed())
        compose.onNodeWithContentDescription("Start or resume").performClick()
        compose.onNodeWithContentDescription("Finish session").performClick()
        compose.onNodeWithText("Task name").performTextInput("Compose focus")
        compose.onNodeWithText("Save session").performClick()
        compose.onNodeWithText("Your time, in bloom.").assertIsDisplayed()
        compose.onNodeWithText("Compose focus").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Session options").performClick()
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithText("Task name").performTextReplacement("Polished journal")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Polished journal").assertIsDisplayed()
    }
    @Test fun pomodoroHasCancellationInsteadOfFinish() {
        compose.onNodeWithText("Flower focus").assertIsDisplayed()
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        compose.onNodeWithContentDescription("Start or resume").performClick()
        compose.onNodeWithContentDescription("Cancel round · tree will die").assertIsDisplayed()
        compose.onNodeWithContentDescription("Finish session").assertDoesNotExist()
        compose.runOnIdle {Repository.engine.accumulated=Pomodoro.FOCUS;Repository.advancePomodoro()}
        compose.waitUntil(5000) {Repository.sessions.size==1}
        compose.runOnIdle {Repository.discard()}
    }
    @Test fun calendarTotalsAndProfileAreAccessible() {
        compose.runOnIdle {
            val now=System.currentTimeMillis()
            fun record(name:String,start:Long,length:Long,platform:String)=JSONObject().put("id",java.util.UUID.randomUUID().toString()).put("name",name).put("start",start).put("end",start+length).put("duration",length).put("platform",platform)
            Repository.replace(listOf(record("Upskill",now-7200000,7200000,"android"),record("Interview prep",now-86400000,3600000,"macos"),record("Deep work garden",now-3*86400000,9*3600000,"android")))
        }
        compose.onNodeWithText("Journal").performClick()
        compose.onAllNodesWithText("2h 0m").onFirst().assertExists()
        capture("journal")
        compose.onNodeWithText("Upskill").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Floating tree").assertIsDisplayed()
        capture("settings")
        compose.onNodeWithText("Your Google profile").performClick()
        compose.onNodeWithText("Your grove, everywhere").assertIsDisplayed()
        compose.onNodeWithText("Username").assertDoesNotExist()
        capture("profile")
    }

    @Test fun backgroundTimingAndStandardPermissions() {
        compose.runOnIdle {
            val info=compose.activity.packageManager.getPackageInfo(compose.activity.packageName,android.content.pm.PackageManager.GET_SERVICES)
            if(!BuildConfig.APP_BLOCKING_AVAILABLE) {
                Assert.assertTrue(info.services.orEmpty().none {it.permission=="android.permission.BIND_ACCESSIBILITY_SERVICE"})
                try {Class.forName("com.cybercat.stopwatch.FocusAccessibilityService");error("Accessibility implementation in standard APK")}catch(expected:ClassNotFoundException){}
            }
        }
        compose.onNodeWithContentDescription("Start or resume").performClick()
        val before=Repository.engine.elapsed()
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        Thread.sleep(650)
        Assert.assertTrue(Repository.engine.elapsed()>=before+600)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.onNodeWithContentDescription("Pause").performClick()
        compose.runOnIdle {Repository.discard()}
    }

    @Test fun floatingCardPausesAndDragsWithoutStartingTimer() {
        val automation=InstrumentationRegistry.getInstrumentation().getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        automation.executeShellCommand("appops set ${compose.activity.packageName} SYSTEM_ALERT_WINDOW allow").close()
        Thread.sleep(400)
        val info=automation.serviceInfo
        info.flags=info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo=info
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Repository.prefs.edit().putBoolean("showPet",true).putFloat("opacity",1f).putInt("petX",20).putInt("petY",150).putString(Appearance.KEY,"dark").apply()
            Repository.start();Repository.engine.accumulated=28*60000L;Repository.engine.anchor=android.os.SystemClock.elapsedRealtime()
            compose.activity.startForegroundService(android.content.Intent(compose.activity,TimerService::class.java).setAction("SHOW"))
        }
        fun find(node:android.view.accessibility.AccessibilityNodeInfo?,label:String):android.view.accessibility.AccessibilityNodeInfo? {
            if(node==null)return null
            if(node.contentDescription?.toString()==label)return node
            for(i in 0 until node.childCount)find(node.getChild(i),label)?.let{return it}
            return null
        }
        fun overlay()=automation.windows.mapNotNull{it.root}.firstOrNull{find(it,"Hide tree; stopwatch continues")!=null}
        compose.waitUntil(6000){overlay()!=null}
        capture("floating-dark")
        Assert.assertTrue(find(overlay(),"Pause")!!.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil(5000){Repository.engine.phase=="paused"}
        val bounds=android.graphics.Rect();find(overlay(),"Start or resume")!!.getBoundsInScreen(bounds)
        val before=Repository.prefs.getInt("petX",20)
        val down=android.os.SystemClock.uptimeMillis()
        for((action,delta) in listOf(android.view.MotionEvent.ACTION_DOWN to 0,android.view.MotionEvent.ACTION_MOVE to 55,android.view.MotionEvent.ACTION_UP to 55)) {
            val event=android.view.MotionEvent.obtain(down,android.os.SystemClock.uptimeMillis(),action,bounds.centerX()+delta.toFloat(),bounds.centerY()+delta/2f,0)
            automation.injectInputEvent(event,true);event.recycle();Thread.sleep(30)
        }
        Assert.assertEquals("paused",Repository.engine.phase)
        Assert.assertNotEquals(before,Repository.prefs.getInt("petX",20))
        find(overlay(),"Hide tree; stopwatch continues")!!.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {Repository.discard();Repository.prefs.edit().putBoolean("showPet",false).apply();compose.activity.startForegroundService(android.content.Intent(compose.activity,TimerService::class.java).setAction("SYNC"))}
    }

    @Test fun appBlockingControlsAreAvailableOutsidePomodoro() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Pause distracting apps").performScrollTo().performClick()
        if(BuildConfig.APP_BLOCKING_AVAILABLE) {
            compose.onNodeWithText("Select social apps").assertIsDisplayed()
            compose.onNodeWithText("Block during focus").assertIsDisplayed()
        } else compose.onNodeWithText("App-blocking edition needed").assertIsDisplayed()
        capture("blocking")
    }

    @Test fun darkAppearancePersistsAndKeepsTimer() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Dark").performClick()
        compose.onNodeWithText("Dark").assertIsSelected()
        compose.runOnIdle {Assert.assertTrue(Appearance.dark(compose.activity));Assert.assertEquals("dark",Appearance.mode())}
        capture("dark-settings")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Dark").assertIsSelected()
        compose.onNodeWithText("Focus").performClick()
        compose.onNodeWithContentDescription("Start or resume").performClick()
        compose.runOnIdle {Repository.engine.accumulated=38*60000L;Repository.engine.anchor=android.os.SystemClock.elapsedRealtime()}
        compose.waitUntil(5000) {compose.onAllNodes(hasText("00:38",substring=true)).fetchSemanticsNodes().isNotEmpty()}
        compose.waitForIdle()
        capture("dark-focus")
        val before=Repository.engine.elapsed()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Light").performClick()
        compose.runOnIdle {Assert.assertFalse(Appearance.dark(compose.activity));Assert.assertEquals("running",Repository.engine.phase);Assert.assertTrue(Repository.engine.elapsed()>=before)}
        compose.onNodeWithText("Dark").performClick()
        compose.onNodeWithText("Your Google profile").performClick()
        capture("dark-profile")
        compose.onNodeWithContentDescription("Close profile").performClick()
        compose.onNodeWithText("Journal").performClick()
        capture("dark-journal")
        compose.runOnIdle {Repository.discard()}
    }

}
