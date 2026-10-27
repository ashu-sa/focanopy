package com.cybercat.stopwatch

/** Deliberate social/video allowlist. System, phone, messaging and browser apps cannot be selected. */
object SocialApps {
    val packages=setOf(
        "com.instagram.android","com.instagram.lite","com.instagram.barcelona",
        "com.facebook.katana","com.facebook.lite","com.twitter.android",
        "com.reddit.frontpage","com.snapchat.android","com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill","com.zhiliaoapp.musically.go","com.pinterest",
        "com.google.android.youtube","com.linkedin.android"
    )
    fun selection(selected:Set<String>)=selected.intersect(packages)
    fun blocked(name:String,selected:Set<String>)=name in packages && name in selected
}
