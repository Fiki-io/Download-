package com.fiki.ytdownloader.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SearchResultItem(
    val id: String,
    val title: String,
    val author: String,
    val duration: String,
    val thumbnailUrl: String
)

object YouTubeSearchHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val SEARCH_ENDPOINT = "https://www.youtube.com/youtubei/v1/search"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun searchVideos(query: String): Result<List<SearchResultItem>> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20230510.00.00")
                        put("hl", "id")
                        put("gl", "ID")
                    })
                })
                put("query", query)
            }

            val request = Request.Builder()
                .url(SEARCH_ENDPOINT)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal menghubungi server pencarian (${response.code})"))
            }

            val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Respon kosong"))
            val json = JSONObject(bodyString)

            val results = mutableListOf<SearchResultItem>()
            val contents = json.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return@withContext Result.success(emptyList())

            for (i in 0 until contents.length()) {
                val section = contents.optJSONObject(i) ?: continue
                val itemSection = section.optJSONObject("itemSectionRenderer")?.optJSONArray("contents") ?: continue

                for (j in 0 until itemSection.length()) {
                    val item = itemSection.optJSONObject(j) ?: continue
                    val videoRenderer = item.optJSONObject("videoRenderer") ?: continue

                    val videoId = videoRenderer.optString("videoId")
                    if (videoId.isNullOrBlank()) continue

                    val titleRuns = videoRenderer.optJSONObject("title")?.optJSONArray("runs")
                    val title = if (titleRuns != null && titleRuns.length() > 0) {
                        titleRuns.optJSONObject(0)?.optString("text") ?: ""
                    } else ""

                    val ownerRuns = videoRenderer.optJSONObject("ownerText")?.optJSONArray("runs")
                    val author = if (ownerRuns != null && ownerRuns.length() > 0) {
                        ownerRuns.optJSONObject(0)?.optString("text") ?: ""
                    } else ""

                    val duration = videoRenderer.optJSONObject("lengthText")?.optString("simpleText") ?: ""

                    val thumbnails = videoRenderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                    val thumbUrl = if (thumbnails != null && thumbnails.length() > 0) {
                        // Pick the last (highest resolution) thumbnail
                        thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url") ?: ""
                    } else {
                        "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    }

                    if (title.isNotBlank()) {
                        results.add(
                            SearchResultItem(
                                id = videoId,
                                title = title,
                                author = author,
                                duration = duration,
                                thumbnailUrl = thumbUrl
                            )
                        )
                    }
                }
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
