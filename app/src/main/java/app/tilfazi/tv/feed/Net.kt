package app.tilfazi.tv.feed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Small blocking HTTP helpers, always called off the main thread. */
object Net {
    suspend fun bytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray = withContext(Dispatchers.IO) {
        val con = URL(url).openConnection() as HttpURLConnection
        con.connectTimeout = 8_000
        con.readTimeout = 15_000
        con.setRequestProperty("User-Agent", "TilfaziTV/0.1")
        headers.forEach { (k, v) -> con.setRequestProperty(k, v) }
        try {
            if (con.responseCode !in 200..299) error("HTTP ${con.responseCode} for $url")
            con.inputStream.use { it.readBytes() }
        } finally {
            con.disconnect()
        }
    }

    suspend fun json(url: String, headers: Map<String, String> = emptyMap()): JSONObject =
        JSONObject(String(bytes(url, headers), Charsets.UTF_8))
}
