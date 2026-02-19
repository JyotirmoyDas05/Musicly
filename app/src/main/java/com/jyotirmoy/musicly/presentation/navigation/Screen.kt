package com.jyotirmoy.musicly.presentation.navigation

import androidx.compose.runtime.Immutable


@Immutable
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Explore : Screen("explore")
    object Search : Screen("search")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object SettingsCategory : Screen("settings_category/{categoryId}") {
        fun createRoute(categoryId: String) = "settings_category/$categoryId"
    }
    object PaletteStyle : Screen("palette_style_settings")
    object Experimental : Screen("experimental_settings")
    object NavBarCrRad : Screen("nav_bar_corner_radius")
    object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }

    object  DailyMixScreen : Screen("daily_mix")
    object RecentlyPlayed : Screen("recently_played")
    object Stats : Screen("stats")
    object GenreDetail : Screen("genre_detail/{genreId}") { // New screen
        fun createRoute(genreId: String) = "genre_detail/$genreId"
    }
    object DJSpace : Screen("dj_space")
    // The base route is "album_detail". The full route with argument is defined in AppNavigation.
    object AlbumDetail : Screen("album_detail/{albumId}") {
        // Helper function to build the navigation route with the album ID.
        fun createRoute(albumId: Long) = "album_detail/$albumId"
        /** For online albums, the ID is a string browseId like "MPREb_..." */
        fun createRoute(browseId: String) = "album_detail/$browseId"
    }

    object ArtistDetail : Screen("artist_detail/{artistId}") {
        fun createRoute(artistId: Long) = "artist_detail/$artistId"
        /** For online artists, the ID is a string browseId like "UC..." */
        fun createRoute(browseId: String) = "artist_detail/$browseId"
    }

    object EditTransition : Screen("edit_transition?playlistId={playlistId}") {
        fun createRoute(playlistId: String?) =
            if (playlistId != null) "edit_transition?playlistId=$playlistId" else "edit_transition"
    }

    object About : Screen("about")

    object ArtistSettings : Screen("artist_settings")
    object DelimiterConfig : Screen("delimiter_config")
    object Equalizer : Screen("equalizer")

    object MoodDetail : Screen("mood_detail/{browseId}?title={title}&color={color}&params={params}") {
        fun createRoute(
            browseId: String,
            title: String,
            color: Long = 0L,
            params: String? = null,
        ): String {
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val encodedParams = params?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
            return "mood_detail/$browseId?title=$encodedTitle&color=$color&params=$encodedParams"
        }
    }

    object OnlinePlaylistDetail : Screen("online_playlist/{playlistId}") {
        fun createRoute(playlistId: String): String = "online_playlist/$playlistId"
    }

}
