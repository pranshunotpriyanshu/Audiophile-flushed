package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class MusicResponsiveListItemRenderer(
    @SerializedName("badges") val badges: List<Badges>? = null,
    @SerializedName("fixedColumns") val fixedColumns: List<FixedColumn>? = null,
    @SerializedName("flexColumns") val flexColumns: List<FlexColumn>? = null,
    @SerializedName("thumbnail") val thumbnail: ThumbnailRenderer? = null,
    @SerializedName("menu") val menu: Menu? = null,
    @SerializedName("playlistItemData") val playlistItemData: PlaylistItemData? = null,
    @SerializedName("overlay") val overlay: Overlay? = null,
    @SerializedName("navigationEndpoint") val navigationEndpoint: NavigationEndpoint? = null
) {
    data class FixedColumn(
        @SerializedName("musicResponsiveListItemFixedColumnRenderer") val musicResponsiveListItemFixedColumnRenderer: MusicResponsiveListItemFixedColumnRenderer? = null
    ) {
        data class MusicResponsiveListItemFixedColumnRenderer(
            @SerializedName("text") val text: Runs? = null,
            @SerializedName("accessibility") val accessibility: Accessibility? = null
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

    data class FlexColumn(
        @SerializedName("musicResponsiveListItemFlexColumnRenderer") val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer? = null
    ) {
        data class MusicResponsiveListItemFlexColumnRenderer(
            @SerializedName("text") val text: Runs? = null,
            @SerializedName("displayPriority") val displayPriority: String? = null
        )
    }

    data class PlaylistItemData(
        @SerializedName("videoId") val videoId: String? = null,
        @SerializedName("playlistSetVideoId") val playlistSetVideoId: String? = null
    )

    data class Overlay(
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
        get() = flexColumns?.any { column ->
            column.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.any { run ->
                run.text?.contains("Song") == true
            } == true
        } == true

    val isPlaylist: Boolean
        get() = flexColumns?.any { column ->
            column.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.any { run ->
                run.text?.contains("Playlist") == true
            } == true
        } == true

    val isAlbum: Boolean
        get() = flexColumns?.any { column ->
            column.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.any { run ->
                run.text?.contains("Album") == true
            } == true
        } == true

    val isArtist: Boolean
        get() = flexColumns?.any { column ->
            column.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.any { run ->
                run.text?.contains("Artist") == true
            } == true
        } == true
}
