package com.pryvn.audiophile.code.api.innertube.models

import com.pryvn.audiophile.code.api.innertube.YouTubeLocale

data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val osName: String = "Windows",
    val osVersion: String = "10.0",
    val platform: String = "DESKTOP",
    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    val requestOrigin: String = "https://music.youtube.com",
    val requestReferer: String = "https://music.youtube.com/",
    val loginSupported: Boolean = true,
    val useSignatureTimestamp: Boolean = true,
    val isEmbedded: Boolean = false,
) {
    fun requestApiUrl(endpoint: String): String = "$API_URL_YOUTUBE_MUSIC/$endpoint"

    fun toContext(locale: YouTubeLocale, visitorData: String? = null, dataSyncId: String? = null): Context {
        return Context(
            client = Context.Client(
                clientName = clientName,
                clientVersion = clientVersion,
                clientId = clientId,
                osName = osName,
                osVersion = osVersion,
                platform = platform,
                hl = locale.hl,
                gl = locale.gl,
                visitorData = visitorData,
            ),
            user = if (dataSyncId != null) Context.User(
                lockedSafetyMode = false,
                onBehalfOfUser = dataSyncId,
            ) else null,
            request = Context.Request(useSsl = true),
        )
    }

    companion object {
        const val API_URL_YOUTUBE_MUSIC = "https://music.youtube.com/youtubei/v1"
        const val API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

        val WEB = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.20250101.00.00",
            clientId = "1",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            loginSupported = true,
            useSignatureTimestamp = false,
        )

        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20250101.00.00",
            clientId = "67",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val ANDROID = YouTubeClient(
            clientName = "ANDROID",
            clientVersion = "18.29.38",
            clientId = "2",
            osName = "Android",
            osVersion = "14",
            platform = "MOBILE",
            userAgent = "com.google.android.apps.youtube.music/18.29.38 (Linux; U; Android 14) gzip",
            loginSupported = false,
            useSignatureTimestamp = true,
        )

        val ANDROID_MUSIC = YouTubeClient(
            clientName = "ANDROID_MUSIC",
            clientVersion = "6.42.52",
            clientId = "21",
            osName = "Android",
            osVersion = "14",
            platform = "MOBILE",
            userAgent = "com.google.android.apps.youtube.music/6.42.52 (Linux; U; Android 14) gzip",
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val IOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "19.09.3",
            clientId = "5",
            osName = "iOS",
            osVersion = "17.2.1",
            platform = "MOBILE",
            userAgent = "com.google.ios.youtubemusic/19.09.3 (iPhone14,3; U; CPU iOS 17_2_1 like Mac OS X)",
            loginSupported = false,
            useSignatureTimestamp = true,
        )

        val TVHTML5 = YouTubeClient(
            clientName = "TVHTML5",
            clientVersion = "7.20250101.00.00",
            clientId = "7",
            loginSupported = false,
            useSignatureTimestamp = false,
        )

        val WEB_CREATOR = YouTubeClient(
            clientName = "WEB_CREATOR",
            clientVersion = "1.20250101.00.00",
            clientId = "62",
            loginSupported = true,
            useSignatureTimestamp = false,
        )

        val TVHTML5_SIMPLY = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED",
            clientVersion = "1.0",
            clientId = "85",
            loginSupported = false,
            useSignatureTimestamp = false,
            isEmbedded = true,
        )
    }
}
