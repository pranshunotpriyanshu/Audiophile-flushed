package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.*
import kotlinx.serialization.json.*

data class ArtistSection(
    val title: String,
    val items: List<YTItem>,
    val moreEndpoint: BrowseEndpoint?,
)

data class ArtistPage(
    val artist: ArtistItem,
    val sections: List<ArtistSection>,
    val description: String?,
) {
    companion object {
        fun fromBrowseResponse(response: JsonObject): ArtistPage? {
            val contents = response["contents"]?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray ?: return null

            val header = contents.firstOrNull()?.jsonObject
                ?.get("musicResponsiveHeaderRenderer")?.jsonObject

            val artist = parseArtistHeader(header) ?: return null

            val description = header?.get("description")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                ?.trim()?.takeIf { it.isNotBlank() }

            val sections = mutableListOf<ArtistSection>()

            for (section in contents) {
                val sectionObj = section.jsonObject

                sectionObj["musicShelfRenderer"]?.jsonObject?.let { shelf ->
                    val parsed = parseMusicShelf(shelf)
                    if (parsed != null) sections.add(parsed)
                }

                sectionObj["musicCarouselShelfRenderer"]?.jsonObject?.let { carousel ->
                    val parsed = parseMusicCarousel(carousel)
                    if (parsed != null) sections.add(parsed)
                }
            }

            return ArtistPage(
                artist = artist,
                sections = sections,
                description = description,
            )
        }

        private fun parseArtistHeader(header: JsonObject?): ArtistItem? {
            if (header == null) return null

            val title = header["title"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull

            val thumbnail = header["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            val subtitleRuns = header["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray

            var subscriberCountText: String? = null
            subtitleRuns?.forEach { run ->
                val text = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                if (text.contains("subscriber", ignoreCase = true) || text.contains("subscriber", ignoreCase = true)) {
                    subscriberCountText = text
                }
            }

            val buttons = header["buttons"]?.jsonArray
            var playEndpoint: WatchEndpoint? = null
            var shuffleEndpoint: WatchEndpoint? = null
            var radioEndpoint: WatchEndpoint? = null

            buttons?.forEach { button ->
                val btnRenderer = button.jsonObject["buttonRenderer"]?.jsonObject ?: return@forEach
                val navEp = btnRenderer["navigationEndpoint"]?.jsonObject ?: return@forEach
                val watchEp = navEp["watchEndpoint"]?.jsonObject ?: return@forEach
                val videoId = watchEp["videoId"]?.jsonPrimitive?.contentOrNull
                val playlistId = watchEp["playlistId"]?.jsonPrimitive?.contentOrNull
                val watchEndpoint = WatchEndpoint(videoId = videoId, playlistId = playlistId)

                val iconType = btnRenderer["icon"]?.jsonObject
                    ?.get("iconType")?.jsonPrimitive?.contentOrNull

                when {
                    iconType == "MUSIC_SHUFFLE" -> shuffleEndpoint = watchEndpoint
                    iconType == "MUSIC_PLAY_ARROW" -> playEndpoint = watchEndpoint
                    iconType == "MIX" -> radioEndpoint = watchEndpoint
                    navEp["watchPlaylistEndpoint"] != null -> {
                        val wpEp = navEp["watchPlaylistEndpoint"]?.jsonObject
                        val wpPlaylistId = wpEp?.get("playlistId")?.jsonPrimitive?.contentOrNull
                        radioEndpoint = WatchEndpoint(playlistId = wpPlaylistId)
                    }
                }
            }

            val browseId = header["menu"]?.jsonObject
                ?.get("menuRenderer")?.jsonObject
                ?.get("topLevelButtons")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("buttonRenderer")?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull

            val channelId = browseId?.takeIf { it.startsWith("UC") }

            return ArtistItem(
                id = channelId,
                title = title,
                thumbnail = thumbnail,
                channelId = channelId,
                playEndpoint = playEndpoint,
                shuffleEndpoint = shuffleEndpoint,
                radioEndpoint = radioEndpoint,
                subscriberCountText = subscriberCountText,
            )
        }

        private fun parseMusicShelf(shelf: JsonObject): ArtistSection? {
            val title = shelf["title"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val contents = shelf["contents"]?.jsonArray ?: return null
            val items = contents.mapNotNull { item ->
                val renderer = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                    ?: return@mapNotNull null
                parseItemFromListItem(renderer)
            }

            val moreEndpoint = shelf["bottomEndpoint"]?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.let {
                    BrowseEndpoint(
                        browseId = it["browseId"]?.jsonPrimitive?.contentOrNull,
                        params = it["params"]?.jsonPrimitive?.contentOrNull,
                    )
                }

            return ArtistSection(
                title = title,
                items = items,
                moreEndpoint = moreEndpoint,
            )
        }

        private fun parseMusicCarousel(carousel: JsonObject): ArtistSection? {
            val header = carousel["header"]?.jsonObject
                ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                ?: carousel["header"]?.jsonObject
                    ?.get("musicCarouselShelfHeaderRenderer")?.jsonObject
            val title = header?.get("title")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val contents = carousel["contents"]?.jsonArray ?: return null
            val items = contents.mapNotNull { item ->
                val twoRow = item.jsonObject["musicTwoRowItemRenderer"]?.jsonObject
                if (twoRow != null) {
                    parseItemFromTwoRow(twoRow)
                } else {
                    val listItem = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                    if (listItem != null) {
                        parseItemFromListItem(listItem)
                    } else {
                        null
                    }
                }
            }

            val moreEndpoint = carousel["header"]?.jsonObject
                ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                ?.get("moreContentButton")?.jsonObject
                ?.get("buttonRenderer")?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.let {
                    BrowseEndpoint(
                        browseId = it["browseId"]?.jsonPrimitive?.contentOrNull,
                        params = it["params"]?.jsonPrimitive?.contentOrNull,
                    )
                }

            return ArtistSection(
                title = title,
                items = items,
                moreEndpoint = moreEndpoint,
            )
        }

        private fun parseItemFromTwoRow(renderer: JsonObject): YTItem? {
            val title = renderer["title"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val navEp = renderer["navigationEndpoint"]?.jsonObject
            val browseId = navEp?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull
            val videoId = navEp?.get("watchEndpoint")?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull

            val thumbnail = renderer["thumbnailRenderer"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            val subtitleRuns = renderer["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray
            val subtitleText = subtitleRuns?.joinToString("") {
                it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: ""
            }?.trim()

            val isExplicit = renderer["subtitleBadges"]?.jsonArray?.any { badge ->
                badge.jsonObject["musicInlineBadgeRenderer"]?.jsonObject
                    ?.get("icon")?.jsonObject
                    ?.get("iconType")?.jsonPrimitive?.contentOrNull == "MUSIC_EXPLICIT_BADGE"
            } == true

            if (browseId?.startsWith("UC") == true) {
                return YTItem(
                    id = browseId,
                    title = title,
                    type = YTItem.Type.ARTIST,
                    artists = listOf(YTItem.Artist(name = title, id = browseId)),
                    thumbnailUrl = thumbnail,
                    browseId = browseId,
                )
            }

            if (browseId?.startsWith("MPRE") == true || browseId?.startsWith("OLAK") == true) {
                return YTItem(
                    id = browseId,
                    title = title,
                    type = YTItem.Type.ALBUM,
                    thumbnailUrl = thumbnail,
                    browseId = browseId,
                )
            }

            if (videoId != null) {
                return YTItem(
                    id = videoId,
                    title = title,
                    type = YTItem.Type.SONG,
                    thumbnailUrl = thumbnail,
                    playlistId = navEp?.get("watchEndpoint")?.jsonObject
                        ?.get("playlistId")?.jsonPrimitive?.contentOrNull,
                )
            }

            return null
        }

        private fun parseItemFromListItem(renderer: JsonObject): YTItem? {
            val videoId = renderer["playlistItemData"]?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: renderer["navigationEndpoint"]?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull
                    ?: return null

            val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null

            val title = flexColumns.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                ?.trim() ?: return null

            val subtitleRuns = flexColumns.getOrNull(1)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray

            val artists = mutableListOf<YTItem.Artist>()
            subtitleRuns?.forEach { run ->
                val text = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val navEp = run.jsonObject["navigationEndpoint"]?.jsonObject
                val browseId = navEp?.get("browseEndpoint")?.jsonObject
                    ?.get("browseId")?.jsonPrimitive?.contentOrNull
                if (browseId?.startsWith("UC") == true) {
                    artists.add(YTItem.Artist(name = text, id = browseId))
                }
            }

            val thumbnailUrl = renderer["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            val playlistId = renderer["navigationEndpoint"]?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("playlistId")?.jsonPrimitive?.contentOrNull

            return YTItem(
                id = videoId,
                title = title,
                type = YTItem.Type.SONG,
                artists = artists,
                thumbnailUrl = thumbnailUrl,
                playlistId = playlistId,
            )
        }
    }
}
