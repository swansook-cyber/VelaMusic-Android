# Vela Music Android — multi-device baseline

Android Phone, Tablet, Android TV, Google TV and Android TV Box. One APK built with
application ID `net.velalab.music`. This is **not** Vela YouTube APK.

The app is a minimal native WebView shell for `https://music.velalab.net/`.
It supports JavaScript, DOM storage, persisted web cookies, media playback, D-pad
focus, fullscreen WebView video and Android TV launcher entry. It does not require
touchscreen hardware, has no third-party update channel, and does not modify the
Vela Music Home Hub backend.

On the Vela Music origin, the APK adds a focusable Play/Pause button to each library
song card. D-pad navigation moves between the card, Play/Pause, Previous, Next,
Details and Lyrics; OK activates only the focused control. The pink focus outline
also applies to other web controls. Android Back closes an open web dialog before
navigating WebView history. This adaptation is packaged in the APK and does not
change the production website, login endpoints, cookies or playback backend.

Launcher icons are generated from `vela-music/static/assets/branding/vela-music/icon-1024.png`
at Android's mdpi through xxxhdpi sizes. The adaptive icon and 320×180 TV banner
use the same repository's `mark-transparent-1024.png` brand mark.

## Build
Use GitHub Actions workflow **Vela Music Android Debug** (manual `workflow_dispatch`).
It builds `app-debug.apk` and lint outputs with Gradle 8.11.1, JDK 17, Android SDK 35.
Or on a PC with Android SDK and Gradle 8.11.1: `gradle :app:assembleDebug :app:lintDebug`.
No Gradle wrapper binary is included; the CI installs a pinned Gradle version.
The workflow first runs `npm ci && npm test` for WebView remote-control and resource checks.

## Testing gates (not yet completed)
* Phone: login and reopen app, search, music and YouTube playback, fullscreen, logout
* Tablet: portrait/landscape, sizing and navigation
* Android TV / Google TV: launcher icon/banner, D-pad arrows, OK, Back, search text
  entry and fullscreen video with *real* remote hardware
* TV Box: mouse/remote, startup, Wi-Fi reconnection
* Car head-unit (parked only): landscape, audio usability; video not for driving
* Confirm cookies/session persistence after WebView process death and device reboot
* Performance and Accessibility; verify older Android OS/WebView environments

This is an initial scaffold, **not a verified multi-device release**. Android TV
focus depends on the website's HTML focusability. WebView persistent cookies do
not override server session lifetime (configured separately in Vela Music).
Debug APK signing keys are CI-generated and are not suitable for stable in-place
updates; establish owned release signing before distribution.
