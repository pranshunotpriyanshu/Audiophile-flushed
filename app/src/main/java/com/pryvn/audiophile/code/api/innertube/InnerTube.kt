package com.pryvn.audiophile.code.api.innertube

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import com.pryvn.audiophile.code.api.innertube.models.Context
import com.pryvn.audiophile.code.api.innertube.models.YouTubeClient
import com.pryvn.audiophile.code.api.innertube.YouTubeLocale
import com.pryvn.audiophile.code.api.innertube.models.body.*
import com.pryvn.audiophile.code.api.innertube.utils.sha1
import okhttp3.Dns
import java.io.IOException
import java.net.Proxy
import java.util.Locale

class InnerTube {
    private var httpClient = createClient()

    var locale = YouTubeLocale(
        gl = Locale.getDefault().country,
        hl = Locale.getDefault().toLanguageTag(),
    )

    @Volatile
    private var authState: PlaybackAuthState = PlaybackAuthState.EMPTY

    var visitorData: String?
        get() = authState.visitorData
        set(value) { authState = authState.copy(visitorData = value).normalized() }

    var dataSyncId: String?
        get() = authState.dataSyncId
        set(value) { authState = authState.copy(dataSyncId = value).normalized() }

    var poToken: String?
        get() = authState.poToken
        set(value) { authState = authState.copy(poToken = value).normalized() }

    var cookie: String?
        get() = authState.cookie
        set(value) { authState = authState.copy(cookie = value).normalized() }

    var proxy: Proxy? = null
        set(value) { field = value; httpClient.close(); httpClient = createClient() }

    var proxyUsername: String? = null
        set(value) { field = value; httpClient.close(); httpClient = createClient() }

    var proxyPassword: String? = null
        set(value) { field = value; httpClient.close(); httpClient = createClient() }

    var dns: Dns = Dns.SYSTEM
        set(value) { field = value; httpClient.close(); httpClient = createClient() }

    var useLoginForBrowse: Boolean = false

    fun currentAuthState(): PlaybackAuthState = authState

    fun applyAuthState(value: PlaybackAuthState) {
        authState = value.normalized()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient() = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
        }
        install(ContentEncoding) {
            gzip(0.9F)
            deflate(0.8F)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 20000
        }
        engine {
            config {
                dns(this@InnerTube.dns)
                if (this@InnerTube.proxy == null && proxyUsername.isNullOrBlank()) {
                    proxy(Proxy.NO_PROXY)
                } else if (this@InnerTube.proxy != null) {
                    proxy(this@InnerTube.proxy!!)
                    if (!proxyUsername.isNullOrBlank()) {
                        proxyAuthenticator { _, response ->
                            val credential = okhttp3.Credentials.basic(proxyUsername!!, proxyPassword ?: "")
                            response.request.newBuilder().header("Proxy-Authorization", credential).build()
                        }
                    }
                }
            }
        }
        defaultRequest {
            url(YouTubeClient.API_URL_YOUTUBE_MUSIC)
        }
    }

    private fun HttpRequestBuilder.ytClient(
        client: YouTubeClient,
        setLogin: Boolean = false,
        authState: PlaybackAuthState = currentAuthState(),
        includeVisitorData: Boolean = true,
    ) {
        val requestOrigin = client.requestOrigin
        val requestReferer = client.requestReferer
        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", requestOrigin)
            append("Referer", requestReferer)
            if (includeVisitorData) {
                authState.visitorData?.let { append("X-Goog-Visitor-Id", it) }
            }
            if (setLogin && client.loginSupported) {
                authState.cookie?.let { cookie ->
                    append("Cookie", cookie)
                    val loginCookieValue = youtubeLoginCookieValue(cookie) ?: return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = "$currentTime $loginCookieValue $requestOrigin".sha1()
                    append("Authorization", "SAPISIDHASH ${currentTime}_$sapisidHash")
                }
            }
        }
        userAgent(client.userAgent)
    }

    private fun HttpRequestBuilder.ytPlaybackTrackingClient(
        client: YouTubeClient,
        authState: PlaybackAuthState = currentAuthState(),
    ) {
        val requestOrigin = client.requestOrigin
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, ContentType.Application.Json.toString())
            append("Accept-Language", "en-US,en;q=0.9")
            append("Cache-Control", "no-cache")
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", requestOrigin)
            append("Referer", client.requestReferer)
            authState.visitorData?.let { append("X-Goog-Visitor-Id", it) }
            if (client.loginSupported) {
                authState.cookie?.let { cookie ->
                    append("Cookie", cookie)
                    val loginCookieValue = youtubeLoginCookieValue(cookie) ?: return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = "$currentTime $loginCookieValue $requestOrigin".sha1()
                    append("Authorization", "SAPISIDHASH ${currentTime}_$sapisidHash")
                }
            }
        }
        userAgent(client.userAgent)
    }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 500L,
        factor: Double = 2.0,
        block: suspend () -> T,
    ): T {
        var currentDelay = initialDelay
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                val transient = e is IOException || e is HttpRequestTimeoutException ||
                    (e is io.ktor.client.plugins.ClientRequestException && e.response.status.value in 500..599)
                if (!transient || attempt >= maxAttempts - 1) throw e
                attempt++
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
    }

    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
        useAccountContext: Boolean = true,
    ) = withRetry {
        httpClient.post("search") {
            ytClient(
                client = client,
                setLogin = useAccountContext && useLoginForBrowse,
                includeVisitorData = useAccountContext,
            )
            setBody(
                SearchBody(
                    context = client.toContext(
                        locale,
                        if (useAccountContext) visitorData else null,
                        if (useAccountContext && useLoginForBrowse) dataSyncId else null,
                    ),
                    query = query,
                    params = params,
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
            parameter("continuation", continuation)
            parameter("ctoken", continuation)
        }
    }

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
    ) = withRetry {
        httpClient.post("browse") {
            ytClient(client, setLogin = setLogin || useLoginForBrowse)
            setBody(
                BrowseBody(
                    context = client.toContext(
                        locale,
                        visitorData,
                        if (setLogin || useLoginForBrowse) dataSyncId else null,
                    ),
                    browseId = browseId,
                    params = params,
                    continuation = continuation,
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ) = withRetry {
        httpClient.post("music/get_search_suggestions") {
            ytClient(client)
            setBody(
                GetSearchSuggestionsBody(
                    context = client.toContext(locale, visitorData, null),
                    input = input,
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun next(
        client: YouTubeClient,
        videoId: String? = null,
        playlistId: String? = null,
        playlistSetVideoId: String? = null,
        index: Int? = null,
        params: String? = null,
        continuation: String? = null,
    ) = withRetry {
        httpClient.post("next") {
            ytClient(client)
            setBody(
                NextBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    videoId = videoId,
                    playlistId = playlistId,
                    playlistSetVideoId = playlistSetVideoId,
                    index = index,
                    params = params,
                    continuation = continuation,
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun accountMenu(client: YouTubeClient) = withRetry {
        httpClient.post("account/account_menu") {
            ytClient(client, setLogin = true)
            setBody(AccountMenuBody(client.toContext(locale, visitorData, dataSyncId)))
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun player(
        client: YouTubeClient,
        videoId: String? = null,
        playlistId: String? = null,
        signatureTimestamp: Int? = null,
        authState: PlaybackAuthState = currentAuthState(),
        includeDataSyncId: Boolean = true,
    ) = withRetry {
        httpClient.post("player") {
            ytClient(client, setLogin = true, authState = authState)
            setBody(
                PlayerBody(
                    context = client.toContext(
                        locale,
                        authState.visitorData,
                        if (includeDataSyncId) authState.dataSyncId else null,
                    ),
                    videoId = videoId.orEmpty(),
                    playlistId = playlistId,
                    playbackContext = signatureTimestamp?.let {
                        PlaybackContext(contentPlaybackContext = ContentPlaybackContext(signatureTimestamp = it))
                    },
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun registerPlayback(
        client: YouTubeClient,
        videoId: String,
        authState: PlaybackAuthState = currentAuthState(),
    ) = withRetry {
        httpClient.post("player") {
            ytClient(client, setLogin = true, authState = authState)
            setBody(
                RegisterPlaybackBody(
                    context = client.toContext(
                        locale,
                        authState.visitorData,
                        authState.dataSyncId,
                    ),
                    videoId = videoId,
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun getSwJsData() = withRetry {
        httpClient.get("https://music.youtube.com/sw.js_data")
    }

    suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>? = null,
        playlistId: String? = null,
    ) = withRetry {
        httpClient.post("player") {
            ytClient(client, setLogin = true)
            setBody(
                GetQueueBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    videoIds = videoIds,
                    playlistId = playlistId,
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun getTranscript(
        client: YouTubeClient,
        params: String,
    ) = withRetry {
        httpClient.post("get_transcript") {
            ytClient(client)
            setBody(
                GetTranscriptBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    params = params,
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun accountChannels(client: YouTubeClient) = withRetry {
        httpClient.post("account/accounts_list") {
            ytClient(client, setLogin = true)
            setBody(AccountsListBody(client.toContext(locale, visitorData, dataSyncId)))
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun likeVideo(
        client: YouTubeClient,
        videoId: String,
        like: Boolean = true,
    ) = withRetry {
        httpClient.post("like/like") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.VideoTarget(videoId),
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
            if (!like) {
                parameter("action", "DISLIKE")
            }
        }
    }

    suspend fun likePlaylist(
        client: YouTubeClient,
        playlistId: String,
        like: Boolean = true,
    ) = withRetry {
        httpClient.post("like/like") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.PlaylistTarget(playlistId),
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
            if (!like) {
                parameter("action", "DISLIKE")
            }
        }
    }

    suspend fun subscribeChannel(
        client: YouTubeClient,
        channelId: String,
        subscribe: Boolean = true,
    ) = withRetry {
        httpClient.post("subscription/subscribe") {
            ytClient(client, setLogin = true)
            setBody(
                SubscribeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    channelIds = listOf(channelId),
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
            if (!subscribe) {
                parameter("action", "SUBSCRIBE_TO_CHANNEL")
            }
        }
    }

    suspend fun addToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(Action.AddVideoAction(videoId)),
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun addSongsToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoIds: List<String>,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = videoIds.map { Action.AddVideoAction(it) },
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun removeFromPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(Action.RemoveVideoAction(videoId, setVideoId)),
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun moveSongPlaylist(
        client: YouTubeClient,
        playlistId: String,
        setVideoId: String,
        successorSetVideoId: String? = null,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(Action.MoveVideoAction(setVideoId, successorSetVideoId)),
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun createPlaylist(
        client: YouTubeClient,
        title: String,
        videoIds: List<String> = emptyList(),
    ) = withRetry {
        httpClient.post("playlist/create") {
            ytClient(client, setLogin = true)
            setBody(
                CreatePlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    title = title,
                    videoIds = videoIds,
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun renamePlaylist(
        client: YouTubeClient,
        playlistId: String,
        name: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(Action.RenamePlaylistAction(name)),
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun deletePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = withRetry {
        httpClient.post("playlist/delete") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = emptyList(),
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun unlikeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        httpClient.post("like/like") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.VideoTarget(videoId),
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
            parameter("action", "DISLIKE")
        }
    }

    suspend fun unsubscribeChannel(
        client: YouTubeClient,
        channelId: String,
    ) = withRetry {
        httpClient.post("subscription/unsubscribe") {
            ytClient(client, setLogin = true)
            setBody(
                SubscribeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    channelIds = listOf(channelId),
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    suspend fun getMediaInfo(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        httpClient.post("next") {
            ytClient(client, setLogin = true)
            setBody(
                NextBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    videoId = videoId,
                ),
            )
            parameter("key", YouTubeClient.API_KEY)
            parameter("prettyPrint", false)
        }
    }

    fun close() {
        httpClient.close()
    }
}
