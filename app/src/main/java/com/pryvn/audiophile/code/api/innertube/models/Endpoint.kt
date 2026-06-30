package com.pryvn.audiophile.code.api.innertube.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Endpoint {

    @Serializable
    data class WatchEndpoint(
        @SerialName("videoId") val videoId: String? = null,
        @SerialName("playlistId") val playlistId: String? = null,
        @SerialName("playlistSetVideoId") val playlistSetVideoId: String? = null,
        @SerialName("params") val params: String? = null,
        @SerialName("index") val index: Int? = null,
    ) : Endpoint()

    @Serializable
    data class BrowseEndpoint(
        @SerialName("browseId") val browseId: String? = null,
        @SerialName("params") val params: String? = null,
    ) : Endpoint() {
        val isArtistEndpoint: Boolean
            get() = browseId?.startsWith("UC") == true

        val isAlbumEndpoint: Boolean
            get() = browseId?.startsWith("MPREb") == true

        val isPlaylistEndpoint: Boolean
            get() = browseId?.startsWith("VL") == true

        companion object {
            const val PAGE_TYPE_UNKNOWN = "UNKNOWN"
            const val PAGE_TYPE_MUSIC_HOME = "MUSIC_HOME"
            const val PAGE_TYPE_MUSIC_SEARCH = "MUSIC_SEARCH"
            const val PAGE_TYPE_MUSIC_LIBRARY = "MUSIC_LIBRARY"
            const val PAGE_TYPE_MUSIC_ARTIST = "MUSIC_ARTIST"
            const val PAGE_TYPE_MUSIC_ALBUM = "MUSIC_ALBUM"
            const val PAGE_TYPE_MUSIC_PLAYLIST = "MUSIC_PLAYLIST"
        }
    }

    @Serializable
    data class SearchEndpoint(
        @SerialName("params") val params: String? = null,
        @SerialName("query") val query: String? = null,
    ) : Endpoint()

    @Serializable
    data class QueueAddEndpoint(
        @SerialName("queueInsertPosition") val queueInsertPosition: String? = null,
        @SerialName("queueTarget") val queueTarget: QueueTarget? = null,
    ) : Endpoint() {
        @Serializable
        data class QueueTarget(
            @SerialName("videoId") val videoId: String? = null,
            @SerialName("playlistId") val playlistId: String? = null,
        )
    }

    @Serializable
    data class ShareEntityEndpoint(
        @SerialName("serializedShareEntity") val serializedShareEntity: String? = null,
    ) : Endpoint()

    @Serializable
    data class WatchPlaylistEndpoint(
        @SerialName("playlistId") val playlistId: String? = null,
        @SerialName("params") val params: String? = null,
        @SerialName("index") val index: Int? = null,
    ) : Endpoint()
}
