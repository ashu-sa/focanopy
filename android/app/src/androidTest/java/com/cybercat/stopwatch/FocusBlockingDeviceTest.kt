package com.cybercat.stopwatch

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith

/** Uses an isolated QA package; no ActivityScenario while intentionally leaving the app. */
@RunWith(AndroidJUnit4::class)
class FocusBlockingDeviceTest {
    private fun waitUntil(timeout:Long,condition:()->Boolean) {
        val deadline=android.os.SystemClock.elapsedRealtime()+timeout
        while(!condition()) {check(android.os.SystemClock.elapsedRealtime()<deadline){"Foreground app did not reach the expected blocking state"};Thread.sleep(100)}
    }
    @Test fun socialOnlyPolicyKeepsBrowserAndSettingsAvailable() {
        if(!BuildConfig.APP_BLOCKING_AVAILABLE)return
        val activity=InstrumentationRegistry.getInstrumentation().targetContext
        check(activity.packageName.endsWith(".qa"))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {Repository.init(activity)}
        val automation=InstrumentationRegistry.getInstrumentation().getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        fun shell(command:String):String=android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use{it.readText().trim()}
        val original=shell("settings get secure enabled_accessibility_services")
        val component="${activity.packageName}/com.cybercat.stopwatch.FocusAccessibilityService"
        val combined=if(original=="null" || original.isBlank())component else "$original:$component"
        try {
            shell("settings put secure enabled_accessibility_services $combined")
            shell("settings put secure accessibility_enabled 1")
            val manager=activity.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
            waitUntil(10000){manager.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any{it.resolveInfo.serviceInfo.name=="com.cybercat.stopwatch.FocusAccessibilityService"}}
            Thread.sleep(500)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                Repository.discard();Repository.selectMode(false)
                Repository.prefs.edit().putBoolean("focusBlocking",true).putBoolean("focusBlockAll",true).putStringSet("blockedApps",setOf("com.android.chrome","com.android.settings","com.android.phone")).putStringSet("excludedApps",emptySet()).putLong("blockSnoozeUntil",0).apply()
                Repository.start()
            }
            fun openChrome(){activity.startActivity(activity.packageManager.getLaunchIntentForPackage("com.android.chrome")!!.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(700)}
            fun chromeVisible()=automation.rootInActiveWindow?.packageName?.toString()=="com.android.chrome"
            openChrome();waitUntil(5000){chromeVisible()}
            Thread.sleep(1400)
            Assert.assertTrue("Legacy selected browser must remain available",chromeVisible())
            Assert.assertTrue("Focus must keep running",Repository.engine.phase=="running")
            activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            waitUntil(5000){automation.rootInActiveWindow?.packageName?.toString()=="com.android.settings"}
            Thread.sleep(1400)
            Assert.assertEquals("com.android.settings",automation.rootInActiveWindow?.packageName?.toString())
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {Repository.discard();Repository.prefs.edit().putBoolean("focusBlocking",false).putLong("blockSnoozeUntil",0).apply()}
            if(original=="null" || original.isBlank())shell("settings delete secure enabled_accessibility_services") else shell("settings put secure enabled_accessibility_services $original")
            shell("input keyevent KEYCODE_HOME")
        }
    }
    @Test fun selectedSocialAppBlocksAndSnoozes() {
        if(!BuildConfig.APP_BLOCKING_AVAILABLE)return
        val activity=InstrumentationRegistry.getInstrumentation().targetContext
        check(activity.packageName.endsWith(".qa"))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {Repository.init(activity)}
        val automation=InstrumentationRegistry.getInstrumentation().getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        fun shell(command:String):String=android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use{it.readText().trim()}
        val original=shell("settings get secure enabled_accessibility_services")
        val component="${activity.packageName}/com.cybercat.stopwatch.FocusAccessibilityService"
        val combined=if(original=="null" || original.isBlank())component else "$original:$component"
        try {
            shell("settings put secure enabled_accessibility_services $combined")
            shell("settings put secure accessibility_enabled 1")
            val manager=activity.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
            waitUntil(10000){manager.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any{it.resolveInfo.serviceInfo.name=="com.cybercat.stopwatch.FocusAccessibilityService"}}
            Thread.sleep(500)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                Repository.discard();Repository.selectMode(false)
                Repository.prefs.edit().putBoolean("focusBlocking",false).putBoolean("focusBlockAll",false).putStringSet("blockedApps",setOf("com.google.android.youtube")).putStringSet("excludedApps",emptySet()).putLong("blockSnoozeUntil",0).apply()
                Repository.start()
            }
            fun openYouTube(){activity.startActivity(activity.packageManager.getLaunchIntentForPackage("com.google.android.youtube")!!.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(700)}
            fun youtubeVisible()=automation.rootInActiveWindow?.packageName?.toString()=="com.google.android.youtube"
            openYouTube();waitUntil(15000){youtubeVisible()}
            InstrumentationRegistry.getInstrumentation().runOnMainSync {Repository.prefs.edit().putBoolean("focusBlocking",true).apply()}
            waitUntil(10000){!youtubeVisible()}
            Assert.assertTrue("blocking must leave the session running",Repository.engine.phase=="running")
            InstrumentationRegistry.getInstrumentation().runOnMainSync {Repository.prefs.edit().putLong("blockSnoozeUntil",System.currentTimeMillis()+5*60000).apply()}
            openYouTube();waitUntil(10000){youtubeVisible()}
            InstrumentationRegistry.getInstrumentation().runOnMainSync {Repository.prefs.edit().putLong("blockSnoozeUntil",0).apply()}
            waitUntil(5000){!youtubeVisible()}
            InstrumentationRegistry.getInstrumentation().runOnMainSync {Repository.pause()}
            openYouTube();waitUntil(10000){youtubeVisible()}
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {Repository.discard();Repository.prefs.edit().putBoolean("focusBlocking",false).putLong("blockSnoozeUntil",0).apply()}
            if(original=="null" || original.isBlank())shell("settings delete secure enabled_accessibility_services") else shell("settings put secure enabled_accessibility_services $original")
            shell("input keyevent KEYCODE_HOME")
        }
    }
}
