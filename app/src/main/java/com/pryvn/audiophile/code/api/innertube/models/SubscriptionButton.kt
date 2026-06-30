package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class SubscriptionButton(
    @SerializedName("subscribeButtonRenderer") val subscribeButtonRenderer: SubscribeButtonRenderer? = null
) {
    data class SubscribeButtonRenderer(
        @SerializedName("subscribed") val subscribed: Boolean? = null,
        @SerializedName("channelId") val channelId: String? = null,
        @SerializedName("showSubscriptionFeedback") val showSubscriptionFeedback: Boolean? = null,
        @SerializedName("onTap") val onTap: NavigationEndpoint? = null
    )
}
