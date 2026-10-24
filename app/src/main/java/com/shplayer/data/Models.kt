package com.shplayer.data

/** A playable audio track from MediaStore. */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val trackNumber: Int,
    val year: Int,
    val dateAdded: Long,
    /** Original file name from MediaStore, used by the file-name sort modes. */
    val fileName: String,
    /** Last modification time in epoch seconds, used by the modified sort modes. */
    val dateModified: Long,
    val genre: String,
    val folder: String,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val year: Int,
)

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
)

data class Genre(
    val name: String,
    val songCount: Int,
)

data class Folder(
    val path: String,
    val name: String,
    val songCount: Int,
)

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
)
