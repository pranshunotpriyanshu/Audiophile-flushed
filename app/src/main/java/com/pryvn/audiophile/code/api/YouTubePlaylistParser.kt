package com.pryvn.audiophile.code.api

import kotlinx.serialization.json.*

internal object YouTubePlaylistParser {

    fun parsePlaylistPage(root: JsonObject, playlistId: String): YTPlaylistPage {
        val contents = root["contents"]?.jsonObject
            ?.get("twoColumnBrowseResultsRenderer")?.jsonObject
            ?.get("tabs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("tabRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("sectionListRenderer")?.jsonObject
            ?.get("contents")?.jsonArray
            ?: error("Cannot parse playlist page")

        val firstColumnContents = contents.toList()
        val headerContent = firstColumnContents.firstOrNull { content ->
            content.jsonObject["musicResponsiveHeaderRenderer"] != null ||
                    content.jsonObject["musicEditablePlaylistDetailHeaderRenderer"] != null
        }?.jsonObject

        val header = headerContent?.get("musicResponsiveHeaderRenderer")?.jsonObject
            ?: headerContent?.get("musicEditablePlaylistDetailHeaderRenderer")?.jsonObject
                ?.get("header")?.jsonObject?.get("musicResponsiveHeaderRenderer")?.jsonObject
            ?: error("Cannot parse playlist header")

        val title = header["title"]?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            ?: error("Missing playlist title")

        val thumbnail = header["thumbnail"]?.jsonObject
            ?.get("musicThumbnailRenderer")?.jsonObject
            ?.get("thumbnail")?.jsonObject
            ?.get("thumbnails")?.jsonArray
            ?.lastOrNull()?.jsonObject
            ?.get("url")?.jsonPrimitive?.contentOrNull

        val subtitleRuns = header["secondSubtitle"]?.jsonObject
            ?.get("runs")?.jsonArray
        val songCount = subtitleRuns?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull
            ?.let { parseSongCount(it) }

        val author = header["straplineTextOne"]?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull

        val secondaryContents = root["contents"]?.jsonObject
            ?.get("twoColumnBrowseResultsRenderer")?.jsonObject
            ?.get("secondaryContents")?.jsonObject
            ?.get("sectionListRenderer")?.jsonObject
            ?.get("contents")?.jsonArray

        val songs = mutableListOf<YTSongItem>()
        var continuation: String? = null

        val songSections = (secondaryContents?.toList() ?: emptyList()) + firstColumnContents
        for (section in songSections) {
            val playlistRenderer = section.jsonObject["musicPlaylistShelfRenderer"]?.jsonObject
            if (playlistRenderer != null) {
                val items = playlistRenderer["contents"]?.jsonArray ?: continue
                for (item in items) {
                    val renderer = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: continue
                    YouTubeSearchParser.parseListItem(renderer)?.let { songs.add(it) }
                }
                val cont = playlistRenderer["continuations"]?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject
                    ?.get("continuation")?.jsonPrimitive?.content
                if (cont != null) continuation = cont
            }
        }

        return YTPlaylistPage(
            playlist = YTPlaylist(
                id = playlistId,
                title = title,
                thumbnailUrl = thumbnail,
                songCount = songCount,
                author = author,
            ),
            songs = songs,
            continuation = continuation,
        )
    }

    fun parseLibraryPlaylists(root: JsonObject): List<YTPlaylist> {
        val result = mutableListOf<YTPlaylist>()
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
            ?: return result

        for (sectionIdx in 0 until (contents?.size ?: 0)) {
            val section = contents?.get(sectionIdx)?.jsonObject ?: continue

            val itemSection = section["itemSectionRenderer"]?.jsonObject
            if (itemSection != null) {
                val secContents = itemSection["contents"]?.jsonArray ?: continue
                for (si in 0 until secContents.size) {
                    val shelf = secContents[si].jsonObject["musicShelfRenderer"]?.jsonObject ?: continue
                    val shelfContents = shelf["contents"]?.jsonArray ?: continue
                    for (sci in 0 until shelfContents.size) {
                        val renderer = shelfContents[sci].jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: continue
                        val flexColumns = renderer["flexColumns"]?.jsonArray ?: continue
                        val title = flexColumns[0].jsonObject
                            ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                            ?.get("text")?.jsonObject
                            ?.get("runs")?.jsonArray
                            ?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.content ?: continue
                        val playlistId = renderer["navigationEndpoint"]?.jsonObject
                            ?.get("browseEndpoint")?.jsonObject
                            ?.get("browseId")?.jsonPrimitive?.content
                            ?.removePrefix("VL") ?: continue

                        val thumb = renderer["thumbnail"]?.jsonObject
                            ?.get("musicThumbnailRenderer")?.jsonObject
                            ?.get("thumbnail")?.jsonObject
                            ?.get("thumbnails")?.jsonArray
                            ?.lastOrNull()?.jsonObject
                            ?.get("url")?.jsonPrimitive?.contentOrNull

                        val subtitleRuns = flexColumns.getOrNull(1)?.jsonObject
                            ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                            ?.get("text")?.jsonObject
                            ?.get("runs")?.jsonArray

                        var author: String? = null
                        var songCount: Int? = null
                        subtitleRuns?.forEach { run ->
                            val text = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                            if (text.contains("song", ignoreCase = true) || text.contains("track", ignoreCase = true)) {
                                val digits = text.replace(Regex("[^0-9]"), "")
                                songCount = digits.toIntOrNull()
                            } else if (run.jsonObject["navigationEndpoint"] == null && text.isNotBlank()) {
                                if (author == null) author = text
                            }
                        }

                        result.add(
                            YTPlaylist(
                                id = playlistId,
                                title = title,
                                thumbnailUrl = thumb,
                                songCount = songCount,
                                author = author,
                            )
                        )
                    }
                }
            }

            val gridRenderer = section["musicGridRenderer"]?.jsonObject
            if (gridRenderer != null) {
                val gridContents = gridRenderer["items"]?.jsonArray ?: continue
                for (gi in 0 until gridContents.size) {
                    val renderer = gridContents[gi].jsonObject["musicTwoRowItemRenderer"]?.jsonObject ?: continue
                    val title = renderer["title"]?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content ?: continue
                    val playlistId = renderer["navigationEndpoint"]?.jsonObject
                        ?.get("browseEndpoint")?.jsonObject
                        ?.get("browseId")?.jsonPrimitive?.content
                        ?.removePrefix("VL") ?: continue
                    val thumb = renderer["thumbnailRenderer"]?.jsonObject
                        ?.get("musicThumbnailRenderer")?.jsonObject
                        ?.get("thumbnail")?.jsonObject
                        ?.get("thumbnails")?.jsonArray
                        ?.lastOrNull()?.jsonObject
                        ?.get("url")?.jsonPrimitive?.contentOrNull
                    val subRuns = renderer["subtitle"]?.jsonObject?.get("runs")?.jsonArray
                    var songCount: Int? = null
                    subRuns?.forEach { run ->
                        val text = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                        val digits = text.replace(Regex("[^0-9]"), "")
                        songCount = digits.toIntOrNull()
                    }
                    result.add(
                        YTPlaylist(
                            id = playlistId,
                            title = title,
                            thumbnailUrl = thumb,
                            songCount = songCount,
                        )
                    )
                }
            }
        }
        return result.distinctBy { it.id }
    }

    private fun parseSongCount(text: String): Int? {
        val digits = text.replace(Regex("[^0-9]"), "")
        return digits.toIntOrNull()
    }
}