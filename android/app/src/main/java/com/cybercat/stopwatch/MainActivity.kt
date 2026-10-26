package com.cybercat.stopwatch

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import org.json.JSONObject
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import java.time.*
import java.time.format.DateTimeFormatter

class MainActivity: androidx.activity.ComponentActivity() {
    private val cyan = Color.rgb(173,218,173)
    private val bg = Color.rgb(17,27,23)
    private val surface = Color.rgb(30,44,36)
    private var refreshFocusPermissions:(()->Unit)?=null
    private var shellReady=false
    private var tab = "Timer"
    private var syncTicks=0
    private val composeRevision = mutableIntStateOf(0)
    private var composeProfileOpen by androidx.compose.runtime.mutableStateOf(false)
    private var composeSaveOpen by androidx.compose.runtime.mutableStateOf(false)
    private var nativeSignInJob:Job?=null
    private var composeBlockingOpen by androidx.compose.runtime.mutableStateOf(false)
    private var phase = ""
    private var pomoStage=""
    private var requestOverlay = false
    private val handler=Handler(Looper.getMainLooper())
    private val tick=object:Runnable { override fun run() {
        Repository.advancePomodoro()
        composeRevision.intValue++
        if(tab=="Timer" && (phase!=Repository.engine.phase || pomoStage!=Repository.pomodoro.stage)) render()
        syncTicks++; if(syncTicks%30==0 && Cloud.owner!=null) Cloud.run({ Cloud.sync() })
        handler.postDelayed(this,1000)
    } }
    override fun onCreate(state:Bundle?) { super.onCreate(state); Repository.init(this); if(!BuildConfig.APP_BLOCKING_AVAILABLE)Repository.prefs.edit().putBoolean("focusBlocking",false).apply(); if(state!=null) { tab=state.getString("tab","Timer"); composeSaveOpen=state.getBoolean("saveOpen"); requestOverlay=state.getBoolean("requestOverlay") }; enableEdgeToEdge(statusBarStyle=SystemBarStyle.light(Color.rgb(245,246,239),Color.rgb(245,246,239)),navigationBarStyle=SystemBarStyle.light(Color.rgb(245,246,239),Color.rgb(245,246,239))); render(); handle(intent); if(Repository.engine.phase!="idle" || Repository.prefs.getBoolean("showPet",false)) safe { service("SYNC") } }
    override fun onNewIntent(intent:Intent) { super.onNewIntent(intent); setIntent(intent); handle(intent) }
    private fun handle(intent:Intent?) {
        val callback=intent?.dataString
        if(intent?.action==Intent.ACTION_VIEW && callback!=null && GoogleOAuth.isCallback(callback)) {
            intent.data=null; intent.action=null
            Cloud.run({ Cloud.completeGoogle(callback) }) { error ->
                if(!isFinishing && !isDestroyed) {
                    render()
                    if(error==null) { Toast.makeText(this,"Signed in with Google",Toast.LENGTH_SHORT).show(); Cloud.run({ Cloud.sync() }) }
                    else { accountDialog() }
                }
            }
            return
        }
        if(intent?.action=="CANCEL_POMODORO") cancelPomodoroDialog()
        else if(intent?.action=="FINISH" && !Repository.pomodoroEnabled) safe { Repository.finish(); service("SYNC"); saveDialog() }
        else if(intent?.action=="SAVE" || Repository.engine.phase=="finished") saveDialog()
        if(intent?.action in setOf("CANCEL_POMODORO","FINISH","SAVE")) intent?.action=null
    }
    override fun onResume() {
        super.onResume(); Cloud.changed={ if(!isFinishing && !isDestroyed) render() }; if(Cloud.owner!=null) Cloud.run({ Cloud.sync() }); handler.removeCallbacks(tick); handler.post(tick)
        if(tab=="Settings" || tab=="Timer") render()
        refreshFocusPermissions?.invoke()
        if(requestOverlay && Settings.canDrawOverlays(this)) { Repository.prefs.edit().putBoolean("showPet",true).apply(); service("SHOW"); requestOverlay=false;render() }
    }
    override fun onSaveInstanceState(state:Bundle) {
        state.putString("tab",tab); state.putBoolean("saveOpen",composeSaveOpen); state.putBoolean("requestOverlay",requestOverlay)
        super.onSaveInstanceState(state)
    }
    override fun onPause() { Cloud.changed=null; handler.removeCallbacks(tick); try { Repository.checkpoint() } catch(e:Exception) { Repository.error=e.localizedMessage }; super.onPause() }
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
    private fun safe(action:()->Unit) { try { action() } catch(e:Exception) { AlertDialog.Builder(this).setTitle("Couldn’t complete action").setMessage(e.localizedMessage).setPositiveButton("OK",null).show() } }
    private fun service(action:String) { startForegroundService(Intent(this,TimerService::class.java).setAction(action)) }
    private fun accountDialog() {
        composeProfileOpen = true; render(); return
    }
    private fun focusApps() { composeBlockingOpen=true;render() }
    private fun notifications() { if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),5) }
    private fun text(value:String,size:Float=15f,color:Int=Color.rgb(238,240,220))=TextView(this).apply { text=value; textSize=size; setTextColor(color); includeFontPadding=false; setLineSpacing(dp(3).toFloat(),1f); setPadding(0,dp(5),0,dp(5)) }
    private fun card(color:Int=surface)=GradientDrawable().apply { setColor(color); cornerRadius=dp(16).toFloat() }
    private fun button(value:String,description:String=value,action:()->Unit)=Button(this).apply {
        text=value;contentDescription=description;textSize=14f;isAllCaps=false;setTextColor(cyan);typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);minWidth=0;minimumWidth=0;minHeight=dp(48);minimumHeight=dp(48);setPadding(dp(14),dp(8),dp(14),dp(8));stateListAnimator=null
        val symbol=mapOf("▶" to "play","Ⅱ" to "pause","✓" to "check","×" to "close","↓" to "save","✎" to "edit","‹" to "left","›" to "right","↺" to "reset")[value]
        if(symbol!=null) {text="";setPadding(dp(13),dp(13),dp(13),dp(13));setCompoundDrawablesRelative(UiIcon(symbol,cyan).apply {setBounds(0,0,dp(22),dp(22))},null,null,null)}
        background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Color.argb(40,173,218,173)),card(),null)
        setOnClickListener {safe(action)}
    }
    private fun render() {
        phase=Repository.engine.phase; pomoStage=Repository.pomodoro.stage
        if(!shellReady) {
            shellReady=true
            setContent {
                FocanopyScreen(composeRevision.intValue, tab, { tab=it; render() }, composeProfileOpen, {composeProfileOpen=it}, composeSaveOpen, {composeSaveOpen=it}, composeBlockingOpen, {composeBlockingOpen=it}, ::composeAction)
            }
        }
        composeRevision.intValue++
    }
    private fun composeAction(action:String) { safe {
        when(action) {
            "primary" -> { notifications(); if(Repository.engine.phase=="running") Repository.pause() else if(Repository.engine.phase=="finished") Repository.resumeFinished() else Repository.start(); service("SYNC") }
            "finish" -> { Repository.finish(); service("SYNC") }
            "saved" -> service("SYNC")
            "cancel" -> cancelPomodoroDialog()
            "reset" -> { Repository.discard(); service("SYNC") }
            "google" -> beginGoogleSignIn()
            "sync" -> if(Cloud.owner!=null) Cloud.run({Cloud.sync()}) else beginGoogleSignIn()
            "signout" -> if(Repository.engine.phase=="idle" && !Cloud.busy.get()) {Cloud.signOut();Repository.loadGuest();lifecycleScope.launch {try {NativeGoogle.clearSession(this@MainActivity)}catch(_:Exception){}}}
            "cancelGoogle" -> {nativeSignInJob?.cancel();Cloud.cancelGoogle()}
            "keepLocal" -> Cloud.run({Cloud.resolve(true);Cloud.sync()})
            "keepCloud" -> Cloud.run({Cloud.resolve(false);Cloud.sync()})
            "apps" -> focusApps()
            "accessibility" -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            "blockingDownload" -> startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/ashu-sa/cyber-cat/releases/latest")))
            "preview" -> treePreviewDialog()
            "export" -> exportHistory()
            "notifications" -> startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))
            "overlay" -> {
                val showing=Repository.prefs.getBoolean("showPet",false) && Settings.canDrawOverlays(this)
                if(showing){Repository.prefs.edit().putBoolean("showPet",false).apply();service("HIDE")}
                else {notifications();if(Settings.canDrawOverlays(this)){Repository.prefs.edit().putBoolean("showPet",true).apply();service("SHOW")}else{requestOverlay=true;startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))}}
            }
        }
        render()
    } }
    private fun beginGoogleSignIn() {
        if(Repository.engine.phase!="idle") {Toast.makeText(this,"Finish your timer before connecting an account",Toast.LENGTH_LONG).show();return}
        if(Cloud.owner!=null || Cloud.busy.get() || nativeSignInJob?.isActive==true)return
        Cloud.cancelGoogle()
        nativeSignInJob=lifecycleScope.launch {
            Cloud.nativeGooglePending=true;Cloud.nativeSignInStatus("Choose your Google account in the Android sign-in sheet.");render()
            try {
                val identity=NativeGoogle.signIn(this@MainActivity)
                Cloud.run({Cloud.completeNativeGoogle(identity.token,identity.rawNonce)}) { error ->
                    render();if(error==null)Cloud.run({Cloud.sync()})
                }
            } catch(_:GetCredentialCancellationException) {
                Cloud.nativeSignInStatus("Sign-in cancelled. Your local sessions are safe.")
            } catch(_:NoCredentialException) {
                Cloud.nativeSignInStatus("No Google account is available. Add one in Android Settings, then try again.")
            } catch(_:CancellationException) {
                Cloud.nativeSignInStatus("Sign-in cancelled. Your local sessions are safe.")
            } catch(_:Exception) {
                Cloud.nativeSignInStatus("Google couldn’t connect. Check your connection and Google Play services, then try again.")
            } finally {Cloud.nativeGooglePending=false;render()}
        }
    }
    private fun focusCard():LinearLayout = LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(18),dp(20),dp(18))
        background=GradientDrawable().apply { setColor(surface);cornerRadius=dp(24).toFloat();setStroke(dp(1),Color.rgb(55,76,61)) }
    }
    private fun treePreviewDialog() {
        val dialog=Dialog(this)
        val content=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(22),dp(22),dp(16));background=card(bg)}
        content.addView(text("The life of a little tree",23f,cyan))
        content.addView(text("Fast-forward preview · Your timer and history stay untouched",12f,Color.rgb(161,181,165)))
        val tree=TreeView(this);content.addView(tree,LinearLayout.LayoutParams(-1,dp(200)))
        val stage=text("",17f,cyan).apply {gravity=Gravity.CENTER};content.addView(stage)
        val hint=text("",12f,Color.rgb(161,181,165)).apply {gravity=Gravity.CENTER};content.addView(hint)
        val slider=SeekBar(this).apply {max=27000;contentDescription="Tree lifecycle preview"};content.addView(slider)
        val controls=LinearLayout(this).apply {gravity=Gravity.CENTER};content.addView(controls)
        var position=0f;var playing=true;var last=SystemClock.elapsedRealtime()
        val play=button("Ⅱ","Pause preview") {};val replay=button("↺","Replay preview") {};controls.addView(play,LinearLayout.LayoutParams(dp(54),dp(48)));controls.addView(replay,LinearLayout.LayoutParams(dp(54),dp(48)))
        fun draw() {
            val frame=TreePreviewFrame.at(position)
            tree.previewMillis=frame.elapsed;tree.previewFlower=true;tree.previewDeath=frame.death;tree.previewMotionTime=position.toDouble()
            tree.previewPhase=if(frame.dead)"dead" else if(playing && (position<10 || position>=12 && position<20))"running" else "resting";tree.refresh()
            stage.text=frame.title;hint.text=if(frame.dead)"Water stops. Petals fall. The stem droops and dries." else "25 minutes of focus shown in 10 seconds."
            slider.progress=(position*1000).toInt();play.text="";play.setCompoundDrawablesRelative(UiIcon(if(playing)"pause" else "play",cyan).apply {setBounds(0,0,dp(22),dp(22))},null,null,null);play.contentDescription=if(playing)"Pause preview" else "Play preview"
        }
        val previewHandler=Handler(Looper.getMainLooper())
        lateinit var tick:Runnable
        tick=Runnable {if(dialog.isShowing && playing){val now=SystemClock.elapsedRealtime();position=(position+(now-last)/1000f).coerceAtMost(27f);last=now;if(position>=27)playing=false;draw();if(playing)previewHandler.postDelayed(tick,50)}}
        play.setOnClickListener {if(position>=27)position=0f;playing=!playing;last=SystemClock.elapsedRealtime();previewHandler.removeCallbacks(tick);draw();if(playing)previewHandler.post(tick)}
        replay.setOnClickListener {position=0f;playing=true;last=SystemClock.elapsedRealtime();previewHandler.removeCallbacks(tick);draw();previewHandler.post(tick)}
        slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar:SeekBar?,value:Int,user:Boolean){if(user){position=value/1000f;last=SystemClock.elapsedRealtime();draw()}}
            override fun onStartTrackingTouch(bar:SeekBar?){}
            override fun onStopTrackingTouch(bar:SeekBar?){}
        })
        content.addView(button("Close") {dialog.dismiss()});dialog.setContentView(ScrollView(this).apply {addView(content);isFillViewport=true});dialog.setOnDismissListener {previewHandler.removeCallbacksAndMessages(null)}
        dialog.show();dialog.window?.setBackgroundDrawable(card(bg));dialog.window?.setLayout(resources.displayMetrics.widthPixels-dp(24),(resources.displayMetrics.heightPixels*.88).toInt());draw();previewHandler.post(tick)
    }
    private fun cancelPomodoroDialog() {
        AlertDialog.Builder(this).setTitle("Cancel Pomodoro?")
            .setMessage("An unfinished tree will die and this partial round will not count. Completed 25-minute rounds stay saved.")
            .setNegativeButton("Keep growing",null).setPositiveButton("Cancel round") { _,_->safe { Repository.cancelPomodoro();service("SYNC");render() } }.show()
    }
    private fun saveDialog() { composeSaveOpen=true; render() }
    private fun exportHistory() {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="application/json"; putExtra(Intent.EXTRA_TITLE,"Focanopy-sessions-${LocalDate.now()}.json") },42)
    }
    @Deprecated("Native Activity document result callback")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==42 && resultCode==RESULT_OK) data?.data?.let { uri -> safe {
            val stream=contentResolver.openOutputStream(uri) ?: error("Could not open selected document")
            stream.bufferedWriter().use { it.write(org.json.JSONArray(Repository.sessions).toString(2)) }
            Toast.makeText(this,"Session history exported",Toast.LENGTH_SHORT).show()
        } }
    }
}
