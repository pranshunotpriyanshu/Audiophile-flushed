package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class Badges(
    @SerializedName("musicInlineBadgeRenderer") val musicInlineBadgeRenderer: MusicInlineBadgeRenderer? = null
) {
    data class MusicInlineBadgeRenderer(
        @SerializedName("icon") val icon: Icon? = null
    )
}
