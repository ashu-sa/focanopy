package com.cybercat.stopwatch
import org.junit.Assert.*
import org.junit.Test

class SocialAppsTest {
    @Test fun supportedSocialAppsStillRequireSelection() {
        assertTrue(SocialApps.blocked("com.instagram.android",setOf("com.instagram.android")))
        assertFalse(SocialApps.blocked("com.instagram.android",emptySet()))
        assertFalse(SocialApps.blocked("com.instagram.android.fake",setOf("com.instagram.android.fake")))
    }
    @Test fun legacyOrTamperedSelectionCannotBlockEssentialOrOtherApps() {
        val other=setOf("com.android.dialer","com.google.android.dialer","com.android.phone","com.android.settings","com.android.systemui","com.android.chrome","com.google.android.inputmethod.latin","com.whatsapp","org.telegram.messenger","com.facebook.orca","com.discord","com.android.calendar","com.jetbrains.android")
        other.forEach {assertFalse(it,SocialApps.blocked(it,other))}
        assertTrue(SocialApps.selection(other).isEmpty())
    }
    @Test fun migrationKeepsOnlyExplicitlySelectedSocialPackages() {
        assertEquals(setOf("com.reddit.frontpage"),SocialApps.selection(setOf("com.reddit.frontpage","com.android.phone","com.android.chrome")))
        assertFalse(SocialApps.packages.any {it.contains("dialer") || it.contains("phone")})
    }
}
