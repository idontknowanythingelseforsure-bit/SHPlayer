package com.shplayer.ui

import com.shplayer.data.Song

/**
 * Library sort modes. [Default] preserves the natural order of each screen
 * (track order for albums, insertion order for playlists, smart-list order,
 * etc.); the other modes are explicit user choices.
 */
enum class SortMode(val label: String) {
    Default("Default order"),
    FileNameAsc("File name (A to Z)"),
    FileNameDesc("File name (Z to A)"),
    ModifiedNewest("Modified (newest first)"),
    ModifiedOldest("Modified (oldest first)"),
    ;

    companion object {
        fun fromKey(key: String?): SortMode =
            entries.firstOrNull { it.name == key } ?: Default
    }
}

fun List<Song>.sortedFor(mode: SortMode): List<Song> {
    if (mode == SortMode.Default) return this

    val byFileName = compareBy<Song> { it.sortFileName() }
        .thenBy { it.title.lowercase() }
        .thenBy { it.id }

    return when (mode) {
        SortMode.Default -> this
        SortMode.FileNameAsc -> sortedWith(byFileName)
        SortMode.FileNameDesc -> sortedWith(byFileName.reversed())
        SortMode.ModifiedNewest -> sortedWith(
            compareByDescending<Song> { it.dateModified }
                .thenBy { it.sortFileName() }
                .thenBy { it.id },
        )
        SortMode.ModifiedOldest -> sortedWith(
            compareBy<Song> { it.dateModified }
                .thenBy { it.sortFileName() }
                .thenBy { it.id },
        )
    }
}

private fun Song.sortFileName(): String =
    fileName.ifBlank { title }.lowercase()
