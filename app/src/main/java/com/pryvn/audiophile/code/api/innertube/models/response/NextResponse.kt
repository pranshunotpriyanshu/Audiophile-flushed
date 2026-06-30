package com.pryvn.audiophile.code.api.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class NextResponse(
    val contents: Contents? = null,
    val continuationContents: ContinuationContents? = null,
    val currentVideoEndpoint: CurrentVideoEndpoint? = null
) {
    @Serializable
    data class CurrentVideoEndpoint(
        val watchEndpoint: WatchEndpoint? = null
    ) {
        @Serializable
        data class WatchEndpoint(
            val videoId: String? = null
        )
    }

    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer? = null,
        val twoColumnWatchNextResults: TwoColumnWatchNextResults? = null
    )

    @Serializable
    data class SingleColumnMusicWatchNextResultsRenderer(
        val tabbedRenderer: TabbedRenderer? = null
    )

    @Serializable
    data class TabbedRenderer(
        val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer? = null
    )

    @Serializable
    data class WatchNextTabbedResultsRenderer(
        val tabs: List<Tab>? = null
    )

    @Serializable
    data class Tab(
        val tabRenderer: TabRenderer? = null
    )

    @Serializable
    data class TabRenderer(
        val content: TabContent? = null
    )

    @Serializable
    data class TabContent(
        val musicQueueRenderer: MusicQueueRenderer? = null
    )

    @Serializable
    data class MusicQueueRenderer(
        val content: MusicQueueContent? = null
    )

    @Serializable
    data class MusicQueueContent(
        val playlistPanelRenderer: PlaylistPanelRenderer? = null
    )

    @Serializable
    data class PlaylistPanelRenderer(
        val title: Title? = null,
        val titleText: TitleText? = null,
        val shortBylineText: ShortBylineText? = null,
        val contents: List<Content>? = null,
        val isInfinite: Boolean? = null,
        val numItemsToShow: Int? = null,
        val playlistId: String? = null,
        val continuations: List<Continuation>? = null
    ) {
        @Serializable
        data class Title(
            val runs: List<Run>? = null
        ) {
            @Serializable
            data class Run(
                val text: String? = null
            )
        }

        @Serializable
        data class TitleText(
            val runs: List<Run>? = null
        ) {
            @Serializable
            data class Run(
                val text: String? = null
            )
        }

        @Serializable
        data class ShortBylineText(
            val runs: List<Run>? = null
        ) {
            @Serializable
            data class Run(
                val text: String? = null,
                val navigationEndpoint: NavigationEndpoint? = null
            )
        }

        @Serializable
        data class Content(
            val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null,
            val automixPreviewVideoRenderer: AutomixPreviewVideoRenderer? = null
        )

        @Serializable
        data class PlaylistPanelVideoRenderer(
            val title: Title? = null,
            val lengthText: LengthText? = null,
            val longBylineText: LongBylineText? = null,
            val shortBylineText: ShortBylineText? = null,
            val badges: List<Badge>? = null,
            val videoId: String? = null,
            val playlistSetVideoId: String? = null,
            val selected: Boolean? = null,
            val thumbnail: Thumbnail? = null,
            val unplayableText: UnplayableText? = null,
            val menu: Menu? = null,
            val navigationEndpoint: NavigationEndpoint? = null
        ) {
            @Serializable
            data class Title(
                val runs: List<Run>? = null
            ) {
                @Serializable
                data class Run(
                    val text: String? = null
                )
            }

            @Serializable
            data class LengthText(
                val simpleText: String? = null
            )

            @Serializable
            data class LongBylineText(
                val runs: List<Run>? = null
            ) {
                @Serializable
                data class Run(
                    val text: String? = null,
                    val navigationEndpoint: NavigationEndpoint? = null
                )
            }

            @Serializable
            data class ShortBylineText(
                val runs: List<Run>? = null
            ) {
                @Serializable
                data class Run(
                    val text: String? = null,
                    val navigationEndpoint: NavigationEndpoint? = null
                )
            }

            @Serializable
            data class Badge(
                val musicInlineBadgeRenderer: MusicInlineBadgeRenderer? = null
            ) {
                @Serializable
                data class MusicInlineBadgeRenderer(
                    val icon: Icon? = null,
                    val tooltip: String? = null
                ) {
                    @Serializable
                    data class Icon(
                        val iconType: String? = null
                    )
                }
            }

            @Serializable
            data class Thumbnail(
                val thumbnails: List<ThumbnailUrl>? = null
            ) {
                @Serializable
                data class ThumbnailUrl(
                    val url: String? = null,
                    val width: Int? = null,
                    val height: Int? = null
                )
            }

            @Serializable
            data class UnplayableText(
                val runs: List<Run>? = null
            ) {
                @Serializable
                data class Run(
                    val text: String? = null
                )
            }

            @Serializable
            data class Menu(
                val menuRenderer: MenuRenderer? = null
            ) {
                @Serializable
                data class MenuRenderer(
                    val items: List<Item>? = null
                ) {
                    @Serializable
                    data class Item(
                        val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null,
                        val menuServiceItemRenderer: MenuServiceItemRenderer? = null
                    )
                }
            }

            @Serializable
            data class MenuNavigationItemRenderer(
                val navigationEndpoint: NavigationEndpoint? = null,
                val text: Text? = null
            ) {
                @Serializable
                data class Text(
                    val simpleText: String? = null
                )
            }

            @Serializable
            data class MenuServiceItemRenderer(
                val serviceEndpoint: ServiceEndpoint? = null,
                val text: Text? = null
            ) {
                @Serializable
                data class Text(
                    val simpleText: String? = null
                )
            }

            @Serializable
            data class ServiceEndpoint(
                val shareEntityEndpoint: ShareEntityEndpoint? = null
            ) {
                @Serializable
                data class ShareEntityEndpoint(
                    val shareEntity: String? = null
                )
            }
        }

        @Serializable
        data class AutomixPreviewVideoRenderer(
            val content: AutomixContent? = null
        ) {
            @Serializable
            data class AutomixContent(
                val playlistPanelRenderer: PlaylistPanelRenderer? = null
            )
        }

        @Serializable
        data class Continuation(
            val continuationEndpoint: ContinuationEndpoint? = null
        )

        @Serializable
        data class ContinuationEndpoint(
            val continuationCommand: ContinuationCommand? = null
        )

        @Serializable
        data class ContinuationCommand(
            val token: String? = null
        )
    }

    @Serializable
    data class NavigationEndpoint(
        val watchEndpoint: WatchEndpoint? = null,
        val shareEntityEndpoint: ShareEntityEndpoint? = null
    ) {
        @Serializable
        data class WatchEndpoint(
            val videoId: String? = null,
            val playlistId: String? = null,
            val index: Int? = null
        )

        @Serializable
        data class ShareEntityEndpoint(
            val shareEntity: String? = null
        )
    }

    @Serializable
    data class TwoColumnWatchNextResults(
        val results: Results? = null
    ) {
        @Serializable
        data class Results(
            val results: ResultsContent? = null
        )

        @Serializable
        data class ResultsContent(
            val contents: List<ResultContent>? = null
        )

        @Serializable
        data class ResultContent(
            val videoPrimaryInfoRenderer: VideoPrimaryInfoRenderer? = null,
            val videoSecondaryInfoRenderer: VideoSecondaryInfoRenderer? = null
        )
    }

    @Serializable
    data class VideoPrimaryInfoRenderer(
        val title: Title? = null,
        val viewCount: ViewCount? = null,
        val dateText: DateText? = null
    ) {
        @Serializable
        data class Title(
            val runs: List<Run>? = null
        ) {
            @Serializable
            data class Run(
                val text: String? = null
            )
        }

        @Serializable
        data class ViewCount(
            val videoViewCountRenderer: VideoViewCountRenderer? = null
        ) {
            @Serializable
            data class VideoViewCountRenderer(
                val viewCount: ViewCountText? = null
            )

            @Serializable
            data class ViewCountText(
                val simpleText: String? = null
            )
        }

        @Serializable
        data class DateText(
            val simpleText: String? = null
        )
    }

    @Serializable
    data class VideoSecondaryInfoRenderer(
        val owner: Owner? = null,
        val description: Description? = null
    ) {
        @Serializable
        data class Owner(
            val videoOwnerRenderer: VideoOwnerRenderer? = null
        )

        @Serializable
        data class VideoOwnerRenderer(
            val title: Title? = null,
            val thumbnail: Thumbnail? = null
        ) {
            @Serializable
            data class Title(
                val runs: List<Run>? = null
            ) {
                @Serializable
                data class Run(
                    val text: String? = null,
                    val navigationEndpoint: NavigationEndpoint? = null
                )
            }

            @Serializable
            data class Thumbnail(
                val thumbnails: List<ThumbnailUrl>? = null
            ) {
                @Serializable
                data class ThumbnailUrl(
                    val url: String? = null,
                    val width: Int? = null,
                    val height: Int? = null
                )
            }
        }

        @Serializable
        data class Description(
            val runs: List<Run>? = null
        ) {
            @Serializable
            data class Run(
                val text: String? = null
            )
        }
    }

    @Serializable
    data class ContinuationContents(
        val playlistPanelContinuation: PlaylistPanelRenderer? = null
    )
}
