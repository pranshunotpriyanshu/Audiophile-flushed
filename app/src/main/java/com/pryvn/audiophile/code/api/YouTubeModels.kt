package com.pryvn.audiophile.code.api

data class YTSongItem(
    val videoId: String,
    val title: String,
    val artists: List<YTArtist> = emptyList(),
    val album: YTAlbum? = null,
    val durationSeconds: Int? = null,
    val thumbnailUrl: String? = null,
    val playlistId: String? = null,
)

data class YTArtist(
    val name: String,
    val id: String? = null,
)

data class YTAlbum(
    val name: String?,
    val id: String? = null,
    val thumbnailUrl: String? = null,
)

data class YTPlaylist(
    val id: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val songCount: Int? = null,
    val author: String? = null,
)

data class YTSearchResult(
    val items: List<YTSongItem>,
    val continuation: String? = null,
)

data class YTPlaylistPage(
    val playlist: YTPlaylist,
    val songs: List<YTSongItem>,
    val continuation: String? = null,
)

data class YTPlayerResponse(
    val videoId: String,
    val title: String?,
    val artist: String?,
    val thumbnailUrl: String?,
    val lengthSeconds: Int?,
    val streamUrl: String?,
    val expiresInSeconds: Int?,
)

data class HomeItem(
    val videoId: String?,
    val title: String,
    val artists: List<YTArtist> = emptyList(),
    val album: YTAlbum? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val playlistId: String? = null,
    val browseId: String? = null,
)

data class HomeSection(
    val title: String,
    val items: List<HomeItem>,
)

data class YTAccountInfo(
    val name: String,
    val email: String? = null,
    val channelHandle: String? = null,
    val avatarUrl: String? = null,
)