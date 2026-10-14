# Jash Files

An iOS-style file manager for Android, built with Kotlin and Jetpack Compose.

## Features
- **Browse**: Internal Storage, SD card, Downloads, Favorites, Categories (Images, Videos, Audio, Documents, Archives, APKs)
- **Search**: across the whole device, or inside the current folder
- **File actions**: Open, Open With, Share, Rename, Copy, Move, Duplicate, Delete, Get Info
- **ZIP**: compress files or folders, extract archives
- **Multi-select** with a bottom toolbar (Share / Copy / Move / Compress / Delete)
- **List or icon grid** view, with photo and video thumbnails
- **Sort** by Name / Date / Size / Kind, ascending or descending
- **Storage analyzer**: usage bar per category, plus a list of large files
- **Recents**: files changed in the last 30 days
- **Settings**: Automatic / Light / Dark appearance, show hidden files, confirm before deleting
- iOS look: large collapsing titles, grouped lists, action sheets, alerts, toggles, segmented controls, tab bar, push and pop slide animations

## Get the APK, option A: GitHub (no PC setup needed)
1. Create a new repository on github.com and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab. The "Build Jash Files APK" workflow runs automatically (or press **Run workflow**).
3. When it finishes (about 5 minutes), open the run and download **JashFiles-APK** under *Artifacts*.
4. Unzip it, copy `app-release.apk` to your phone and install it (allow "Install unknown apps").

## Get the APK, option B: Android Studio
1. Open this folder in Android Studio (Ladybug or newer).
2. Let Gradle sync, then use **Build › Build App Bundle(s) / APK(s) › Build APK(s)**.
3. Or press ▶ Run with your phone connected.

## First launch
Tap **Continue** and turn on **Allow access to manage all files** for Jash Files. On Android 10 and below, the app asks for storage permission instead.

## Notes
- Package name: `com.jash.files`, minimum Android 8.0 (API 26).
- The release build is signed with the debug key so it installs directly. Use your own keystore before publishing to Google Play. Play also restricts the "All files access" permission to file-manager apps, and it reviews that use.
