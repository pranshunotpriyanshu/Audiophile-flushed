package com.pryvn.audiophile.code.api

import kotlinx.serialization.json.*

internal object YouTubePlayerParser {

    fun parsePlayerResponse(root: JsonObject): YTPlayerResponse {
        val videoDetails = root["videoDetails"]?.jsonObject
            ?: error("Missing videoDetails")

        val videoId = videoDetails["videoId"]?.jsonPrimitive?.content ?: ""
        val title = videoDetails["title"]?.jsonPrimitive?.contentOrNull
        val artist = videoDetails["author"]?.jsonPrimitive?.contentOrNull
        val thumbnailUrl = videoDetails["thumbnail"]?.jsonObject
            ?.get("thumbnails")?.jsonArray
            ?.lastOrNull()?.jsonObject
            ?.get("url")?.jsonPrimitive?.contentOrNull
        val lengthSeconds = videoDetails["lengthSeconds"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

        val streamingData = root["streamingData"]?.jsonObject
        val streamUrl = streamingData?.get("adaptiveFormats")?.jsonArray
            ?.firstOrNull { fmt ->
                val mime = fmt.jsonObject["mimeType"]?.jsonPrimitive?.contentOrNull ?: ""
                mime.contains("audio/mp4") || mime.contains("audio/webm")
            }?.let { extractUrlFromFormat(it.jsonObject) }
            ?: streamingData?.get("formats")?.jsonArray
                ?.firstOrNull()?.let { extractUrlFromFormat(it.jsonObject) }
            ?: streamingData?.get("hlsManifestUrl")?.jsonPrimitive?.contentOrNull
            ?: streamingData?.get("dashManifestUrl")?.jsonPrimitive?.contentOrNull

        val expiresInSeconds = streamingData?.get("expiresInSeconds")?.jsonPrimitive?.contentOrNull?.toIntOrNull()

        return YTPlayerResponse(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            lengthSeconds = lengthSeconds,
            streamUrl = streamUrl,
            expiresInSeconds = expiresInSeconds,
        )
    }

    private fun extractUrlFromFormat(fmt: JsonObject): String? {
        fmt["url"]?.jsonPrimitive?.contentOrNull?.let { return it }
        val cipher = fmt["cipher"]?.jsonPrimitive?.contentOrNull
            ?: fmt["signatureCipher"]?.jsonPrimitive?.contentOrNull ?: return null
        val params = cipher.split("&").associate { param ->
            val eq = param.indexOf("=")
            if (eq > 0) param.substring(0, eq) to param.substring(eq + 1)
            else param to ""
        }
        val url = params["url"]?.replace("%3A", ":")?.replace("%2F", "/")
            ?.replace("%3F", "?")?.replace("%3D", "=")?.replace("%26", "&") ?: return null
        val sig = params["s"] ?: params["sig"]
        val sp = params["sp"] ?: "sig"
        if (sig != null) {
            val separator = if (url.contains("?")) "&" else "?"
            return "$url$separator$sp=$sig"
        }
        return url
    }
}