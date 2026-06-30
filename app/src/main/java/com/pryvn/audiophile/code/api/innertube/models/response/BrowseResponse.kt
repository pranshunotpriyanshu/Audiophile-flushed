package com.pryvn.audiophile.code.api.innertube.models.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BrowseResponse(
    val contents: Contents? = null,
    val header: Header? = null,
    val continuationContents: ContinuationContents? = null,
    val onResponseReceivedActions: List<JsonElement>? = null,
) {
    @Serializable
    data class Contents(
    val singleColumnBrowseResultsRenderer: SingleColumnRenderer? = null,
    val twoColumnBrowseResultsRenderer: TwoColumnRenderer? = null,
    ) {
        @Serializable
        data class SingleColumnRenderer(
            val tabs: List<Tab>? = null,
        ) {
            @Serializable
            data class Tab(
                val tabRenderer: TabRenderer? = null,
            ) {
                @Serializable
                data class TabRenderer(
                    val content: TabContent? = null,
                ) {
                    @Serializable
                    data class TabContent(
                        val sectionListRenderer: SectionListRenderer? = null,
                    ) {
                        @Serializable
                        data class SectionListRenderer(
                            val contents: List<SectionContent>? = null,
                            val continuations: List<JsonElement>? = null,
                            val header: SectionListHeader? = null,
                        ) {
                            @Serializable
                            data class SectionContent(
                                val musicShelfRenderer: MusicShelfRenderer? = null,
                                val musicCarouselShelfRenderer: MusicCarouselShelfRenderer? = null,
                                val musicResponsiveHeaderRenderer: JsonElement? = null,
                                val musicEditablePlaylistDetailHeaderRenderer: JsonElement? = null,
                                val musicDescriptionShelfRenderer: JsonElement? = null,
                                val itemSectionRenderer: JsonElement? = null,
                                val gridRenderer: JsonElement? = null,
                                val musicPlaylistShelfRenderer: JsonElement? = null,
                            ) {
                                @Serializable
                                data class MusicShelfRenderer(
                                    val header: ShelfHeader? = null,
                                    val contents: List<ShelfContent>? = null,
                                    val continuations: List<JsonElement>? = null,
                                    val bottomStatus: JsonElement? = null,
                                    val title: ShelfTitle? = null,
                                ) {
                                    @Serializable
                                    data class ShelfHeader(
                                        val musicShelfBasicHeaderRenderer: BasicHeaderRenderer? = null,
                                    ) {
                                        @Serializable
                                        data class BasicHeaderRenderer(
                                            val title: Title? = null,
                                        ) {
                                            @Serializable
                                            data class Title(
                                                val runs: List<Run>? = null,
                                            )
                                        }
                                    }

                                    @Serializable
                                    data class ShelfTitle(
                                        val runs: List<Run>? = null,
                                    )

                                    @Serializable
                                    data class ShelfContent(
                                        val musicResponsiveListItemRenderer: JsonElement? = null,
                                    )
                                }

                                @Serializable
                                data class MusicCarouselShelfRenderer(
                                    val header: CarouselHeader? = null,
                                    val contents: List<CarouselContent>? = null,
                                ) {
                                    @Serializable
                                    data class CarouselHeader(
                                        val musicCarouselShelfBasicHeaderRenderer: BasicHeaderRenderer? = null,
                                    ) {
                                        @Serializable
                                        data class BasicHeaderRenderer(
                                            val title: Title? = null,
                                            val moreContentButton: JsonElement? = null,
                                        ) {
                                            @Serializable
                                            data class Title(
                                                val runs: List<Run>? = null,
                                            )
                                        }
                                    }

                                    @Serializable
                                    data class CarouselContent(
                                        val musicTwoRowItemRenderer: JsonElement? = null,
                                        val musicNavigationButtonRenderer: JsonElement? = null,
                                    )
                                }
                            }

                            @Serializable
                            data class SectionListHeader(
                                val chipCloudRenderer: ChipCloudRenderer? = null,
                            ) {
                                @Serializable
                                data class ChipCloudRenderer(
                                    val chips: List<Chip>? = null,
                                ) {
                                    @Serializable
                                    data class Chip(
                                        val chipCloudChipRenderer: ChipRenderer? = null,
                                    ) {
                                        @Serializable
                                        data class ChipRenderer(
                                            val text: Text? = null,
                                            val navigationEndpoint: NavEndpoint? = null,
                                        ) {
                                            @Serializable
                                            data class Text(
                                                val runs: List<Run>? = null,
                                            )

                                            @Serializable
                                            data class NavEndpoint(
                                                val browseEndpoint: BrowseEndpoint? = null,
                                            ) {
                                                @Serializable
                                                data class BrowseEndpoint(
                                                    val browseId: String? = null,
                                                    val params: String? = null,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @Serializable
        data class TwoColumnRenderer(
            val tabs: List<Tab>? = null,
            val secondaryContents: SecondaryContents? = null,
        ) {
            @Serializable
            data class Tab(
                val tabRenderer: TabRenderer? = null,
            ) {
                @Serializable
                data class TabRenderer(
                    val content: TabContent? = null,
                ) {
                    @Serializable
                    data class TabContent(
                        val sectionListRenderer: SectionListRenderer? = null,
                    ) {
                        @Serializable
                        data class SectionListRenderer(
                            val contents: List<SectionContent>? = null,
                            val continuations: List<JsonElement>? = null,
                        ) {
                            @Serializable
                            data class SectionContent(
                                val musicResponsiveHeaderRenderer: JsonElement? = null,
                                val musicEditablePlaylistDetailHeaderRenderer: JsonElement? = null,
                                val musicPlaylistShelfRenderer: JsonElement? = null,
                                val musicCarouselShelfRenderer: JsonElement? = null,
                                val musicDescriptionShelfRenderer: JsonElement? = null,
                                val itemSectionRenderer: JsonElement? = null,
                            )
                        }
                    }
                }
            }

            @Serializable
            data class SecondaryContents(
                val sectionListRenderer: SectionListRenderer? = null,
            ) {
                @Serializable
                data class SectionListRenderer(
                    val contents: List<SectionContent>? = null,
                    val continuations: List<JsonElement>? = null,
                ) {
                    @Serializable
                    data class SectionContent(
                        val musicPlaylistShelfRenderer: JsonElement? = null,
                        val musicResponsiveListItemRenderer: JsonElement? = null,
                        val musicCarouselShelfRenderer: JsonElement? = null,
                    )
                }
            }
        }
    }

    @Serializable
    data class Header(
        val musicImmersiveHeaderRenderer: JsonElement? = null,
        val musicVisualHeaderRenderer: JsonElement? = null,
        val musicHeaderRenderer: JsonElement? = null,
        val musicDetailHeaderRenderer: JsonElement? = null,
        val musicResponsiveHeaderRenderer: JsonElement? = null,
    )

    @Serializable
    data class ContinuationContents(
        val sectionListContinuation: SectionListContinuation? = null,
        val musicShelfContinuation: JsonElement? = null,
        val gridContinuation: JsonElement? = null,
        val musicPlaylistShelfContinuation: JsonElement? = null,
    ) {
        @Serializable
        data class SectionListContinuation(
            val contents: List<SectionContent>? = null,
            val continuations: List<JsonElement>? = null,
        ) {
            @Serializable
            data class SectionContent(
                val musicCarouselShelfRenderer: JsonElement? = null,
            )
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
        ) {
            @Serializable
            data class BrowseEndpoint(
                val browseId: String? = null,
                val signatureTimestamp: String? = null,
            )
        }
    }
}
