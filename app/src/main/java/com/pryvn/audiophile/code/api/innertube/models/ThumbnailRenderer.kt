package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class ThumbnailRenderer(
    @SerializedName("musicThumbnailRenderer") val musicThumbnailRenderer: MusicThumbnailRenderer? = null
) {
    data class MusicThumbnailRenderer(
        @SerializedName("thumbnail") val thumbnail: Thumbnail? = null,
        @SerializedName("thumbnailCrop") val thumbnailCrop: String? = null,
        @SerializedName("thumbnailScale") val thumbnailScale: String? = null,
        @SerializedName("rendererContext") val rendererContext: RendererContext? = null
    ) {
        data class Thumbnail(
            @SerializedName("thumbnails") val thumbnails: List<ThumbnailUrl>? = null
        ) {
            data class ThumbnailUrl(
                @SerializedName("url") val url: String? = null,
                @SerializedName("width") val width: Int? = null,
                @SerializedName("height") val height: Int? = null
            )
        }

        data class RendererContext(
            @SerializedName("commandMetadata") val commandMetadata: CommandMetadata? = null
        ) {
            data class CommandMetadata(
                @SerializedName("webCommandMetadata") val webCommandMetadata: WebCommandMetadata? = null
            ) {
                data class WebCommandMetadata(
                    @SerializedName("url") val url: String? = null
                )
            }
        }
    }

    fun getThumbnailUrl(): String? {
        return musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url
    }
}
