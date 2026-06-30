package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.AlbumItem
import com.pryvn.audiophile.code.api.innertube.models.BrowseEndpoint
import kotlinx.serialization.json.*

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenre>,
) {
    data class MoodAndGenre(
        val title: String,
        val items: List<Item>,
    )

    data class Item(
        val title: String,
        val stripeColor: Long,
        val endpoint: BrowseEndpoint,
    )

    companion object {
        fun fromBrowseResponse(json: JsonObject): ExplorePage {
            val contents = json["contents"]?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?: json["contents"]?.jsonObject

            val sections = contents?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray

            val newReleaseAlbums = mutableListOf<AlbumItem>()
            val moodAndGenres = mutableListOf<MoodAndGenre>()

            sections?.forEach { section ->
                val carousel = section.jsonObject["musicCarouselShelfRenderer"]?.jsonObject
                val shelf = section.jsonObject["musicShelfRenderer"]?.jsonObject

                if (carousel != null) {
                    val header = carousel["header"]?.jsonObject
                        ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject ?: return@forEach
                    val title = header["title"]?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull ?: return@forEach

                    val isMoodAndGenre = title.contains("Mood", ignoreCase = true) ||
                            title.contains("Genre", ignoreCase = true)
                    val isNewReleases = title.contains("New Releases", ignoreCase = true) ||
                            title.contains("New release", ignoreCase = true)

                    val carouselContents = carousel["contents"]?.jsonArray ?: return@forEach

                    if (isNewReleases) {
                        carouselContents.forEach { content ->
                            val twoRow = content.jsonObject["musicTwoRowItemRenderer"]?.jsonObject ?: return@forEach
                            parseAlbumItem(twoRow)?.let { newReleaseAlbums.add(it) }
                        }
                    } else if (isMoodAndGenre) {
                        val items = carouselContents.mapNotNull { content ->
                            val twoRow = content.jsonObject["musicTwoRowItemRenderer"]?.jsonObject ?: return@mapNotNull null
                            parseMoodAndGenreItem(twoRow)
                        }
                        moodAndGenres.add(MoodAndGenre(title = title, items = items))
                    }
                }

                if (shelf != null) {
                    val title = shelf["title"]?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull ?: return@forEach

                    val shelfContents = shelf["contents"]?.jsonArray ?: return@forEach
                    val items = shelfContents.mapNotNull { content ->
                        val twoRow = content.jsonObject["musicTwoRowItemRenderer"]?.jsonObject ?: return@mapNotNull null
                        parseMoodAndGenreItem(twoRow)
                    }
                    moodAndGenres.add(MoodAndGenre(title = title, items = items))
                }
            }

            return ExplorePage(newReleaseAlbums = newReleaseAlbums, moodAndGenres = moodAndGenres)
        }

        private fun parseAlbumItem(renderer: JsonObject): AlbumItem? {
            val title = renderer["title"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val navEp = renderer["navigationEndpoint"]?.jsonObject
            val browseId = navEp?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull
            val playlistId = navEp?.get("watchEndpoint")?.jsonObject
                ?.get("playlistId")?.jsonPrimitive?.contentOrNull

            val subtitleRuns = renderer["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray
            val subtitle = subtitleRuns?.joinToString("") {
                it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: ""
            }?.takeIf { it.isNotBlank() }

            val thumbnailUrl = renderer["thumbnailRenderer"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            val hasExplicit = renderer["subtitleBadges"]?.jsonArray?.any { badge ->
                badge.jsonObject["musicInlineBadgeRenderer"]?.jsonObject
                    ?.get("icon")?.jsonObject
                    ?.get("iconType")?.jsonPrimitive?.contentOrNull == "EXPLICIT"
            } ?: false

            return AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = title,
                artists = subtitle,
                thumbnail = thumbnailUrl,
                explicit = hasExplicit,
            )
        }

        private fun parseMoodAndGenreItem(renderer: JsonObject): Item? {
            val title = renderer["title"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val navEp = renderer["navigationEndpoint"]?.jsonObject
            val browseId = navEp?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull
            val params = navEp?.get("browseEndpoint")?.jsonObject
                ?.get("params")?.jsonPrimitive?.contentOrNull

            val stripeColor = renderer["thumbnailOverlay"]?.jsonObject
                ?.get("musicItemThumbnailOverlayRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("musicPlayButtonRenderer")?.jsonObject
                ?.get("icon")?.jsonObject
                ?.get("iconColor")?.jsonPrimitive?.longOrNull ?: 0L

            val thumbnailColor = renderer["thumbnailRenderer"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            val resolvedColor = if (stripeColor == 0L) {
                thumbnailColor?.hashCode()?.toLong() ?: 0L
            } else {
                stripeColor
            }

            return Item(
                title = title,
                stripeColor = resolvedColor,
                endpoint = BrowseEndpoint(browseId = browseId, params = params),
            )
        }
    }
}
