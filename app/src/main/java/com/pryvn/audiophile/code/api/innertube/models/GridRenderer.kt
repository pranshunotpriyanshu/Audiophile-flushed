package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class GridRenderer(
    @SerializedName("header") val header: Any? = null,
    @SerializedName("items") val items: List<Item>? = null,
    @SerializedName("continuations") val continuations: List<ContinuationResponse>? = null
) {
    data class Item(
        @SerializedName("musicResponsiveListItemRenderer") val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        @SerializedName("musicTwoRowItemRenderer") val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null
    )
}
