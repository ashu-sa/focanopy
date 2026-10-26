package com.cybercat.stopwatch
import org.junit.Assert.*
import org.junit.Test
class AppearanceTest {
    @Test fun explicitChoiceOverridesDevice() {assertTrue(Appearance.dark("dark",false));assertFalse(Appearance.dark("light",true))}
    @Test fun systemChoiceFollowsDevice() {assertTrue(Appearance.dark("system",true));assertFalse(Appearance.dark("system",false));assertTrue(Appearance.dark("invalid",true))}
}
