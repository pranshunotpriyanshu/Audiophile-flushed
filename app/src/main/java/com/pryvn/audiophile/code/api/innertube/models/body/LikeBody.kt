package com.pryvn.audiophile.code.api.innertube.models.body

import com.pryvn.audiophile.code.api.innertube.models.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LikeBody(
    val context: Context,
    val target: Target,
) {
    @Serializable
    data class Target(
        val videoId: String? = null,
        val playlistId: String? = null,
    ) {
        companion object {
            fun VideoTarget(videoId: String) = Target(videoId = videoId)
            fun PlaylistTarget(playlistId: String) = Target(playlistId = playlistId)
        }
    }
}
