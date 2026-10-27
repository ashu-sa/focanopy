package com.cybercat.stopwatch

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.os.SystemClock

/** App names only; screen contents and typed text are never requested or read. */
class FocusAccessibilityService:AccessibilityService() {
    private var lastNotice=0L
    private var lastPackage:String?=null
    private val handler=android.os.Handler(android.os.Looper.getMainLooper())
    private val watch=object:Runnable {override fun run(){lastPackage?.let{blockIfNeeded(it)};handler.postDelayed(this,1000)}}
    override fun onServiceConnected(){super.onServiceConnected();handler.removeCallbacks(watch);handler.post(watch)}
    override fun onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy()}
    override fun onAccessibilityEvent(event:AccessibilityEvent?) {
        if(event?.eventType!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)return
        lastPackage=event.packageName?.toString()
        lastPackage?.let{blockIfNeeded(it)}
    }
    private fun blockIfNeeded(name:String) {
        Repository.init(this);Repository.advancePomodoro()
        if(Repository.prefs.getLong("blockSnoozeUntil",0)>System.currentTimeMillis())return
        if(!focusBlockingActive(Repository.pomodoroEnabled,Repository.engine.phase,Repository.pomodoro.stage,Repository.prefs.getBoolean("focusBlocking",false)))return
        if(!SocialApps.blocked(name,Repository.prefs.getStringSet("blockedApps",emptySet()).orEmpty()))return
        performGlobalAction(GLOBAL_ACTION_HOME)
        val now=SystemClock.elapsedRealtime()
        if(now-lastNotice>2000) { lastNotice=now;Toast.makeText(this,"Focus is growing. Pause the timer or pause social blocking to allow this app.",Toast.LENGTH_LONG).show() }
    }
    override fun onInterrupt() {}
}
