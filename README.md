# Tributary

A 64-bit Android launcher inspired by FlowHome (Larva Labs): a scrolling feed instead of an icon grid, notifications on top, a thumb-reach dock and fast fuzzy app search. Kotlin + Jetpack Compose, no native code, so it builds for arm64-v8a and x86_64 by default.

## Features
- FlowHome-style home: round photo in the header with time and date on either side; notifications pinned at the top of the feed (grant Notification access once)
- Feed layouts: mosaic, wide or narrow tiles
- Corner tray (bottom-right, or bottom-left in left-handed mode): tap for your favourite apps, long-press to jump to search
- Swipe left for the app drawer: a vertical list with search-as-you-type, hide apps, add to tray, app info, uninstall
- 4 themes, custom header photo
- Your own RSS / Atom feeds (Reddit, Mastodon, Tumblr, YouTube, blogs, news)

Twitter, Instagram, Facebook and Feedly integrations from the original are not possible through public APIs anymore, which is why feeds are RSS/Atom.

## Build
**GitHub Actions (no PC needed):** every push runs `.github/workflows/build.yml`. Open the Actions tab, pick the latest run, download the `Tributary-debug-apk` artifact, unzip it and install the `.apk` (allow "install unknown apps" for the app you open it with).

**Android Studio:** open this folder (Koala or newer, JDK 17), let Gradle sync, press Run. Needs Android 8.0+ (API 26).

Then set Tributary as your Home app in Android settings.

## Ideas next
Icon pack support, widget hosting, weather (Open-Meteo needs no API key), Mastodon/Bluesky-specific cards, work-profile apps via LauncherApps.
