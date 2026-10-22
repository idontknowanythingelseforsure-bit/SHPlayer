package com.shplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shplayer.data.Album
import com.shplayer.data.Artist
import com.shplayer.data.Folder
import com.shplayer.data.Genre
import com.shplayer.data.LocalPlaylist
import com.shplayer.data.MusicRepository
import com.shplayer.data.Playlist
import com.shplayer.data.Prefs
import com.shplayer.data.Song
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val loading: Boolean = true,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val localPlaylists: List<Playlist> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val playCounts: Map<Long, Int> = emptyMap(),
    val recentlyPlayed: List<Long> = emptyList(),
    val folderRoots: Set<String> = emptySet(),
    val error: String? = null,
) {
    /** MediaStore playlists plus user-created ones (negative ids). */
    val allPlaylists: List<Playlist> get() = localPlaylists + playlists
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val _state = MutableStateFlow(
        LibraryUiState(
            favorites = Prefs.favorites,
            playCounts = Prefs.playCounts,
            recentlyPlayed = Prefs.recentlyPlayed,
            localPlaylists = localPlaylistsOf(Prefs.localPlaylists),
            folderRoots = Prefs.folderRoots,
        ),
    )
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    favorites = Prefs.favorites,
                    playCounts = Prefs.playCounts,
                    recentlyPlayed = Prefs.recentlyPlayed,
                    localPlaylists = localPlaylistsOf(Prefs.localPlaylists),
                    folderRoots = Prefs.folderRoots,
                )
            }
            runCatching {
                val songsDeferred = async { repository.loadSongs() }
                val artistsDeferred = async { repository.loadArtists() }
                val genresDeferred = async { repository.loadGenres() }
                val playlistsDeferred = async { repository.loadPlaylists() }

                val songs = songsDeferred.await()
                _state.update {
                    it.copy(
                        loading = false,
                        songs = songs,
                        // Derived from songs: the MediaStore Albums table is unreliable.
                        albums = repository.albumsFromSongs(songs),
                        artists = artistsDeferred.await(),
                        genres = genresDeferred.await(),
                        playlists = playlistsDeferred.await(),
                    )
                }
                val folders = repository.loadFolders(songs)
                _state.update { it.copy(folders = folders) }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = throwable.message ?: "Unable to read the music library",
                    )
                }
            }
        }
    }

    fun toggleFavorite(songId: Long): Boolean {
        val added = Prefs.toggleFavorite(songId)
        _state.update { it.copy(favorites = Prefs.favorites) }
        return added
    }

    fun recordPlay(songId: Long) {
        Prefs.recordPlay(songId)
        _state.update {
            it.copy(
                playCounts = Prefs.playCounts,
                recentlyPlayed = Prefs.recentlyPlayed,
            )
        }
    }

    /** Creates an app-local playlist and returns its (negative) id. */
    fun createPlaylist(name: String): Long {
        val playlist = Prefs.createLocalPlaylist(name)
        refreshLocalPlaylists()
        return playlist.id
    }

    /** @return false when the playlist no longer exists. */
    fun addSongToPlaylist(playlistId: Long, songId: Long): Boolean {
        val added = Prefs.addSongsToLocalPlaylist(playlistId, listOf(songId))
        if (added > 0) refreshLocalPlaylists()
        return added >= 0
    }

    /** @return the number of newly added songs (duplicates are skipped). */
    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>): Int {
        val added = Prefs.addSongsToLocalPlaylist(playlistId, songIds)
        if (added > 0) refreshLocalPlaylists()
        return added
    }

    fun addFolderRoot(path: String) {
        val normalized = path.trimEnd('/')
        if (normalized.isBlank()) return
        Prefs.folderRoots = Prefs.folderRoots + normalized
        _state.update { it.copy(folderRoots = Prefs.folderRoots) }
    }

    fun clearFolderRoots() {
        Prefs.folderRoots = emptySet()
        _state.update { it.copy(folderRoots = emptySet()) }
    }

    private fun refreshLocalPlaylists() {
        _state.update { it.copy(localPlaylists = localPlaylistsOf(Prefs.localPlaylists)) }
    }

    /** Resolves playlist contents, then hands the result back for playback. */
    fun loadPlaylistSongs(playlistId: Long, onReady: (List<Song>) -> Unit) {
        if (playlistId < 0L) {
            onReady(localPlaylistSongs(playlistId))
            return
        }
        viewModelScope.launch {
            onReady(resolvePlaylistSongs(playlistId))
        }
    }

    /** Suspend resolver used by the app bar's "add current list" action. */
    suspend fun resolvePlaylistSongs(playlistId: Long): List<Song> =
        if (playlistId < 0L) {
            localPlaylistSongs(playlistId)
        } else {
            runCatching { repository.playlistSongs(playlistId) }.getOrDefault(emptyList())
        }

    /** Suspend resolver used by the app bar's "add current list" action. */
    suspend fun resolveGenreSongs(genreName: String): List<Song> =
        runCatching { repository.genreSongs(genreName) }.getOrDefault(emptyList())

    private fun localPlaylistSongs(playlistId: Long): List<Song> {
        val ids = Prefs.localPlaylists.firstOrNull { it.id == playlistId }?.songIds.orEmpty()
        val songsById = _state.value.songs.associateBy { it.id }
        return ids.mapNotNull { songsById[it] }
    }

    /** Resolves genre contents, then hands the result back for playback. */
    fun loadGenreSongs(genreName: String, onReady: (List<Song>) -> Unit) {
        viewModelScope.launch {
            val songs = runCatching { repository.genreSongs(genreName) }.getOrDefault(emptyList())
            onReady(songs)
        }
    }

    private fun localPlaylistsOf(playlists: List<LocalPlaylist>): List<Playlist> =
        playlists.map { Playlist(id = it.id, name = it.name, songCount = it.songIds.size) }
}
