package com.cybercat.stopwatch

import android.content.ComponentName
import android.content.pm.PackageManager
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
    var selected by remember {mutableStateOf(SocialApps.selection(prefs.getStringSet("blockedApps",emptySet()).orEmpty()))}
    var search by remember {mutableStateOf("")}
    var apps by remember {mutableStateOf<List<android.content.pm.ApplicationInfo>>(emptyList())}
    val component=ComponentName(context.packageName,FocusServiceName).flattenToString()
    val granted=remember(revision) {(Settings.Secure.getString(context.contentResolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "").split(':').any {ComponentName.unflattenFromString(it)==ComponentName.unflattenFromString(component)}}
    val paused=prefs.getLong("blockSnoozeUntil",0)>System.currentTimeMillis()
    fun persist() {prefs.edit().putBoolean("focusBlocking",enabled).putBoolean("focusBlockAll",false).putStringSet("blockedApps",SocialApps.selection(selected)).remove("excludedApps").apply();onAction("refresh")}
    LaunchedEffect(Unit) {
        if(!BuildConfig.APP_BLOCKING_AVAILABLE)return@LaunchedEffect
        val installed=withContext(Dispatchers.IO) {
            val pm=context.packageManager
            SocialApps.packages.mapNotNull {id->
                try {pm.getApplicationInfo(id,0).takeIf {it.enabled}}catch(_:PackageManager.NameNotFoundException){null}
            }.sortedBy {it.loadLabel(pm).toString().lowercase()}
        }
        apps=installed
    }
    LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.9f),contentPadding=PaddingValues(24.dp,0.dp,24.dp,32.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Row(verticalAlignment=Alignment.CenterVertically) {Text("Pause social apps",fontSize=23.sp,modifier=Modifier.weight(1f));IconButton(onClick=onClose){Icon(Icons.Default.Close,"Close app blocking")}}}
        item {Text("Choose social and video apps to pause while you focus. Phone, messages, browsers and system apps are always allowed.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(!BuildConfig.APP_BLOCKING_AVAILABLE) {
            item {Card {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Shield,null)
                Text("App-blocking edition needed",fontSize=18.sp)
                Text("The standard edition keeps Accessibility access out of the app. The optional social-blocking APK can pause supported social apps, but still needs Accessibility and may be blocked by Play Protect. Your history stays with the same app.",fontSize=13.sp)
                Button(onClick={onAction("blockingDownload")}){Text("Get app-blocking edition")}
            }}}
        } else {
            item {Card {Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)){Text("Block during focus");Text("Selected social apps only · released on pauses and breaks",fontSize=11.sp)}
                Switch(enabled,onCheckedChange={enabled=it;persist()},modifier=Modifier.semantics {contentDescription="Block during focus"})
            }}}
            item {Card {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(if(granted)"Accessibility access is ready" else "Allow app-blocking access",fontSize=15.sp)
                Text("Detects which app opens and returns selected social apps to Home. Screen contents, typing and notification content are not read. Accessibility remains a sensitive permission; enabling it is optional.",fontSize=12.sp)
                TextButton(onClick={onAction("accessibility")}){Text(if(granted)"Manage access" else "Enable access")}
            }}}
            item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={selected=apps.map{it.packageName}.toSet();enabled=true;persist()},modifier=Modifier.weight(1f)){Text("Select social apps",fontSize=11.sp)}
                OutlinedButton(onClick={selected=emptySet();persist()},modifier=Modifier.weight(1f)){Text("Clear selection",fontSize=11.sp)}
            }}
            item {Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(if(paused)"Blocks paused" else "Need a quick check-in?",fontSize=12.sp,modifier=Modifier.weight(1f))
                TextButton(onClick={prefs.edit().putLong("blockSnoozeUntil",if(paused)0 else System.currentTimeMillis()+5*60000).apply();onAction("refresh")}){Text(if(paused)"Resume blocks" else "Pause for 5 min",fontSize=12.sp)}
            }}
            item {OutlinedTextField(search,{search=it},label={Text("Find a social app")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp))}
            if(apps.isEmpty()) item {Text("No supported social apps are installed. Phone, messages and other apps stay available.",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            items(apps.filter{it.loadLabel(context.packageManager).toString().contains(search,true)},key={it.packageName}) {app->
                val id=app.packageName
                val on=id in selected
                Card {Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    AndroidView(factory={android.widget.ImageView(it).apply {importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO}},update={it.setImageDrawable(app.loadIcon(context.packageManager))},modifier=Modifier.size(34.dp))
                    Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(app.loadLabel(context.packageManager).toString(),fontSize=14.sp);Text(if(on)"Blocked while focusing" else "Allowed",fontSize=11.sp)}
                    Switch(on,onCheckedChange={value->selected=if(value)selected+id else selected-id;persist()},modifier=Modifier.semantics {contentDescription="Block ${app.loadLabel(context.packageManager)}"})
                }}
            }
        }
    }
}
private const val FocusServiceName="com.cybercat.stopwatch.FocusAccessibilityService"
