# Focanopy 1.16.0 — Social-only focus blocking on Android

- The optional Android blocker now supports only a fixed list of social/video apps. All-app mode and arbitrary app selection are removed. Phone, messaging, browsers and system tools cannot be blocked.
- Old selections are filtered on upgrade. Runtime enforcement also checks the allowlist, so stale or modified settings cannot block other apps.
- Only known social-app metadata is queried; the full installed-launcher inventory is no longer queried. Per-app icons, switches and the five-minute snooze remain.
- Calls, SMS/messaging and tools stay available. WhatsApp, Telegram, Messenger and Discord are outside the social/video allowlist.

**This does not guarantee Play Protect clearance.** The optional **Focanopy-Android-app-blocking.apk** still declares Accessibility. Google can block Internet-sideloaded APKs using that permission, even with social-only behavior. Keep Play Protect enabled.

Recommended: **Focanopy-Android-standard.apk**. It omits the Accessibility service and social-package inventory queries. Timing, history, dark mode, Google sign-in, sync and the optional floating card remain available; blocking is not included. **Focanopy-Android-debug.apk** aliases standard. Signing identity and app IDs are preserved for upgrades.

Validation: both editions build, pass lint and 24 JVM tests each. Isolated Android 15 checks cover social/video blocking and snooze, release on pause, rejection of legacy browser/system selections, the social-only settings UI and normal timer flows. These tests do not establish Google's Play Protect verdict on a physical device.

Desktop packages are carried forward unchanged from v1.15.0.
