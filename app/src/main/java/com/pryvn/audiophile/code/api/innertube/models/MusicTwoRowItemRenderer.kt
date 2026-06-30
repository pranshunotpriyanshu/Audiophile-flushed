package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class MusicTwoRowItemRenderer(
    @SerializedName("title") val title: Runs? = null,
    @SerializedName("subtitle") val subtitle: Runs? = null,
    @SerializedName("subtitleBadges") val subtitleBadges: List<Badges>? = null,
    @SerializedName("menu") val menu: Menu? = null,
    @SerializedName("thumbnailRenderer") val thumbnailRenderer: ThumbnailRenderer? = null,
    @SerializedName("navigationEndpoint") val navigationEndpoint: NavigationEndpoint? = null,
    @SerializedName("thumbnailOverlay") val thumbnailOverlay: ThumbnailOverlay? = null
) {
    data class ThumbnailOverlay(
        @SerializedName("musicItemThumbnailOverlayRenderer") val musicItemThumbnailOverlayRenderer: MusicItemThumbnailOverlayRenderer? = null
    ) {
        data class MusicItemThumbnailOverlayRenderer(
            @SerializedName("content") val content: Content? = null
        ) {
            data class Content(
                @SerializedName("musicPlayButtonRenderer") val musicPlayButtonRenderer: MusicPlayButtonRenderer? = null
            ) {
                data class MusicPlayButtonRenderer(
                    @SerializedName("playNavigationEndpoint") val playNavigationEndpoint: NavigationEndpoint? = null,
                    @SerializedName("icon") val icon: Icon? = null
                )
            }
        }
    }

    val isSong: Boolean
        get() = subtitle?.runs?.any { it.text?.contains("Song") == true } == true

    val isPlaylist: Boolean
        get() = subtitle?.runs?.any { it.text?.contains("Playlist") == true } == true

    val isAlbum: Boolean
        get() = subtitle?.runs?.any { it.text?.contains("Album") == true } == true

    val isArtist: Boolean
        get() = subtitle?.runs?.any { it.text?.contains("Artist") == true } == true
}
