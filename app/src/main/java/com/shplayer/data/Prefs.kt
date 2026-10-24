package com.shplayer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** App-local playlist. Negative ids keep them apart from MediaStore playlists. */
data class LocalPlaylist(
    val id: Long,
    val name: String,
    val songIds: List<Long>,
)

/**
 * Small settings store.
 *
 * OPTIMIZATION HOOK: several playback settings are persisted and exposed in the
 * Settings screen but are not applied to the player yet. Search for
 * `OPTIMIZATION HOOK` to find every deferred extension point.
 */
object Prefs {
    private const val FILE = "sh_player_prefs"

    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_SORT_MODE = "sort_mode"
    private const val KEY_GAPLESS = "gapless"
    private const val KEY_CROSSFADE = "crossfade_seconds"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_PLAY_COUNTS = "play_counts"
    private const val KEY_RECENTLY_PLAYED = "recently_played"
    private const val KEY_LOCAL_PLAYLISTS = "local_playlists"
    private const val KEY_FOLDER_ROOTS = "folder_roots"

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, THEME_DARK)
        set(value) = prefs.edit { putInt(KEY_THEME_MODE, value) }

    /** Selected library sort mode, stored by enum name. */
    var sortMode: String
        get() = prefs.getString(KEY_SORT_MODE, "Default").orEmpty()
        set(value) = prefs.edit { putString(KEY_SORT_MODE, value) }

    /** Reserved: needs a gapless-capable audio sink before it can be enabled. */
    var gaplessEnabled: Boolean
        get() = prefs.getBoolean(KEY_GAPLESS, false)
        set(value) = prefs.edit { putBoolean(KEY_GAPLESS, value) }

    /** Reserved: crossfade window in seconds. */
    var crossfadeSeconds: Int
        get() = prefs.getInt(KEY_CROSSFADE, 0)
        set(value) = prefs.edit { putInt(KEY_CROSSFADE, value) }

    /** Song ids the user marked as favourite ("Top rated" smart list). */
    var favorites: Set<Long>
        get() = prefs.getStringSet(KEY_FAVORITES, emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        set(value) = prefs.edit { putStringSet(KEY_FAVORITES, value.map { it.toString() }.toSet()) }

    /** @return true when the song was added to favourites. */
    fun toggleFavorite(songId: Long): Boolean {
        val updated = favorites.toMutableSet()
        val added = if (updated.contains(songId)) {
            updated.remove(songId)
            false
        } else {
            updated.add(songId)
            true
        }
        favorites = updated
        return added
    }

    /** Play counters used by the "Most played" smart list. */
    var playCounts: Map<Long, Int>
        get() = prefs.getStringSet(KEY_PLAY_COUNTS, emptySet())
            .orEmpty()
            .mapNotNull { entry ->
                val parts = entry.split('=')
                if (parts.size == 2) {
                    parts[0].toLongOrNull()?.let { it to (parts[1].toIntOrNull() ?: 0) }
                } else {
                    null
                }
            }
            .toMap()
        set(value) = prefs.edit {
            putStringSet(KEY_PLAY_COUNTS, value.map { "${it.key}=${it.value}" }.toSet())
        }

    fun recordPlay(songId: Long) {
        if (songId <= 0L) return
        val updated = playCounts.toMutableMap()
        updated[songId] = (updated[songId] ?: 0) + 1
        playCounts = updated

        val recent = recentlyPlayed.toMutableList()
        recent.remove(songId)
        recent.add(0, songId)
        recentlyPlayed = recent.take(MAX_RECENT)
    }

    /** Most recently started songs, newest first (max [MAX_RECENT] ids). */
    var recentlyPlayed: List<Long>
        get() = prefs.getString(KEY_RECENTLY_PLAYED, "")
            .orEmpty()
            .split(',')
            .mapNotNull { it.toLongOrNull() }
        set(value) = prefs.edit {
            putString(KEY_RECENTLY_PLAYED, value.distinct().take(MAX_RECENT).joinToString(","))
        }

    /** User-created playlists, stored as JSON because MediaStore playlists are read-only here. */
    var localPlaylists: List<LocalPlaylist>
        get() {
            val raw = prefs.getString(KEY_LOCAL_PLAYLISTS, null) ?: return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val ids = mutableListOf<Long>()
                        val songArray = item.optJSONArray("songs") ?: JSONArray()
                        for (j in 0 until songArray.length()) {
                            ids.add(songArray.optLong(j))
                        }
                        add(
                            LocalPlaylist(
                                id = item.optLong("id"),
                                name = item.optString("name"),
                                songIds = ids,
                            ),
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }
        set(value) = prefs.edit {
            val array = JSONArray()
            value.forEach { playlist ->
                array.put(
                    JSONObject().apply {
                        put("id", playlist.id)
                        put("name", playlist.name)
                        put("songs", JSONArray(playlist.songIds))
                    },
                )
            }
            putString(KEY_LOCAL_PLAYLISTS, array.toString())
        }

    fun createLocalPlaylist(name: String): LocalPlaylist {
        val playlist = LocalPlaylist(
            id = -System.currentTimeMillis(),
            name = name.trim().ifBlank { "Playlist" },
            songIds = emptyList(),
        )
        localPlaylists = localPlaylists + playlist
        return playlist
    }

    /** @return true when the playlist exists (song is added once). */
    fun addToLocalPlaylist(playlistId: Long, songId: Long): Boolean =
        addSongsToLocalPlaylist(playlistId, listOf(songId)) >= 0

    /**
     * Adds every id that is not already in the playlist.
     *
     * @return the number of newly added songs, or -1 when the playlist is gone.
     */
    fun addSongsToLocalPlaylist(playlistId: Long, songIds: List<Long>): Int {
        val playlists = localPlaylists.toMutableList()
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index < 0) return -1
        val playlist = playlists[index]
        val existing = playlist.songIds.toSet()
        val newIds = songIds.distinct().filter { it > 0L && it !in existing }
        if (newIds.isEmpty()) return 0
        playlists[index] = playlist.copy(songIds = playlist.songIds + newIds)
        localPlaylists = playlists
        return newIds.size
    }

    /** @return true when the playlist existed and the new name was saved. */
    fun renameLocalPlaylist(playlistId: Long, name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val playlists = localPlaylists.toMutableList()
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index < 0) return false
        playlists[index] = playlists[index].copy(name = trimmed)
        localPlaylists = playlists
        return true
    }

    /** @return true when the playlist existed and was removed. */
    fun removeLocalPlaylist(playlistId: Long): Boolean {
        val playlists = localPlaylists
        val remaining = playlists.filterNot { it.id == playlistId }
        if (remaining.size == playlists.size) return false
        localPlaylists = remaining
        return true
    }

    /**
     * Removes songs from a local playlist.
     *
     * @return the number of songs removed, or -1 when the playlist is gone.
     */
    fun removeSongsFromLocalPlaylist(playlistId: Long, songIds: List<Long>): Int {
        val playlists = localPlaylists.toMutableList()
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index < 0) return -1
        val playlist = playlists[index]
        val removeIds = songIds.toSet()
        val updated = playlist.songIds.filterNot { it in removeIds }
        val removed = playlist.songIds.size - updated.size
        if (removed > 0) {
            playlists[index] = playlist.copy(songIds = updated)
            localPlaylists = playlists
        }
        return removed
    }

    /**
     * Folders the user picked through the system file manager (SAF tree picker).
     * When non-empty the Folders screen shows only these roots unless "Show all".
     */
    var folderRoots: Set<String>
        get() = prefs.getStringSet(KEY_FOLDER_ROOTS, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit { putStringSet(KEY_FOLDER_ROOTS, value) }

    private const val MAX_RECENT = 100
}
