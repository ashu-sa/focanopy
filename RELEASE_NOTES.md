# Focanopy 1.15.0 — A darker, cozier grove

- Android Settings now offers **System**, **Light**, and **Dark** appearance choices, remembered across app restarts.
- Dark mode uses forest surfaces, soft sage accents and readable light text across focus, journal, profile, session dialogs and app-blocking settings.
- The floating timer and Android system bars follow the same choice. Switching themes keeps the current timer and history intact; System responds to device theme changes.
- The GitHub repository is now **ashu-sa/focanopy**. Documentation and app download links use the new address. App IDs, signing identity, local storage and account configuration remain compatible with existing installs.

Recommended: **Focanopy-Android-standard.apk**. Choose **Focanopy-Android-app-blocking.apk** for optional distraction blocking with explicitly granted Accessibility access. Keep Play Protect enabled. **Focanopy-Android-debug.apk** aliases the standard APK.

Validation: both variants build, pass lint and 22 JVM tests each. Android 15 QA checks cover saved dark-mode selection, Activity recreation, changing themes during a running timer, floating controls, session editing, journal, profile and Pomodoro. Real Google consent still requires user verification on a Google-enabled device.

Desktop packages are carried forward unchanged from v1.14.0.
