package com.cybercat.stopwatch

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** One saved choice shared by the Compose app and its native floating window. */
object Appearance {
    const val KEY="appearance"
    fun mode()=Repository.prefs.getString(KEY,"system").takeIf {it in setOf("system","light","dark")} ?: "system"
    fun dark(mode:String,systemDark:Boolean)=when(mode){"dark"->true;"light"->false;else->systemDark}
    fun dark(context:Context)=dark(mode(),context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK==Configuration.UI_MODE_NIGHT_YES)
    val light=lightColorScheme(
        primary=Color(0xFF476948),onPrimary=Color.White,secondary=Color(0xFF7B8562),
        background=Color(0xFFF5F6EF),surface=Color(0xFFFEFFF9),onSurface=Color(0xFF203B2B),onBackground=Color(0xFF203B2B),
        surfaceVariant=Color(0xFFE6EEDC),onSurfaceVariant=Color(0xFF6B7869),outline=Color(0xFFC1CCB9),
        secondaryContainer=Color(0xFFE6EEDC),primaryContainer=Color(0xFFE6EEDC),onPrimaryContainer=Color(0xFF203B2B),onSecondaryContainer=Color(0xFF203B2B),onTertiaryContainer=Color(0xFF203B2B),
        surfaceContainerHighest=Color(0xFFE4E9DF),surfaceContainer=Color(0xFFF5F6EF),error=Color(0xFFAE6854))
    val night=darkColorScheme(
        primary=Color(0xFFADDAAD),onPrimary=Color(0xFF18351F),secondary=Color(0xFFC3CCAB),
        background=Color(0xFF111B17),surface=Color(0xFF1E2C24),onSurface=Color(0xFFEEF2E8),onBackground=Color(0xFFEEF2E8),
        surfaceVariant=Color(0xFF2A3D2E),onSurfaceVariant=Color(0xFFB1C2AF),outline=Color(0xFF657A64),
        secondaryContainer=Color(0xFF2A3D2E),primaryContainer=Color(0xFF2A3D2E),onPrimaryContainer=Color(0xFFEEF2E8),onSecondaryContainer=Color(0xFFEEF2E8),onTertiaryContainer=Color(0xFFEEF2E8),
        surfaceContainerHighest=Color(0xFF34483A),surfaceContainer=Color(0xFF1E2C24),error=Color(0xFFF0B19C))
}
