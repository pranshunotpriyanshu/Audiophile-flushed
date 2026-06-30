package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class PlaylistPanelRenderer(
    @SerializedName("title") val title: String? = null,
    @SerializedName("subtitle") val subtitle: Runs? = null,
    @SerializedName("contents") val contents: List<Content>? = null,
    @SerializedName("continuations") val continuations: List<ContinuationResponse>? = null,
    @SerializedName("autoPlay") val autoPlay: Boolean? = null,
    @SerializedName("header") val header: Header? = null
) {
    data class Content(
        @SerializedName("playlistPanelVideoRenderer") val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null
    ) {
        data class PlaylistPanelVideoRenderer(
            @SerializedName("videoId") val videoId: String? = null,
            @SerializedName("title") val title: Runs? = null,
            @SerializedName("shortBylineText") val shortBylineText: Runs? = null,
            @SerializedName("longBylineText") val longBylineText: Runs? = null,
            @SerializedName("thumbnail") val thumbnail: ThumbnailRenderer? = null,
            @SerializedName("lengthSeconds") val lengthSeconds: String? = null,
            @SerializedName("index") val index: Int? = null,
            @SerializedName("playlistSetVideoId") val playlistSetVideoId: String? = null,
            @SerializedName("navigationEndpoint") val navigationEndpoint: NavigationEndpoint? = null,
            @SerializedName("menu") val menu: Menu? = null
        )
    }

    data class Header(
        @SerializedName("playlistPanelHeaderRenderer") val playlistPanelHeaderRenderer: PlaylistPanelHeaderRenderer? = null
    ) {
        data class PlaylistPanelHeaderRenderer(
            @SerializedName("title") val title: Runs? = null,
            @SerializedName("subtitle") val subtitle: Runs? = null
        )
    }
}
