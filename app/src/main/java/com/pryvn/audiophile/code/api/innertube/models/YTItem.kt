package com.pryvn.audiophile.code.api.innertube.models

data class YTItem(
    val id: String,
    val title: String,
    val type: Type = Type.SONG,
    val artists: List<Artist> = emptyList(),
    val album: Album? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val playlistId: String? = null,
    val browseId: String? = null,
) {
    enum class Type {
        SONG, VIDEO, ALBUM, ARTIST, PLAYLIST
    }

    data class Artist(
        val name: String,
        val id: String? = null,
    )

    data class Album(
        val name: String?,
        val id: String? = null,
    )
}

data class SongItem(
    val id: String,
    val title: String,
    val artists: List<YTItem.Artist> = emptyList(),
    val album: YTItem.Album? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val playlistId: String? = null,
)

data class AlbumItem(
    val browseId: String?,
    val playlistId: String?,
    val title: String,
    val artists: String? = null,
    val year: String? = null,
    val thumbnail: String? = null,
    val explicit: Boolean = false,
)

data class ArtistItem(
    val id: String? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    val channelId: String? = null,
    val playEndpoint: WatchEndpoint? = null,
    val shuffleEndpoint: WatchEndpoint? = null,
    val radioEndpoint: WatchEndpoint? = null,
    val subscriberCountText: String? = null,
    val monthlyListenerCountText: String? = null,
)

data class PlaylistItem(
    val id: String,
    val title: String,
    val author: YTItem.Artist? = null,
    val songCountText: String? = null,
    val thumbnail: String? = null,
    val description: String? = null,
    val playEndpoint: WatchEndpoint? = null,
    val shuffleEndpoint: WatchEndpoint? = null,
    val radioEndpoint: WatchEndpoint? = null,
    val isEditable: Boolean = false,
)

data class WatchEndpoint(
    val videoId: String? = null,
    val playlistId: String? = null,
)

data class BrowseEndpoint(
    val browseId: String? = null,
    val params: String? = null,
)

data class MediaInfo(
    val videoId: String,
    val title: String? = null,
    val author: String? = null,
    val authorId: String? = null,
    val authorThumbnail: String? = null,
    val description: String? = null,
    val subscribers: String? = null,
    val uploadDate: String? = null,
    val viewCount: Long? = null,
    val like: Long? = null,
    val dislike: Long? = null,
)

data class AccountInfo(
    val name: String,
    val email: String? = null,
    val channelHandle: String? = null,
    val avatarUrl: String? = null,
)

data class AccountChannel(
    val name: String,
    val channelId: String? = null,
    val thumbnailUrl: String? = null,
)
