# Flower Pomodoro

The stopwatch is the default. Enable the optional **Pomodoro effect** for a 25-minute focus round. Existing sessions keep their original mode; cancel/reset the current round before changing the effect.

- **Android:** Timer → Pomodoro effect. Choose a 5- or 10-minute break before starting. App blocking is available with the effect off, too.
- **macOS / Windows / Ubuntu:** click the floating tree → Pomodoro & App Blocking. The same option is in the menu bar / tray.

A separate **Focus time** clock shows elapsed focus while the main countdown shows the round or break remaining. Focus time stops during pauses and breaks.

The pink flowering tree grows continuously during 25 minutes of focus. At 25 minutes it blooms fully, watering stops, and a reminder asks you to stretch and move a little. The chosen break counts down separately. When it ends, another reminder says your next flower needs water. **The next focus round never starts automatically.** Press Start next focus when ready; an explicit Skip break action can start it earlier.

Manual pauses freeze growth and stop watering. Breaks preserve the healthy completed flower. During the next round a completed flower remains beside the next growing one. Pomodoro has no Finish action. Each completed 25-minute round saves automatically under **Pomodoro focus**, adding to the same session across rounds; you can rename it in the journal. Cancel during an unfinished round and its tree dies: watering stops, foliage turns brown, petals fall, and that partial round is not saved. Earlier completed rounds stay saved. The dead tree remains after relaunch until you start a fresh 25-minute round. Cancelling during a break simply exits the completed cycle. Pausing rests the tree without killing it. Existing journal/calendar hourly tree and garden totals continue to use actual focused time, without treating breaks as work. Flower counts shown on the timer describe the current Pomodoro session.

Focus time is capped at the 25-minute boundary even if the app wakes late or restores after process exit. A late wake cannot silently run additional rounds. Drafts preserve the mode, target, break choice and break deadline. Runtime intervals use monotonic clocks; restoring a break after process exit uses the saved wall-clock deadline, so changing the system clock while the app is closed can affect the restored break countdown.

## Optional app blocking

Blocking is **off by default**. When enabled, it applies while the ordinary stopwatch runs or during a running Pomodoro focus round. Pauses, breaks, waiting for the next round and finished sessions and cancelled trees release blocking. Apps are never terminated or suspended; their documents stay open.

**Android:** open **Settings → Pause social apps** in the optional app-blocking edition. Enable the master switch, select supported social/video apps and explicitly grant Accessibility access. On means blocked; off means allowed. Phone, messaging, browsers and system tools cannot be selected. The former all-app mode and arbitrary app selection are removed; old settings cannot broaden the fixed allowlist. The service receives window-package changes and returns selected social apps to Home without reading screen contents, typing or notification content. **Pause for 5 min** temporarily releases blocking; the timer's pauses and breaks release it too.

Accessibility remains sensitive even with social-only behavior. Google may block installation of this APK from browser/file-manager downloads. Do not turn off Play Protect. Use the standard APK if you need the timer without Accessibility or app blocking. See [Google’s warning guidance](https://developers.google.com/android/play-protect/warning-dev-guidance).

**Desktop:** the **Block all except browsers** toggle and individual app switches remain available. Choose exclusions to keep apps accessible; Focanopy and essential system controls remain available. These settings are separate from Android's social-only list.

- **macOS:** choose application bundles using the native picker. Both bundle IDs and application paths are saved to cover Java helpers such as Android Studio. Activation events and a watchdog check blocked apps while focus is active. Apps are hidden, with a full-screen blocking panel if needed. **Enable stronger window minimization** requests optional Accessibility permission to minimize resistant windows. The panel offers Pause timer & allow apps and Turn app blocking off. Switching to an allowed app releases the panel. Browsers are identified through macOS registered HTTPS handlers. Previously selected bundle IDs are also resolved to installed app paths automatically.
- **Windows:** choose `.exe` files to block, or exclusions in all-app mode. Foreground windows are minimized. Known browser executables, Explorer, Task Manager and essential system processes are protected in all-app mode. Add an uncommon browser as an exclusion. Elevated/protected applications can refuse minimization.
- **Ubuntu/GNOME 45+:** choose apps/exclusions, then Install optional GNOME focus extension. Enable **Focanopy Flower Focus** yourself in GNOME Extensions; logout/login may be needed after first installation. The extension minimizes windows through GNOME Shell, including Wayland. Browser apps advertising HTTP/HTTPS support are allowed in all-app mode. The local policy expires within ten seconds if Focanopy exits unexpectedly. Disable/remove the extension in GNOME Extensions at any time. Other Linux desktops retain the timer without this integration.

These are reversible focus aids, not an operating-system security lockdown. App selections, exclusions, permissions and blocking preferences are device-local. Disabling blocking or pausing lets you reopen apps; hidden/minimized windows are not automatically reopened.

## Notifications and verification

Allow notifications when prompted if you want background break/return reminders. Android uses its ongoing countdown notification plus a separate Flower focus reminders channel. macOS requests notification permission only when you choose Pomodoro; Windows/Linux use native tray notifications where the desktop supports them.

Automated tests cover capped focus, delayed wakes, break recovery, pause at the boundary, manual next-round behavior, optional blocking scope, expiring GNOME policies and unchanged stopwatch timing. The isolated Android device test exercises focus → bloom → break reminder → ready → manual next focus → focus-only save. Platform builds and native UI/packaged smoke checks run in CI. Android Accessibility enforcement and Windows/GNOME window handling still need real-device/desktop permission validation; the Mac development host cannot stand in for those desktops.
