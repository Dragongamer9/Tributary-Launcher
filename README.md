# Tributary

A 64-bit Android launcher inspired by FlowHome (Larva Labs): a scrolling feed instead of an icon grid, notifications on top, a thumb-reach dock and fast fuzzy app search. Kotlin + Jetpack Compose, no native code, so it builds for arm64-v8a and x86_64 by default.

## Features
- Scrolling feed with the date/time header and your notifications at the top (grant Notification access once)
- Dock with up to 5 apps; long-press any app to add/remove it, open app info, or uninstall
- Fuzzy app search and drawer (tap the search pill or swipe up on the dock)
- 4 themes
- Your own RSS / Atom feeds (Reddit, Mastodon, Tumblr, YouTube, blogs, news)

Twitter, Instagram, Facebook and Feedly integrations from the original are not possible through public APIs anymore, which is why feeds are RSS/Atom.

## Build
**GitHub Actions (no PC needed):** every push runs `.github/workflows/build.yml`. Open the Actions tab, pick the latest run, download the `Tributary-debug-apk` artifact, unzip it and install the `.apk` (allow "install unknown apps" for the app you open it with).

**Android Studio:** open this folder (Koala or newer, JDK 17), let Gradle sync, press Run. Needs Android 8.0+ (API 26).

Then set Tributary as your Home app in Android settings.

## Ideas next
Icon pack support, widget hosting, weather (Open-Meteo needs no API key), Mastodon/Bluesky-specific cards, work-profile apps via LauncherApps.
