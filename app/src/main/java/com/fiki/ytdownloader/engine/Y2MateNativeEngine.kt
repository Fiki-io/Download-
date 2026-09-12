package com.fiki.ytdownloader.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AvailableQuality(
    val id: String,
    val label: String,       // e.g. "720p HD (.mp4)", "360p (.mp4)", "MP3 Audio (128 kbps)"
    val size: String,        // e.g. "Video HD" or "Audio Musik"
    val formatType: String,  // "video" or "audio"
    val qualityValue: String // "720", "360", "128"
)

sealed class EngineState {
    object Idle : EngineState()
    object LoadingFormats : EngineState()
    data class FormatsReady(val formats: List<AvailableQuality>) : EngineState()
    data class Converting(val targetQuality: String) : EngineState()
    data class Downloading(val targetQuality: String, val percent: Int, val bytesDownloaded: Long, val totalBytes: Long) : EngineState()
    data class Success(val fileName: String) : EngineState()
    data class Error(val message: String) : EngineState()
}

data class DownloadResult(
    val url: String,
    val filename: String,
    val mimeType: String
)

object Y2MateNativeEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private const val REFERER_URL = "https://iframe.y2meta-uk.com/"

    suspend fun getAvailableQualities(videoId: String): List<AvailableQuality> = withContext(Dispatchers.IO) {
        listOf(
            // Video Formats
            AvailableQuality("mp4_1080", "1080p Full HD", "MP4", "video", "1080"),
            AvailableQuality("mp4_720", "720p HD", "MP4", "video", "720"),
            AvailableQuality("mp4_360", "360p", "MP4", "video", "360"),
            AvailableQuality("mp4_240", "240p", "MP4", "video", "240"),
            AvailableQuality("mp4_144", "144p", "MP4", "video", "144"),

            // Audio Formats (MP3)
            AvailableQuality("mp3_320", "320 kbps", "MP3 • Kualitas Terbaik", "audio", "320"),
            AvailableQuality("mp3_256", "256 kbps", "MP3 • Kualitas Tinggi", "audio", "256"),
            AvailableQuality("mp3_128", "128 kbps", "MP3 • Standar", "audio", "128")
        )
    }

    suspend fun convertVideo(
        videoId: String,
        quality: AvailableQuality
    ): Result<DownloadResult> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null

        // Auto-retry up to 3 attempts with 1s delay
        for (attempt in 1..3) {
            try {
                // Step 1: Request sanity key from y2mate backend
                val keyUrl = "https://cnv.cx/v2/sanity/key?id=$videoId"
                val keyReq = Request.Builder()
                    .url(keyUrl)
                    .header("Referer", REFERER_URL)
                    .header("Origin", "https://iframe.y2meta-uk.com")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36")
                    .build()

                val tokenKey = client.newCall(keyReq).execute().use { keyResponse ->
                    val keyBody = keyResponse.body?.string() ?: ""
                    val keyJson = JSONObject(keyBody)
                    keyJson.optString("key", "")
                }

                if (tokenKey.isEmpty()) {
                    if (attempt < 3) {
                        kotlinx.coroutines.delay(1000)
                        continue
                    }
                    return@withContext Result.failure(Exception("Gagal mengambil kunci otorisasi"))
                }

                // Step 2: Request conversion with token
                val isMp3 = quality.formatType == "audio"
                val formatParam = if (isMp3) "mp3" else "mp4"
                val audioBitrate = if (isMp3) quality.qualityValue else "128"
                val videoQuality = if (isMp3) "720" else quality.qualityValue

                val formBody = FormBody.Builder()
                    .add("link", "https://youtu.be/$videoId")
                    .add("format", formatParam)
                    .add("audioBitrate", audioBitrate)
                    .add("videoQuality", videoQuality)
                    .add("filenameStyle", "pretty")
                    .add("vCodec", "h264")
                    .build()

                val convReq = Request.Builder()
                    .url("https://cnv.cx/v2/converter")
                    .header("Referer", REFERER_URL)
                    .header("Origin", "https://iframe.y2meta-uk.com")
                    .header("key", tokenKey)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36")
                    .post(formBody)
                    .build()

                val convJson = client.newCall(convReq).execute().use { convResponse ->
                    val convBody = convResponse.body?.string() ?: ""
                    JSONObject(convBody)
                }

                val downloadUrl = convJson.optString("url", "")
                val filename = convJson.optString("filename", "YouTube_${quality.qualityValue}.$formatParam")

                if (downloadUrl.isNotEmpty()) {
                    return@withContext Result.success(
                        DownloadResult(downloadUrl, filename, if (isMp3) "audio/mpeg" else "video/mp4")
                    )
                } else {
                    val errMsg = convJson.optString("errorMsg", "")
                    if (errMsg.isNotBlank() && errMsg.contains("progress", ignoreCase = true) && attempt < 3) {
                        // Server still processing video, wait and retry
                        kotlinx.coroutines.delay(1500)
                        continue
                    }
                    val finalMsg = errMsg.ifBlank { "Server sedang memproses, silakan coba beberapa saat lagi" }
                    lastError = Exception(finalMsg)
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt < 3) {
                    kotlinx.coroutines.delay(1000)
                }
            }
        }

        Result.failure(lastError ?: Exception("Gagal memproses video"))
    }
}
