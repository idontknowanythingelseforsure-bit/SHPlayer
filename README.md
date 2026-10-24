# SH Player

A local music player for Android, built as a working reimplementation of the
PlayerPro playback model: a long-lived media service, a swappable DSP seam, and
a PlayerPro-inspired black & orange interface.

## Download the APK

[**Download the latest SH Player APK**](https://github.com/idontknowanythingelseforsure-bit/SHPlayer/releases/latest/download/SH-Player.apk)

Android 8.0+ (API 26+). The APK is a debug build signed with the standard
Android debug key, so it installs directly. You may need to allow "Install
unknown apps" for your browser or file manager.

Package: `com.shplayer` (debug builds install as `com.shplayer.debug`)
Min SDK 26 · Target/Compile SDK 35 · Kotlin 2.0 · Jetpack Compose · Media3

## What works

- Builds and installs as a normal Android app (debug APK is signed and ready).
- PlayerPro-style UI in black + orange:
  - **Home** shows a **Recently added** shortcut (opens every automatically
    scanned song, newest first in the normal smart-list layout), the
    **Recently played** list and a "See all" link; every other browser and
    smart list lives in the navigation drawer
  - hamburger navigation drawer
  - top bar with back / drawer, contextual icon, title, search and overflow menu
  - the contextual playlist icon in the top bar is also an **"Add to
    playlist"** shortcut whenever the current screen shows songs (on Home it
    uses the recently played list; with no history it explains what to do)
  - overflow menu adds **"Add to playlist"** whenever the current screen shows
    songs, so an album, artist, genre, folder, smart list or whole library can
    be added in one go
  - overflow menu adds **"Sort by"** on song lists, with Default order plus
    **File name (A to Z)**, **File name (Z to A)**,
    **Modified (newest first)** and **Modified (oldest first)**. The choice is
    saved and applied to the current list (including its Play all / Shuffle
    actions).
  - flat song rows with title, artist, duration and a per-row context menu
  - two-column album / artist / genre grids with disc or silhouette placeholders
  - CD-disc artwork drawn in Compose when an album has no embedded art
  - mini player docked at the bottom on list screens
- Draggable fast-scroll slider on the right edge of long lists and grids.
- MediaStore library: Songs, Albums, Artists, Genres, Playlists, Folders.
  Albums are derived from the song table so they are not empty on Android 13+.
- **Playlist creation:** a "+ Create New Playlist" row and orange FAB on the
  Playlists screen, plus "Add to playlist" in every song's overflow menu.
  Playlists are stored app-locally as JSON (negative ids) alongside the
  read-only MediaStore playlists.
- **Playlist management:** each app-created playlist row and its detail-screen
  overflow menu offer **Edit playlist name** and **Remove playlist** (with a
  confirmation). System MediaStore playlists stay read-only.
- **Filling and emptying a playlist:** local playlist details have an orange
  "+" FAB with "Add songs" (searchable multi-select picker with select-all) and
  "Add a folder" (adds every track in the folder). The overflow menu has
  **Remove songs** (multi-select), and each song's row menu gains
  **Remove from playlist** while viewing an editable playlist. The bulk top-bar
  action and the per-song row action cover the add direction.
- **Folder access through the file manager:** the Folders screen can launch the
  system folder picker (`ACTION_OPEN_DOCUMENT_TREE`). Selected roots are
  persisted and the list can show only those folders (with a "Show all" toggle).
- **"Open with" support:** SH Player appears in the chooser for `audio/*`
  content/file uris and `ACTION_SEND` streams, plays the file directly and
  opens Now Playing.
- **Home-screen widget:** 4×1 PlayerPro-style widget with disc, queue counter,
  title/artist, previous / play-pause / next, and a progress line. Tap the
  progress line to open a compact seek sheet with a real draggable handle; it
  closes 2 seconds after you release it. The line refreshes once per second.
- App launcher icon generated from the supplied SH Player artwork (legacy PNG
  mipmaps for every density, plus a round variant).
- Global search across title / artist / album with grouped artist + song results.
- Smart lists: Recently played (persisted play history), Recently added
  (`DATE_ADDED`), Top rated (favorites) and Most played (play counters).
- Per-song actions: play, play next, enqueue, add/remove favorite, add to
  playlist, remove from playlist (inside editable playlists), song info, share.
- Playback via Media3 `ExoPlayer` inside a `MediaSessionService`:
  background playback, media notification, headset/Bluetooth controls.
- Now Playing: large disc artwork, queue position badge (`6/8`), seek bar,
  previous/next, shuffle, repeat off/all/one, format line
  (`mpeg 44.1kHz 32kbps stereo`), go to album/folder, song info and share.
- Sleep timer (15/30/45/60/90 minutes).
- Equalizer screen driven by the platform AudioFX chain: vertical band sliders,
  presets, reverb presets, rotary bass/virtualizer knobs, volume slider and
  master ON/OFF switch. The Now Playing button uses the four-vertical-slider
  icon (`res/drawable/ic_eq_sliders.xml`).

## Build

The project is a standard Gradle build. `local.properties` points at the SDK
used during development; change it to your own SDK path.

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "I:\Projects\REA\.rea-work\android-sdk"
$env:GRADLE_USER_HOME = "I:\Projects\REA\.rea-work\gradle-home"
.\gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Install:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

The app asks for audio access on first launch (`READ_MEDIA_AUDIO` on Android 13+,
`READ_EXTERNAL_STORAGE` below that). Grant it, and the library loads.

## Architecture

```
com.shplayer
├── MainActivity.kt          permission gate, Compose host, "Open with" intents
├── SeekOverlayActivity.kt   compact widget seek sheet with draggable handle
├── ShPlayerApp.kt           app-scoped init + widget state observer
├── data/
│   ├── Models.kt            Song / Album / Artist / Genre / Folder / Playlist
│   ├── MusicRepository.kt   MediaStore queries (IO dispatcher), album grouping
│   ├── ArtworkLoader.kt     album art + memory cache
│   ├── Permissions.kt       version-aware audio permission
│   └── Prefs.kt             settings, favorites, play counts, recent plays,
│                            local playlists (JSON), folder roots
├── playback/
│   ├── PlaybackService.kt   MediaSessionService + ExoPlayer
│   └── PlayerController.kt  UI-facing singleton, StateFlow<PlayerUiState>,
│                            whenConnected() queue for widget commands
├── widget/
│   └── PlayerWidgetProvider.kt  RemoteViews widget + transport broadcasts
├── audio/
│   └── AudioEffects.kt      EffectsBackend seam + AudioFX implementation
└── ui/
    ├── AppRoot.kt           drawer, top bar, route stack, mini player, dialogs
    ├── Routes.kt            in-memory navigation graph
    ├── SortMode.kt          persisted sort modes + list ordering helper
    ├── PlayActions.kt       playback callbacks + shared row menu builder
    ├── LibraryViewModel.kt  library, favorites, play counts, playlists, folders
    ├── components/
    │   ├── Common.kt        disc art, song rows, grids, fast scroller, mini player
    │   └── Dialogs.kt       song info, playlist create/rename/remove,
    │                        add-to-playlist and sort dialogs
    ├── screens/             Home, list/browse/detail, Search, Song/Folder
    │                        pickers, NowPlaying, Equalizer, Settings, Permission
    └── theme/Theme.kt       black + orange palette
```

The UI never touches `ExoPlayer` directly: it talks to `PlayerController`, which
owns a `MediaController` bound to `PlaybackService`. That is the same split
PlayerPro uses between its activities and `MediaPlaybackService`.

## Playback engine note

PlayerPro ships two effect backends behind one interface and drives its own
FFMpeg/`l7.*` engine for the DSP Pack. SH Player keeps the *shape* of that
design with `EffectsBackend`:

- `SystemAudioEffectsBackend` — implemented, uses `android.media.audiofx`.
- `NativeDspBackend` — reserved slot for a future native engine.

Swapping engines is a one-line change in `EffectsBackendFactory`.

## Not implemented yet

Deliberately deferred, so the first version stays understandable and stable:

- reordering songs inside a playlist (drag-and-drop)
- editing MediaStore/system playlists (the app-created playlists are fully
  editable)
- gapless playback and crossfade (settings UI exists, disabled)
- queue persistence across process death
- secondary storage volumes (SD cards) in the MediaStore query — folder roots
  for removable volumes already work through the file-manager picker
- Android Auto, Chromecast
- widget artwork (the widget currently shows the disc placeholder)
- tag editing, scrobbling, lyrics, ringtone, delete
- a native DSP engine

See `docs/OPTIMIZATION.md` for the exact extension points.
