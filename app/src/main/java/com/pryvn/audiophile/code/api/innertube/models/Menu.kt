package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class Menu(
    @SerializedName("menuRenderer") val menuRenderer: MenuRenderer? = null
) {
    data class MenuRenderer(
        @SerializedName("items") val items: List<MenuItem>? = null,
        @SerializedName("topLevelButtons") val topLevelButtons: List<TopLevelButton>? = null
    ) {
        data class MenuItem(
            @SerializedName("menuNavigationItemRenderer") val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null,
            @SerializedName("menuServiceItemRenderer") val menuServiceItemRenderer: MenuServiceItemRenderer? = null
        ) {
            data class MenuNavigationItemRenderer(
                @SerializedName("text") val text: Runs? = null,
                @SerializedName("navigationEndpoint") val navigationEndpoint: NavigationEndpoint? = null,
                @SerializedName("icon") val icon: Icon? = null
            )

            data class MenuServiceItemRenderer(
                @SerializedName("text") val text: Runs? = null,
                @SerializedName("serviceEndpoint") val serviceEndpoint: NavigationEndpoint? = null,
                @SerializedName("icon") val icon: Icon? = null
            )
        }

        data class TopLevelButton(
            @SerializedName("buttonRenderer") val buttonRenderer: Button.ButtonRenderer? = null,
            @SerializedName("toggleButtonRenderer") val toggleButtonRenderer: ToggleButtonRenderer? = null
        ) {
            data class ToggleButtonRenderer(
                @SerializedName("isToggled") val isToggled: Boolean? = null,
                @SerializedName("isDisabled") val isDisabled: Boolean? = null,
                @SerializedName("defaultIcon") val defaultIcon: Icon? = null,
                @SerializedName("toggledIcon") val toggledIcon: Icon? = null,
                @SerializedName("defaultText") val defaultText: Runs? = null,
                @SerializedName("toggledText") val toggledText: Runs? = null,
                @SerializedName("defaultTooltip") val defaultTooltip: String? = null,
                @SerializedName("toggledTooltip") val toggledTooltip: String? = null,
                @SerializedName("toggleButtonSupportedData") val toggleButtonSupportedData: ToggleButtonSupportedData? = null,
                @SerializedName("defaultNavigationEndpoint") val defaultNavigationEndpoint: NavigationEndpoint? = null
            ) {
                data class ToggleButtonSupportedData(
                    @SerializedName("toggleButtonId") val toggleButtonId: String? = null,
                    @SerializedName("accessibilityData") val accessibilityData: AccessibilityData? = null
                ) {
                    data class AccessibilityData(
                        @SerializedName("accessibilityData") val accessibilityData: AccessibilityLabel? = null
                    ) {
                        data class AccessibilityLabel(
                            @SerializedName("label") val label: String? = null
                        )
                    }
                }
            }
        }
    }
}
