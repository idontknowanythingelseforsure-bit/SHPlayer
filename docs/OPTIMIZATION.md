# Optimization hooks

Every deferred improvement is marked in code with `OPTIMIZATION HOOK`. This is
the index, roughly in order of payoff.

## 1. Native DSP backend

**File:** `app/src/main/java/com/shplayer/audio/AudioEffects.kt`

`NativeDspBackend` is a no-op placeholder. To ship a real engine:

1. Add the native library + JNI wrapper.
2. Implement `attach`, `setBandLevel`, `setPreset`, `setBassBoost`,
   `setVirtualizer`, `setReverb` and `snapshot`.
3. Return it from `EffectsBackendFactory.create()` when the library is present,
   keeping `SystemAudioEffectsBackend` as the fallback.

This mirrors PlayerPro's `u7.j` interface with its `u7.d` (platform) and
`u7.m` (soundpack) implementations.

## 2. Artwork pipeline

**File:** `app/src/main/java/com/shplayer/data/ArtworkLoader.kt`

Currently an in-memory `LruCache` keyed by album id. For large libraries:

- add a disk cache keyed by `(albumId, size)`
- request the exact decode size instead of the 512 px default
- prefetch artwork for the visible grid page

Only `load()` is used by the UI, so the swap is local.

## 3. MediaStore loading

**File:** `app/src/main/java/com/shplayer/data/MusicRepository.kt`

`loadSongs()` materialises the whole library, and genre/playlist song counts
issue one cursor per row. Replace with:

- Paging 3 (`PagingSource` per tab)
- a single `GROUP BY` aggregation instead of per-genre counts
- incremental refresh via `ContentObserver`

## 4. Player controller lifecycle

**File:** `app/src/main/java/com/shplayer/playback/PlayerController.kt`

`PlayerController` is a process singleton and never releases its
`MediaController`. If the app grows multiple entry points, move it into a
DI-scoped object with an explicit `disconnect()` tied to process lifecycle.

Also worth adding: persist the queue and current position so playback can resume
after the process is killed.

## 5. Gapless + crossfade

**Files:** `data/Prefs.kt`, `playback/PlaybackService.kt`, `ui/screens/SettingsScreen.kt`

The settings exist and persist, but the switches are disabled. To enable:

- gapless: build the `ExoPlayer` with a `DefaultLoadControl`/renderer config that
  supports gapless transitions, and stop inserting silence on format change
- crossfade: needs two player instances or a custom audio processor; the
  `Prefs.crossfadeSeconds` value is already wired to the UI

## 6. Skins / themes

**File:** `app/src/main/java/com/shplayer/ui/theme/Theme.kt`

`ShPalette` is a single object and the screens use `MaterialTheme.colorScheme`.
The app is dark-first (the black + orange look), while a light scheme is still
defined. Add alternative palettes and select one in `ShPlayerTheme`, mirroring
PlayerPro's separate skin packages.

## 7. Release build

**File:** `app/build.gradle.kts`

`isMinifyEnabled = false` for the initial build. Before distribution:

- enable R8 and resource shrinking
- verify the Media3 keep rules in `proguard-rules.pro`
- add a signing config (this APK is debug-signed only)

## 8. Missing surfaces

- quick-settings tile
- Android Auto (`MediaBrowserServiceCompat` / media3 browsing)
- Chromecast (`media3-cast`)
- notification: custom `MediaNotificationProvider` using `ic_stat_music`
- widget artwork: the widget shows the disc placeholder; load the current
  album bitmap and call `RemoteViews.setImageViewBitmap` if needed

## 9. Testing

There are no automated tests yet. Highest-value first tests:

- `MusicRepository` cursor mapping with a fake `Cursor`
- `EffectsBackendFactory` fallback behaviour
- Compose UI test for the permission gate and empty-library state

## 10. Fast-scroll slider

**File:** `app/src/main/java/com/shplayer/ui/components/Common.kt`

`FastScrollBar` maps the thumb position to a list/grid index and calls
`scrollToItem` — cheap and predictable, but it jumps instead of dragging
proportionally. Improvements:

- interpolate between items while dragging (pixel scroll)
- show a section bubble (letter / date) next to the thumb
- auto-hide the track while idle and fade it in during scroll

## 11. Smart lists

**Files:** `ui/screens/ListScreens.kt`, `ui/LibraryViewModel.kt`

`Recently played`, `Recently added`, `Top rated` and `Most played` are recomputed
on every recomposition from the full song list. For large libraries, cache them
in the view model keyed by `(dateAdded, favorites, playCounts, recentlyPlayed)`.

Play counts, favorites and the recent-play history are stored as
`SharedPreferences` values. This is fine for a few thousand songs; move to
DataStore or Room before adding scrobbling or per-song statistics.

## 12. Navigation

**Files:** `ui/AppRoot.kt`, `ui/Routes.kt`

Navigation is a hand-rolled `mutableStateListOf<Route>` back stack. It covers the
current single-activity graph, but move to `navigation-compose` when deep links,
predictive back or per-screen state restoration are needed.

## 13. Queue rebuilding

**File:** `playback/PlayerController.kt`

`playNext`, `addToQueue` and `enqueue` rebuild the entire `MediaItem` list and
restore the current index/position. This keeps the UI queue and the player queue
in sync but is O(n) per insertion. If queue editing becomes frequent, switch to
`MediaController.addMediaItem` / `moveMediaItem` and keep the `Song` list as the
source of truth.

## 14. Local playlists

**Files:** `data/Prefs.kt`, `ui/LibraryViewModel.kt`, `ui/screens/BrowseScreens.kt`,
`ui/screens/PickerScreens.kt`, `ui/AppRoot.kt`

Playlists created in the app are stored as a JSON array in `SharedPreferences`
with negative ids, so they never collide with MediaStore playlist ids.
`resolvePlaylistSongs` resolves negative ids from the ids stored in `Prefs` and
maps them through the in-memory song list; MediaStore playlists are fetched
from the repository.

Bulk filling:

- `Prefs.addSongsToLocalPlaylist(playlistId, songIds)` adds all new ids in one
  JSON write and returns the number added (−1 when the playlist is gone).
- The top-bar overflow action and the contextual playlist icon resolve the
  current screen's songs in `AppRoot` (`routeSongs`) and feed them to
  `AddToPlaylistDialog`.
- A local playlist's detail screen shows a "+" FAB. `AddSongsToPlaylistDialog`
  opens `Route.SongPicker` (searchable multi-select) or `Route.FolderPicker`
  (one tap adds a whole folder).
- `SearchScreen` reports its current result page through `onResultsChanged`, so
  the top-bar bulk action also works on filtered search results.

Editing and removal (local playlists only):

- `Prefs.renameLocalPlaylist`, `Prefs.removeLocalPlaylist` and
  `Prefs.removeSongsFromLocalPlaylist` are the single JSON-write mutations.
  `LibraryViewModel` refreshes `localPlaylists` after each successful change.
- The Playlists screen gives each local row an overflow menu, and the playlist
  detail top bar repeats the actions. Removal asks for confirmation.
- The playlist detail top bar has **Remove songs**, which opens
  `Route.RemoveSongs`; it reuses `SongPickerScreen` with remove-specific button
  text. Individual rows gain **Remove from playlist** through the optional
  `SongMenuActions.onRemoveFromPlaylist` callback.
- MediaStore playlists stay read-only because the app does not request
  write access to them.

Deferred extensions:

- drag-and-drop reordering of songs inside a playlist
- persist to Room when playlists grow beyond a few hundred entries
- allow editing *MediaStore* playlists (requires `WRITE_EXTERNAL_STORAGE`
  below API 29 and a user confirmation on newer versions)

## 15. Folder roots (SAF)

**Files:** `ui/screens/BrowseScreens.kt`, `data/Prefs.kt`

The Folders screen launches `ActivityResultContracts.OpenDocumentTree`, then
converts the tree document id (`primary:Music`, `1234-5678:Music`) to the
filesystem path MediaStore stores in `DATA` and saves it in
`Prefs.folderRoots`. Filtering is a prefix match, and the persisted URI
permission is taken for future document browsing.

Deferred extensions:

- a true SAF browser (`DocumentFile`) so folders that are not in MediaStore can
  be opened directly
- show the picked roots as a hierarchy instead of a flat filtered list
- remove individual roots (currently "Clear" removes all)
- resolve tree paths through `DocumentsContract` for providers other than
  `com.android.externalstorage.documents`

## 16. "Open with" playback

**Files:** `AndroidManifest.xml`, `MainActivity.kt`, `ui/AppRoot.kt`

`MainActivity` is `singleTop` and accepts `ACTION_VIEW` (`content`/`file`
schemes, `audio/*` and `application/ogg`) plus `ACTION_SEND` streams. The uri is
held in a Compose `MutableState` until the permission gate opens; `AppRoot`
resolves it with `MediaMetadataRetriever` on `Dispatchers.IO`, starts playback
and pushes `Route.NowPlaying`.

Deferred extensions:

- if the uri maps to a MediaStore track, reuse the library `Song` instead of a
  transient `id = 0` entry (this also keeps recent-play/favourites working)
- persist a document uri permission when the user wants it in the library
- queue multiple `EXTRA_STREAM` items from a share intent

## 17. Home-screen widget

**Files:** `widget/PlayerWidgetProvider.kt`, `ShPlayerApp.kt`,
`res/layout/widget_player_v2.xml`, `res/xml/widget_player_info.xml`

A 4×1 `AppWidgetProvider` draws disc, queue counter, title/artist,
previous/play-pause/next and a progress line. Controls are explicit broadcasts
handled with `goAsync()`; `PlayerController.whenConnected` queues the command
until the `MediaController` is ready, with a 5 s safety timeout.

**Layout versioning:** `AppWidgetHostView` reuses the existing view tree when a
new `RemoteViews` has the same layout resource id, even if the XML changed. That
leaves old widgets with stale view ids and shows "Problem loading widget".
Whenever the widget view structure changes, add a new layout file (currently
`widget_player_v2.xml`) and keep the old name as an unused placeholder so the
new layout receives a different resource id. Existing widget placements then
re-inflate on the next update; if a launcher still shows the error, remove and
re-add the widget once.

`RemoteViews` cannot host an interactive `SeekBar` or receive drag gestures, so
the progress line is a level-clipped `ImageView` (updated through
`ImageView.setImageLevel`) and tapping it launches `SeekOverlayActivity`: a
translucent activity with a real Material slider handle. The sheet closes two
seconds after the handle is released (three seconds if it is never touched).
The bar is fed from `PlayerUiState.positionMs/durationMs`, and `ShPlayerApp`
allows one widget refresh per elapsed second while playing.

Deferred extensions:

- inline drag inside the widget itself: impossible through `RemoteViews`;
  would require a custom launcher / widget host
- album art bitmap instead of the disc placeholder
- additional sizes/layouts (`targetCellWidth` 2 and 5) and a lock-screen widget
- update via `Glance` for Compose-native styling
- a queue shortcut

## 18. Library sorting

**Files:** `ui/SortMode.kt`, `ui/AppRoot.kt`, `ui/screens/ListScreens.kt`,
`data/MusicRepository.kt`, `data/Prefs.kt`

`SortMode` has a `Default` entry that preserves each screen's natural order
(track order for albums, insertion order for playlists, smart-list order) and
the four requested explicit modes:

- `File name (A to Z)` / `File name (Z to A)` use
  `MediaStore.Audio.Media.DISPLAY_NAME`.
- `Modified (newest first)` / `Modified (oldest first)` use
  `MediaStore.Audio.Media.DATE_MODIFIED`.

The selected mode is persisted by enum name in `Prefs.sortMode`; `AppRoot`
applies it to `routeSongs` and passes it to every song-list screen. The shared
`SongListPage` is deliberately not responsible for ordering so each screen's
header (Play all / Shuffle / count) and its row indices always use the same
ordered list.

The sort menu currently applies to Songs, smart lists, album/artist/genre
details, local/system playlist details and folder details. If Home should gain
its own sort control later, pass `sortMode` to `HomeScreen` and sort
`recentSongs` there; keeping it out of Home preserves the "recently played"
meaning.
