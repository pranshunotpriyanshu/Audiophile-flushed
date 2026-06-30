package com.pryvn.audiophile.code.api.innertube.utils

import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.floor

fun String.sha1(): String {
    val digest = MessageDigest.getInstance("SHA-1")
    return digest.digest(this.toByteArray()).joinToString("") { "%02x".format(it) }
}

fun youtubeLoginCookieValue(cookie: String): String? {
    if (cookie.isBlank()) return null
    return cookie.split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("SAPISID=") || it.startsWith("__Secure-3PAPISID=") }
        ?.substringAfter("=")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

fun parseQueryString(query: String): Map<String, String> {
    if (query.isBlank()) return emptyMap()
    return query.split("&")
        .filter { it.contains("=") }
        .associate {
            val key = it.substringBefore("=").trim()
            val value = it.substringAfter("=").trim()
                .let { v -> java.net.URLDecoder.decode(v, "UTF-8") }
            key to value
        }
}

fun baseUrl(url: String): String {
    return try {
        val uri = java.net.URI(url)
        val scheme = uri.scheme ?: return url
        val host = uri.host ?: return url
        val port = if (uri.port in 1..65535 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
        "$scheme://$host$port"
    } catch (_: Throwable) {
        url.substringBefore("?").substringBefore("#")
    }
}

fun getDocumentId(url: String): String? {
    return try {
        val uri = java.net.URI(url)
        val path = uri.path ?: return null
        path.trimStart('/').split("/").lastOrNull()?.takeIf { it.isNotEmpty() }
    } catch (_: Throwable) {
        null
    }
}

fun String.splitOnFirst(delimiter: String): Pair<String, String> {
    val idx = this.indexOf(delimiter)
    return if (idx >= 0) {
        this.substring(0, idx) to this.substring(idx + delimiter.length)
    } else {
        this to ""
    }
}

fun parseDuration(text: String): Long {
    if (text.isBlank()) return 0L
    val cleaned = text.trim()
    if (cleaned.contains(":")) {
        val parts = cleaned.split(":")
        return when (parts.size) {
            2 -> {
                val min = parts[0].toLongOrNull() ?: 0L
                val sec = parts[1].toLongOrNull() ?: 0L
                TimeUnit.MINUTES.toMillis(min) + TimeUnit.SECONDS.toMillis(sec)
            }
            3 -> {
                val hour = parts[0].toLongOrNull() ?: 0L
                val min = parts[1].toLongOrNull() ?: 0L
                val sec = parts[2].toLongOrNull() ?: 0L
                TimeUnit.HOURS.toMillis(hour) + TimeUnit.MINUTES.toMillis(min) + TimeUnit.SECONDS.toMillis(sec)
            }
            else -> 0L
        }
    }
    val match = Regex("""(\d+)""").find(cleaned)
    return match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = abs(durationMs) / 1000
    val hours = floor(totalSeconds / 3600.0).toInt()
    val minutes = floor((totalSeconds % 3600) / 60.0).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun getTranscriptParams(videoId: String): String {
    val innerParams = "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"2.20240101.00.00\"}},\"videoId\":\"$videoId\"}"
    return java.util.Base64.getEncoder().encodeToString(innerParams.toByteArray())
        .replace("+", "-")
        .replace("/", "_")
}
