# Focanopy 1.14.0 — Native Android sign-in and focus protection

- Google sign-in now uses Android’s native Credential Manager account sheet. No browser or browser fallback. Login remains encrypted and remembered; sync uses the same cross-device account. Requires Google Play services.
- A Compose focus-protection sheet adds social-app selection, app icons and switches, search, all-except-browser mode and exclusions. Works with the ordinary timer and Flower Focus.
- Pause app blocks for five minutes or resume them early. Pausing timing and Pomodoro breaks release blocked apps. The optional Accessibility service returns selected apps to Home; it does not suspend packages.
- Redesigned floating timer: compact cream card, moss-green controls, readable digits, phase label, animated tree and drag support across controls without accidental activation.
- README refreshed with current Android screenshots and setup details.

Recommended: **Focanopy-Android-standard.apk**. App blocking requires **Focanopy-Android-app-blocking.apk**, explicit Accessibility access and may be restricted by Play Protect. Keep Play Protect enabled. Both editions preserve the package and signing identity for upgrades; **Focanopy-Android-debug.apk** aliases standard.

Validation: both Android variants build, with 20 JVM tests each and lint. Isolated Android 15 UI checks cover timer/session flows, journal, profile, background timing, floating controls and focus-protection settings. Seven app-blocking edition device tests also verify selected-app enforcement, five-minute snooze, resumption and release on timer pause. Real Google consent and cross-device login require verification on a Google-enabled device; no personal Google account was used in automated tests.

Desktop packages are carried forward unchanged from v1.13.0.
