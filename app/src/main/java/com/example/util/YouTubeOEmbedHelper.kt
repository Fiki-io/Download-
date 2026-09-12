package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class VideoInfo(
    val id: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String
)

object YouTubeOEmbedHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchVideoInfo(videoId: String): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android Mobile)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    VideoInfo(
                        id = videoId,
                        title = json.optString("title", "YouTube Video"),
                        author = json.optString("author_name", "YouTube"),
                        thumbnailUrl = json.optString("thumbnail_url", "https://img.youtube.com/vi/$videoId/hqdefault.jpg")
                    )
                } else {
                    VideoInfo(
                        id = videoId,
                        title = "YouTube Video ($videoId)",
                        author = "YouTube",
                        thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to basic info with thumbnail
            VideoInfo(
                id = videoId,
                title = "YouTube Video ($videoId)",
                author = "YouTube",
                thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            )
        }
    }
}
