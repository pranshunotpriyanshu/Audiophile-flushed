package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class Button(
    @SerializedName("buttonRenderer") val buttonRenderer: ButtonRenderer? = null
) {
    data class ButtonRenderer(
        @SerializedName("text") val text: Runs? = null,
        @SerializedName("navigationEndpoint") val navigationEndpoint: NavigationEndpoint? = null,
        @SerializedName("icon") val icon: Icon? = null,
        @SerializedName("accessibility") val accessibility: Accessibility? = null,
        @SerializedName("size") val size: String? = null,
        @SerializedName("style") val style: String? = null
    ) {
        data class Accessibility(
            @SerializedName("accessibilityData") val accessibilityData: AccessibilityData? = null
        ) {
            data class AccessibilityData(
                @SerializedName("label") val label: String? = null
            )
        }
    }
}
