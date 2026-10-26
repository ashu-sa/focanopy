package com.cybercat.stopwatch

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BlockingSheet(revision:Int,onAction:(String)->Unit,onClose:()->Unit) {
    val context=LocalContext.current
    val prefs=Repository.prefs
    var enabled by remember {mutableStateOf(prefs.getBoolean("focusBlocking",false))}
    var all by remember {mutableStateOf(prefs.getBoolean("focusBlockAll",false))}
    var selected by remember {mutableStateOf(prefs.getStringSet("blockedApps",emptySet()).orEmpty().toSet())}
    var excluded by remember {mutableStateOf(prefs.getStringSet("excludedApps",emptySet()).orEmpty().toSet())}
    var search by remember {mutableStateOf("")}
    var apps by remember {mutableStateOf<List<android.content.pm.ResolveInfo>>(emptyList())}
    var browsers by remember {mutableStateOf<Set<String>>(emptySet())}
    val component=ComponentName(context.packageName,FocusServiceName).flattenToString()
    val granted=remember(revision) {(Settings.Secure.getString(context.contentResolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "").split(':').any {it.equals(component,true)}}
    val paused=prefs.getLong("blockSnoozeUntil",0)>System.currentTimeMillis()
    fun persist() {prefs.edit().putBoolean("focusBlocking",enabled).putBoolean("focusBlockAll",all).putStringSet("blockedApps",selected).putStringSet("excludedApps",excluded).apply();onAction("refresh")}
    LaunchedEffect(Unit) {
        if(!BuildConfig.APP_BLOCKING_AVAILABLE)return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val pm=context.packageManager
            val homes=pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0).map{it.activityInfo.packageName}.toSet()
            val keyboards=(context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).enabledInputMethodList.map{it.packageName}.toSet()
            val protected=homes+keyboards+setOf(context.packageName,"com.android.settings","com.android.phone","com.google.android.dialer","com.android.dialer")
            val installed=pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0).filter{it.activityInfo.packageName !in protected}.distinctBy{it.activityInfo.packageName}.sortedBy{it.loadLabel(pm).toString().lowercase()}
            val browserApps=pm.queryIntentActivities(Intent(Intent.ACTION_VIEW,Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE),PackageManager.GET_RESOLVED_FILTER).filter{it.filter?.countDataAuthorities()==0}.map{it.activityInfo.packageName}.toSet()
            withContext(Dispatchers.Main) {apps=installed;browsers=browserApps}
        }
    }
    LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f),contentPadding=PaddingValues(24.dp,0.dp,24.dp,32.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Row(verticalAlignment=Alignment.CenterVertically) {Text("Protect your focus",fontSize=23.sp,modifier=Modifier.weight(1f));IconButton(onClick=onClose){Icon(Icons.Default.Close,"Close app blocking")}}}
        item {Text("Give distracting apps a little time out. Calls, your keyboard and essential system screens stay available.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(!BuildConfig.APP_BLOCKING_AVAILABLE) {
            item {Card {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Shield,null)
                Text("App-blocking edition needed",fontSize=18.sp)
                Text("The standard edition keeps Accessibility access out of the app. Install the separate app-blocking APK to choose social apps and other distractions. Your history stays with the same app.",fontSize=13.sp)
                Button(onClick={onAction("blockingDownload")}){Text("Get app-blocking edition")}
            }}}
        } else {
            item {Card {Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)){Text("Block during focus");Text("Released when you pause or take a break",fontSize=11.sp)}
                Switch(enabled,onCheckedChange={enabled=it;persist()},modifier=Modifier.semantics {contentDescription="Block during focus"})
            }}}
            item {Card {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(if(granted)"Accessibility access is ready" else "Allow app-blocking access",fontSize=15.sp)
                Text("Used to detect app openings and return to Home. Screen contents and typing are not read.",fontSize=12.sp)
                TextButton(onClick={onAction("accessibility")}){Text(if(granted)"Manage access" else "Enable access")}
            }}}
            item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={selected=selected+apps.map{it.activityInfo.packageName}.filter{it in socialPackages};excluded=excluded-socialPackages;all=false;enabled=true;persist()},modifier=Modifier.weight(1f)){Text("Select social apps",fontSize=11.sp)}
                OutlinedButton(onClick={selected=emptySet();all=false;persist()},modifier=Modifier.weight(1f)){Text("Clear selection",fontSize=11.sp)}
            }}
            item {Row(verticalAlignment=Alignment.CenterVertically) {
                Text("All apps except browsers",fontSize=13.sp,modifier=Modifier.weight(1f))
                Switch(all,onCheckedChange={all=it;persist()},modifier=Modifier.semantics {contentDescription="All apps except browsers"})
            }}
            item {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(if(paused)"Blocks paused" else "Need a quick check-in?",fontSize=12.sp,modifier=Modifier.weight(1f))
                TextButton(onClick={prefs.edit().putLong("blockSnoozeUntil",if(paused)0 else System.currentTimeMillis()+5*60000).apply();onAction("refresh")}){Text(if(paused)"Resume blocks" else "Pause for 5 min",fontSize=12.sp)}
            }}
            item {OutlinedTextField(search,{search=it},label={Text("Find an app")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp))}
            items(apps.filter{it.loadLabel(context.packageManager).toString().contains(search,true)},key={it.activityInfo.packageName}) {app->
                val id=app.activityInfo.packageName
                val browserAllowed=all && id in browsers
                val on=!browserAllowed && id !in excluded && (all || id in selected)
                Card {Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    AndroidView(factory={android.widget.ImageView(it).apply {importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO}},update={it.setImageDrawable(app.loadIcon(context.packageManager))},modifier=Modifier.size(34.dp))
                    Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(app.loadLabel(context.packageManager).toString(),fontSize=14.sp);Text(if(browserAllowed)"Browser · allowed" else if(on)"Blocked while focusing" else "Allowed",fontSize=11.sp)}
                    Switch(on,onCheckedChange={value->if(all)excluded=if(value)excluded-id else excluded+id else {selected=if(value)selected+id else selected-id;if(value)excluded=excluded-id};persist()},enabled=!browserAllowed,modifier=Modifier.semantics {contentDescription="Block ${app.loadLabel(context.packageManager)}"})
                }}
            }
        }
    }
}
private const val FocusServiceName="com.cybercat.stopwatch.FocusAccessibilityService"
private val socialPackages=setOf("com.instagram.android","com.facebook.katana","com.facebook.orca","com.twitter.android","com.reddit.frontpage","com.snapchat.android","com.zhiliaoapp.musically","com.ss.android.ugc.trill","com.pinterest","org.telegram.messenger","com.discord","com.whatsapp","com.google.android.youtube")
