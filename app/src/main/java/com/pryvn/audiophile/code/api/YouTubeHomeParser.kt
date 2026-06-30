package com.pryvn.audiophile.code.api

import kotlinx.serialization.json.*

internal object YouTubeHomeParser {

    fun parseHomeSections(root: JsonObject): List<HomeSection> {
        val sections = mutableListOf<HomeSection>()
        val contents = root["contents"]?.jsonObject
            ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
            ?.get("tabs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("tabRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("sectionListRenderer")?.jsonObject
            ?.get("contents")?.jsonArray
            ?: root["contents"]?.jsonObject
                ?.get("twoColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray
            ?: return sections

        for (sectionIdx in 0 until (contents?.size ?: 0)) {
            val section = contents?.get(sectionIdx)?.jsonObject ?: continue
            val sectionItems = mutableListOf<HomeItem>()

            val carousel = section["musicCarouselShelfRenderer"]?.jsonObject
            if (carousel != null) {
                val title = carousel["header"]?.jsonObject
                    ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                    ?.get("title")?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content ?: continue

                val carouselContents = carousel["contents"]?.jsonArray
                if (carouselContents != null) {
                    for (ci in 0 until carouselContents.size) {
                        val item = carouselContents[ci].jsonObject
                        val twoRow = item["musicTwoRowItemRenderer"]?.jsonObject
                        if (twoRow != null) {
                            val itemTitle = twoRow["title"]?.jsonObject
                                ?.get("runs")?.jsonArray
                                ?.firstOrNull()?.jsonObject
                                ?.get("text")?.jsonPrimitive?.content ?: continue
                            val thumb = twoRow["thumbnailRenderer"]?.jsonObject
                                ?.get("musicThumbnailRenderer")?.jsonObject
                                ?.get("thumbnail")?.jsonObject
                                ?.get("thumbnails")?.jsonArray
                                ?.lastOrNull()?.jsonObject
                                ?.get("url")?.jsonPrimitive?.contentOrNull
                            val navEp = twoRow["navigationEndpoint"]?.jsonObject
                            val browseId = navEp?.get("browseEndpoint")?.jsonObject
                                ?.get("browseId")?.jsonPrimitive?.contentOrNull
                            val watchId = navEp?.get("watchEndpoint")?.jsonObject
                                ?.get("videoId")?.jsonPrimitive?.contentOrNull

                            val subtitleRuns = twoRow["subtitle"]?.jsonObject
                                ?.get("runs")?.jsonArray
                            val artists = mutableListOf<YTArtist>()
                            subtitleRuns?.forEach { run ->
                                val text = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                                val navId = run.jsonObject["navigationEndpoint"]?.jsonObject
                                    ?.get("browseEndpoint")?.jsonObject
                                    ?.get("browseId")?.jsonPrimitive?.contentOrNull
                                if (navId?.startsWith("UC") == true) {
                                    artists.add(YTArtist(text, navId))
                                }
                            }

                            sectionItems.add(
                                HomeItem(
                                    videoId = watchId,
                                    title = itemTitle,
                                    artists = artists,
                                    thumbnailUrl = thumb,
                                    browseId = browseId,
                                )
                            )
                        }
                    }
                }
                if (sectionItems.isNotEmpty()) {
                    sections.add(HomeSection(title, sectionItems))
                }
                continue
            }

            val musicShelf = section["musicShelfRenderer"]?.jsonObject
            if (musicShelf != null) {
                val title = musicShelf["header"]?.jsonObject
                    ?.get("musicShelfBasicHeaderRenderer")?.jsonObject
                    ?.get("title")?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content

                val shelfContents = musicShelf["contents"]?.jsonArray
                if (shelfContents != null) {
                    for (sci in 0 until shelfContents.size) {
                        val renderer = shelfContents[sci].jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: continue
                        YouTubeSearchParser.parseListItem(renderer)?.let { song ->
                            sectionItems.add(
                                HomeItem(
                                    videoId = song.videoId,
                                    title = song.title,
                                    artists = song.artists,
                                    album = song.album,
                                    thumbnailUrl = song.thumbnailUrl,
                                    durationSeconds = song.durationSeconds,
                                    playlistId = song.playlistId,
                                )
                            )
                        }
                    }
                }
                if (sectionItems.isNotEmpty()) {
                    sections.add(HomeSection(title ?: "Songs", sectionItems))
                }
                continue
            }

            val itemSection = section["itemSectionRenderer"]?.jsonObject
            if (itemSection != null) {
                val secContents = itemSection["contents"]?.jsonArray
                if (secContents != null) {
                    for (si in 0 until secContents.size) {
                        val subShelf = secContents[si].jsonObject["musicShelfRenderer"]?.jsonObject ?: continue
                        val subTitle = subShelf["header"]?.jsonObject
                            ?.get("musicShelfBasicHeaderRenderer")?.jsonObject
                            ?.get("title")?.jsonObject
                            ?.get("runs")?.jsonArray
                            ?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.content
                        val subContents = subShelf["contents"]?.jsonArray ?: continue
                        for (sci in 0 until subContents.size) {
                            val renderer = subContents[sci].jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: continue
                            YouTubeSearchParser.parseListItem(renderer)?.let { song ->
                                sectionItems.add(
                                    HomeItem(
                                        videoId = song.videoId,
                                        title = song.title,
                                        artists = song.artists,
                                        album = song.album,
                                        thumbnailUrl = song.thumbnailUrl,
                                        durationSeconds = song.durationSeconds,
                                        playlistId = song.playlistId,
                                    )
                                )
                            }
                        }
                        if (sectionItems.isNotEmpty()) {
                            sections.add(HomeSection(subTitle ?: "Songs", sectionItems))
                            sectionItems.clear()
                        }
                    }
                }
            }
        }
        return sections
    }
}