package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class SectionListRenderer(
    @SerializedName("header") val header: Header? = null,
    @SerializedName("contents") val contents: List<Content>? = null,
    @SerializedName("continuations") val continuations: List<ContinuationResponse>? = null
) {
    data class Header(
        @SerializedName("chipCloudRenderer") val chipCloudRenderer: ChipCloudRenderer? = null
    ) {
        data class ChipCloudRenderer(
            @SerializedName("chips") val chips: List<Chip>? = null
        ) {
            data class Chip(
                @SerializedName("chipCloudChipRenderer") val chipCloudChipRenderer: ChipCloudChipRenderer? = null
            ) {
                data class ChipCloudChipRenderer(
                    @SerializedName("text") val text: Runs? = null,
                    @SerializedName("navigationEndpoint") val navigationEndpoint: NavigationEndpoint? = null,
                    @SerializedName("selected") val selected: Boolean? = null
                )
            }
        }
    }

    data class Content(
        @SerializedName("musicCarouselShelfRenderer") val musicCarouselShelfRenderer: MusicCarouselShelfRenderer? = null,
        @SerializedName("musicShelfRenderer") val musicShelfRenderer: MusicShelfRenderer? = null
    ) {
        data class MusicCarouselShelfRenderer(
            @SerializedName("header") val header: Header? = null,
            @SerializedName("contents") val contents: List<CarouselContent>? = null
        ) {
            data class Header(
                @SerializedName("musicCarouselShelfBasicHeaderRenderer") val musicCarouselShelfBasicHeaderRenderer: MusicCarouselShelfBasicHeaderRenderer? = null
            ) {
                data class MusicCarouselShelfBasicHeaderRenderer(
                    @SerializedName("title") val title: Runs? = null,
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

            data class CarouselContent(
                @SerializedName("musicResponsiveListItemRenderer") val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
                @SerializedName("musicTwoRowItemRenderer") val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null
            )
        }
    }
}
