package com.pryvn.audiophile.code.api

import android.util.Log
import com.pryvn.audiophile.data.libraries.SettingsLibrary
import kotlinx.serialization.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.pryvn.audiophile.code.api.InnerTubeClient
import com.pryvn.audiophile.code.api.InnerTubeClient.ClientConfig
import java.net.HttpURLConnection
import java.net.URL

object YouTubeApi {

    private val searchParser = YouTubeSearchParser
    private val playlistParser = YouTubePlaylistParser
    private val playerParser = YouTubePlayerParser
    private val homeParser = YouTubeHomeParser
    private val suggestionParser = YouTubeSuggestionParser

    suspend fun fetchAccountInfo(): Result<YTAccountInfo> {
        return runCatching {
            val result = InnerTubeClient.accountMenu()
            val header = result.getOrNull()?.get("actions")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("openPopupAction")?.jsonObject
                ?.get("popup")?.jsonObject
                ?.get("accountMenuPopupRenderer")?.jsonObject
                ?.get("header")?.jsonObject
                ?.get("activeAccountHeaderRenderer")?.jsonObject
            val name = header?.get("accountName")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
            val email = header?.get("email")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
            val channelHandle = header?.get("channelHandle")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
            val avatarUrl = header?.get("accountPhoto")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull
            YTAccountInfo(
                name = name,
                email = email,
                channelHandle = channelHandle,
                avatarUrl = avatarUrl
            )
        }
    }

    /**
     * Fetches a fresh visitor ID from YouTube's homepage (works even without login)
     */
    private suspend fun fetchVisitorIdFromHomepage(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://www.youtube.com")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                val cookieHeader = connection.getHeaderField("Set-Cookie")
                connection.disconnect()
                if (cookieHeader != null) {
                    val visitorCookie = cookieHeader.split(";")
                        .firstOrNull { it.trim().startsWith("VISITOR_INFO1_LIVE=") }
                        ?.substringAfter("=")
                        ?.trim()
                    if (!visitorCookie.isNullOrBlank()) {
                        Log.d("YouTubeApi", "Fetched visitor ID from homepage: $visitorCookie")
                        return@withContext visitorCookie
                    }
                }
                null
            } catch (e: Exception) {
                Log.e("YouTubeApi", "Error fetching visitor ID from homepage", e)
                null
            }
        }
    }

    private suspend fun fetchVisitorData(): String? {
        return runCatching {
            Log.d("YouTubeApi", "Fetching visitor data...")
            // First try via InnerTubeClient.browse (may fail if no visitor)
            val result = InnerTubeClient.browse(
                browseId = "FEmusic_home",
                setLogin = false,
                clientName = InnerTubeClient.CLIENT_NAME,
                clientVersion = InnerTubeClient.CLIENT_VERSION,
                clientId = InnerTubeClient.CLIENT_ID,
                ua = InnerTubeClient.USER_AGENT,
                origin = "https://music.youtube.com",
                referer = "https://music.youtube.com/"
            )
            val json = result.getOrNull()
            var visitorData = json?.get("visitorData")?.jsonPrimitive?.contentOrNull
                ?: json?.get("responseContext")?.jsonObject
                    ?.get("visitorData")?.jsonPrimitive?.contentOrNull

            if (visitorData.isNullOrBlank()) {
                Log.w("YouTubeApi", "Browse call didn't return visitorData, trying homepage...")
                visitorData = fetchVisitorIdFromHomepage()
            }

            if (!visitorData.isNullOrBlank()) {
                InnerTubeClient.visitorData = visitorData
                SettingsLibrary.YtMusicVisitorData = visitorData
                Log.d("YouTubeApi", "Visitor data set: $visitorData")
            } else {
                Log.e("YouTubeApi", "Could not obtain visitorData")
            }
            InnerTubeClient.visitorData
        }.getOrNull()
    }

    suspend fun search(query: String, filter: String? = null): Result<YTSearchResult> {
        Log.d("YouTubeApi", "Search called with query: $query")
        val result = searchWithFallback(query, filter)
        result.onSuccess {
            Log.d("YouTubeApi", "Search success, items: ${it.items.size}")
        }.onFailure {
            Log.e("YouTubeApi", "Search failed", it)
        }
        return result
    }

    private suspend fun searchWithFallback(query: String, filter: String? = null): Result<YTSearchResult> =
        withContext(Dispatchers.IO) {
            if (InnerTubeClient.visitorData.isNullOrBlank()) {
                Log.w("YouTubeApi", "visitorData missing, attempting to fetch...")
                fetchVisitorData()
            }

            val setLogin = InnerTubeClient.hasLoginCookie
            val params = filter
            val configs = listOf(
                ClientConfig(
                    InnerTubeClient.CLIENT_NAME,
                    InnerTubeClient.CLIENT_VERSION,
                    InnerTubeClient.CLIENT_ID,
                    ua = InnerTubeClient.USER_AGENT,
                    origin = "https://music.youtube.com",
                    referer = "https://music.youtube.com/"
                ),
                ClientConfig(
                    "WEB_REMIX",
                    "2.20250101.00.00",
                    "67",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
                    "https://www.youtube.com",
                    "https://www.youtube.com/"
                ),
                ClientConfig(
                    "WEB",
                    "2.20250101.00.00",
                    "1",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
                    "https://www.youtube.com",
                    "https://www.youtube.com/"
                )
            )

            for (config in configs) {
                try {
                    val result = InnerTubeClient.search(
                        query = query,
                        params = params,
                        setLogin = setLogin,
                        clientName = config.clientName,
                        clientVersion = config.clientVersion,
                        clientId = config.clientId,
                        ua = config.ua,
                        origin = config.origin,
                        referer = config.referer
                    )
                    if (result.isSuccess) {
                        val json = result.getOrNull() ?: continue
                        return@withContext Result.success(searchParser.parseSearchResults(json))
                    } else {
                        Log.w("YouTubeApi", "Search attempt with ${config.clientName} failed: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.w("YouTubeApi", "Search attempt with ${config.clientName} threw exception", e)
                }
            }

            // final fallback – unwrap the Result inside runCatching
            return@withContext runCatching {
                val fallbackResult = InnerTubeClient.search(query, params, setLogin = setLogin)
                if (fallbackResult.isSuccess) {
                    searchParser.parseSearchResults(fallbackResult.getOrThrow())
                } else {
                    throw fallbackResult.exceptionOrNull() ?: Exception("Fallback search failed")
                }
            }
        }

    suspend fun searchContinuation(continuation: String): Result<YTSearchResult> {
        val result = InnerTubeClient.search(
            query = null,
            continuation = continuation,
            setLogin = InnerTubeClient.hasLoginCookie
        )
        return result.map { json -> searchParser.parseSearchContinuation(json) }
    }

    suspend fun artist(browseId: String): Result<JsonObject> = InnerTubeClient.browse(browseId)
    suspend fun album(browseId: String): Result<JsonObject> = InnerTubeClient.browse(browseId)

    suspend fun playlist(playlistId: String): Result<YTPlaylistPage> {
        val result = InnerTubeClient.browse(
            browseId = "VL$playlistId",
            setLogin = InnerTubeClient.hasLoginCookie,
        )
        return result.map { json -> playlistParser.parsePlaylistPage(json, playlistId) }
    }

    suspend fun home(continuation: String? = null): Result<JsonObject> =
        homeWithFallback(continuation)

    private suspend fun homeWithFallback(continuation: String? = null): Result<JsonObject> =
        withContext(Dispatchers.IO) {
            if (InnerTubeClient.visitorData.isNullOrBlank()) {
                Log.w("YouTubeApi", "visitorData missing for home, attempting to fetch...")
                fetchVisitorData()
            }

            val setLogin = InnerTubeClient.hasLoginCookie
            val configs = listOf(
                ClientConfig(
                    InnerTubeClient.CLIENT_NAME,
                    InnerTubeClient.CLIENT_VERSION,
                    InnerTubeClient.CLIENT_ID,
                    ua = InnerTubeClient.USER_AGENT,
                    origin = "https://music.youtube.com",
                    referer = "https://music.youtube.com/"
                ),
                ClientConfig(
                    "WEB_REMIX",
                    "2.20250101.00.00",
                    "67",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
                    "https://www.youtube.com",
                    "https://www.youtube.com/"
                ),
                ClientConfig(
                    "WEB",
                    "2.20250101.00.00",
                    "1",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
                    "https://www.youtube.com",
                    "https://www.youtube.com/"
                )
            )

            val browseId = if (continuation != null) null else "FEmusic_home"

            for (config in configs) {
                try {
                    val result = if (continuation != null) {
                        InnerTubeClient.browse(
                            continuation = continuation,
                            setLogin = setLogin,
                            clientName = config.clientName,
                            clientVersion = config.clientVersion,
                            clientId = config.clientId,
                            ua = config.ua,
                            origin = config.origin,
                            referer = config.referer
                        )
                    } else {
                        InnerTubeClient.browse(
                            browseId = browseId,
                            setLogin = setLogin,
                            clientName = config.clientName,
                            clientVersion = config.clientVersion,
                            clientId = config.clientId,
                            ua = config.ua,
                            origin = config.origin,
                            referer = config.referer
                        )
                    }
                    if (result.isSuccess) {
                        val json = result.getOrNull() ?: continue
                        return@withContext Result.success(json)
                    } else {
                        Log.w("YouTubeApi", "Home attempt with ${config.clientName} failed: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.w("YouTubeApi", "Home attempt with ${config.clientName} threw exception", e)
                }
            }

            // final fallback – unwrap the Result inside runCatching
            return@withContext runCatching {
                val browseResult = if (continuation != null) {
                    InnerTubeClient.browse(
                        continuation = continuation,
                        setLogin = setLogin,
                        clientName = InnerTubeClient.CLIENT_NAME,
                        clientVersion = InnerTubeClient.CLIENT_VERSION,
                        clientId = InnerTubeClient.CLIENT_ID,
                        ua = InnerTubeClient.USER_AGENT,
                        origin = "https://music.youtube.com",
                        referer = "https://music.youtube.com/"
                    )
                } else {
                    InnerTubeClient.browse(
                        browseId = "FEmusic_home",
                        setLogin = setLogin,
                        clientName = InnerTubeClient.CLIENT_NAME,
                        clientVersion = InnerTubeClient.CLIENT_VERSION,
                        clientId = InnerTubeClient.CLIENT_ID,
                        ua = InnerTubeClient.USER_AGENT,
                        origin = "https://music.youtube.com",
                        referer = "https://music.youtube.com/"
                    )
                }
                if (browseResult.isSuccess) {
                    browseResult.getOrThrow()
                } else {
                    throw browseResult.exceptionOrNull() ?: Exception("Fallback home browse failed")
                }
            }
        }

    suspend fun library(browseId: String = "FEmusic_liked_playlists"): Result<JsonObject> =
        InnerTubeClient.browse(
            browseId = browseId,
            setLogin = InnerTubeClient.hasLoginCookie,
            clientName = InnerTubeClient.CLIENT_NAME,
            clientVersion = InnerTubeClient.CLIENT_VERSION,
            clientId = InnerTubeClient.CLIENT_ID,
            ua = InnerTubeClient.USER_AGENT,
            origin = "https://music.youtube.com",
            referer = "https://music.youtube.com/"
        )

    fun parseHomeSections(root: JsonObject): List<HomeSection> =
        homeParser.parseHomeSections(root)

    fun parseLibraryPlaylists(root: JsonObject): List<YTPlaylist> =
        playlistParser.parseLibraryPlaylists(root)

    suspend fun getSearchSuggestions(input: String): Result<List<String>> {
        val result = InnerTubeClient.getSearchSuggestions(input)
        return result.map { json -> suggestionParser.parseSuggestions(json) }
    }

    suspend fun player(videoId: String, playlistId: String? = null): Result<YTPlayerResponse> {
        val result = InnerTubeClient.playerWithFallback(videoId, playlistId)
        return result.map { json -> playerParser.parsePlayerResponse(json) }
    }

    suspend fun playerSingle(videoId: String, playlistId: String? = null): Result<YTPlayerResponse> {
        return runCatching {
            val sigTs = fetchSignatureTimestamp()
            val result = InnerTubeClient.player(videoId, playlistId, signatureTimestamp = sigTs)
            playerParser.parsePlayerResponse(result.getOrThrow())
        }
    }

    private var cachedSignatureTimestamp: Int? = null

    suspend fun fetchSignatureTimestamp(): Int {
        cachedSignatureTimestamp?.let { return it }
        return runCatching {
            val result = InnerTubeClient.browse(browseId = "FEmusic_home")
            val tabs = result.getOrNull()?.get("contents")?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?: result.getOrNull()?.get("contents")?.jsonObject
                    ?.get("twoColumnBrowseResultsRenderer")?.jsonObject
                    ?.get("tabs")?.jsonArray
                ?: return@runCatching 24007
            val ts = tabs?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("musicShelfRenderer")?.jsonObject
                ?.get("bottomStatus")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.get("signatureTimestamp")?.jsonPrimitive?.contentOrNull
                ?.toIntOrNull()
            if (ts != null) {
                cachedSignatureTimestamp = ts
                ts
            } else {
                24007
            }
        }.getOrElse { 24007 }
    }
}