package com.pryvn.audiophile.code.api.innertube.models.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SearchResponse(
    val contents: SearchContents? = null,
    val continuationContents: ContinuationContents? = null,
) {
    @Serializable
    data class SearchContents(
        val tabbedSearchResultsRenderer: TabbedSearchResultsRenderer? = null,
    ) {
        @Serializable
        data class TabbedSearchResultsRenderer(
            val tabs: List<Tab>? = null,
        ) {
            @Serializable
            data class Tab(
                val tabRenderer: TabRenderer? = null,
            ) {
                @Serializable
                data class TabRenderer(
                    val content: Content? = null,
                ) {
                    @Serializable
                    data class Content(
                        val sectionListRenderer: SectionListRenderer? = null,
                    ) {
                        @Serializable
                        data class SectionListRenderer(
                            val contents: List<SectionContent>? = null,
                        ) {
                            @Serializable
                            data class SectionContent(
                                val musicShelfRenderer: MusicShelfRenderer? = null,
                                val musicCardShelfRenderer: MusicCardShelfRenderer? = null,
                                val itemSectionRenderer: ItemSectionRenderer? = null,
                            ) {
                                @Serializable
                                data class MusicShelfRenderer(
                                    val title: Title? = null,
                                    val contents: List<ShelfItem>? = null,
                                    val continuations: List<JsonElement>? = null,
                                ) {
                                    @Serializable
                                    data class Title(
                                        val runs: List<Run>? = null,
                                    )

                                    @Serializable
                                    data class ShelfItem(
                                        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
                                    )
                                }

                                @Serializable
                                data class MusicCardShelfRenderer(
                                    val title: Title? = null,
                                    val contents: List<ShelfItem>? = null,
                                    val subtitle: Subtitle? = null,
                                    val thumbnail: Thumbnail? = null,
                                ) {
                                    @Serializable
                                    data class Title(
                                        val runs: List<Run>? = null,
                                    )

                                    @Serializable
                                    data class Subtitle(
                                        val runs: List<Run>? = null,
                                    )

                                    @Serializable
                                    data class Thumbnail(
                                        val musicThumbnailRenderer: MusicThumbnailRenderer? = null,
                                    ) {
                                        @Serializable
                                        data class MusicThumbnailRenderer(
                                            val thumbnail: ThumbnailData? = null,
                                        ) {
                                            @Serializable
                                            data class ThumbnailData(
                                                val thumbnails: List<ThumbnailItem>? = null,
                                            ) {
                                                @Serializable
                                                data class ThumbnailItem(
                                                    val url: String? = null,
                                                )
                                            }
                                        }
                                    }

                                    @Serializable
                                    data class ShelfItem(
                                        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
                                    )
                                }

                                @Serializable
                                data class ItemSectionRenderer(
                                    val contents: List<ItemSectionContent>? = null,
                                ) {
                                    @Serializable
                                    data class ItemSectionContent(
                                        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
                                        val musicShelfRenderer: MusicShelfRenderer? = null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Serializable
    data class ContinuationContents(
        val musicShelfContinuation: MusicShelfContinuation? = null,
    ) {
        @Serializable
        data class MusicShelfContinuation(
            val contents: List<ContinuationItem>? = null,
            val continuations: List<JsonElement>? = null,
        ) {
            @Serializable
            data class ContinuationItem(
                val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
                val continuationItemRenderer: ContinuationItemRenderer? = null,
            ) {
                @Serializable
                data class ContinuationItemRenderer(
                    val continuationEndpoint: ContinuationEndpoint? = null,
                ) {
                    @Serializable
                    data class ContinuationEndpoint(
                        val continuationCommand: ContinuationCommand? = null,
                    ) {
                        @Serializable
                        data class ContinuationCommand(
                            val token: String? = null,
                        )
                    }
                }
            }
        }
    }

    @Serializable
    data class MusicResponsiveListItemRenderer(
        val flexColumns: List<FlexColumn>? = null,
        val fixedColumns: List<FixedColumn>? = null,
        val thumbnail: ThumbnailData? = null,
        val overlay: Overlay? = null,
        val navigationEndpoint: NavigationEndpoint? = null,
        val playlistItemData: PlaylistItemData? = null,
        val menu: Menu? = null,
    ) {
        @Serializable
        data class FlexColumn(
            val musicResponsiveListItemFlexColumnRenderer: FlexColumnRenderer? = null,
        ) {
            @Serializable
            data class FlexColumnRenderer(
                val text: Text? = null,
            ) {
                @Serializable
                data class Text(
                    val runs: List<Run>? = null,
                )
            }
        }

        @Serializable
        data class FixedColumn(
            val musicResponsiveListItemFixedColumnRenderer: FixedColumnRenderer? = null,
        ) {
            @Serializable
            data class FixedColumnRenderer(
                val text: Text? = null,
            ) {
                @Serializable
                data class Text(
                    val runs: List<Run>? = null,
                )
            }
        }

        @Serializable
        data class ThumbnailData(
            val musicThumbnailRenderer: MusicThumbnailRenderer? = null,
        ) {
            @Serializable
            data class MusicThumbnailRenderer(
                val thumbnail: ThumbnailDetails? = null,
            ) {
                @Serializable
                data class ThumbnailDetails(
                    val thumbnails: List<ThumbnailItem>? = null,
                ) {
                    @Serializable
                    data class ThumbnailItem(
                        val url: String? = null,
                    )
                }
            }
        }

        @Serializable
        data class Overlay(
            val musicItemThumbnailOverlayRenderer: OverlayRenderer? = null,
        ) {
            @Serializable
            data class OverlayRenderer(
                val content: OverlayContent? = null,
            ) {
                @Serializable
                data class OverlayContent(
                    val musicPlayButtonRenderer: PlayButtonRenderer? = null,
                ) {
                    @Serializable
                    data class PlayButtonRenderer(
                        val playNavigationEndpoint: PlayNavEndpoint? = null,
                    ) {
                        @Serializable
                        data class PlayNavEndpoint(
                            val watchEndpoint: WatchEndpoint? = null,
                        ) {
                            @Serializable
                            data class WatchEndpoint(
                                val videoId: String? = null,
                                val playlistId: String? = null,
                            )
                        }
                    }
                }
            }
        }

        @Serializable
        data class NavigationEndpoint(
            val watchEndpoint: WatchEndpoint? = null,
            val browseEndpoint: BrowseEndpoint? = null,
            val watchPlaylistEndpoint: WatchPlaylistEndpoint? = null,
        ) {
            @Serializable
            data class WatchEndpoint(
                val videoId: String? = null,
                val playlistId: String? = null,
            )

            @Serializable
            data class BrowseEndpoint(
                val browseId: String? = null,
            )

            @Serializable
            data class WatchPlaylistEndpoint(
                val videoId: String? = null,
                val playlistId: String? = null,
            )
        }

        @Serializable
        data class PlaylistItemData(
            val videoId: String? = null,
        )

        @Serializable
        data class Menu(
            val menuRenderer: MenuRenderer? = null,
        ) {
            @Serializable
            data class MenuRenderer(
                val items: List<MenuItem>? = null,
            ) {
                @Serializable
                data class MenuItem(
                    val menuNavigationItemRenderer: MenuNavItemRenderer? = null,
                ) {
                    @Serializable
                    data class MenuNavItemRenderer(
                        val icon: Icon? = null,
                        val navigationEndpoint: NavEndpoint? = null,
                    ) {
                        @Serializable
                        data class Icon(
                            val iconType: String? = null,
                        )

                        @Serializable
                        data class NavEndpoint(
                            val watchPlaylistEndpoint: WatchPlaylistEndpoint? = null,
                        ) {
                            @Serializable
                            data class WatchPlaylistEndpoint(
                                val videoId: String? = null,
                                val playlistId: String? = null,
                            )
                        }
                    }
                }
            }
        }
    }

    @Serializable
    data class Run(
        val text: String? = null,
        val navigationEndpoint: NavEndpoint? = null,
    ) {
        @Serializable
        data class NavEndpoint(
            val browseEndpoint: BrowseEndpoint? = null,
            val watchEndpoint: WatchEndpoint? = null,
        ) {
            @Serializable
            data class BrowseEndpoint(
                val browseId: String? = null,
            )

            @Serializable
            data class WatchEndpoint(
                val videoId: String? = null,
            )
        }
    }
}
