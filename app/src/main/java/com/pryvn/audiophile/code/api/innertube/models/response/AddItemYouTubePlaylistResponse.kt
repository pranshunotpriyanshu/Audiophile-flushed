package com.pryvn.audiophile.code.api.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class AddItemYouTubePlaylistResponse(
    val playlistEditResults: List<PlaylistEditResult> = emptyList(),
) {
    @Serializable
    data class PlaylistEditResult(
        val playlistEditVideoAddedResultData: PlaylistEditVideoAddedResultData? = null,
    )

    @Serializable
    data class PlaylistEditVideoAddedResultData(
        val videoId: String? = null,
        val setVideoId: String? = null,
    )
}
