package com.nuvio.app.features.vezie

import com.nuvio.app.features.addons.httpPostJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object FlareSolverr {
    private var flareSolverrUrl: String? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun getUrl(): String? = flareSolverrUrl

    fun configure(url: String) {
        flareSolverrUrl = url.trimEnd('/')
    }

    suspend fun scrapePage(url: String): String? {
        val baseUrl = flareSolverrUrl ?: return null
        if (baseUrl.isBlank()) return null

        return try {
            val requestBody = buildString {
                append("""{"cmd":"request.get","url":"""")
                append(escapeJsonString(url))
                append(""","maxTimeout":60000}""")
            }
            val response = httpPostJson("$baseUrl/v1", requestBody)
            val obj = json.parseToJsonElement(response).jsonObject
            val solution = obj["solution"]?.jsonObject ?: return null
            solution["response"]?.jsonPrimitive?.contentOrNull
        } catch (_: Exception) { null }
    }

    private fun escapeJsonString(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
