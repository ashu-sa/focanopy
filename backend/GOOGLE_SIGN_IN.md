# Google sign-in setup

Android uses **Credential Manager + Supabase ID-token authentication**. Desktop clients use **Supabase Auth + Google OAuth with PKCE**. A Google **Web application** OAuth client supplies the token audience and desktop callback; Android also needs a package/certificate registration in the same Google project; no Google client secret belongs in an APK, executable, repository or chat.

## Hosted configuration status

Verified on 24 October 2026: Google is enabled in Supabase, nonce checks remain enabled, users must have an email, and both native redirect patterns below are saved. The authorization endpoint redirects to Google with the configured Web client and only `email profile` scopes. Google Cloud remains External / Testing; basic identity-only requests are exempt from the test-user allowlist restriction ([Google audience policy](https://support.google.com/cloud/answer/15549945?hl=en)). A full user-driven consent, native callback and cross-device sync test remains to be completed. No client secret is stored in the repository.

## One-time owner setup

1. Open [Google Auth Platform](https://console.cloud.google.com/auth/overview). Choose a Google Cloud project (or create a Focanopy project). Configure Branding with the app name **Focanopy** and your support/developer contact email. Set Audience to External if people outside your organization need to sign in. Basic identity-only sign-in can be used while in Testing without adding test users; if adding other scopes, review Google’s test-user restrictions. Publish to Production when your branding and public launch are ready.
2. Add only the basic scopes **openid**, **email**, and **profile**. The app does not request Drive, Gmail, calendar or contacts access.
3. Under **Clients → Create client**, choose **Web application**. Name it **Focanopy Supabase**. Add this **Authorized redirect URI** exactly:

   ```text
   https://fwlengqpmrtmpbrhtxis.supabase.co/auth/v1/callback
   ```

   Google sends the user to Supabase first, so native app schemes and loopback URLs do **not** belong in Google's redirect URI list. No JavaScript origin is needed for this browser-based native app flow.
4. Open [Supabase → Authentication → Sign In / Providers → Google](https://supabase.com/dashboard/project/fwlengqpmrtmpbrhtxis/auth/providers?provider=Google). Enter the Web client ID and client secret **directly into Supabase**, enable Google and save. Keep **Skip nonce checks** and **Allow users without an email** off. Do not change existing email, PIN gateway, RLS, project keys or PIN_PEPPER settings.
5. In [Supabase → Authentication → URL Configuration](https://supabase.com/dashboard/project/fwlengqpmrtmpbrhtxis/auth/url-configuration), add these two **Redirect URLs**, preserving existing entries:

   ```text
   focanopy://auth/**
   http://127.0.0.1:*/focanopy/**
   ```

   The first handles Android and macOS. The second is a Windows/Linux callback bound only to this computer, on a random port and random single-use path. PKCE protects the code exchange; apps also validate the callback destination and state. The desktop listener closes on completion, cancellation or timeout. Do not use a global `**` allowlist entry or alter the Site URL.

The account owner must create and enter the Google OAuth credential. Do not send the client secret in chat. The clients detect a disabled provider and show a setup message instead of opening a broken sign-in page.

## User experience

On Android, tap the profile icon and choose Google in the native Android sheet. Desktop clients use the system authentication browser and return to Focanopy. There is no separate registration form or PIN for Google users. Use the same Google account on every device to see one combined journal with device labels. Saved sessions are retained in the existing OS-secured credential stores and account-specific caches, so sign-in is not shown on every launch.

The optional **Add this device’s existing sessions to my account** checkbox controls local-only history upload. Google accounts and the older synthetic username/PIN accounts are separate identities; the app does not silently merge unrelated accounts. Signing out returns to local-only history, and account switching is blocked during an active timer.

Android opens Credential Manager’s Google account sheet and exchanges its ID token through Supabase `grant_type=id_token`. A fresh random raw nonce is sent to Supabase and its SHA-256 hash to Google. There is no browser fallback. macOS uses ASWebAuthenticationSession; Windows/Linux open the default browser and use a short-lived IPv4 loopback listener. No embedded login WebView is used.

## End-to-end verification after setup

- On each platform, sign in using a disposable Google test account. Save one session, then verify it appears on another device signed into that same Google account.
- Restart each app: it should retain sign-in and restore the same account cache.
- Cancel consent or the waiting flow: local history and timer state must remain unchanged.
- Try a second Google account: the first account's sessions must not be visible. Sign back in to recover its journal.
- Confirm an active timer prevents account switching, and test optional local-session upload separately.

Unit tests cover PKCE challenge generation, exact callback/state checks, duplicate parameter rejection, cancellation/timeout, loopback ownership, safe vault/cache adoption and Google profile names. They do not replace the final real Google consent check, which requires the owner's configured OAuth client.

References: [Supabase Google setup](https://supabase.com/docs/guides/auth/social-login/auth-google), [PKCE flow](https://supabase.com/docs/guides/auth/sessions/pkce-flow), [redirect allowlists](https://supabase.com/docs/guides/auth/redirect-urls).

## Android native registration (26 October 2026)

Created **Focanopy Android** in Google project `prismatic-cider-512606-m9`:

- Package: `com.cybercat.stopwatch`
- Signing SHA-1: `53:BC:60:F2:0A:32:83:9C:52:F9:5D:97:F6:65:92:DB:CE:54:5B:71`
- Android client: `265421497125-7ut55643aeht34o5fvm5kfsmibmdeeio.apps.googleusercontent.com`
- Web client / Credential Manager server audience: `265421497125-o0i3rkkj1mlckg0bnup48o67i3s0fm4n.apps.googleusercontent.com`

Both APK editions use this package and certificate. Credential Manager requests the Web client audience, which is already configured in Supabase; no Android secret is embedded. Keep nonce validation enabled. Google configuration can take time to propagate. Google Cloud currently shows External / Testing; follow the console’s audience restrictions when testing. A Play Store signing key or different package requires its own Android registration. The isolated `.qa` build is not registered for real Google login.

Implementation reference: [Android Credential Manager Google sign-in](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation).
