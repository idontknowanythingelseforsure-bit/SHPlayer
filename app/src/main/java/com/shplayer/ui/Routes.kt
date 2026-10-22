package com.shplayer.ui

enum class SmartKind(val title: String, val subtitle: String) {
    RecentlyPlayed("Recently played", "Smart playlist"),
    RecentlyAdded("Recently added", "Smart playlist"),
    TopRated("Top rated", "Favorites"),
    MostPlayed("Most played", "Smart playlist"),
}

sealed interface Route {
    data object Home : Route
    data object Songs : Route
    data object Albums : Route
    data class AlbumDetail(val albumId: Long) : Route
    data object Artists : Route
    data class ArtistDetail(val artist: String) : Route
    data object Genres : Route
    data class GenreDetail(val genre: String) : Route
    data object Playlists : Route
    data class PlaylistDetail(val playlistId: Long, val name: String) : Route
    data object Folders : Route
    data class FolderDetail(val path: String, val name: String) : Route
    data object Search : Route
    data object NowPlaying : Route
    data object Equalizer : Route
    data object Settings : Route
    data class Smart(val kind: SmartKind) : Route
    data class SongPicker(val playlistId: Long, val playlistName: String) : Route
    data class FolderPicker(val playlistId: Long, val playlistName: String) : Route
}

fun Route.title(): String = when (this) {
    Route.Home -> "SH Player"
    Route.Songs -> "Songs"
    Route.Albums -> "Albums"
    is Route.AlbumDetail -> "Album"
    Route.Artists -> "Artists"
    is Route.ArtistDetail -> artist
    Route.Genres -> "Genres"
    is Route.GenreDetail -> genre
    Route.Playlists -> "Playlists"
    is Route.PlaylistDetail -> name
    Route.Folders -> "Folders"
    is Route.FolderDetail -> name
    Route.Search -> "Search"
    Route.NowPlaying -> "Now playing"
    Route.Equalizer -> "Equalizer"
    Route.Settings -> "Settings"
    is Route.Smart -> kind.title
    is Route.SongPicker -> "Add songs"
    is Route.FolderPicker -> "Add folder"
}
