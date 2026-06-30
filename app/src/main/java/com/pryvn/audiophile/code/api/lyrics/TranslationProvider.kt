package com.pryvn.audiophile.code.api.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.call.body
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

class TranslationProvider {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) { json(this@TranslationProvider.json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }
        }
    }

    suspend fun fetchTranslation(
        lyrics: String,
        targetLang: String = "zh",
        sourceLang: String = "auto",
    ): Result<String> {
        return try {
            val encoded = URLEncoder.encode(lyrics.take(2000), "UTF-8")
            val response = client.get("https://lingva.ml/api/v1/$sourceLang/$targetLang/$encoded")
            val body = response.body<String>()
            val root = json.parseToJsonElement(body).jsonObject
            val translation = root["translation"]?.jsonPrimitive?.content
                ?: throw Exception("No translation in response")
            Result.success(translation)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
