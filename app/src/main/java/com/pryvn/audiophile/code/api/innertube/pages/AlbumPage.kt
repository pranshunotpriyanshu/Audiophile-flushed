package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.*
import kotlinx.serialization.json.*

data class AlbumPage(
    val album: AlbumItem,
    val songs: List<SongItem>,
    val otherVersions: List<AlbumItem>,
) {
    companion object {
        fun fromBrowseResponse(response: JsonObject): AlbumPage? {
            val contents = response["contents"]?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray ?: return null

            val header = contents.firstOrNull()?.jsonObject
                ?.get("musicResponsiveHeaderRenderer")?.jsonObject ?: return null

            val title = getTitle(header) ?: return null
            val artists = getArtists(header)
            val year = getYear(header)
            val thumbnail = getThumbnail(header)
            val playlistId = getPlaylistId(header)

            val browseId = header["menu"]?.jsonObject
                ?.get("menuRenderer")?.jsonObject
                ?.get("topLevelButtons")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("buttonRenderer")?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull

            val album = AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = title,
                artists = artists,
                year = year,
                thumbnail = thumbnail,
            )

            val songs = mutableListOf<SongItem>()
            val otherVersions = mutableListOf<AlbumItem>()

            for (section in contents) {
                val sectionObj = section.jsonObject

                sectionObj["musicShelfRenderer"]?.jsonObject?.let { shelf ->
                    val songList = parseSongShelf(shelf)
                    songs.addAll(songList)
                }

                sectionObj["musicCarouselShelfRenderer"]?.jsonObject?.let { carousel ->
                    val albumList = parseOtherVersionsCarousel(carousel)
                    otherVersions.addAll(albumList)
                }
            }

            return AlbumPage(
                album = album,
                songs = songs,
                otherVersions = otherVersions,
            )
        }

        private fun parseSongShelf(shelf: JsonObject): List<SongItem> {
            val contents = shelf["contents"]?.jsonArray ?: return emptyList()
            return contents.mapNotNull { item ->
                val renderer = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                    ?: return@mapNotNull null
                parseSongItem(renderer)
            }
        }

        fun parseSongItemFromListItem(renderer: JsonObject): SongItem? {
            return parseSongItem(renderer)
        }

        private fun parseSongItem(renderer: JsonObject): SongItem? {
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
            var albumName: String? = null
            var albumId: String? = null

            subtitleRuns?.forEach { run ->
                val text = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val navEp = run.jsonObject["navigationEndpoint"]?.jsonObject
                val browseId = navEp?.get("browseEndpoint")?.jsonObject
                    ?.get("browseId")?.jsonPrimitive?.contentOrNull
                if (browseId?.startsWith("UC") == true) {
                    artists.add(YTItem.Artist(name = text, id = browseId))
                } else if (browseId?.startsWith("MPRE") == true) {
                    albumName = text
                    albumId = browseId
                }
            }

            val thumbnailUrl = renderer["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            var durationSeconds: Int? = null
            renderer["fixedColumns"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFixedColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?.let { text ->
                    val parts = text.split(":")
                    durationSeconds = when (parts.size) {
                        2 -> parts[0].toIntOrNull()?.let { m ->
                            parts[1].toIntOrNull()?.let { s -> m * 60 + s }
                        }
                        3 -> parts[0].toIntOrNull()?.let { h ->
                            parts[1].toIntOrNull()?.let { m ->
                                parts[2].toIntOrNull()?.let { s -> h * 3600 + m * 60 + s }
                            }
                        }
                        else -> null
                    }
                }

            val playlistId = renderer["navigationEndpoint"]?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("playlistId")?.jsonPrimitive?.contentOrNull

            return SongItem(
                id = videoId,
                title = title,
                artists = artists,
                album = YTItem.Album(name = albumName, id = albumId),
                thumbnailUrl = thumbnailUrl,
                durationSeconds = durationSeconds,
                playlistId = playlistId,
            )
        }

        private fun parseOtherVersionsCarousel(carousel: JsonObject): List<AlbumItem> {
            val contents = carousel["contents"]?.jsonArray ?: return emptyList()
            return contents.mapNotNull { item ->
                val renderer = item.jsonObject["musicTwoRowItemRenderer"]?.jsonObject
                    ?: return@mapNotNull null
                parseAlbumItem(renderer)
            }
        }

        private fun parseAlbumItem(renderer: JsonObject): AlbumItem? {
            val title = renderer["title"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val navEp = renderer["navigationEndpoint"]?.jsonObject
            val browseId = navEp?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull

            val thumbnail = renderer["thumbnailRenderer"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            val subtitleRuns = renderer["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray
            val subtitle = subtitleRuns?.joinToString("") {
                it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: ""
            }

            var artists: String? = null
            var year: String? = null
            subtitle?.split(" • ")?.forEach { part ->
                val trimmed = part.trim()
                if (trimmed.matches(Regex("^\\d{4}$"))) {
                    year = trimmed
                } else if (artists == null) {
                    artists = trimmed
                }
            }

            return AlbumItem(
                browseId = browseId,
                playlistId = null,
                title = title,
                artists = artists,
                year = year,
                thumbnail = thumbnail,
            )
        }

        private fun getPlaylistId(header: JsonObject): String? {
            return header["buttons"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("buttonRenderer")?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("watchPlaylistEndpoint")?.jsonObject
                ?.get("playlistId")?.jsonPrimitive?.contentOrNull
        }

        private fun getTitle(header: JsonObject): String? {
            return header["title"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
        }

        private fun getYear(header: JsonObject): String? {
            return header["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.matches(Regex("^\\d{4}$")) }
        }

        private fun getThumbnail(header: JsonObject): String? {
            return header["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull
        }

        private fun getArtists(header: JsonObject): String? {
            val subtitleRuns = header["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray ?: return null
            val artistRuns = subtitleRuns.dropLast(1)
            return artistRuns.joinToString("") {
                it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: ""
            }.trim().takeIf { it.isNotBlank() }
        }
    }
}
