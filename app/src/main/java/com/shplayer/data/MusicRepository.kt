package com.shplayer.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Read-only MediaStore access.
 *
 * OPTIMIZATION HOOK: everything is loaded eagerly into memory. For very large
 * libraries this should move to Paging 3 or incremental cursors; the suspend
 * signatures are stable so callers do not need to change.
 */
class MusicRepository(private val context: Context) {

    private val resolver get() = context.contentResolver

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 3000"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val songs = mutableListOf<Song>()
        runCatching {
            resolver.query(collection, projection, selection, null, sort)?.use { cursor ->
                while (cursor.moveToNext()) {
                    songs += readSong(cursor)
                }
            }
        }
        songs
    }

    suspend fun loadAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            "max_year",
        )
        val albums = mutableListOf<Album>()
        runCatching {
            resolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Audio.Albums.ALBUM} COLLATE NOCASE ASC",
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    albums += Album(
                        id = cursor.long(MediaStore.Audio.Albums._ID),
                        title = cursor.text(MediaStore.Audio.Albums.ALBUM) ?: UNKNOWN,
                        artist = cursor.text(MediaStore.Audio.Albums.ARTIST) ?: UNKNOWN_ARTIST,
                        songCount = cursor.int(MediaStore.Audio.Albums.NUMBER_OF_SONGS),
                        year = cursor.int("max_year"),
                    )
                }
            }
        }
        albums.filter { it.songCount > 0 }
    }

    /**
     * Builds the album list from the song table.
     *
     * The MediaStore album table reports zero songs for every row on some
     * Android 13+ devices, which leaves the Albums browser empty. Grouping the
     * already-loaded songs is reliable and keeps the counts correct.
     */
    fun albumsFromSongs(songs: List<Song>): List<Album> = songs
        .filter { it.albumId > 0L }
        .groupBy { it.albumId }
        .map { (id, tracks) ->
            val first = tracks.first()
            Album(
                id = id,
                title = first.album.ifBlank { UNKNOWN },
                artist = first.artist.ifBlank { UNKNOWN_ARTIST },
                songCount = tracks.size,
                year = tracks.maxOf { it.year },
            )
        }
        .sortedBy { it.title.lowercase() }

    suspend fun loadArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
        )
        val artists = mutableListOf<Artist>()
        runCatching {
            resolver.query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Audio.Artists.ARTIST} COLLATE NOCASE ASC",
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    artists += Artist(
                        name = cursor.text(MediaStore.Audio.Artists.ARTIST) ?: UNKNOWN_ARTIST,
                        songCount = cursor.int(MediaStore.Audio.Artists.NUMBER_OF_TRACKS),
                        albumCount = cursor.int(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS),
                    )
                }
            }
        }
        artists.filter { it.songCount > 0 }
    }

    suspend fun loadGenres(): List<Genre> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Genres._ID,
            MediaStore.Audio.Genres.NAME,
        )
        val genres = mutableListOf<Genre>()
        runCatching {
            resolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Audio.Genres.NAME} COLLATE NOCASE ASC",
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.long(MediaStore.Audio.Genres._ID)
                    val name = cursor.text(MediaStore.Audio.Genres.NAME) ?: continue
                    genres += Genre(name = name, songCount = countGenreMembers(id))
                }
            }
        }
        genres.filter { it.songCount > 0 }
    }

    suspend fun loadFolders(songs: List<Song>): List<Folder> = withContext(Dispatchers.Default) {
        songs.filter { it.folder.isNotBlank() }
            .groupBy { it.folder }
            .map { (path, list) ->
                Folder(
                    path = path,
                    name = path.substringAfterLast('/', path),
                    songCount = list.size,
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun loadPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME,
        )
        val playlists = mutableListOf<Playlist>()
        runCatching {
            resolver.query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Audio.Playlists.NAME} COLLATE NOCASE ASC",
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.long(MediaStore.Audio.Playlists._ID)
                    val name = cursor.text(MediaStore.Audio.Playlists.NAME) ?: continue
                    playlists += Playlist(id = id, name = name, songCount = countPlaylistMembers(id))
                }
            }
        }
        playlists
    }

    suspend fun playlistSongs(playlistId: Long): List<Song> = withContext(Dispatchers.IO) {
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
        querySongs(uri, MediaStore.Audio.Playlists.Members.AUDIO_ID, null, null)
    }

    suspend fun genreSongs(genreName: String): List<Song> = withContext(Dispatchers.IO) {
        val genreId = runCatching {
            resolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Genres._ID),
                "${MediaStore.Audio.Genres.NAME} = ?",
                arrayOf(genreName),
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
        }.getOrNull() ?: return@withContext emptyList()

        val uri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
        querySongs(uri, MediaStore.Audio.Genres.Members.AUDIO_ID, null, null)
    }

    private fun querySongs(
        uri: Uri,
        idColumn: String,
        selection: String?,
        args: Array<String>?,
    ): List<Song> {
        val projection = arrayOf(
            idColumn,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
        )
        val songs = mutableListOf<Song>()
        runCatching {
            resolver.query(uri, projection, selection, args, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    songs += readSong(cursor, idColumn)
                }
            }
        }
        return songs
    }

    private fun countGenreMembers(genreId: Long): Int = runCatching {
        resolver.query(
            MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
            arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
            null,
            null,
            null,
        )?.use { it.count }
    }.getOrNull() ?: 0

    private fun countPlaylistMembers(playlistId: Long): Int = runCatching {
        resolver.query(
            MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
            arrayOf(MediaStore.Audio.Playlists.Members.AUDIO_ID),
            null,
            null,
            null,
        )?.use { it.count }
    }.getOrNull() ?: 0

    private fun readSong(cursor: Cursor, idColumn: String = MediaStore.Audio.Media._ID): Song {
        val id = cursor.long(idColumn)
        val data = cursor.text(MediaStore.Audio.Media.DATA).orEmpty()
        return Song(
            id = id,
            title = cursor.text(MediaStore.Audio.Media.TITLE) ?: UNKNOWN,
            artist = cursor.text(MediaStore.Audio.Media.ARTIST) ?: UNKNOWN_ARTIST,
            album = cursor.text(MediaStore.Audio.Media.ALBUM) ?: UNKNOWN,
            albumId = cursor.long(MediaStore.Audio.Media.ALBUM_ID),
            durationMs = cursor.long(MediaStore.Audio.Media.DURATION),
            uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id,
            ).toString(),
            trackNumber = cursor.int(MediaStore.Audio.Media.TRACK),
            year = cursor.int(MediaStore.Audio.Media.YEAR),
            dateAdded = cursor.long(MediaStore.Audio.Media.DATE_ADDED),
            fileName = cursor.text(MediaStore.Audio.Media.DISPLAY_NAME)
                ?: data.substringAfterLast('/').ifBlank { cursor.text(MediaStore.Audio.Media.TITLE).orEmpty() },
            dateModified = cursor.long(MediaStore.Audio.Media.DATE_MODIFIED),
            genre = "",
            folder = data.substringBeforeLast('/', ""),
        )
    }

    private fun Cursor.long(column: String): Long {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getLong(index) else 0L
    }

    private fun Cursor.int(column: String): Int {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getInt(index) else 0
    }

    private fun Cursor.text(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private companion object {
        const val UNKNOWN = "<unknown>"
        const val UNKNOWN_ARTIST = "<unknown>"
    }
}
