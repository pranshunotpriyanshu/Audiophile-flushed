package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class MusicShelfRenderer(
    @SerializedName("title") val title: Runs? = null,
    @SerializedName("contents") val contents: List<Content>? = null,
    @SerializedName("continuations") val continuations: List<ContinuationResponse>? = null,
    @SerializedName("bottomEndpoint") val bottomEndpoint: NavigationEndpoint? = null,
    @SerializedName("moreContentButton") val moreContentButton: Button? = null
) {
    data class Content(
        @SerializedName("musicResponsiveListItemRenderer") val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null
    )
}

fun MusicShelfRenderer?.getItems(): List<MusicResponsiveListItemRenderer> {
    return this?.contents?.mapNotNull { it.musicResponsiveListItemRenderer } ?: emptyList()
}

fun MusicShelfRenderer?.getContinuation(): String? {
    return this?.continuations.getContinuation()
}
